package com.shopflow.order;

public enum OrderStatus {
    PENDING,
    ACCEPTED,
    PREPARING,
    READY,
    COMPLETED,
    REJECTED,
    CANCELLED;

    /**
     * Returns true if transition from current status to target is allowed.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == ACCEPTED || target == REJECTED || target == CANCELLED;
            case ACCEPTED -> target == PREPARING || target == CANCELLED;
            case PREPARING -> target == READY || target == CANCELLED;
            case READY -> target == COMPLETED || target == CANCELLED;
            // Terminal states — no transitions allowed
            case COMPLETED, REJECTED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED;
    }
}
