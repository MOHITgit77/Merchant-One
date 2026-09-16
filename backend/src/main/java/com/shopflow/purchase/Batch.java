package com.shopflow.purchase;

import com.shopflow.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tracks individual batches for inventory items with expiry dates.
 * Each batch is linked to a variant and optionally to a purchase.
 */
@Entity
@Table(name = "batches", indexes = {
    @Index(name = "idx_batches_variant_id", columnList = "variant_id"),
    @Index(name = "idx_batches_store_id", columnList = "store_id"),
    @Index(name = "idx_batches_expiry", columnList = "expiry_date")
})
public class Batch extends BaseEntity {

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "purchase_id")
    private UUID purchaseId;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    private java.math.BigDecimal unitCost;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_expired", nullable = false)
    private boolean isExpired = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public UUID getVariantId() { return variantId; }
    public void setVariantId(UUID variantId) { this.variantId = variantId; }
    public UUID getPurchaseId() { return purchaseId; }
    public void setPurchaseId(UUID purchaseId) { this.purchaseId = purchaseId; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(int remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public java.math.BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(java.math.BigDecimal unitCost) { this.unitCost = unitCost; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public boolean isExpired() { return isExpired; }
    public void setExpired(boolean expired) { isExpired = expired; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
