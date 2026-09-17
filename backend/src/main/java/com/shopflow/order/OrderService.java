package com.shopflow.order;

import com.shopflow.exception.BusinessRuleException;
import com.shopflow.exception.InvalidStateTransitionException;
import com.shopflow.exception.ResourceNotFoundException;
import com.shopflow.inventory.InventoryService;
import com.shopflow.order.dto.*;
import com.shopflow.product.Product;
import com.shopflow.product.ProductVariant;
import com.shopflow.product.ProductVariantRepository;
import com.shopflow.store.Store;
import com.shopflow.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final StoreService storeService;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository,
                        ProductVariantRepository variantRepository,
                        StoreService storeService,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.storeService = storeService;
        this.inventoryService = inventoryService;
    }

    // ========== Customer-Facing Flow ==========

    @Transactional
    public OrderDto createOrder(UUID storeId, CreateOrderRequest request) {
        // Idempotency check via idempotencyKey header in controller would be ideal, 
        // assuming standard controller implementation

        Order order = new Order();
        order.setStoreId(storeId);
        order.setOrderNumber(generateOrderNumber(storeId));
        order.setStatus(OrderStatus.PENDING);
        order.setOrderType(request.getOrderType());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", itemReq.getVariantId()));
            Product product = variant.getProduct();

            if (!product.getStoreId().equals(storeId)) {
                throw new BusinessRuleException("INVALID_PRODUCT", "Product does not belong to this store");
            }
            if (!product.isActive() || !variant.isActive()) {
                throw new BusinessRuleException("INACTIVE_PRODUCT", "Product is not active");
            }

            // Check stock availability BEFORE allowing order creation
            int available = variant.getAvailableQuantity();
            if (available < itemReq.getQuantity()) {
                throw new BusinessRuleException("INSUFFICIENT_STOCK", 
                        "Insufficient stock for " + variant.getName() + ". Available: " + available);
            }

            BigDecimal itemPrice = variant.getPrice();
            BigDecimal itemTaxPercent = product.getTaxPercent();
            BigDecimal itemLineTotal = itemPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            BigDecimal itemTaxAmount = itemLineTotal.multiply(itemTaxPercent).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariantId(variant.getId());
            orderItem.setProductName(product.getName());
            orderItem.setVariantName(variant.getName());
            orderItem.setSku(variant.getSku());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(itemPrice);
            orderItem.setTaxPercent(itemTaxPercent);
            orderItem.setTaxAmount(itemTaxAmount);
            orderItem.setLineTotal(itemLineTotal);

            order.getItems().add(orderItem);

            subtotal = subtotal.add(itemLineTotal);
            taxAmount = taxAmount.add(itemTaxAmount);
        }

        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        // Assuming no discount at order creation for now unless we add coupons
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(subtotal.add(taxAmount));

        order = orderRepository.save(order);
        log.info("Order placed: {} for store {}", order.getOrderNumber(), storeId);

        return toDto(order);
    }

    // ========== Merchant-Facing Flow (Lifecycle) ==========

    @Transactional
    public OrderDto acceptOrder(UUID storeId, UUID orderId, UUID merchantId) {
        Order order = getOrderForMerchant(storeId, orderId, merchantId);

        transitionStatus(order, OrderStatus.ACCEPTED);
        order.setAcceptedAt(Instant.now());

        // Reserve stock
        for (OrderItem item : order.getItems()) {
            inventoryService.reserveForOrder(storeId, item.getVariantId(), item.getQuantity(), order.getId(), merchantId);
        }

        order = orderRepository.save(order);
        log.info("Order accepted: {}", order.getOrderNumber());
        return toDto(order);
    }

    @Transactional
    public OrderDto rejectOrder(UUID storeId, UUID orderId, RejectOrderRequest request, UUID merchantId) {
        Order order = getOrderForMerchant(storeId, orderId, merchantId);

        transitionStatus(order, OrderStatus.REJECTED);
        order.setRejectedAt(Instant.now());
        if (request != null && request.getReason() != null) {
            order.setMerchantNotes(request.getReason());
        }

        order = orderRepository.save(order);
        log.info("Order rejected: {}", order.getOrderNumber());
        return toDto(order);
    }

    @Transactional
    public OrderDto updateOrderStatus(UUID storeId, UUID orderId, UpdateOrderStatusRequest request, UUID merchantId) {
        Order order = getOrderForMerchant(storeId, orderId, merchantId);

        // Disallow terminal/major transitions via this simple update method
        OrderStatus target = request.getStatus();
        if (target == OrderStatus.ACCEPTED || target.isTerminal()) {
            throw new BusinessRuleException("INVALID_METHOD", "Use specific endpoints for ACCEPT, REJECT, COMPLETE, CANCEL");
        }

        transitionStatus(order, target);
        order = orderRepository.save(order);
        log.info("Order {} status updated to {}", order.getOrderNumber(), target);
        return toDto(order);
    }

    @Transactional
    public OrderDto completeOrder(UUID storeId, UUID orderId, UUID merchantId) {
        Order order = getOrderForMerchant(storeId, orderId, merchantId);

        transitionStatus(order, OrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());

        // Deduct stock (removes from reserved and onHand)
        for (OrderItem item : order.getItems()) {
            inventoryService.deductForOrder(storeId, item.getVariantId(), item.getQuantity(), order.getId(), merchantId);
        }

        order = orderRepository.save(order);
        log.info("Order completed: {}", order.getOrderNumber());
        return toDto(order);
    }

    @Transactional
    public OrderDto cancelOrder(UUID storeId, UUID orderId, CancelOrderRequest request, UUID merchantId) {
        Order order = getOrderForMerchant(storeId, orderId, merchantId);
        OrderStatus oldStatus = order.getStatus();

        transitionStatus(order, OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        if (request != null && request.getReason() != null) {
            order.setCancellationReason(request.getReason());
        }

        // If order was in a state where stock was reserved (ACCEPTED, PREPARING, READY), release it
        if (oldStatus == OrderStatus.ACCEPTED || oldStatus == OrderStatus.PREPARING || oldStatus == OrderStatus.READY) {
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseReservation(storeId, item.getVariantId(), item.getQuantity(), order.getId(), merchantId);
            }
        }

        order = orderRepository.save(order);
        log.info("Order cancelled: {}", order.getOrderNumber());
        return toDto(order);
    }

    // ========== Queries ==========

    @Transactional(readOnly = true)
    public Page<OrderDto> getOrders(UUID storeId, OrderStatus status, UUID merchantId, Pageable pageable) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status, pageable);
        } else {
            orders = orderRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable);
        }
        return orders.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID storeId, UUID orderId, UUID merchantId) {
        return toDto(getOrderForMerchant(storeId, orderId, merchantId));
    }

    // ========== Helpers ==========

    private Order getOrderForMerchant(UUID storeId, UUID orderId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        return orderRepository.findByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    }

    private void transitionStatus(Order order, OrderStatus targetStatus) {
        if (!order.getStatus().canTransitionTo(targetStatus)) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot transition order %s from %s to %s", 
                            order.getOrderNumber(), order.getStatus(), targetStatus));
        }
        order.setStatus(targetStatus);
    }

    private String generateOrderNumber(UUID storeId) {
        int max = orderRepository.findMaxOrderNumber(storeId);
        return String.format("ORD-%06d", max + 1);
    }

    private OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setStoreId(order.getStoreId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setOrderType(order.getOrderType());
        dto.setCustomerName(order.getCustomerName());
        dto.setCustomerPhone(order.getCustomerPhone());
        dto.setCustomerEmail(order.getCustomerEmail());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setSubtotal(order.getSubtotal());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setNotes(order.getNotes());
        dto.setMerchantNotes(order.getMerchantNotes());
        dto.setCancellationReason(order.getCancellationReason());
        dto.setAcceptedAt(order.getAcceptedAt());
        dto.setRejectedAt(order.getRejectedAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<OrderDto.OrderItemDto> itemDtos = order.getItems().stream().map(item -> {
            OrderDto.OrderItemDto idto = new OrderDto.OrderItemDto();
            idto.setId(item.getId());
            idto.setVariantId(item.getVariantId());
            idto.setProductName(item.getProductName());
            idto.setVariantName(item.getVariantName());
            idto.setSku(item.getSku());
            idto.setQuantity(item.getQuantity());
            idto.setUnitPrice(item.getUnitPrice());
            idto.setTaxPercent(item.getTaxPercent());
            idto.setTaxAmount(item.getTaxAmount());
            idto.setLineTotal(item.getLineTotal());
            return idto;
        }).collect(Collectors.toList());
        dto.setItems(itemDtos);

        return dto;
    }
}
