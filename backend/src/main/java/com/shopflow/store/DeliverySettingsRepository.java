package com.shopflow.store;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DeliverySettingsRepository extends JpaRepository<DeliverySettings, UUID> {
    Optional<DeliverySettings> findByStoreId(UUID storeId);
}
