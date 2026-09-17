package com.shopflow.purchase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    Page<Purchase> findByStoreIdOrderByPurchaseDateDesc(UUID storeId, Pageable pageable);
    Optional<Purchase> findByIdAndStoreId(UUID id, UUID storeId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.purchaseNumber, 4) AS int)), 0) FROM Purchase p WHERE p.storeId = :storeId")
    int findMaxPurchaseNumber(@Param("storeId") UUID storeId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.purchaseNumber, 4) AS int)), 0) FROM Purchase p")
    int findMaxPurchaseNumberGlobal();
}
