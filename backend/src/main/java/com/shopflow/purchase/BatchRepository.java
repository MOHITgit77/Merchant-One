package com.shopflow.purchase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Page<Batch> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);
    List<Batch> findByVariantIdAndRemainingQuantityGreaterThanOrderByExpiryDateAsc(UUID variantId, int minQty);
    List<Batch> findByStoreIdAndIsExpiredFalseAndExpiryDateBefore(UUID storeId, LocalDate date);

    @Query("SELECT b FROM Batch b WHERE b.storeId = :storeId AND b.isExpired = false AND b.expiryDate IS NOT NULL AND b.expiryDate <= :thresholdDate ORDER BY b.expiryDate ASC")
    List<Batch> findExpiringBatches(@Param("storeId") UUID storeId, @Param("thresholdDate") LocalDate thresholdDate);

    @Query("SELECT b FROM Batch b WHERE b.storeId = :storeId AND b.remainingQuantity > 0 ORDER BY b.expiryDate ASC NULLS LAST")
    Page<Batch> findActiveBatches(@Param("storeId") UUID storeId, Pageable pageable);
}
