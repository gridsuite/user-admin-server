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
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.useradmin.server.dto.UserGroup;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.ConnectionRepository;
import org.gridsuite.useradmin.server.repository.UserGroupRepository;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.gridsuite.useradmin.server.Utils.ROLES_HEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.gridsuite.useradmin.server.utils.TestConstants.*;

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
    private UserInfosRepository userInfosRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @AfterEach
    void cleanDB() {
        userGroupRepository.deleteAll();
        userInfosRepository.deleteAll();
        userProfileRepository.deleteAll();
        connectionRepository.deleteAll();
    }

    private static final String USER_SUB = "user1";
    private static final String USER_SUB2 = "user2";
    private static final String USER_UNKNOWN = "UNKNOWN";
    private static final String ADMIN_USER = "admin1";
    private static final String NOT_ADMIN = "notAdmin";
    private static final String PROFILE_1 = "profile_1";
    private static final String GROUP_1 = "group_1";
    private static final String GROUP_2 = "group_2";

    @Test
    void testUserAdmin() throws Exception {

        List<UserInfos> userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });

        assertEquals(0, userInfos.size());

        mockMvc.perform(head(RECORD_CONNECTION_URL, ADMIN_USER)
                        .param(IS_CONNECTION_ACCEPTED_PARAM, "true"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                )
                .andExpect(status().isCreated())
                .andReturn();

        userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });

        assertEquals(1, userInfos.size());

        mockMvc.perform(head(RECORD_CONNECTION_URL, USER_SUB)
                        .param(IS_CONNECTION_ACCEPTED_PARAM, "true"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(head(RECORD_CONNECTION_URL, USER_UNKNOWN)
                        .param(IS_CONNECTION_ACCEPTED_PARAM, "false"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(3, connectionRepository.findAll().size());
        assertTrue(connectionRepository.findBySub(USER_SUB).get(0).getConnectionAccepted());
        assertFalse(connectionRepository.findBySub(USER_UNKNOWN).get(0).getConnectionAccepted());
        LocalDateTime firstConnectionDate = connectionRepository.findBySub(USER_SUB).get(0).getFirstConnexionDate();
        //firstConnectionDate and lastConnectionDate are equals cause this is the first connection for this user
        assertTrue(firstConnectionDate.toEpochSecond(ZoneOffset.UTC) < connectionRepository.findBySub(USER_SUB).get(0).getLastConnexionDate().toEpochSecond(ZoneOffset.UTC) + 2);

        mockMvc.perform(head(RECORD_CONNECTION_URL, USER_SUB)
                        .param(IS_CONNECTION_ACCEPTED_PARAM, "true"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(firstConnectionDate, connectionRepository.findBySub(USER_SUB).get(0).getFirstConnexionDate());

        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                )
                .andExpect(status().isNoContent())
                .andReturn();

        userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
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
                        .content("[\"" + USER_SUB + "\"]"))
                .andExpect(status().isForbidden())
                .andReturn();

        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users")
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andReturn();

        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/users")
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON)
                        .content("[\"" + USER_UNKNOWN + "\"]"))
                .andExpect(status().isNotFound())
                .andReturn();

        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/cases-alert-threshold")
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void testUpdateUser() throws Exception {
        createUser(USER_SUB);
        createProfile(PROFILE_1);
        createGroup(GROUP_1);
        createGroup(GROUP_2);

        // udpate the user: change its name and link it to the profile and to the first group
        UserInfos userInfo = new UserInfos(USER_SUB2, PROFILE_1, null, null, null, Set.of(GROUP_1));
        updateUserWithAdmin(USER_SUB, userInfo, HttpStatus.OK);

        // Get and check user profile
        UserProfile userProfile = getUserProfile(USER_SUB2, HttpStatus.OK);
        assertNotNull(userProfile);
        assertEquals(PROFILE_1, userProfile.name());

        // Get and check user groups
        List<UserGroup> userGroups = getUserGroups(USER_SUB2, HttpStatus.OK);
        assertEquals(1, CollectionUtils.size(userGroups));
        assertEquals(GROUP_1, userGroups.get(0).name());

        // udpate the user: change groups
        userInfo = new UserInfos(USER_SUB2, PROFILE_1, null, null, null, Set.of(GROUP_2));
        updateUserWithAdmin(USER_SUB2, userInfo, HttpStatus.OK);

        // Get and check user groups
        userGroups = getUserGroups(USER_SUB2, HttpStatus.OK);
        assertEquals(1, CollectionUtils.size(userGroups));
        assertEquals(GROUP_2, userGroups.get(0).name());
    }

    @Test
    void testUpdateUserNotFound() throws Exception {
        updateUserWithAdmin("nofFound", new UserInfos("nofFound", "prof", null, null, null, null), HttpStatus.NOT_FOUND);
    }

    @Test
    void testUpdateUserForbidden() throws Exception {
        updateUserWithNotAdmin("dummy", new UserInfos("dummy", "prof", null, null, null, null));
    }

    @Test
    void testGetUserProfileNotFound() throws Exception {
        getUserProfile("BadUser", HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetConnections() throws Exception {
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                )
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB2)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                )
                .andExpect(status().isCreated())
                .andReturn();

        List<UserInfos> userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });

        assertEquals(2, userInfos.size());

        mockMvc.perform(head(RECORD_CONNECTION_URL, USER_SUB)
                        .param(IS_CONNECTION_ACCEPTED_PARAM, "true"))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(head(RECORD_CONNECTION_URL, USER_SUB2)
                        .param(IS_CONNECTION_ACCEPTED_PARAM, "true"))
                .andExpect(status().isOk())
                .andReturn();

        List<ConnectionEntity> connectionEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/connections")
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });

        assertEquals(2, connectionEntities.size());

        connectionRepository.save(new ConnectionEntity(USER_SUB, LocalDateTime.now(), LocalDateTime.now(), true));
        connectionRepository.save(new ConnectionEntity(USER_SUB, LocalDateTime.now().minusSeconds(5), LocalDateTime.now(), true));
        connectionRepository.save(new ConnectionEntity(USER_SUB2, LocalDateTime.now(), LocalDateTime.now(), false));

        connectionEntities = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/connections")
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
        assertEquals(2, connectionEntities.size());

        mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/connections")
                        .header("userId", NOT_ADMIN)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private void createUser(String userName) throws Exception {
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", userName)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                )
                .andExpect(status().isCreated())
                .andReturn();
        UserInfos userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}", userName)
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
        // the new user has no profile by default
        assertNotNull(userInfos);
        assertNull(userInfos.profileName());
        assertEquals(userName, userInfos.sub());

        // user already exists
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", userName)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE))
            .andExpect(status().isBadRequest());
    }

    private void createProfile(String profileName) throws Exception {
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        UserProfile profileInfo = new UserProfile(null, profileName, null, null, null, null, null, false, null, null, null, null);
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(profileInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                )
                .andExpect(status().isCreated())
                .andReturn();
    }

    private void createGroup(String groupName) throws Exception {
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/groups/{name}", groupName)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                )
                .andExpect(status().isCreated())
                .andReturn();
        UserGroup groupInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/groups/{name}", groupName)
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
        // the new group has no users by default
        assertNotNull(groupInfos);
        assertTrue(CollectionUtils.isEmpty(groupInfos.users()));
    }

    private void updateUserWithAdmin(String updatedUserName, UserInfos userInfos, HttpStatusCode status) throws Exception {
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{sub}", updatedUserName)
                        .content(objectWriter.writeValueAsString(userInfos))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE))
                .andExpect(status().is(status.value()));

        if (status == HttpStatus.OK) {
            UserInfos updatedUserInfos = objectMapper.readValue(
                    mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}", userInfos.sub())
                                    .header("userId", ADMIN_USER)
                                    .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                    .contentType(APPLICATION_JSON))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    new TypeReference<>() { });
            // the new user has the new name and profile
            assertNotNull(updatedUserInfos);
            assertEquals(userInfos.sub(), updatedUserInfos.sub());
            assertEquals(userInfos.profileName(), updatedUserInfos.profileName());
        }
    }

    private void updateUserWithNotAdmin(String updatedUserName, UserInfos userInfos) throws Exception {
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/users/{sub}", updatedUserName)
                        .content(objectWriter.writeValueAsString(userInfos))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", NOT_ADMIN))
                .andExpect(status().isForbidden());
    }

    private UserProfile getUserProfile(String userName, HttpStatusCode status) throws Exception {
        String response = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/" + userName + "/profile")
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().is(status.value()))
                        .andReturn().getResponse().getContentAsString();
        if (status == HttpStatus.OK) {
            return objectMapper.readValue(response, new TypeReference<>() { });
        }
        return null;
    }

    private List<UserGroup> getUserGroups(String userName, HttpStatusCode status) throws Exception {
        String response = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/" + userName + "/groups")
                .contentType(APPLICATION_JSON))
            .andExpect(status().is(status.value()))
            .andReturn().getResponse().getContentAsString();
        if (status == HttpStatus.OK) {
            return objectMapper.readValue(response, new TypeReference<>() { });
        }
        return null;
    }
}
