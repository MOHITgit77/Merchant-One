package com.shopflow.sales;

import com.shopflow.exception.BusinessRuleException;
import com.shopflow.exception.ResourceNotFoundException;
import com.shopflow.inventory.InventoryService;
import com.shopflow.product.Product;
import com.shopflow.product.ProductVariant;
import com.shopflow.product.ProductVariantRepository;
import com.shopflow.sales.dto.CreateSaleRequest;
import com.shopflow.sales.dto.SaleDto;
import com.shopflow.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SalesService {

    private static final Logger log = LoggerFactory.getLogger(SalesService.class);

    private final SaleRepository saleRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryService inventoryService;
    private final StoreService storeService;

    public SalesService(SaleRepository saleRepository, ProductVariantRepository variantRepository,
                        InventoryService inventoryService, StoreService storeService) {
        this.saleRepository = saleRepository;
        this.variantRepository = variantRepository;
        this.inventoryService = inventoryService;
        this.storeService = storeService;
    }

    @Transactional
    public SaleDto createSale(UUID storeId, CreateSaleRequest request, String idempotencyKey, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);

        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = saleRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toDto(existing.get());
            }
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("EMPTY_SALE", "Sale must have at least one item");
        }

        Sale sale = new Sale();
        sale.setStoreId(storeId);
        sale.setIdempotencyKey(idempotencyKey);
        sale.setInvoiceNumber(generateInvoiceNumber(storeId));
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setCustomerName(request.getCustomerName());
        sale.setCustomerPhone(request.getCustomerPhone());
        sale.setNotes(request.getNotes());
        sale.setPerformedBy(merchantId);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateSaleRequest.SaleItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", itemReq.getVariantId()));

            Product product = variant.getProduct();
            if (!product.getStoreId().equals(storeId)) {
                throw new org.springframework.security.access.AccessDeniedException("Product does not belong to this store");
            }

            BigDecimal unitPrice = variant.getPrice();
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            BigDecimal taxPercent = product.getTaxPercent() != null ? product.getTaxPercent() : BigDecimal.ZERO;
            BigDecimal lineTax = lineSubtotal.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineSubtotal.add(lineTax);

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setVariantId(variant.getId());
            item.setProductName(product.getName());
            item.setVariantName(variant.getName());
            item.setSku(variant.getSku());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setTaxPercent(taxPercent);
            item.setTaxAmount(lineTax);
            item.setLineTotal(lineTotal);

            sale.getItems().add(item);
            subtotal = subtotal.add(lineSubtotal);
            totalTax = totalTax.add(lineTax);
        }

        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal grandTotal = subtotal.add(totalTax).subtract(discount);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;

        sale.setSubtotal(subtotal);
        sale.setTaxAmount(totalTax);
        sale.setDiscountAmount(discount);
        sale.setTotalAmount(grandTotal);

        sale = saleRepository.save(sale);

        // Deduct inventory for each item
        for (SaleItem item : sale.getItems()) {
            ProductVariant variant = variantRepository.findById(item.getVariantId()).orElseThrow();
            if (variant.getProduct().isTrackInventory()) {
                inventoryService.deductForSale(storeId, item.getVariantId(), item.getQuantity(), sale.getId(), merchantId);
            }
        }

        log.info("Sale created: {} (₹{}) in store {}", sale.getInvoiceNumber(), sale.getTotalAmount(), storeId);
        return toDto(sale);
    }

    @Transactional(readOnly = true)
    public Page<SaleDto> getSales(UUID storeId, UUID merchantId, Pageable pageable) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        return saleRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public SaleDto getSale(UUID storeId, UUID saleId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Sale sale = saleRepository.findByIdAndStoreId(saleId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", saleId));
        return toDto(sale);
    }

    private String generateInvoiceNumber(UUID storeId) {
        int maxGlobal = saleRepository.findMaxInvoiceNumberGlobal();
        int maxStore = saleRepository.findMaxInvoiceNumber(storeId);
        int max = Math.max(maxGlobal, maxStore);
        return "INV-" + String.format("%06d", max + 1);
    }

    private SaleDto toDto(Sale sale) {
        SaleDto dto = new SaleDto();
        dto.setId(sale.getId());
        dto.setStoreId(sale.getStoreId());
        dto.setInvoiceNumber(sale.getInvoiceNumber());
        dto.setSubtotal(sale.getSubtotal());
        dto.setTaxAmount(sale.getTaxAmount());
        dto.setDiscountAmount(sale.getDiscountAmount());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setPaymentMethod(sale.getPaymentMethod());
        dto.setCustomerName(sale.getCustomerName());
        dto.setCustomerPhone(sale.getCustomerPhone());
        dto.setNotes(sale.getNotes());
        dto.setItemCount(sale.getItems().size());
        dto.setCreatedAt(sale.getCreatedAt());

        List<SaleDto.SaleItemDto> items = sale.getItems().stream().map(i -> {
            SaleDto.SaleItemDto id = new SaleDto.SaleItemDto();
            id.setId(i.getId());
            id.setVariantId(i.getVariantId());
            id.setProductName(i.getProductName());
            id.setVariantName(i.getVariantName());
            id.setSku(i.getSku());
            id.setQuantity(i.getQuantity());
            id.setUnitPrice(i.getUnitPrice());
            id.setTaxPercent(i.getTaxPercent());
            id.setTaxAmount(i.getTaxAmount());
            id.setLineTotal(i.getLineTotal());
            return id;
        }).collect(Collectors.toList());
        dto.setItems(items);

        return dto;
    }
}
