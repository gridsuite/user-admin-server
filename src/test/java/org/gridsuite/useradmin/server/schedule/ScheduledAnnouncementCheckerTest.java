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
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.gridsuite.useradmin.server.service.NotificationService.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@SpringBootTest(classes = {UserAdminApplication.class, TestChannelBinderConfiguration.class})
class ScheduledAnnouncementCheckerTest implements WithAssertions {

    @Autowired
    private ScheduledAnnouncement scheduledAnnouncement;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private OutputDestination output;

    private static final String ANNOUNCEMENT_DESTINATION = "config.message";
    private static final String DIRECTORY_UPDATE_DESTINATION = "directory.update";
    private static final long TIMEOUT = 2000;

    @AfterEach
    void cleanDB() {
        announcementRepository.deleteAll();
        assertQueuesEmptyThenClear(List.of(ANNOUNCEMENT_DESTINATION, DIRECTORY_UPDATE_DESTINATION), output);
    }

    @Test
    void testSendScheduledAnnouncement() {
        Instant now = Instant.now();
        String payload = "Test message";
        AnnouncementEntity announcement1 = new AnnouncementEntity(now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), payload, AnnouncementSeverity.WARN);
        AnnouncementEntity announcement2 = new AnnouncementEntity(now.minus(6, ChronoUnit.DAYS), now.minus(5, ChronoUnit.DAYS), "Test message 2", AnnouncementSeverity.WARN);

        announcementRepository.save(announcement1);
        announcementRepository.save(announcement2);
        assertEquals(2, announcementRepository.findAll().size());

        scheduledAnnouncement.sendNotificationIfAnnouncements();

        assertAnnouncementMessageSent(payload, Duration.between(Instant.now(), announcement1.getEndDate()).toMillis(), announcement1.getSeverity());
    }

    @Test
    void testSendScheduledAnnouncementShouldNotSendNotificationTwice() {
        Instant now = Instant.now();
        String payload = "Test message";
        AnnouncementEntity announcement1 = new AnnouncementEntity(now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), payload, AnnouncementSeverity.WARN);

        announcementRepository.save(announcement1);

        scheduledAnnouncement.sendNotificationIfAnnouncements();
        assertAnnouncementMessageSent(payload, Duration.between(Instant.now(), announcement1.getEndDate()).toMillis(), announcement1.getSeverity());

        scheduledAnnouncement.sendNotificationIfAnnouncements();
        assertNull(output.receive(TIMEOUT, ANNOUNCEMENT_DESTINATION));
    }

    @Test
    void testSendScheduledAnnouncementShouldDoNothing() {
        Instant now = Instant.now();
        AnnouncementEntity announcement1 = new AnnouncementEntity(now.plus(1, ChronoUnit.DAYS), now.plus(2, ChronoUnit.DAYS), "Test message", AnnouncementSeverity.WARN);

        announcementRepository.save(announcement1);
        assertEquals(1, announcementRepository.findAll().size());

        scheduledAnnouncement.sendNotificationIfAnnouncements();

        assertNull(output.receive(TIMEOUT, ANNOUNCEMENT_DESTINATION));
    }

    private void assertAnnouncementMessageSent(String expectedMessage, long expectedDuration, AnnouncementSeverity expectedSeverity) {
        Message<byte[]> message = output.receive(TIMEOUT, ANNOUNCEMENT_DESTINATION);
        MessageHeaders headers = message.getHeaders();
        assertEquals(MESSAGE_TYPE_ANNOUNCEMENT, headers.get(HEADER_MESSAGE_TYPE));
        assertThat((Long) headers.get(HEADER_DURATION)).isCloseTo(expectedDuration, within(200L));
        assertEquals(expectedSeverity, headers.get(HEADER_SEVERITY));
        assertEquals(expectedMessage, new String(message.getPayload()));
    }

    private static void assertQueuesEmptyThenClear(List<String> destinations, OutputDestination output) {
        try {
            destinations.forEach(destination -> assertNull(output.receive(TIMEOUT, destination), "Should not be any messages in queue " + destination + " : "));
        } catch (NullPointerException e) {
            // Ignoring
        } finally {
            output.clear(); // purge in order to not fail the other tests
        }
    }
}
