/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.gridsuite.useradmin.server.dto.AnnouncementMapper;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class ScheduledAnnouncementChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledAnnouncementChecker.class);

    private final AnnouncementRepository announcementRepository;

    private final NotificationService notificationService;

    public ScheduledAnnouncementChecker(AnnouncementRepository announcementRepository, NotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${check-announcement-cron:0 */1 * * * *}", zone = "UTC")
    @SchedulerLock(name = "checkAnnouncement", lockAtLeastFor = "1s", lockAtMostFor = "59s")
    public void sendNotificationIfAnnouncements() {
        LOGGER.debug("check announcement cron starting execution");
        announcementRepository
            .findCurrentAnnouncement(Instant.now())
            .ifPresent(announcement -> {
                if (!announcement.isNotified()) {
                    LOGGER.info("new announcement ({}) to notify", announcement.getId());
                    notificationService.emitAnnouncementMessage(AnnouncementMapper.fromEntity(announcement));
                    announcement.setNotified(true);
                    announcementRepository.save(announcement);
                } else {
                    LOGGER.debug("No new announcement to notify");
                }
            });
    }
}
