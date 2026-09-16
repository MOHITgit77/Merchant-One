package com.shopflow.store;

import com.shopflow.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "store_holidays")
public class StoreHoliday extends BaseEntity {

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    private String reason;

    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
