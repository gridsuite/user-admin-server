/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.SneakyThrows;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Achour Berrahma <achour.berrahma at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {UserAdminApplication.class})
@ActiveProfiles({"default", "noquota"})
class NoQuotaTest {
    private static final String ADMIN_USER = "admin1";

    private static final String PROFILE_ONE = "profile_one";

    private static final String USER_SUB = "user_one";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserInfosRepository userInfosRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ObjectWriter objectWriter;

    @BeforeEach
    public void setUp() {
        objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
    }

    @AfterEach
    public void cleanDB() {
        userInfosRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    void testProfileCreationAndQuotas() {
        UserProfile profileInfo = new UserProfile(null, PROFILE_ONE, null, false, null, null);
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(profileInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().is(HttpStatus.CREATED.value()))
                .andReturn();

        Optional<UserProfileEntity> profile1 = userProfileRepository.findByName(PROFILE_ONE);
        assertTrue(profile1.isPresent());
        assertNull(profile1.get().getLoadFlowParameterId());
        assertNull(profile1.get().getMaxAllowedCases());
        assertNull(profile1.get().getMaxAllowedBuilds());

        // create a user with the profile
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();

        // test default quotas for cases and builds when not provided
        MvcResult buildsResult = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-builds", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isOk())
                .andReturn();
        String content = buildsResult.getResponse().getContentAsString();
        assertTrue(content.isEmpty() || content.equals("null"));

        MvcResult casesResult = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-cases", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isOk())
                .andReturn();
        content = casesResult.getResponse().getContentAsString();
        assertTrue(content.isEmpty() || content.equals("null"));
    }

    @Test
    @SneakyThrows
    void testGetUserQuotasWithNoProfileSet() {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), USER_SUB, null);
        userInfosRepository.save(userInfosEntity);

        MvcResult casesResult = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-cases", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isOk())
                .andReturn();
        String content = casesResult.getResponse().getContentAsString();
        assertTrue(content.isEmpty() || content.equals("null"));

        MvcResult buildsResult = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-builds", USER_SUB)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isOk())
                .andReturn();
        content = buildsResult.getResponse().getContentAsString();
        assertTrue(content.isEmpty() || content.equals("null"));
    }

}
