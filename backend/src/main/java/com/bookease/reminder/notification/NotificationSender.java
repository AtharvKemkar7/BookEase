package com.bookease.reminder.notification;

import com.bookease.reminder.Reminder;
import com.bookease.reminder.ReminderChannel;

/**
 * Notification abstraction. Reminder processing depends on this interface rather than on a concrete provider,
 * so a real email/notification provider can be plugged in later without touching the reminder logic.
 */
public interface NotificationSender {

    ReminderChannel channel();

    void send(Reminder reminder);
}
