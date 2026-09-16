package com.shopflow.store;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface StoreHolidayRepository extends JpaRepository<StoreHoliday, UUID> {
    List<StoreHoliday> findByStoreIdOrderByHolidayDate(UUID storeId);
}
