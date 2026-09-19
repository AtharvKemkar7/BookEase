package com.bookease.appointment;

import java.util.EnumSet;
import java.util.Set;

public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW;

    public boolean canTransitionTo(AppointmentStatus target) {
        return allowedTargets().contains(target);
    }

    public boolean occupiesSchedule() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == COMPLETED || this == NO_SHOW;
    }

    private Set<AppointmentStatus> allowedTargets() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED, NO_SHOW);
            case CONFIRMED -> EnumSet.of(COMPLETED, CANCELLED, NO_SHOW);
            case CANCELLED, COMPLETED, NO_SHOW -> EnumSet.noneOf(AppointmentStatus.class);
        };
    }
}
