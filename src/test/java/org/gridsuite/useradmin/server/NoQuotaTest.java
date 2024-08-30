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
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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

    private static final String PROFILE_TWO = "profile_two";

    private static final String USER_SUB = "user_one";

    private static final String USER_SUB_TWO = "user_two";

    private static final String API_BASE_PATH = "/" + UserAdminApi.API_VERSION;

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
    void testProfileCreation() {
        createProfile(PROFILE_ONE, null, null);
        // test with quotas
        createProfile(PROFILE_TWO, 10, 20);
    }

    @Test
    @SneakyThrows
    void testUserCreationWithoutProfile() {
        createUser(USER_SUB);

        assertTrue(getMaxAllowedBuilds(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedCases(USER_SUB).isEmpty());
    }

    @Test
    @SneakyThrows
    void testUserCreationWithProfile() {
        //profile with no quotas
        createProfile(PROFILE_ONE, null, null);
        createUser(USER_SUB);
        associateProfileToUser(USER_SUB, PROFILE_ONE);

        assertTrue(getMaxAllowedBuilds(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedCases(USER_SUB).isEmpty());

        //profile with quotas
        createProfile(PROFILE_TWO, 10, 20);
        createUser(USER_SUB_TWO);
        associateProfileToUser(USER_SUB_TWO, PROFILE_TWO);

        assertEquals("10", getMaxAllowedCases(USER_SUB_TWO));
        assertEquals("20", getMaxAllowedBuilds(USER_SUB_TWO));
    }

    @SneakyThrows
    private void createProfile(String profileName, Integer maxAllowedCases, Integer maxAllowedBuilds) {
        UserProfile profileInfo = new UserProfile(null, profileName, null, false, maxAllowedCases, maxAllowedBuilds);
        performPost(API_BASE_PATH + "/profiles", profileInfo);

        Optional<UserProfileEntity> createdProfile = userProfileRepository.findByName(profileName);
        assertTrue(createdProfile.isPresent());
        assertNull(createdProfile.get().getLoadFlowParameterId());
        assertEquals(maxAllowedCases, createdProfile.get().getMaxAllowedCases());
        assertEquals(maxAllowedBuilds, createdProfile.get().getMaxAllowedBuilds());
    }

    @SneakyThrows
    private void createUser(String userSub) {
        performPost(API_BASE_PATH + "/users/" + userSub, null);

        // check user creation
        UserInfos userInfos = getUserInfos(userSub);
        assertNotNull(userInfos);
        assertNull(userInfos.profileName());
        assertEquals(userSub, userInfos.sub());
    }

    @SneakyThrows
    private UserInfos getUserInfos(String userSub) {
        MvcResult result = performGet(API_BASE_PATH + "/users/" + userSub);
        return objectMapper.readValue(result.getResponse().getContentAsString(), UserInfos.class);
    }

    @SneakyThrows
    private void associateProfileToUser(String userSub, String profileName) {
        UserInfos userInfos = new UserInfos(userSub, false, profileName);
        performPut(API_BASE_PATH + "/users/" + userSub, userInfos);
    }

    @SneakyThrows
    private String getMaxAllowedBuilds(String userSub) {
        MvcResult result = performGet(API_BASE_PATH + "/users/" + userSub + "/profile/max-builds");
        return result.getResponse().getContentAsString();
    }

    @SneakyThrows
    private String getMaxAllowedCases(String userSub) {
        MvcResult result = performGet(API_BASE_PATH + "/users/" + userSub + "/profile/max-cases");
        return result.getResponse().getContentAsString();
    }

    @SneakyThrows
    private void performPost(String url, Object content) {
        mockMvc.perform(post(url)
                        .content(content != null ? objectWriter.writeValueAsString(content) : "")
                        .contentType(APPLICATION_JSON)
                        .header("userId", ADMIN_USER))
                .andExpect(status().isCreated());
    }

    @SneakyThrows
    private void performPut(String url, Object content) {
        mockMvc.perform(put(url)
                        .content(objectWriter.writeValueAsString(content))
                        .contentType(APPLICATION_JSON)
                        .header("userId", ADMIN_USER))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private MvcResult performGet(String url) {
        return mockMvc.perform(get(url)
                        .header("userId", ADMIN_USER))
                .andExpect(status().isOk())
                .andReturn();
    }
}
