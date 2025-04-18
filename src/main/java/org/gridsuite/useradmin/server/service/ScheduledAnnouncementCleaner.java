/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

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
public class ScheduledAnnouncementCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledAnnouncementCleaner.class);

    private final AnnouncementRepository announcementRepository;

    public ScheduledAnnouncementCleaner(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Scheduled(cron = "${clean-announcement-cron:0 0 2 * * ?}", zone = "UTC")
    public void deleteExpiredAnnouncements() {
        LOGGER.info("delete expired announcement cron starting");
        announcementRepository.deleteExpiredAnnouncements(Instant.now());
    }
}
