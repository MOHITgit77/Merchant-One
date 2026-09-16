package com.shopflow.store;

import com.shopflow.common.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "stores", indexes = {
    @Index(name = "idx_stores_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_stores_slug", columnList = "slug")
})
public class Store extends BaseEntity {

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "shop_category")
    private ShopCategory shopCategory;

    private String address;

    private Double latitude;

    private Double longitude;

    private String phone;

    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    @Column(name = "pickup_enabled", nullable = false)
    private boolean pickupEnabled = false;

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
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
}
