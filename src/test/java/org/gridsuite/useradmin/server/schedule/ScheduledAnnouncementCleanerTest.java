/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.schedule;

import org.gridsuite.useradmin.server.UserAdminApplication;
import org.gridsuite.useradmin.server.dto.AnnouncementMapper;
import org.gridsuite.useradmin.server.entity.AnnouncementEntity;
import org.gridsuite.useradmin.server.entity.AnnouncementSeverity;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@SpringBootTest(classes = {UserAdminApplication.class, TestChannelBinderConfiguration.class})
class ScheduledAnnouncementCleanerTest {

    @Autowired
    private ScheduledAnnouncementCleaner scheduledAnnouncementCleaner;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @AfterEach
    void cleanDB() {
        announcementRepository.deleteAll();
    }

    @Test
    void testSendScheduledAnnouncement() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant nowMinus6Days = now.minus(6, ChronoUnit.DAYS);
        Instant nowMinus4Days = now.minus(4, ChronoUnit.DAYS);
        Instant nowMinus3Days = now.minus(3, ChronoUnit.DAYS);
        Instant nowMinus2Days = now.minus(2, ChronoUnit.DAYS);
        Instant nowPlus3Days = now.plus(3, ChronoUnit.DAYS);
        Instant nowPlus2Days = now.plus(2, ChronoUnit.DAYS);
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        String msg1 = "Test message";
        String msg2 = "Test message 2";
        String msg3 = "Test message 3";
        String msg4 = "Test message 4";

        AnnouncementEntity announcement1ShouldBeDeleted = new AnnouncementEntity(nowMinus6Days, nowMinus4Days, msg1, AnnouncementSeverity.WARN);
        AnnouncementEntity announcement2ShouldBeDeleted = new AnnouncementEntity(nowMinus3Days, nowMinus2Days, msg2, AnnouncementSeverity.INFO);
        AnnouncementEntity announcement3 = new AnnouncementEntity(yesterday, tomorrow, msg3, AnnouncementSeverity.WARN);
        AnnouncementEntity announcement4 = new AnnouncementEntity(nowPlus2Days, nowPlus3Days, msg4, AnnouncementSeverity.INFO);

        announcementRepository.save(announcement1ShouldBeDeleted);
        announcementRepository.save(announcement2ShouldBeDeleted);
        announcementRepository.save(announcement3);
        announcementRepository.save(announcement4);

        assertEquals(4, announcementRepository.findAll().size());
        scheduledAnnouncementCleaner.deleteExpiredAnnouncements();
        assertEquals(2, announcementRepository.findAll().size());

        assertThat(announcementRepository.findAll().stream().map(AnnouncementMapper::fromEntity))
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "remainingDuration").containsExactlyElementsOf(List.of(AnnouncementMapper.fromEntity(announcement3), AnnouncementMapper.fromEntity(announcement4)));
    }
}
