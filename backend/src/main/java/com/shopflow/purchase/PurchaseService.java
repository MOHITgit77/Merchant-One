package com.shopflow.purchase;

import com.shopflow.exception.ResourceNotFoundException;
import com.shopflow.inventory.InventoryService;
import com.shopflow.product.Product;
import com.shopflow.product.ProductVariant;
import com.shopflow.product.ProductVariantRepository;
import com.shopflow.purchase.dto.BatchDto;
import com.shopflow.purchase.dto.CreatePurchaseRequest;
import com.shopflow.purchase.dto.PurchaseDto;
import com.shopflow.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PurchaseService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseService.class);
    private static final int EXPIRY_WARNING_DAYS = 30;

    private final PurchaseRepository purchaseRepository;
    private final BatchRepository batchRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryService inventoryService;
    private final StoreService storeService;

    public PurchaseService(PurchaseRepository purchaseRepository, BatchRepository batchRepository,
                           ProductVariantRepository variantRepository, InventoryService inventoryService,
                           StoreService storeService) {
        this.purchaseRepository = purchaseRepository;
        this.batchRepository = batchRepository;
        this.variantRepository = variantRepository;
        this.inventoryService = inventoryService;
        this.storeService = storeService;
    }

    @Transactional
    public PurchaseDto createPurchase(UUID storeId, CreatePurchaseRequest request, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);

        Purchase purchase = new Purchase();
        purchase.setStoreId(storeId);
        purchase.setPurchaseNumber(generatePurchaseNumber(storeId));
        purchase.setSupplierName(request.getSupplierName());
        purchase.setSupplierContact(request.getSupplierContact());
        purchase.setPurchaseDate(request.getPurchaseDate() != null ? request.getPurchaseDate() : LocalDate.now());
        purchase.setNotes(request.getNotes());
        purchase.setPerformedBy(merchantId);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreatePurchaseRequest.PurchaseItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", itemReq.getVariantId()));
            Product product = variant.getProduct();

            if (!product.getStoreId().equals(storeId)) {
                throw new org.springframework.security.access.AccessDeniedException("Product does not belong to this store");
            }

            BigDecimal lineTotal = itemReq.getUnitCost().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setVariantId(variant.getId());
            item.setProductName(product.getName());
            item.setVariantName(variant.getName());
            item.setSku(variant.getSku());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitCost(itemReq.getUnitCost());
            item.setLineTotal(lineTotal);
            item.setBatchNumber(itemReq.getBatchNumber());
            item.setExpiryDate(itemReq.getExpiryDate());

            purchase.getItems().add(item);
            totalAmount = totalAmount.add(lineTotal);
        }

        purchase.setTotalAmount(totalAmount);

        // Save purchase and all items in one go via cascade (avoids double-save batch conflict)
        purchase = purchaseRepository.save(purchase);

        // Now that purchase is persisted, perform stock-in and batch operations
        for (int idx = 0; idx < request.getItems().size(); idx++) {
            CreatePurchaseRequest.PurchaseItemRequest itemReq = request.getItems().get(idx);
            PurchaseItem savedItem = purchase.getItems().get(idx);

            // Stock in via inventory service with PURCHASE movement type
            inventoryService.stockInForPurchase(storeId, savedItem.getVariantId(), itemReq.getQuantity(),
                    itemReq.getUnitCost(), purchase.getId(), merchantId);

            // Create batch record if batch number provided
            if (itemReq.getBatchNumber() != null && !itemReq.getBatchNumber().isBlank()) {
                Batch batch = new Batch();
                batch.setStoreId(storeId);
                batch.setVariantId(savedItem.getVariantId());
                batch.setPurchaseId(purchase.getId());
                batch.setBatchNumber(itemReq.getBatchNumber());
                batch.setQuantity(itemReq.getQuantity());
                batch.setRemainingQuantity(itemReq.getQuantity());
                batch.setUnitCost(itemReq.getUnitCost());
                batch.setManufacturingDate(itemReq.getManufacturingDate());
                batch.setExpiryDate(itemReq.getExpiryDate());
                batchRepository.save(batch);
            }
        }

        log.info("Purchase created: {} (₹{}) in store {}", purchase.getPurchaseNumber(), totalAmount, storeId);
        return toDto(purchase);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseDto> getPurchases(UUID storeId, UUID merchantId, Pageable pageable) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        return purchaseRepository.findByStoreIdOrderByPurchaseDateDesc(storeId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public PurchaseDto getPurchase(UUID storeId, UUID purchaseId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        Purchase purchase = purchaseRepository.findByIdAndStoreId(purchaseId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", "id", purchaseId));
        return toDto(purchase);
    }

    @Transactional(readOnly = true)
    public Page<BatchDto> getBatches(UUID storeId, UUID merchantId, Pageable pageable) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        return batchRepository.findActiveBatches(storeId, pageable).map(this::toBatchDto);
    }

    @Transactional(readOnly = true)
    public List<BatchDto> getExpiringBatches(UUID storeId, UUID merchantId) {
        storeService.verifyStoreOwnership(storeId, merchantId);
        LocalDate threshold = LocalDate.now().plusDays(EXPIRY_WARNING_DAYS);
        return batchRepository.findExpiringBatches(storeId, threshold).stream()
                .map(this::toBatchDto).collect(Collectors.toList());
    }

    private String generatePurchaseNumber(UUID storeId) {
        int maxGlobal = purchaseRepository.findMaxPurchaseNumberGlobal();
        int maxStore = purchaseRepository.findMaxPurchaseNumber(storeId);
        int max = Math.max(maxGlobal, maxStore);
        return "PO-" + String.format("%06d", max + 1);
    }

    private PurchaseDto toDto(Purchase p) {
        PurchaseDto dto = new PurchaseDto();
        dto.setId(p.getId());
        dto.setPurchaseNumber(p.getPurchaseNumber());
        dto.setSupplierName(p.getSupplierName());
        dto.setSupplierContact(p.getSupplierContact());
        dto.setPurchaseDate(p.getPurchaseDate());
        dto.setTotalAmount(p.getTotalAmount());
        dto.setNotes(p.getNotes());
        dto.setItemCount(p.getItems().size());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setItems(p.getItems().stream().map(i -> {
            PurchaseDto.PurchaseItemDto id = new PurchaseDto.PurchaseItemDto();
            id.setId(i.getId());
            id.setVariantId(i.getVariantId());
            id.setProductName(i.getProductName());
            id.setVariantName(i.getVariantName());
            id.setSku(i.getSku());
            id.setQuantity(i.getQuantity());
            id.setUnitCost(i.getUnitCost());
            id.setLineTotal(i.getLineTotal());
            id.setBatchNumber(i.getBatchNumber());
            id.setExpiryDate(i.getExpiryDate());
            return id;
        }).collect(Collectors.toList()));
        return dto;
    }

    private BatchDto toBatchDto(Batch b) {
        BatchDto dto = new BatchDto();
        dto.setId(b.getId());
        dto.setVariantId(b.getVariantId());
        dto.setBatchNumber(b.getBatchNumber());
        dto.setQuantity(b.getQuantity());
        dto.setRemainingQuantity(b.getRemainingQuantity());
        dto.setUnitCost(b.getUnitCost());
        dto.setManufacturingDate(b.getManufacturingDate());
        dto.setExpiryDate(b.getExpiryDate());
        dto.setExpired(b.isExpired());
        dto.setNotes(b.getNotes());
        dto.setCreatedAt(b.getCreatedAt());

        if (b.getExpiryDate() != null) {
            dto.setExpiringSoon(!b.isExpired() && b.getExpiryDate().isBefore(LocalDate.now().plusDays(EXPIRY_WARNING_DAYS)));
        }

        variantRepository.findById(b.getVariantId()).ifPresent(v -> {
            dto.setVariantName(v.getName());
            dto.setSku(v.getSku());
            dto.setProductName(v.getProduct().getName());
        });

        return dto;
    }
}
