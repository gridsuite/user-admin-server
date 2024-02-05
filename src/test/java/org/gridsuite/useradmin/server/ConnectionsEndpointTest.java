/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.service.ConnectionsService;
import org.gridsuite.useradmin.server.service.UserAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.gridsuite.useradmin.server.TestsTools.*;
import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAdminController.class)
@ExtendWith({ SpringExtension.class, MockitoExtension.class })
class ConnectionsEndpointTest {
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ConnectionsService connectionsService;

    @MockBean
    UserAdminService userAdminService; //because dep of controller

    @AfterEach
    public void verifyMocks() {
        Mockito.verifyNoInteractions(userAdminService);
        Mockito.verifyNoMoreInteractions(connectionsService);
    }

    @Test
    void testGetConnections() throws Exception {
        Mockito.when(connectionsService.getConnections(ADMIN_USER)).thenReturn(List.of(
                new UserConnection(USER_SUB, Instant.now(), Instant.now(), false),
                new UserConnection(USER_SUB2, Instant.now(), Instant.now(), false)
        ));
        List<ConnectionEntity> connectionEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/connections")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
        assertThat(connectionEntities).hasSize(2);
        Mockito.verify(connectionsService, Mockito.only()).getConnections(ADMIN_USER);
    }

    @Test
    void testGetConnectionsWhenNonAdmin() throws Exception {
        Mockito.when(connectionsService.getConnections(NOT_ADMIN)).thenThrow(new UserAdminException(FORBIDDEN));
        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/connections")
                        .header("userId", NOT_ADMIN)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
        Mockito.verify(connectionsService, Mockito.only()).getConnections(NOT_ADMIN);
    }
}
