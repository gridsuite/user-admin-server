/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.ConnectionRepository;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {UserAdminApplication.class})
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

    @Test
    void testUserAdmin() throws Exception {
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}/isElevatedUser", USER_SUB))
                .andExpect(status().isForbidden())
                .andReturn();

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}/isElevatedUser", ADMIN_USER))
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

        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
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

        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    void testGetConnections() throws Exception {
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB2)
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
}
