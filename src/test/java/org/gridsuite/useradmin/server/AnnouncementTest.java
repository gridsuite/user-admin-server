/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.WithAssertions;
import org.gridsuite.useradmin.server.dto.Announcement;
import org.gridsuite.useradmin.server.entity.AnnouncementEntity;
import org.gridsuite.useradmin.server.entity.AnnouncementSeverity;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.gridsuite.useradmin.server.UserAdminException.Type.*;
import static org.gridsuite.useradmin.server.service.NotificationService.HEADER_MESSAGE_TYPE;
import static org.gridsuite.useradmin.server.service.NotificationService.MESSAGE_TYPE_CANCEL_ANNOUNCEMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {UserAdminApplication.class, TestChannelBinderConfiguration.class})
@ActiveProfiles({"default"})
class AnnouncementTest implements WithAssertions {

    private static final String ANNOUNCEMENT_DESTINATION = "config.message";
    private static final String DIRECTORY_UPDATE_DESTINATION = "directory.update";
    private static final long TIMEOUT = 1000;
    private static final String ADMIN_USER = "admin1";
    private static final String NOT_ADMIN = "notAdmin";
    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OutputDestination output;

    @AfterEach
    void cleanDB() {
        announcementRepository.deleteAll();
        assertQueuesEmptyThenClear(List.of(ANNOUNCEMENT_DESTINATION, DIRECTORY_UPDATE_DESTINATION), output);
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

    @Test
    void testCreateAnnouncement() throws Exception {

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        Announcement announcementToBeCreated = new Announcement(UUID.randomUUID(), now, now.plus(2, ChronoUnit.DAYS), "Test message", AnnouncementSeverity.INFO);
        assertEquals(0, announcementRepository.findAll().size());

        // Not allowed because not admin
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements", announcementToBeCreated.id())
                .header("userId", NOT_ADMIN)
                .queryParam("severity", announcementToBeCreated.severity().name())
                .queryParam("startDate", announcementToBeCreated.startDate().toString())
                .queryParam("endDate", announcementToBeCreated.endDate().toString())
                .content(announcementToBeCreated.message())
            )
            .andExpect(status().isForbidden());
        assertEquals(0, announcementRepository.findAll().size());

        // Should NOT be ok because startDate > endDate
        MvcResult result = mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements", announcementToBeCreated.id())
                .header("userId", ADMIN_USER)
                .queryParam("severity", announcementToBeCreated.severity().name())
                .queryParam("startDate", announcementToBeCreated.endDate().toString())
                .queryParam("endDate", announcementToBeCreated.startDate().toString())
                .content(announcementToBeCreated.message())
            )
            .andExpect(status().isBadRequest())
            .andReturn();
        assertTrue(result.getResponse().getContentAsString().contains(START_DATE_SAME_OR_AFTER_END_DATE.name()));
        assertEquals(0, announcementRepository.findAll().size());

