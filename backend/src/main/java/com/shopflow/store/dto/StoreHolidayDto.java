package com.shopflow.store.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class StoreHolidayDto {
    private java.util.UUID id;

    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;

    private String reason;

    public java.util.UUID getId() { return id; }
    public void setId(java.util.UUID id) { this.id = id; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static StoreHolidayDto fromEntity(com.shopflow.store.StoreHoliday holiday) {
        StoreHolidayDto dto = new StoreHolidayDto();
        dto.setId(holiday.getId());
        dto.setHolidayDate(holiday.getHolidayDate());
        dto.setReason(holiday.getReason());
        return dto;
    }
}
