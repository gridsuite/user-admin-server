/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.ConnectionRepository;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
    private OutputDestination output;

    private final String maintenanceMessageDestination = "config.message";

    private static final long TIMEOUT = 1000;

    @AfterEach
    public void cleanDB() {
        userAdminRepository.deleteAll();
        connectionRepository.deleteAll();
    }

    private static final String USER_SUB = "user1";
    private static final String USER_SUB2 = "user2";
    private static final String USER_UNKNOWN = "UNKNOWN";
    private static final String ADMIN_USER = "admin1";
    private static final String NOT_ADMIN = "notAdmin";
    private static final String PROFILE_1 = "profile_1";

    @Test
    void testUserAdmin() throws Exception {
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}/isAdmin", USER_SUB))
                .andExpect(status().isForbidden())
                .andReturn();

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}/isAdmin", ADMIN_USER))
                .andExpect(status().isOk())
                .andReturn();

        List<UserInfos> userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertEquals(0, userInfos.size());

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", ADMIN_USER))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();

        userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertEquals(1, userInfos.size());

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

        userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(0, userInfos.size());

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
    void testUpdateUser() throws Exception {
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        // add a user
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();
        UserInfos userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        // the new user has no profile by default
        assertNotNull(userInfos);
        assertNull(userInfos.profileName());
        assertEquals(USER_SUB, userInfos.sub());

        // Create a profile
        UserProfile profileInfo = new UserProfile(null, PROFILE_1, null, false);
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(profileInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();

        // udpate the user: change its name and link it to the profile
        UserInfos userInfo = new UserInfos(USER_SUB2, false, PROFILE_1);
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .content(objectWriter.writeValueAsString(userInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER))
                .andExpect(status().isOk());

        userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB2)
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        // the new user has the new name and profile
        assertNotNull(userInfos);
        assertEquals(USER_SUB2, userInfos.sub());
        assertEquals(PROFILE_1, userInfos.profileName());

        // Get profiles for existing single user
        List<UserProfile> userProfiles = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles?sub=" + USER_SUB2)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(1, userProfiles.size());
        assertEquals(PROFILE_1, userProfiles.get(0).name());

        // Get profiles for bad user
        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles?sub=BAD_USER")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        // bad update
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{sub}", "bad user")
                        .content(objectWriter.writeValueAsString(userInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER))
                .andExpect(status().isNotFound());
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

        List<UserInfos> userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertEquals(2, userInfos.size());

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
    void testSendMaintenanceMessage() throws Exception {
        //Send a maintenance message and expect everything to be ok
        String requestBody = objectMapper.writeValueAsString("The application will be on maintenance until the end of the maintenance");
        Integer duration = 300;
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/messages/maintenance", USER_SUB)
                .queryParam("durationInSeconds", duration.toString())
                .header("userId", ADMIN_USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
        assertMaintenanceMessageSent(requestBody, duration);

        //Send a maintenance message without duration and expect everything to be ok
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/messages/maintenance")
                        .header("userId", ADMIN_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
        assertMaintenanceMessageSent(requestBody, null);

        //Send a maintenance message with a user that's not an admin and expect 403 FORBIDDEN
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/messages/maintenance")
                        .queryParam("durationInSeconds", String.valueOf(duration))
                        .header("userId", NOT_ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

    }

    @Test
    void testCancelMaintenanceMessage() throws Exception {
        //Send a cancel maintenance message and expect everything to be ok
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/messages/cancel-maintenance")
                        .header("userId", ADMIN_USER))
                .andExpect(status().isOk());
        assertCancelMaintenanceMessageSent();

        //Send a cancel maintenance message with a user that's not an admin and expect 403 FORBIDDEN
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/messages/cancel-maintenance")
                        .header("userId", NOT_ADMIN))
                .andExpect(status().isForbidden());
    }

    private void assertMaintenanceMessageSent(String maintenanceMessage, Integer duration) {
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
