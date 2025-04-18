/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import org.gridsuite.useradmin.server.entity.AnnouncementEntity;
import org.gridsuite.useradmin.server.entity.AnnouncementSeverity;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.gridsuite.useradmin.server.service.ScheduledAnnouncementChecker;
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
import java.util.UUID;

import static org.gridsuite.useradmin.server.service.NotificationService.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@SpringBootTest(classes = {UserAdminApplication.class, TestChannelBinderConfiguration.class})
class ScheduledAnnouncementCheckerTest {

    @Autowired
    private ScheduledAnnouncementChecker scheduledAnnouncementChecker;

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
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        String payload = "Test message";
        String payload2 = "Test message 2";
        AnnouncementEntity announcement1 = new AnnouncementEntity(yesterday, tomorrow, payload, AnnouncementSeverity.WARN);
        AnnouncementEntity announcement2 = new AnnouncementEntity(yesterday.minus(5, ChronoUnit.DAYS), yesterday.minus(4, ChronoUnit.DAYS), payload2, AnnouncementSeverity.WARN);

        announcementRepository.save(announcement1);
        announcementRepository.save(announcement2);
        assertEquals(2, announcementRepository.findAll().size());

        scheduledAnnouncementChecker.sendNotificationIfAnnouncements();

        assertAnnouncementMessageSent(payload, announcement1.getId(), Duration.between(announcement1.getStartDate(), announcement1.getEndDate()).toMillis(), announcement1.getSeverity());
    }

    @Test
    void testSendScheduledAnnouncementShouldNotSendNotificationTwice() {
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        String payload = "Test message";
        AnnouncementEntity announcement1 = new AnnouncementEntity(yesterday, tomorrow, payload, AnnouncementSeverity.WARN);

        announcementRepository.save(announcement1);

        scheduledAnnouncementChecker.sendNotificationIfAnnouncements();
        assertAnnouncementMessageSent(payload, announcement1.getId(), Duration.between(announcement1.getStartDate(), announcement1.getEndDate()).toMillis(), announcement1.getSeverity());

        scheduledAnnouncementChecker.sendNotificationIfAnnouncements();
        assertNull(output.receive(TIMEOUT, ANNOUNCEMENT_DESTINATION));
    }

    @Test
    void testSendScheduledAnnouncementShouldDoNothing() {
        Instant now = Instant.now();
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        String payload = "Test message";
        AnnouncementEntity announcement1 = new AnnouncementEntity(tomorrow, tomorrow.plus(1, ChronoUnit.DAYS), payload, AnnouncementSeverity.WARN);

        announcementRepository.save(announcement1);
        assertEquals(1, announcementRepository.findAll().size());

        scheduledAnnouncementChecker.sendNotificationIfAnnouncements();

        assertNull(output.receive(TIMEOUT, ANNOUNCEMENT_DESTINATION));
    }

    private void assertAnnouncementMessageSent(String expectedMessage, UUID expectedUuid, long expectedDuration, AnnouncementSeverity expectedSeverity) {
        Message<byte[]> message = output.receive(TIMEOUT, ANNOUNCEMENT_DESTINATION);
        MessageHeaders headers = message.getHeaders();
        assertEquals(MESSAGE_TYPE_ANNOUNCEMENT, headers.get(HEADER_MESSAGE_TYPE));
        assertEquals(expectedUuid, headers.get(HEADER_ANNOUNCEMENT_ID));
        assertEquals(expectedDuration, headers.get(HEADER_DURATION));
        assertEquals(expectedSeverity, headers.get(HEADER_SEVERITY));
        assertEquals(expectedMessage, new String(message.getPayload()));
    }

    private void assertQueuesEmptyThenClear(List<String> destinations, OutputDestination output) {
        try {
            destinations.forEach(destination -> assertNull(output.receive(TIMEOUT, destination), "Should not be any messages in queue " + destination + " : "));
        } catch (NullPointerException e) {
            // Ignoring
        } finally {
            output.clear(); // purge in order to not fail the other tests
        }
    }
}
