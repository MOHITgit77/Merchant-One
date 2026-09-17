package com.shopflow.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
    Page<Sale> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);
    Optional<Sale> findByIdAndStoreId(UUID id, UUID storeId);
    Optional<Sale> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.storeId = :storeId AND s.createdAt >= :since")
    long countSalesSince(@Param("storeId") UUID storeId, @Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.storeId = :storeId AND s.createdAt >= :since")
    BigDecimal totalRevenueSince(@Param("storeId") UUID storeId, @Param("since") Instant since);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(s.invoiceNumber, 5) AS int)), 0) FROM Sale s WHERE s.storeId = :storeId")
    int findMaxInvoiceNumber(@Param("storeId") UUID storeId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(s.invoiceNumber, 5) AS int)), 0) FROM Sale s")
    int findMaxInvoiceNumberGlobal();
}
