package com.shopflow.order;

import com.shopflow.exception.BusinessRuleException;
import com.shopflow.exception.InvalidStateTransitionException;
import com.shopflow.inventory.InventoryService;
import com.shopflow.order.dto.CreateOrderRequest;
import com.shopflow.order.dto.OrderDto;
import com.shopflow.product.Product;
import com.shopflow.product.ProductVariant;
import com.shopflow.product.ProductVariantRepository;
import com.shopflow.store.PaymentMethod;
import com.shopflow.store.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private StoreService storeService;
    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    private UUID storeId;
    private UUID merchantId;
    private UUID variantId;
    private ProductVariant variant;
    private Product product;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setStoreId(storeId);
        product.setName("Test Product");
        product.setActive(true);
        product.setTaxPercent(BigDecimal.valueOf(10));

        variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProduct(product);
        variant.setName("Default");
        variant.setSku("TEST-01");
        variant.setPrice(new BigDecimal("100.00"));
        variant.setQuantityOnHand(50);
        variant.setReservedQuantity(0);
        variant.setActive(true);
    }

    @Test
    void createOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("John Doe");
        request.setCustomerPhone("1234567890");
        request.setOrderType(OrderType.PICKUP);
        request.setPaymentMethod(PaymentMethod.CASH);

        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setVariantId(variantId);
        itemReq.setQuantity(2);
        request.setItems(List.of(itemReq));

        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(orderRepository.findMaxOrderNumber(storeId)).thenReturn(0);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderDto result = orderService.createOrder(storeId, request);

        assertNotNull(result);
        assertEquals("ORD-000001", result.getOrderNumber());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("220.00"), result.getTotalAmount()); // (100 * 2) + 10% tax = 220
        assertEquals(1, result.getItems().size());
        
        // Stock shouldn't be reserved yet (only on ACCEPT)
        verify(inventoryService, never()).reserveForOrder(any(), any(), anyInt(), any(), any());
    }

    @Test
    void createOrder_InsufficientStock() {
        variant.setQuantityOnHand(5);
        variant.setReservedQuantity(4); // Only 1 available

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("John Doe");
        request.setCustomerPhone("1234567890");
        request.setOrderType(OrderType.PICKUP);
        request.setPaymentMethod(PaymentMethod.CASH);

        CreateOrderRequest.OrderItemRequest itemReq = new CreateOrderRequest.OrderItemRequest();
        itemReq.setVariantId(variantId);
        itemReq.setQuantity(2);
        request.setItems(List.of(itemReq));

        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, 
                () -> orderService.createOrder(storeId, request));
        assertEquals("INSUFFICIENT_STOCK", ex.getCode());
    }

    @Test
    void acceptOrder_ReservesStock() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStoreId(storeId);
        order.setStatus(OrderStatus.PENDING);
        
        OrderItem item = new OrderItem();
        item.setVariantId(variantId);
        item.setQuantity(3);
        order.getItems().add(item);

        when(orderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderService.acceptOrder(storeId, orderId, merchantId);

        assertEquals(OrderStatus.ACCEPTED, result.getStatus());
        assertNotNull(result.getAcceptedAt());
        
        verify(inventoryService).reserveForOrder(storeId, variantId, 3, orderId, merchantId);
    }

    @Test
    void completeOrder_DeductsStock() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStoreId(storeId);
        order.setStatus(OrderStatus.READY);
        
        OrderItem item = new OrderItem();
        item.setVariantId(variantId);
        item.setQuantity(3);
        order.getItems().add(item);

        when(orderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderService.completeOrder(storeId, orderId, merchantId);

        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        
        verify(inventoryService).deductForOrder(storeId, variantId, 3, orderId, merchantId);
    }

    @Test
    void cancelOrder_ReleasesStockIfAccepted() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStoreId(storeId);
        order.setStatus(OrderStatus.ACCEPTED); // Stock is reserved
        
        OrderItem item = new OrderItem();
        item.setVariantId(variantId);
        item.setQuantity(3);
        order.getItems().add(item);

        when(orderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderService.cancelOrder(storeId, orderId, null, merchantId);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
        
        verify(inventoryService).releaseReservation(storeId, variantId, 3, orderId, merchantId);
    }

    @Test
    void cancelOrder_NoStockReleaseIfPending() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStoreId(storeId);
        order.setStatus(OrderStatus.PENDING); // Stock is NOT reserved yet
        
        OrderItem item = new OrderItem();
        item.setVariantId(variantId);
        item.setQuantity(3);
        order.getItems().add(item);

        when(orderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDto result = orderService.cancelOrder(storeId, orderId, null, merchantId);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        
        // Should NOT call release since it was never reserved
        verify(inventoryService, never()).releaseReservation(any(), any(), anyInt(), any(), any());
    }

    @Test
    void invalidTransition_ThrowsException() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStoreId(storeId);
        order.setStatus(OrderStatus.COMPLETED); // Terminal state

        when(orderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.of(order));

        assertThrows(InvalidStateTransitionException.class, 
                () -> orderService.cancelOrder(storeId, orderId, null, merchantId));
    }
}
