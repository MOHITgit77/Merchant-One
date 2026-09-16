package com.shopflow.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByStoreId(UUID storeId, Pageable pageable);
    Page<Product> findByStoreIdAndCategoryId(UUID storeId, UUID categoryId, Pageable pageable);
    Page<Product> findByStoreIdAndIsActiveTrue(UUID storeId, Pageable pageable);
    Page<Product> findByStoreIdAndNameContainingIgnoreCase(UUID storeId, String name, Pageable pageable);
    Optional<Product> findByIdAndStoreId(UUID id, UUID storeId);
    List<Product> findByStoreIdAndCategoryIdAndIsActiveTrue(UUID storeId, UUID categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.storeId = :storeId")
    long countByStoreId(@Param("storeId") UUID storeId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.storeId = :storeId AND p.categoryId = :categoryId")
    long countByStoreIdAndCategoryId(@Param("storeId") UUID storeId, @Param("categoryId") UUID categoryId);
}
