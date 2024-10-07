/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.useradmin.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.useradmin.server.UserAdminApi;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.gridsuite.useradmin.server.service.DirectoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anis Touri <anis.touri at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
class UserInfosController {

    private static final String PROFILE_A = "profile_A";
    private static final String USER_A = "user_A";

    private static final String API_BASE_PATH = "/" + UserAdminApi.API_VERSION;

    @Autowired
    UserProfileRepository userProfileRepository;
    @Autowired
    private UserInfosRepository userInfosRepository;

    @MockBean
    private DirectoryService directoryService;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    public void cleanDB() {
        userInfosRepository.deleteAll();
    }

    @Test
    void getUserDetail() throws Exception {
        // Create a profile
        UserProfileEntity profileEntity = new UserProfileEntity(UUID.randomUUID(), PROFILE_A, null, 10, 20);
        userProfileRepository.save(profileEntity);
        // Create a user
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), USER_A, profileEntity);
        userInfosRepository.save(userInfosEntity);

        // Mock the calls to the directory service and the database
        when(directoryService.getCasesCount(USER_A)).thenReturn(5);

        UserInfos userInfos = objectMapper.readValue(
                mockMvc.perform(head(API_BASE_PATH + "/users/{sub}/detail", USER_A))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), UserInfos.class);
        assertNotNull(userInfos);
        assertEquals(USER_A, userInfos.sub());
        assertEquals(false, userInfos.isAdmin());
        assertEquals(PROFILE_A, userInfos.profileName());
        assertEquals(10, userInfos.maxAllowedCases());
        assertEquals(5, userInfos.numberCasesUsed());
        assertEquals(20, userInfos.maxAllowedBuilds());

    }
}
