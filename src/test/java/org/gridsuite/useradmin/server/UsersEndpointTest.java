/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.service.ConnectionsService;
import org.gridsuite.useradmin.server.service.UserAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.gridsuite.useradmin.server.TestsTools.*;
import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAdminController.class)
@ExtendWith({ SpringExtension.class, MockitoExtension.class })
@TestMethodOrder(MethodOrderer.MethodName.class)
class UsersEndpointTest {
    private final UserAdminException notAdminException = new UserAdminException(FORBIDDEN);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserAdminService userAdminService;

    @MockBean
    ConnectionsService connectionsService;

    @AfterEach
    public void verifyMocks() {
        Mockito.verifyNoInteractions(connectionsService);
        Mockito.verifyNoMoreInteractions(userAdminService);
    }

    @Test
    void testCheckUserNotAuthorized() throws Exception {
        Mockito.when(userAdminService.subExists(USER_NOT_REGISTERED)).thenReturn(false);
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", "NOT_REGISTERED_USER"))
                .andExpect(status().isNoContent())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).subExists(USER_NOT_REGISTERED);
    }

    @Test
    void testCheckUser() throws Exception {
        Mockito.when(userAdminService.subExists(USER_SUB)).thenReturn(true);
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB))
                .andExpect(status().isOk())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).subExists(USER_SUB);
    }

    @Test
    void testCheckUserIsAdmin() throws Exception {
        Mockito.when(userAdminService.userIsAuthorizedAdmin(ADMIN_USER)).thenReturn(true);
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/me/isElevatedUser")
                        .header("userId", ADMIN_USER))
                .andExpect(status().isOk())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).userIsAuthorizedAdmin(ADMIN_USER);
    }

    @Test
    void testCheckUserIsAdminWhenNotAdmin() throws Exception {
        Mockito.when(userAdminService.userIsAuthorizedAdmin(NOT_ADMIN)).thenReturn(false);
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/me/isElevatedUser")
                        .header("userId", NOT_ADMIN))
                .andExpect(status().isForbidden())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).userIsAuthorizedAdmin(NOT_ADMIN);
    }

    @Test
    void testGetUsersWhenDbEmpty() throws Exception {
        Mockito.when(userAdminService.getUsers(ADMIN_USER)).thenReturn(Collections.emptyList());
        List<UserInfos> userEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
        assertThat(userEntities).isEmpty();
        Mockito.verify(userAdminService, Mockito.only()).getUsers(ADMIN_USER);
    }

    @Test
    void testGetUsers() throws Exception {
        Mockito.when(userAdminService.getUsers(ADMIN_USER)).thenReturn(List.of(
                new UserInfos(USER_SUB, false)
        ));
        List<UserInfos> userEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
        assertThat(userEntities).hasSize(1);
        Mockito.verify(userAdminService, Mockito.only()).getUsers(ADMIN_USER);
    }

    @Test
    void testGetUsersAsNonAdmin() throws Exception {
        Mockito.when(userAdminService.getUsers(NOT_ADMIN)).thenThrow(notAdminException);
        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).getUsers(NOT_ADMIN);
    }

    @Test
    void testGetUser() throws Exception {
        Mockito.when(userAdminService.getUser(USER_SUB, ADMIN_USER)).thenReturn(Optional.of(new UserInfos(USER_SUB, false)));
        UserInfos userEntity = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
        assertThat(userEntity).isNotNull();
        Mockito.verify(userAdminService, Mockito.only()).getUser(USER_SUB, ADMIN_USER);
    }

    @Test
    void testGetUserNotExisting() throws Exception {
        Mockito.when(userAdminService.getUser(USER_SUB, ADMIN_USER)).thenReturn(Optional.empty());
        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isNotFound())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).getUser(USER_SUB, ADMIN_USER);
    }

    @Test
    void testGetUserAsNonAdmin() throws Exception {
        Mockito.when(userAdminService.getUser(USER_SUB, NOT_ADMIN)).thenThrow(notAdminException);
        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).getUser(USER_SUB, NOT_ADMIN);
    }

    @Test
    void testSearchUsers() throws Exception {
        Mockito.when(userAdminService.searchUsers(USER_SEARCH, ADMIN_USER)).thenReturn(List.of(
                new UserInfos(USER_SUB, false),
                new UserInfos(USER_SUB2, false)
        ));
        List<UserInfos> userEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users", USER_SUB)
                                .queryParam("search", USER_SEARCH)
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
        assertThat(userEntities).hasSize(2);
        Mockito.verify(userAdminService, Mockito.only()).searchUsers(USER_SEARCH, ADMIN_USER);
    }

    @Test
    void testSearchUsersAsNonAdmin() throws Exception {
        Mockito.when(userAdminService.searchUsers(USER_SEARCH, NOT_ADMIN)).thenThrow(notAdminException);
        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users", USER_SUB)
                        .queryParam("search", USER_SEARCH)
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).searchUsers(USER_SEARCH, NOT_ADMIN);
    }

    @Test
    void testCreateUser() throws Exception {
        Mockito.doNothing().when(userAdminService).createUser(USER_SUB, ADMIN_USER);
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).createUser(USER_SUB, ADMIN_USER);
    }

    @Test
    void testCreateUserAsNonAdmin() throws Exception {
        Mockito.doThrow(notAdminException).when(userAdminService).createUser(USER_SUB, NOT_ADMIN);
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).createUser(USER_SUB, NOT_ADMIN);
    }

    @Test
    void testDeleteUser() throws Exception {
        Mockito.when(userAdminService.delete(USER_SUB, ADMIN_USER)).thenReturn(1L);
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isNoContent())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).delete(USER_SUB, ADMIN_USER);
    }

    @Test
    void testDeleteUserNotExisting() throws Exception {
        Mockito.when(userAdminService.delete(USER_SUB, ADMIN_USER)).thenReturn(0L);
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isNotFound())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).delete(USER_SUB, ADMIN_USER);
    }

    @Test
    void testDeleteUserAsNonAdmin() throws Exception {
        Mockito.when(userAdminService.delete(USER_SUB, NOT_ADMIN)).thenThrow(notAdminException);
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users/{id}", USER_SUB)
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();
        Mockito.verify(userAdminService, Mockito.only()).delete(USER_SUB, NOT_ADMIN);
    }
}
