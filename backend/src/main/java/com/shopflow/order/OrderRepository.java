package com.shopflow.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);
    Page<Order> findByStoreIdAndStatusOrderByCreatedAtDesc(UUID storeId, OrderStatus status, Pageable pageable);
    Optional<Order> findByIdAndStoreId(UUID id, UUID storeId);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(o.orderNumber, 5) AS int)), 0) FROM Order o WHERE o.storeId = :storeId")
    int findMaxOrderNumber(@Param("storeId") UUID storeId);

    long countByStoreIdAndStatus(UUID storeId, OrderStatus status);
}
