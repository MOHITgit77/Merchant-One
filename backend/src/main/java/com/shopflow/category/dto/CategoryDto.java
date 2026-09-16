package com.shopflow.category.dto;

import java.time.Instant;
import java.util.UUID;

public class CategoryDto {
    private UUID id;
    private UUID storeId;
    private String name;
    private String description;
    private boolean isActive;
    private int displayOrder;
    private long productCount;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public long getProductCount() { return productCount; }
    public void setProductCount(long productCount) { this.productCount = productCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static CategoryDto fromEntity(com.shopflow.category.Category category) {
        return fromEntity(category, 0);
    }

    public static CategoryDto fromEntity(com.shopflow.category.Category category, long productCount) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setStoreId(category.getStoreId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setActive(category.isActive());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setProductCount(productCount);
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }
}
