package com.shopflow.category.dto;

import java.util.List;
import java.util.UUID;

public class ReorderRequest {
    private List<UUID> categoryIds;

    public List<UUID> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<UUID> categoryIds) { this.categoryIds = categoryIds; }
}
