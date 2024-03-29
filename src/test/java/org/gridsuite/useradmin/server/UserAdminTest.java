/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.useradmin.server.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.gridsuite.useradmin.server.service.NotificationService.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {UserAdminApplication.class, TestChannelBinderConfiguration.class})
@ActiveProfiles({"default"})
class UserAdminTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserAdminRepository userAdminRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private OutputDestination output;

    private final String maintenanceMessageDestination = "config.message";

    private static final long TIMEOUT = 1000;

    @AfterEach
    public void cleanDB() {
        userAdminRepository.deleteAll();
        connectionRepository.deleteAll();
        announcementRepository.deleteAll();
    }

    private static final String USER_SUB = "user1";
    private static final String USER_SUB2 = "user2";
    private static final String USER_UNKNOWN = "UNKNOWN";
    private static final String ADMIN_USER = "admin1";
    private static final String NOT_ADMIN = "notAdmin";

    @Test
    void testUserAdmin() throws Exception {
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}/isAdmin", USER_SUB))
                .andExpect(status().isForbidden())
                .andReturn();

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}/isAdmin", ADMIN_USER))
                .andExpect(status().isOk())
                .andReturn();

        List<UserInfosEntity> userEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertEquals(0, userEntities.size());

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", ADMIN_USER))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();

        userEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertEquals(1, userEntities.size());

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_UNKNOWN))
                .andExpect(status().isNoContent())
                .andReturn();
        assertEquals(3, connectionRepository.findAll().size());
        assertTrue(connectionRepository.findBySub(USER_SUB).get(0).getConnectionAccepted());
        assertFalse(connectionRepository.findBySub(USER_UNKNOWN).get(0).getConnectionAccepted());
        LocalDateTime firstConnectionDate = connectionRepository.findBySub(USER_SUB).get(0).getFirstConnexionDate();
        //firstConnectionDate and lastConnectionDate are equals cause this is the first connection for this user
        assertTrue(firstConnectionDate.toEpochSecond(ZoneOffset.UTC) < connectionRepository.findBySub(USER_SUB).get(0).getLastConnexionDate().toEpochSecond(ZoneOffset.UTC) + 2);

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(firstConnectionDate, connectionRepository.findBySub(USER_SUB).get(0).getFirstConnexionDate());

        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isNoContent())
                .andReturn();

        userEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(0, userEntities.size());

        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();

        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();

        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();

        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users")
                        .header("userId", NOT_ADMIN)
                        .contentType(APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden())
                .andReturn();

        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users")
                        .header("userId", ADMIN_USER)
                        .contentType(APPLICATION_JSON)
                        .content("[\"" + USER_UNKNOWN + "\"]"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    void testGetConnections() throws Exception {
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB2)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();

        List<UserInfosEntity> userEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertEquals(2, userEntities.size());

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB2))
                .andExpect(status().isOk())
                .andReturn();

        List<ConnectionEntity> connectionEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/connections")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertEquals(2, connectionEntities.size());

        connectionRepository.save(new ConnectionEntity(USER_SUB, LocalDateTime.now(), LocalDateTime.now(), true));
        connectionRepository.save(new ConnectionEntity(USER_SUB, LocalDateTime.now().minusSeconds(5), LocalDateTime.now(), true));
        connectionRepository.save(new ConnectionEntity(USER_SUB2, LocalDateTime.now(), LocalDateTime.now(), false));

        connectionEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/connections")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(2, connectionEntities.size());

        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/connections")
                        .header("userId", NOT_ADMIN)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSendMaintenanceMessage() throws Exception {
        //Send a maintenance message and expect everything to be ok
        String message = "The application will be on maintenance until the end of the maintenance";
        Duration duration = Duration.ofSeconds(300);
        AnnouncementEntity announcement = new AnnouncementEntity(message, duration);
        String requestBody = objectMapper.writeValueAsString(announcement);
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements")
                .header("userId", ADMIN_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
        assertMaintenanceMessageSent(message, duration.toSeconds());
        assertEquals(1, announcementRepository.findAll().size());

        //Send a maintenance message without duration and expect everything to be ok
        announcement = new AnnouncementEntity(message, null);
        requestBody = objectMapper.writeValueAsString(announcement);
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements")
                        .header("userId", ADMIN_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
        assertMaintenanceMessageSent(message, null);
        assertEquals(1, announcementRepository.findAll().size()); // only one at a time for now

        //Send a maintenance message with a user that's not an admin and expect 403 FORBIDDEN
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/announcements")
                        .header("userId", NOT_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCancelMaintenanceMessage() throws Exception {
        AnnouncementEntity announcement = announcementRepository.save(new AnnouncementEntity("I think dangling line is a funny name for a line", Duration.ofSeconds(60)));
        assertEquals(1, announcementRepository.findAll().size());

        //Send a cancel maintenance message and expect everything to be ok
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/announcements/" + announcement.getId())
                        .header("userId", ADMIN_USER))
                .andExpect(status().isOk());
        assertCancelMaintenanceMessageSent();
        assertEquals(0, announcementRepository.findAll().size());

        // With a non-existing ID, expect 404
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/announcements/" + UUID.randomUUID())
                .header("userId", ADMIN_USER))
            .andExpect(status().isNotFound());

        //Send a cancel maintenance message with a user that's not an admin and expect 403 FORBIDDEN
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/announcements/" + UUID.randomUUID())
                        .header("userId", NOT_ADMIN))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllMaintenanceMessages() throws Exception {
        announcementRepository.save(new AnnouncementEntity("I think dangling line is a funny name for a line", Duration.ofSeconds(60)));
        assertEquals(1, announcementRepository.findAll().size());

        // Try to retrieve all the messages and expect everything to be ok
        MvcResult mvcResult = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/announcements")
                .header("userId", ADMIN_USER))
            .andExpect(status().isOk()).andReturn();
        List<AnnouncementEntity> announcements = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertEquals(1, announcements.size());

        // Try to retrieve all the messages with a user that's not an admin and expect 403 FORBIDDEN
        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/announcements")
                        .header("userId", NOT_ADMIN))
                .andExpect(status().isForbidden());
    }

    private void assertMaintenanceMessageSent(String maintenanceMessage, Long duration) {
        Message<byte[]> message = output.receive(TIMEOUT, maintenanceMessageDestination);
        assertEquals(maintenanceMessage, new String(message.getPayload()));
        MessageHeaders headers = message.getHeaders();
        assertEquals(MESSAGE_TYPE_MAINTENANCE, headers.get(HEADER_MESSAGE_TYPE));
        assertEquals(duration, headers.get(HEADER_DURATION));
    }

    private void assertCancelMaintenanceMessageSent() {
        Message<byte[]> message = output.receive(TIMEOUT, maintenanceMessageDestination);
        assertEquals("", new String(message.getPayload()));
        MessageHeaders headers = message.getHeaders();
        assertEquals(MESSAGE_TYPE_CANCEL_MAINTENANCE, headers.get(HEADER_MESSAGE_TYPE));
    }
}
