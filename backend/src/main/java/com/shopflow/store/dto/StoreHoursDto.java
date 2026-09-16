package com.shopflow.store.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class StoreHoursDto {
    private DayOfWeek dayOfWeek;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private boolean isClosed;

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getOpeningTime() { return openingTime; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }
    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }

    public static StoreHoursDto fromEntity(com.shopflow.store.StoreHours hours) {
        StoreHoursDto dto = new StoreHoursDto();
        dto.setDayOfWeek(hours.getDayOfWeek());
        dto.setOpeningTime(hours.getOpeningTime());
        dto.setClosingTime(hours.getClosingTime());
        dto.setClosed(hours.isClosed());
        return dto;
    }
}
