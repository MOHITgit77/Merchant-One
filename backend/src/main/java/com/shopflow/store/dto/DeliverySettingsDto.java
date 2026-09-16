package com.shopflow.store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public class DeliverySettingsDto {
    private boolean enabled;

    @Min(value = 0, message = "Delivery radius cannot be negative")
    private Double deliveryRadius;

    @DecimalMin(value = "0.00", message = "Delivery fee cannot be negative")
    private BigDecimal deliveryFee;

    @DecimalMin(value = "0.00", message = "Minimum order cannot be negative")
    private BigDecimal minimumOrder;

    @DecimalMin(value = "0.00", message = "Free delivery threshold cannot be negative")
    private BigDecimal freeDeliveryThreshold;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Double getDeliveryRadius() { return deliveryRadius; }
    public void setDeliveryRadius(Double deliveryRadius) { this.deliveryRadius = deliveryRadius; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public BigDecimal getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(BigDecimal minimumOrder) { this.minimumOrder = minimumOrder; }
    public BigDecimal getFreeDeliveryThreshold() { return freeDeliveryThreshold; }
    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) { this.freeDeliveryThreshold = freeDeliveryThreshold; }

    public static DeliverySettingsDto fromEntity(com.shopflow.store.DeliverySettings ds) {
        DeliverySettingsDto dto = new DeliverySettingsDto();
        dto.setEnabled(ds.isEnabled());
        dto.setDeliveryRadius(ds.getDeliveryRadius());
        dto.setDeliveryFee(ds.getDeliveryFee());
        dto.setMinimumOrder(ds.getMinimumOrder());
        dto.setFreeDeliveryThreshold(ds.getFreeDeliveryThreshold());
        return dto;
    }
}
