package com.ticks.event_service.entity;

public enum ScheduleStatus {
    SCHEDULED,   // upcoming, inventory not yet created
    ON_SALE,     // inventory created, tickets available for purchase
    SOLD_OUT,    // InventoryService signalled zero remaining stock
    CANCELLED,   // this specific date cancelled, others may still run
    COMPLETED    // show has ended
}