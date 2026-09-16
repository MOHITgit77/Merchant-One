package com.shopflow.store;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PaymentPreferenceRepository extends JpaRepository<PaymentPreference, UUID> {
    List<PaymentPreference> findByStoreId(UUID storeId);
    void deleteByStoreId(UUID storeId);
}
