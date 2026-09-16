package com.shopflow.store;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface StoreHoursRepository extends JpaRepository<StoreHours, UUID> {
    List<StoreHours> findByStoreIdOrderByDayOfWeek(UUID storeId);
    void deleteByStoreId(UUID storeId);
}
