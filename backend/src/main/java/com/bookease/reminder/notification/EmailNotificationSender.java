package com.bookease.reminder.notification;

import com.bookease.reminder.Reminder;
import com.bookease.reminder.ReminderChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder email sender. It records the intent to send so an email provider can be integrated later without
 * changing reminder processing.
 */
@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.EMAIL;
    }

    @Override
    public void send(Reminder reminder) {
        log.info(
                "Email reminder {} queued for user {} (appointment {})",
                reminder.getId(),
                reminder.getUser().getId(),
                reminder.getAppointment().getId());
    }
}
