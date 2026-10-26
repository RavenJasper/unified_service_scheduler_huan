package com.unified.scheduler.domain;

public enum AppointmentStatus {
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    RESCHEDULED;

    public boolean isActive() {
        return this == CONFIRMED || this == IN_PROGRESS;
    }

    public boolean canBeCancelled() {
        return this == CONFIRMED;
    }

    public boolean canBeRescheduled() {
        return this == CONFIRMED;
    }
}

