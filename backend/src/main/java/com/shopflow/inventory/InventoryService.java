package com.shopflow.inventory;

import com.shopflow.exception.BusinessRuleException;
import com.shopflow.exception.ResourceNotFoundException;
import com.shopflow.inventory.dto.LedgerEntryDto;
import com.shopflow.inventory.dto.StockAdjustmentRequest;
import com.shopflow.inventory.dto.StockMovementRequest;
import com.shopflow.product.ProductVariant;
import com.shopflow.product.ProductVariantRepository;
import com.shopflow.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductVariantRepository variantRepository;
    private final InventoryLedgerRepository ledgerRepository;
    private final StoreService storeService;

    public InventoryService(ProductVariantRepository variantRepository,
                            InventoryLedgerRepository ledgerRepository,
                            StoreService storeService) {
        this.variantRepository = variantRepository;
        this.ledgerRepository = ledgerRepository;
        this.storeService = storeService;
    }

    @Transactional
    public LedgerEntryDto stockIn(UUID storeId, StockMovementRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        ProductVariant variant = getVariant(request.getVariantId());
        verifyVariantBelongsToStore(variant, storeId);

        int before = variant.getQuantityOnHand();
        variant.setQuantityOnHand(before + request.getQuantity());
        variantRepository.save(variant);

        InventoryLedger entry = createLedgerEntry(storeId, variant.getId(), MovementType.STOCK_IN,
                request.getQuantity(), before, variant.getQuantityOnHand(),
                request.getUnitCost(), request.getNotes(),
                request.getReferenceType(), request.getReferenceId(), merchantId);

        log.info("Stock IN: {} units of SKU {} (store {})", request.getQuantity(), variant.getSku(), storeId);
        return toLedgerDto(entry, variant);
    }

    @Transactional
    public LedgerEntryDto stockOut(UUID storeId, StockMovementRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        ProductVariant variant = getVariant(request.getVariantId());
        verifyVariantBelongsToStore(variant, storeId);

        int before = variant.getQuantityOnHand();
        if (before < request.getQuantity()) {
            throw new BusinessRuleException("INSUFFICIENT_STOCK",
                    "Insufficient stock. Available: " + before + ", requested: " + request.getQuantity());
        }

        variant.setQuantityOnHand(before - request.getQuantity());
        variantRepository.save(variant);

        InventoryLedger entry = createLedgerEntry(storeId, variant.getId(), MovementType.STOCK_OUT,
                -request.getQuantity(), before, variant.getQuantityOnHand(),
                request.getUnitCost(), request.getNotes(),
                request.getReferenceType(), request.getReferenceId(), merchantId);

        log.info("Stock OUT: {} units of SKU {} (store {})", request.getQuantity(), variant.getSku(), storeId);
        return toLedgerDto(entry, variant);
    }

    @Transactional
    public LedgerEntryDto adjustStock(UUID storeId, StockAdjustmentRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        ProductVariant variant = getVariant(request.getVariantId());
        verifyVariantBelongsToStore(variant, storeId);

        int before = variant.getQuantityOnHand();
        int delta = request.getNewQuantity() - before;
        variant.setQuantityOnHand(request.getNewQuantity());
        variantRepository.save(variant);

        InventoryLedger entry = createLedgerEntry(storeId, variant.getId(), MovementType.ADJUSTMENT,
                delta, before, request.getNewQuantity(), null,
                request.getReason(), null, null, merchantId);

        log.info("Stock ADJUST: SKU {} from {} to {} (store {})", variant.getSku(), before, request.getNewQuantity(), storeId);
        return toLedgerDto(entry, variant);
    }

    /**
     * Internal method for sale deductions — called by SalesService
     */
    @Transactional
    public void deductForSale(UUID storeId, UUID variantId, int quantity, UUID saleId, UUID performedBy) {
        ProductVariant variant = getVariant(variantId);

        int before = variant.getQuantityOnHand();
        if (before < quantity) {
            throw new BusinessRuleException("INSUFFICIENT_STOCK",
                    "Insufficient stock for SKU " + variant.getSku() + ". Available: " + before + ", needed: " + quantity);
        }

        variant.setQuantityOnHand(before - quantity);
        variantRepository.save(variant);

        createLedgerEntry(storeId, variantId, MovementType.SALE,
                -quantity, before, variant.getQuantityOnHand(), null,
                "Sale deduction", "SALE", saleId, performedBy);
    }

    /**
     * Internal method for purchase stock-in — uses PURCHASE movement type
     */
    @Transactional
    public void stockInForPurchase(UUID storeId, UUID variantId, int quantity,
                                    java.math.BigDecimal unitCost, UUID purchaseId, UUID performedBy) {
        ProductVariant variant = getVariant(variantId);

        int before = variant.getQuantityOnHand();
        variant.setQuantityOnHand(before + quantity);
        variantRepository.save(variant);

        createLedgerEntry(storeId, variantId, MovementType.PURCHASE,
                quantity, before, variant.getQuantityOnHand(), unitCost,
                "Purchase stock-in", "PURCHASE", purchaseId, performedBy);

        log.info("Purchase STOCK IN: {} units of variant {} (store {})", quantity, variantId, storeId);
    }

    /**
     * Reserve stock for an accepted order — increments reserved_quantity
     */
    @Transactional
    public void reserveForOrder(UUID storeId, UUID variantId, int quantity, UUID orderId, UUID performedBy) {
        ProductVariant variant = getVariant(variantId);

        int available = variant.getAvailableQuantity();
        if (available < quantity) {
            throw new BusinessRuleException("INSUFFICIENT_STOCK",
                    "Insufficient available stock for SKU " + variant.getSku()
                    + ". Available: " + available + ", needed: " + quantity);
        }

        variant.setReservedQuantity(variant.getReservedQuantity() + quantity);
        variantRepository.save(variant);

        log.info("Stock RESERVED: {} units of variant {} for order {} (store {})", quantity, variantId, orderId, storeId);
    }

    /**
     * Release reserved stock when an order is cancelled
     */
    @Transactional
    public void releaseReservation(UUID storeId, UUID variantId, int quantity, UUID orderId, UUID performedBy) {
        ProductVariant variant = getVariant(variantId);

        int newReserved = Math.max(0, variant.getReservedQuantity() - quantity);
        variant.setReservedQuantity(newReserved);
        variantRepository.save(variant);

        log.info("Stock RELEASED: {} units of variant {} for order {} (store {})", quantity, variantId, orderId, storeId);
    }

    /**
     * Deduct stock for a completed order — decrements quantity_on_hand and reserved_quantity, creates ledger entry
     */
    @Transactional
    public void deductForOrder(UUID storeId, UUID variantId, int quantity, UUID orderId, UUID performedBy) {
        ProductVariant variant = getVariant(variantId);

        int before = variant.getQuantityOnHand();
        variant.setQuantityOnHand(before - quantity);
        variant.setReservedQuantity(Math.max(0, variant.getReservedQuantity() - quantity));
        variantRepository.save(variant);

        createLedgerEntry(storeId, variantId, MovementType.SALE,
                -quantity, before, variant.getQuantityOnHand(), null,
                "Order fulfillment", "ORDER", orderId, performedBy);

        log.info("Order DEDUCT: {} units of variant {} for order {} (store {})", quantity, variantId, orderId, storeId);
    }


    @Transactional(readOnly = true)
    public Page<LedgerEntryDto> getHistory(UUID storeId, MovementType type, UUID merchantId, Pageable pageable) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Page<InventoryLedger> entries;
        if (type != null) {
            entries = ledgerRepository.findByStoreIdAndMovementTypeOrderByCreatedAtDesc(storeId, type, pageable);
        } else {
            entries = ledgerRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable);
        }
        return entries.map(e -> {
            ProductVariant v = variantRepository.findById(e.getVariantId()).orElse(null);
            return toLedgerDto(e, v);
        });
    }

    // ========== Helpers ==========

    private ProductVariant getVariant(UUID variantId) {
        return variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", variantId));
    }

    private void verifyVariantBelongsToStore(ProductVariant variant, UUID storeId) {
        if (!variant.getProduct().getStoreId().equals(storeId)) {
            throw new org.springframework.security.access.AccessDeniedException("Variant does not belong to this store");
        }
    }

    private InventoryLedger createLedgerEntry(UUID storeId, UUID variantId, MovementType type,
                                               int quantity, int before, int after,
                                               java.math.BigDecimal unitCost, String notes,
                                               String refType, UUID refId, UUID performedBy) {
        InventoryLedger entry = new InventoryLedger();
        entry.setStoreId(storeId);
        entry.setVariantId(variantId);
        entry.setMovementType(type);
        entry.setQuantity(quantity);
        entry.setQuantityBefore(before);
        entry.setQuantityAfter(after);
        entry.setUnitCost(unitCost);
        entry.setNotes(notes);
        entry.setReferenceType(refType);
        entry.setReferenceId(refId);
        entry.setPerformedBy(performedBy);
        return ledgerRepository.save(entry);
    }

    private LedgerEntryDto toLedgerDto(InventoryLedger entry, ProductVariant variant) {
        LedgerEntryDto dto = new LedgerEntryDto();
        dto.setId(entry.getId());
        dto.setVariantId(entry.getVariantId());
        if (variant != null) {
            dto.setVariantName(variant.getName());
            dto.setVariantSku(variant.getSku());
            dto.setProductName(variant.getProduct().getName());
        }
        dto.setMovementType(entry.getMovementType());
        dto.setQuantity(entry.getQuantity());
        dto.setQuantityBefore(entry.getQuantityBefore());
        dto.setQuantityAfter(entry.getQuantityAfter());
        dto.setUnitCost(entry.getUnitCost());
        dto.setReferenceType(entry.getReferenceType());
        dto.setReferenceId(entry.getReferenceId());
        dto.setNotes(entry.getNotes());
        dto.setCreatedAt(entry.getCreatedAt());
        return dto;
    }
}
