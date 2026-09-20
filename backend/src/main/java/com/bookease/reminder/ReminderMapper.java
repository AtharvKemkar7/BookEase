package com.bookease.reminder;

public final class ReminderMapper {

    private ReminderMapper() {
    }

    public static ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getAppointment().getId(),
                reminder.getUser().getId(),
                reminder.getReminderAt(),
                reminder.getChannel(),
                reminder.getStatus(),
                reminder.getSentAt(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt());
    }
}
