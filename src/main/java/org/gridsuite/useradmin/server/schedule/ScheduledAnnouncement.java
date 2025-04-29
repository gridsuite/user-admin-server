/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.schedule;

import lombok.AllArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.gridsuite.useradmin.server.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Component
@AllArgsConstructor
public class ScheduledAnnouncement {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledAnnouncement.class);

    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${useradmin.cron.announcement-check:-}", zone = "UTC")
    @SchedulerLock(name = "checkAnnouncement", lockAtLeastFor = "1s", lockAtMostFor = "59s")
    public void sendNotificationIfAnnouncements() {
        LOGGER.debug("check announcement cron starting execution");
        announcementRepository
            .findCurrentAnnouncement()
            .ifPresent(announcement -> {
                if (!announcement.isNotified()) {
                    LOGGER.info("New announcement ({}) to notify", announcement.getId());
                    notificationService.emitAnnouncementMessage(announcement.toDto());
                    announcement.setNotified(true);
                    announcementRepository.save(announcement);
                } else {
                    LOGGER.debug("No new announcement to notify");
                }
            });
    }

    @Scheduled(cron = "${useradmin.cron.announcement-clean:-}", zone = "UTC")
    @SchedulerLock(name = "deleteExpiredAnnouncements", lockAtLeastFor = "30s")
    public void deleteExpiredAnnouncements() {
        LOGGER.debug("Delete expired announcement cron starting");
        final long count = announcementRepository.deleteExpiredAnnouncements();
        LOGGER.atLevel(count > 0L ? Level.DEBUG : Level.INFO).log("{} expired announcement(s) deleted.", count);
    }
}
