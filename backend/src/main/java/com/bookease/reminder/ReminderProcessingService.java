package com.bookease.reminder;

import com.bookease.appointment.Appointment;
import com.bookease.reminder.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ReminderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ReminderProcessingService.class);

    private final ReminderRepository reminderRepository;
    private final Map<ReminderChannel, NotificationSender> senders = new EnumMap<>(ReminderChannel.class);
    private final TransactionTemplate transactionTemplate;

    @Value("${bookease.reminders.batch-size:100}")
    private int batchSize;

    public ReminderProcessingService(ReminderRepository reminderRepository,
                                     List<NotificationSender> senders,
                                     PlatformTransactionManager transactionManager) {
        this.reminderRepository = reminderRepository;
        for (NotificationSender sender : senders) {
            this.senders.put(sender.channel(), sender);
        }
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${bookease.reminders.processing-interval-ms:60000}")
    public void scheduledProcess() {
        int processed = processDueReminders();
        if (processed > 0) {
            log.info("Processed {} due reminders", processed);
        }
    }

    public int processDueReminders() {
        Integer processed = transactionTemplate.execute(status -> processBatch());
        return processed == null ? 0 : processed;
    }

    private int processBatch() {
        Instant now = Instant.now();
        List<Reminder> due = reminderRepository.findDueForUpdate(now, PageRequest.of(0, batchSize));
        if (due.isEmpty()) {
            return 0;
        }

        for (Reminder reminder : due) {
            Appointment appointment = reminder.getAppointment();
            if (appointment.getStatus().isTerminal()) {
                reminder.setStatus(ReminderStatus.CANCELLED);
                continue;
            }
            NotificationSender sender = senders.get(reminder.getChannel());
            if (sender == null) {
                reminder.setStatus(ReminderStatus.FAILED);
                continue;
            }
            try {
                sender.send(reminder);
                reminder.setStatus(ReminderStatus.SENT);
                reminder.setSentAt(Instant.now());
            } catch (RuntimeException ex) {
                log.warn("Reminder {} failed to send", reminder.getId(), ex);
                reminder.setStatus(ReminderStatus.FAILED);
            }
        }

        reminderRepository.saveAll(due);
        return due.size();
    }
}
