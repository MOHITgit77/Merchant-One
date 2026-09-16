package com.shopflow.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class StockAdjustmentRequest {
    @NotNull(message = "Variant ID is required")
    private UUID variantId;

    @Min(value = 0, message = "New quantity cannot be negative")
    private int newQuantity;

    private String reason;

    public UUID getVariantId() { return variantId; }
    public void setVariantId(UUID variantId) { this.variantId = variantId; }
    public int getNewQuantity() { return newQuantity; }
    public void setNewQuantity(int newQuantity) { this.newQuantity = newQuantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
