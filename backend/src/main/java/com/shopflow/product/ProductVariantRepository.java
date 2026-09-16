package com.shopflow.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductIdOrderByDisplayOrder(UUID productId);
    Optional<ProductVariant> findBySku(String sku);
    boolean existsBySku(String sku);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.storeId = :storeId AND pv.quantityOnHand <= :threshold AND pv.isActive = true")
    List<ProductVariant> findLowStockVariants(@Param("storeId") UUID storeId, @Param("threshold") int threshold);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.storeId = :storeId AND pv.isActive = true ORDER BY pv.product.name")
    List<ProductVariant> findAllActiveByStoreId(@Param("storeId") UUID storeId);
}
