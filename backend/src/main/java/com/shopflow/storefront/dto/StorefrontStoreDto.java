package com.shopflow.storefront.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class StorefrontStoreDto {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String coverImageUrl;
    
    // Contact Info (matches StoreDto)
    private String phone;
    private String email;
    private String address;

    // Fulfillment Info
    private boolean pickupEnabled;
    private boolean deliveryEnabled;
    private Double deliveryRadius;
    private BigDecimal deliveryFee;
    private BigDecimal minimumOrder;
    private BigDecimal freeDeliveryThreshold;

    // Payment Info
    private List<String> paymentMethods;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public boolean isPickupEnabled() { return pickupEnabled; }
    public void setPickupEnabled(boolean pickupEnabled) { this.pickupEnabled = pickupEnabled; }
    public boolean isDeliveryEnabled() { return deliveryEnabled; }
    public void setDeliveryEnabled(boolean deliveryEnabled) { this.deliveryEnabled = deliveryEnabled; }
    public Double getDeliveryRadius() { return deliveryRadius; }
    public void setDeliveryRadius(Double deliveryRadius) { this.deliveryRadius = deliveryRadius; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    public BigDecimal getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(BigDecimal minimumOrder) { this.minimumOrder = minimumOrder; }
    public BigDecimal getFreeDeliveryThreshold() { return freeDeliveryThreshold; }
    public void setFreeDeliveryThreshold(BigDecimal freeDeliveryThreshold) { this.freeDeliveryThreshold = freeDeliveryThreshold; }
    
    public List<String> getPaymentMethods() { return paymentMethods; }
    public void setPaymentMethods(List<String> paymentMethods) { this.paymentMethods = paymentMethods; }
}
