package com.bookease.reminder.notification;

import com.bookease.reminder.Reminder;
import com.bookease.reminder.ReminderChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);

    @Override
    public ReminderChannel channel() {
        return ReminderChannel.IN_APP;
    }

    @Override
    public void send(Reminder reminder) {
        log.info(
                "Delivered in-app reminder {} for appointment {} at {}",
                reminder.getId(),
                reminder.getAppointment().getId(),
                reminder.getReminderAt());
    }
}
