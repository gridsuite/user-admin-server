/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.useradmin.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.dto.UserGroup;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.repository.UserGroupRepository;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.service.RoleService;
import org.gridsuite.useradmin.server.service.DirectoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.gridsuite.useradmin.server.utils.TestConstants.*;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
class UserGroupControllerTest {
    private static final String USER_A = "user_A";
    private static final String USER_B = "user_B";
    private static final String USER_C = "user_C";
    private static final String USER_D = "user_D";
    private static final String USER_E = "user_E";
    private static final String GROUP = "group";
    private static final String GROUP_NEW_NAME = "group_new_name";

    private static final String ADMIN_USER = "admin1";

    @Autowired
    private UserAdminApplicationProps userAdminApplicationProps;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserInfosRepository userInfosRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DirectoryService directoryService;

    @AfterEach
    public void cleanDB() {
        userGroupRepository.deleteAll();
        userInfosRepository.deleteAll();
    }

    private void createGroup(String groupName) throws Exception {
        mockMvc.perform(post(API_BASE_PATH + "/groups/{group}", groupName)
                        .header("userId", ADMIN_USER)
                        .header(RoleService.ROLES_HEADER, userAdminApplicationProps.getAdminRole())
                )
                .andExpect(status().isCreated())
                .andReturn();
    }

    private UserGroup getGroup(String groupName) throws Exception {
        return objectMapper.readValue(
            mockMvc.perform(get(API_BASE_PATH + "/groups/" + groupName)
                    .header("userId", ADMIN_USER)
                    .header(RoleService.ROLES_HEADER, userAdminApplicationProps.getAdminRole())
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() { });
    }

    private void updateGroup(UUID uuidGroupe, String groupName, Set<String> users) throws Exception {
        mockMvc.perform(put(API_BASE_PATH + "/groups/" + uuidGroupe)
                .content(objectMapper.writeValueAsString(new UserGroup(uuidGroupe, groupName, users)))
                .contentType(APPLICATION_JSON)
                .header("userId", ADMIN_USER)
                .header(RoleService.ROLES_HEADER, userAdminApplicationProps.getAdminRole())
            )
            .andExpect(status().isOk())
            .andReturn();
    }

    private Set<UserGroup> getAllGroups() throws Exception {
        return objectMapper.readValue(
            mockMvc.perform(get(API_BASE_PATH + "/groups")
                    .header("userId", ADMIN_USER)
                    .header(RoleService.ROLES_HEADER, userAdminApplicationProps.getAdminRole())
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() { });
    }

    private UserInfos getUserInfos(String userName) throws Exception {
        return objectMapper.readValue(
            mockMvc.perform(get(API_BASE_PATH + "/users/" + userName + "/detail")
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() { });
    }

    private Set<UserGroup> getUserGroups(String userName) throws Exception {
        return objectMapper.readValue(
            mockMvc.perform(get(API_BASE_PATH + "/users/" + userName + "/groups")
                    .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<>() { });
    }

    private void deleteGroups(List<String> groupNames, ResultMatcher resultExpected) throws Exception {
        mockMvc.perform(delete(API_BASE_PATH + "/groups")
                .content(objectMapper.writeValueAsString(groupNames))
                .contentType(APPLICATION_JSON)
                .header("userId", ADMIN_USER)
                .header(RoleService.ROLES_HEADER, userAdminApplicationProps.getAdminRole())
            )
            .andExpect(resultExpected)
            .andReturn();
    }

    private void checkUserGroup(String userName, String groupName) throws Exception {
        UserInfos userInfo = getUserInfos(userName);
        assertEquals(userName, userInfo.sub());
        if (groupName != null) {
            assertEquals(Set.of(groupName), userInfo.groups());
        } else {
            assertTrue(userInfo.groups().isEmpty());
        }
    }

    @Test
    void testGroups() throws Exception {
        // Mock the calls to the directory service and the database
        when(directoryService.getCasesCount(USER_A)).thenReturn(5);

        // Create users USER_A, USER_B, USER_C in database
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_A, null, null));
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_B, null, null));
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_C, null, null));

        // create new group GROUP
        createGroup(GROUP);

        // get group GROUP
        UserGroup group = getGroup(GROUP);
        assertEquals(GROUP, group.name());
        assertTrue(group.users().isEmpty());

        // update group GROUP with users USER_A, USER_B, USER_C
        updateGroup(group.id(), GROUP, Set.of(USER_A, USER_B, USER_C));

        // get all groups
        Set<UserGroup> groups = getAllGroups();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        group = groups.iterator().next();
        assertEquals(GROUP, group.name());
        assertEquals(Set.of(USER_A, USER_B, USER_C), group.users());

        // check users group
        checkUserGroup(USER_A, GROUP);
        checkUserGroup(USER_B, GROUP);
        checkUserGroup(USER_C, GROUP);

        // update the group with new name and new users : USER_A, USER_D, USER_E
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_D, null, null));
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_E, null, null));

        updateGroup(group.id(), GROUP_NEW_NAME, Set.of(USER_A, USER_D, USER_E));

        // check group contains users : USER_A, USER_D, USER_E
        group = getGroup(GROUP_NEW_NAME);
        assertEquals(GROUP_NEW_NAME, group.name());
        assertEquals(Set.of(USER_A, USER_D, USER_E), group.users());

        // check users group
        checkUserGroup(USER_A, GROUP_NEW_NAME);
        checkUserGroup(USER_D, GROUP_NEW_NAME);
        checkUserGroup(USER_E, GROUP_NEW_NAME);
        checkUserGroup(USER_B, null);
        checkUserGroup(USER_C, null);

        Set<UserGroup> userGroups = getUserGroups(USER_E);
        assertEquals(Set.of(GROUP_NEW_NAME), userGroups.stream().map(UserGroup::name).collect(Collectors.toSet()));

        // delete group : error because of users still referencing it
        deleteGroups(List.of(GROUP_NEW_NAME), status().isUnprocessableEntity());

        updateGroup(group.id(), GROUP_NEW_NAME, Set.of()); // this removes all users from group

        // delete group : ok because no more users are referencing it
        deleteGroups(List.of(GROUP_NEW_NAME), status().isNoContent());

        groups = getAllGroups();
        assertTrue(groups.isEmpty());

        //  check users group
        checkUserGroup(USER_A, null);
        checkUserGroup(USER_B, null);
        checkUserGroup(USER_C, null);
        checkUserGroup(USER_D, null);
        checkUserGroup(USER_E, null);
    }
}
