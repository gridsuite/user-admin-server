/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;

import static org.gridsuite.useradmin.server.Utils.ROLES_HEADER;
import static org.gridsuite.useradmin.server.utils.TestConstants.*;
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
    void setUp() {
        objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
    }

    @AfterEach
    void cleanDB() {
        userInfosRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void testProfileCreation() throws Exception {
        createProfile(PROFILE_ONE, null, null);
        // test with quotas
        createProfile(PROFILE_TWO, 10, 20);
    }

    @Test
    void testUserCreationWithoutProfile() throws Exception {
        createUser(USER_SUB);

        assertTrue(getMaxAllowedBuilds(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedCases(USER_SUB).isEmpty());
    }

    @Test
    void testUserCreationWithProfile() throws Exception {
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

    private void createProfile(String profileName, Integer maxAllowedCases, Integer maxAllowedBuilds) throws Exception {
        UserProfile profileInfo = new UserProfile(null, profileName, null, null, null, null, null, false, maxAllowedCases, maxAllowedBuilds, null, null);
        performPost(API_BASE_PATH + "/profiles", profileInfo);

        Optional<UserProfileEntity> createdProfile = userProfileRepository.findByName(profileName);
        assertTrue(createdProfile.isPresent());
        assertNull(createdProfile.get().getLoadFlowParameterId());
        assertNull(createdProfile.get().getSecurityAnalysisParameterId());
        assertNull(createdProfile.get().getSensitivityAnalysisParameterId());
        assertNull(createdProfile.get().getShortcircuitParameterId());
        assertNull(createdProfile.get().getVoltageInitParameterId());
        assertEquals(maxAllowedCases, createdProfile.get().getMaxAllowedCases());
        assertEquals(maxAllowedBuilds, createdProfile.get().getMaxAllowedBuilds());
        assertNull(createdProfile.get().getSpreadsheetConfigCollectionId());
        assertNull(createdProfile.get().getNetworkVisualizationParameterId());
    }

    private void createUser(String userSub) throws Exception {
        performPost(API_BASE_PATH + "/users/" + userSub, null);

        // check user creation
        UserInfos userInfos = getUserInfos(userSub);
        assertNotNull(userInfos);
        assertNull(userInfos.profileName());
        assertEquals(userSub, userInfos.sub());
    }

    private UserInfos getUserInfos(String userSub) throws Exception {
        MvcResult result = performGet(API_BASE_PATH + "/users/" + userSub);
        return objectMapper.readValue(result.getResponse().getContentAsString(), UserInfos.class);
    }

    private void associateProfileToUser(String userSub, String profileName) throws Exception {
        UserInfos userInfos = new UserInfos(userSub, profileName, null, null, null, null);
        performPut(API_BASE_PATH + "/users/" + userSub, userInfos);
    }

    private String getMaxAllowedBuilds(String userSub) throws Exception {
        MvcResult result = performGet(API_BASE_PATH + "/users/" + userSub + "/profile/max-builds");
        return result.getResponse().getContentAsString();
    }

    private String getMaxAllowedCases(String userSub) throws Exception {
        MvcResult result = performGet(API_BASE_PATH + "/users/" + userSub + "/profile/max-cases");
        return result.getResponse().getContentAsString();
    }

    private void performPost(String url, Object content) throws Exception {
        mockMvc.perform(post(url)
                        .content(content != null ? objectWriter.writeValueAsString(content) : "")
                        .contentType(APPLICATION_JSON)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE))
                .andExpect(status().isCreated());
    }

    private void performPut(String url, Object content) throws Exception {
        mockMvc.perform(put(url)
                        .content(objectWriter.writeValueAsString(content))
                        .contentType(APPLICATION_JSON)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE))
                .andExpect(status().isOk());
    }

    private MvcResult performGet(String url) throws Exception {
        return mockMvc.perform(get(url)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE))
                .andExpect(status().isOk())
                .andReturn();
    }
}
