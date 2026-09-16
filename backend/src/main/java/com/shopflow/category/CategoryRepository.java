package com.shopflow.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByStoreIdOrderByDisplayOrder(UUID storeId);

    List<Category> findByStoreIdAndIsActiveTrueOrderByDisplayOrder(UUID storeId);

    Optional<Category> findByIdAndStoreId(UUID id, UUID storeId);

    boolean existsByStoreIdAndName(UUID storeId, String name);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.storeId = :storeId")
    int countByStoreId(@Param("storeId") UUID storeId);

    @Query("SELECT MAX(c.displayOrder) FROM Category c WHERE c.storeId = :storeId")
    Integer findMaxDisplayOrder(@Param("storeId") UUID storeId);
}