        // Should NOT be ok because severity doesn't exist
        result = mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements", announcementToBeCreated.id())
                .header("userId", ADMIN_USER)
                .queryParam("severity", "NOT A SEVERITY")
                .queryParam("startDate", announcementToBeCreated.startDate().toString())
                .queryParam("endDate", announcementToBeCreated.endDate().toString())
                .content(announcementToBeCreated.message())
            )
            .andExpect(status().isBadRequest())
            .andReturn();
        assertTrue(result.getResponse().getContentAsString().contains(SEVERITY_DOES_NOT_EXIST.name()));
        assertEquals(0, announcementRepository.findAll().size());

        // Should NOT be ok because startDate = endDate
        result = mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements", announcementToBeCreated.id())
                .header("userId", ADMIN_USER)
                .queryParam("severity", announcementToBeCreated.severity().name())
                .queryParam("startDate", announcementToBeCreated.startDate().toString())
                .queryParam("endDate", announcementToBeCreated.startDate().toString())
                .content(announcementToBeCreated.message())
            )
            .andExpect(status().isBadRequest())
            .andReturn();
        assertTrue(result.getResponse().getContentAsString().contains(START_DATE_SAME_OR_AFTER_END_DATE.name()));
        assertEquals(0, announcementRepository.findAll().size());

        // Should be ok because user is admin
        result = mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements", announcementToBeCreated.id())
                .header("userId", ADMIN_USER)
                .queryParam("severity", announcementToBeCreated.severity().name())
                .queryParam("startDate", announcementToBeCreated.startDate().toString())
                .queryParam("endDate", announcementToBeCreated.endDate().toString())
                .content(announcementToBeCreated.message())
            )
            .andExpect(status().isOk())
            .andReturn();

        Announcement createdAnnouncement = objectMapper.readValue(result.getResponse().getContentAsString(), Announcement.class);
        assertThat(createdAnnouncement)
            .usingRecursiveComparison()
            .ignoringFields("id", "remainingDuration")
            .isEqualTo(announcementToBeCreated);
        List<AnnouncementEntity> all = announcementRepository.findAll();
        assertEquals(1, all.size());
        assertThat(all.getFirst().toDto())
            .usingRecursiveComparison()
            .ignoringFields("id", "remainingDuration")
            .isEqualTo(announcementToBeCreated);

        // Should NOT be ok because the date of announcement overlaps with another registered announcement
        result = mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements", announcementToBeCreated.id())
                .header("userId", ADMIN_USER)
                .queryParam("severity", announcementToBeCreated.severity().name())
                .queryParam("startDate", announcementToBeCreated.startDate().minus(1, ChronoUnit.DAYS).toString())
                .queryParam("endDate", announcementToBeCreated.endDate().plus(1, ChronoUnit.DAYS).toString())
                .content(announcementToBeCreated.message())
            )
            .andExpect(status().isBadRequest())
            .andReturn();
        assertEquals(1, all.size());
        assertThat(all.getFirst().toDto())
            .usingRecursiveComparison()
            .ignoringFields("id", "remainingDuration")
            .isEqualTo(announcementToBeCreated);
        assertTrue(result.getResponse().getContentAsString().contains(OVERLAPPING_ANNOUNCEMENTS.name()));
    }

    @Test
    void testDeleteAnnouncement() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        AnnouncementEntity announcementToBeCreated = new AnnouncementEntity(now, now.plus(2, ChronoUnit.DAYS), "Test message", AnnouncementSeverity.INFO);
        announcementRepository.save(announcementToBeCreated);
        assertEquals(1, announcementRepository.findAll().size());
        assertThat(announcementRepository.findAll().getFirst().toDto())
            .usingRecursiveComparison()
            .ignoringFields("id", "remainingDuration")
            .isEqualTo(announcementToBeCreated.toDto());

        // Should be ok even if the id doesn't exist (it just doesn't do anything)
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/announcements/{id}", UUID.randomUUID())
                .header("userId", ADMIN_USER))
            .andExpect(status().isOk());
        assertEquals(1, announcementRepository.findAll().size());

        // Should NOT be ok because user is not admin
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/announcements/{id}", UUID.randomUUID())
                .header("userId", NOT_ADMIN))
            .andExpect(status().isForbidden());
        assertEquals(1, announcementRepository.findAll().size());

        // Should be ok and the entry should be gone
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/announcements/{id}", announcementToBeCreated.getId())
                .header("userId", ADMIN_USER))
            .andExpect(status().isOk());
        assertEquals(0, announcementRepository.findAll().size());
        assertCancelAnnouncementMessageSent();

        // Add an announcement 5 days in the future
        AnnouncementEntity futureAnnouncement = new AnnouncementEntity(now.plus(5, ChronoUnit.DAYS), now.plus(7, ChronoUnit.DAYS), "Test message", AnnouncementSeverity.WARN);
        announcementRepository.save(futureAnnouncement);
        assertEquals(1, announcementRepository.findAll().size());
        // Delete the announcement that was just added, it should not trigger a message in the broker because the announcement was not sent
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/announcements/{id}", futureAnnouncement.getId())
                .header("userId", ADMIN_USER))
            .andExpect(status().isOk());
        assertEquals(0, announcementRepository.findAll().size());
        assertNull(output.receive(TIMEOUT, ANNOUNCEMENT_DESTINATION));
    }

    private void assertCancelAnnouncementMessageSent() {
        Message<byte[]> message = output.receive(TIMEOUT, ANNOUNCEMENT_DESTINATION);
        MessageHeaders headers = message.getHeaders();
        assertEquals(MESSAGE_TYPE_CANCEL_ANNOUNCEMENT, headers.get(HEADER_MESSAGE_TYPE));
    }

    @Test
    void testGetAnnouncements() throws Exception {

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        assertEquals(0, announcementRepository.findAll().size());

        // Not allowed because not admin
        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/announcements")
                .header("userId", NOT_ADMIN)
            )
            .andExpect(status().isForbidden());

        // Should be ok but empty result
        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/announcements")
                .header("userId", ADMIN_USER)
            )
            .andExpect(status().isOk())
            .andReturn();
        assertEquals("[]", result.getResponse().getContentAsString());

        // insert a list of announcement
        AnnouncementEntity announcementToBeCreated1 = new AnnouncementEntity(now, now.plus(1, ChronoUnit.DAYS), "Test message 1", AnnouncementSeverity.INFO);
        AnnouncementEntity announcementToBeCreated2 = new AnnouncementEntity(now.plus(2, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), "Test message 2", AnnouncementSeverity.INFO);
        AnnouncementEntity announcementToBeCreated3 = new AnnouncementEntity(now.plus(4, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS), "Test message 3", AnnouncementSeverity.INFO);

        announcementRepository.save(announcementToBeCreated1);
        announcementRepository.save(announcementToBeCreated2);
        announcementRepository.save(announcementToBeCreated3);

        assertEquals(3, announcementRepository.findAll().size());

        // Should be ok
        result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/announcements")
                .header("userId", ADMIN_USER)
            )
            .andExpect(status().isOk())
            .andReturn();
        List<Announcement> announcements = Arrays.stream(objectMapper.readValue(result.getResponse().getContentAsString(), Announcement[].class)).toList();
        assertEquals(3, announcements.size());
        assertThat(announcements)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("remainingDuration")
            .containsExactlyInAnyOrder(announcementToBeCreated1.toDto(), announcementToBeCreated2.toDto(), announcementToBeCreated3.toDto());
    }

    @Test
    void testGetCurrentAnnouncement() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        assertEquals(0, announcementRepository.findAll().size());

        // insert a list of announcement
        AnnouncementEntity announcementToBeCreated1 = new AnnouncementEntity(now.minus(1, ChronoUnit.HOURS), now.minus(1, ChronoUnit.MINUTES), "Test message 1", AnnouncementSeverity.INFO);
        AnnouncementEntity announcementToBeCreated2 = new AnnouncementEntity(now, now.plus(1, ChronoUnit.HOURS), "Test message 2", AnnouncementSeverity.INFO);
        AnnouncementEntity announcementToBeCreated3 = new AnnouncementEntity(now.plus(2, ChronoUnit.HOURS), now.plus(1, ChronoUnit.DAYS), "Test message 3", AnnouncementSeverity.INFO);

        announcementRepository.save(announcementToBeCreated1);
        announcementRepository.save(announcementToBeCreated2);
        announcementRepository.save(announcementToBeCreated3);

        var result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/announcements/current")
                .header("userId", ADMIN_USER)
            )
            .andExpect(status().isOk())
            .andReturn();
        Announcement announcement = objectMapper.readValue(result.getResponse().getContentAsString(), Announcement.class);
        assertThat(announcement)
            .usingRecursiveComparison()
            .ignoringFields("remainingDuration")
            .isEqualTo(announcementToBeCreated2.toDto());
    }

    @Test
    void testGetCurrentAnnouncementWithNoResult() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        assertEquals(0, announcementRepository.findAll().size());

        // insert a list of announcement
        AnnouncementEntity announcementToBeCreated1 = new AnnouncementEntity(now.minus(1, ChronoUnit.HOURS), now.minus(1, ChronoUnit.MINUTES), "Test message 1", AnnouncementSeverity.INFO);
        AnnouncementEntity announcementToBeCreated2 = new AnnouncementEntity(now.plus(2, ChronoUnit.HOURS), now.plus(1, ChronoUnit.DAYS), "Test message 3", AnnouncementSeverity.INFO);

        announcementRepository.save(announcementToBeCreated1);
        announcementRepository.save(announcementToBeCreated2);

        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/announcements/current")
                .header("userId", ADMIN_USER)
            )
            .andExpect(status().isNoContent())
            .andReturn();
    }

}
