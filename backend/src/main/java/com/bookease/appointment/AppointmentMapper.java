package com.bookease.appointment;

public final class AppointmentMapper {

    private AppointmentMapper() {
    }

    public static AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getUser().getId(),
                appointment.getProvider().getId(),
                appointment.getProvider().getBusinessName(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt());
    }
}
