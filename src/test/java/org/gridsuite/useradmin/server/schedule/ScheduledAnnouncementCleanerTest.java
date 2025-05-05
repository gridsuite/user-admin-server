/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.schedule;

import org.assertj.core.api.WithAssertions;
import org.gridsuite.useradmin.server.UserAdminApplication;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@SpringBootTest(classes = {UserAdminApplication.class, TestChannelBinderConfiguration.class})
class ScheduledAnnouncementCleanerTest implements WithAssertions {

    @Autowired
    private ScheduledAnnouncement scheduledAnnouncement;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @AfterEach
    void cleanDB() {
        announcementRepository.deleteAll();
    }

    @Test
    void testSendScheduledAnnouncement() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        AnnouncementEntity announcement1ShouldBeDeleted = new AnnouncementEntity(now.minus(6, ChronoUnit.DAYS), now.minus(4, ChronoUnit.DAYS), "Test message", AnnouncementSeverity.WARN);
        AnnouncementEntity announcement2ShouldBeDeleted = new AnnouncementEntity(now.minus(3, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS), "Test message 2", AnnouncementSeverity.INFO);
        AnnouncementEntity announcement3 = new AnnouncementEntity(now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), "Test message 3", AnnouncementSeverity.WARN);
        AnnouncementEntity announcement4 = new AnnouncementEntity(now.plus(2, ChronoUnit.DAYS), now.plus(3, ChronoUnit.DAYS), "Test message 4", AnnouncementSeverity.INFO);

        announcementRepository.save(announcement1ShouldBeDeleted);
        announcementRepository.save(announcement2ShouldBeDeleted);
        announcementRepository.save(announcement3);
        announcementRepository.save(announcement4);

        assertEquals(4, announcementRepository.findAll().size());
        scheduledAnnouncement.deleteExpiredAnnouncements();
        assertEquals(2, announcementRepository.findAll().size());

        assertThat(announcementRepository.findAll().stream().map(AnnouncementEntity::toDto))
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactlyElementsOf(List.of(announcement3.toDto(), announcement4.toDto()));
    }
}
