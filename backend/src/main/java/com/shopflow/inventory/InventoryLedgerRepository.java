package com.shopflow.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, UUID> {
    Page<InventoryLedger> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);
    Page<InventoryLedger> findByVariantIdOrderByCreatedAtDesc(UUID variantId, Pageable pageable);
    Page<InventoryLedger> findByStoreIdAndMovementTypeOrderByCreatedAtDesc(UUID storeId, MovementType movementType, Pageable pageable);
}
