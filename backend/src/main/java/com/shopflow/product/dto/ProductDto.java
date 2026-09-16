package com.shopflow.product.dto;

import com.shopflow.product.ProductUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ProductDto {
    private UUID id;
    private UUID storeId;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private BigDecimal costPrice;
    private BigDecimal taxPercent;
    private ProductUnit unit;
    private boolean isActive;
    private boolean hasVariants;
    private boolean trackInventory;
    private Integer lowStockThreshold;
    private int totalStock;
    private List<VariantDto> variants;
    private List<ImageDto> images;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public void setCompareAtPrice(BigDecimal compareAtPrice) { this.compareAtPrice = compareAtPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal taxPercent) { this.taxPercent = taxPercent; }
    public ProductUnit getUnit() { return unit; }
    public void setUnit(ProductUnit unit) { this.unit = unit; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isHasVariants() { return hasVariants; }
    public void setHasVariants(boolean hasVariants) { this.hasVariants = hasVariants; }
    public boolean isTrackInventory() { return trackInventory; }
    public void setTrackInventory(boolean trackInventory) { this.trackInventory = trackInventory; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
    public List<VariantDto> getVariants() { return variants; }
    public void setVariants(List<VariantDto> variants) { this.variants = variants; }
    public List<ImageDto> getImages() { return images; }
    public void setImages(List<ImageDto> images) { this.images = images; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class VariantDto {
        private UUID id;
        private String name;
        private String sku;
        private BigDecimal price;
        private BigDecimal costPrice;
        private int quantityOnHand;
        private int reservedQuantity;
        private int availableQuantity;
        private boolean isActive;
        private String barcode;
        private Double weight;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public int getQuantityOnHand() { return quantityOnHand; }
        public void setQuantityOnHand(int quantityOnHand) { this.quantityOnHand = quantityOnHand; }
        public int getReservedQuantity() { return reservedQuantity; }
        public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
        public int getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
    }

    public static class ImageDto {
        private UUID id;
        private String imageUrl;
        private int displayOrder;
        private String altText;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public int getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
        public String getAltText() { return altText; }
        public void setAltText(String altText) { this.altText = altText; }
    }
}
