/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.entity.AnnouncementEntity;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
    public void sendNotificationIfAnnouncements() {
        Instant now = Instant.now();
        LOGGER.info("check announcement cron starting execution");

        List<AnnouncementEntity> announcementsList = announcementRepository.findAll().stream()
                .filter(announcement -> announcement.getStartDate().isBefore(now) && announcement.getEndDate().isAfter(now)).toList();

        if (!announcementsList.isEmpty()) {
            //We shouldn't have more than 1 announcement covering the same time interval
            AnnouncementEntity announcement = announcementsList.getFirst();
            long duration = Duration.between(announcement.getStartDate(), announcement.getEndDate()).toMillis();
            notificationService.emitAnnouncementMessage(announcement.getId(), announcement.getMessage(), duration, announcement.getSeverity());
        }
    }
}
