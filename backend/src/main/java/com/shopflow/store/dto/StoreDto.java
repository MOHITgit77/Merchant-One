package com.shopflow.store.dto;

import com.shopflow.store.ShopCategory;
import java.time.Instant;
import java.util.UUID;

public class StoreDto {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private ShopCategory shopCategory;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
    private String logoUrl;
    private String coverImageUrl;
    private boolean isPublished;
    private boolean pickupEnabled;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ShopCategory getShopCategory() { return shopCategory; }
    public void setShopCategory(ShopCategory shopCategory) { this.shopCategory = shopCategory; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }
    public boolean isPickupEnabled() { return pickupEnabled; }
    public void setPickupEnabled(boolean pickupEnabled) { this.pickupEnabled = pickupEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static StoreDto fromEntity(com.shopflow.store.Store store) {
        StoreDto dto = new StoreDto();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setSlug(store.getSlug());
        dto.setDescription(store.getDescription());
        dto.setShopCategory(store.getShopCategory());
        dto.setAddress(store.getAddress());
        dto.setLatitude(store.getLatitude());
        dto.setLongitude(store.getLongitude());
        dto.setPhone(store.getPhone());
        dto.setEmail(store.getEmail());
        dto.setLogoUrl(store.getLogoUrl());
        dto.setCoverImageUrl(store.getCoverImageUrl());
        dto.setPublished(store.isPublished());
        dto.setPickupEnabled(store.isPickupEnabled());
        dto.setCreatedAt(store.getCreatedAt());
        dto.setUpdatedAt(store.getUpdatedAt());
        return dto;
    }
}
