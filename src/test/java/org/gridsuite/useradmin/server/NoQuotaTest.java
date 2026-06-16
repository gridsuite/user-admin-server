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

import static org.gridsuite.useradmin.server.Utils.ROLES_HEADER;
import static org.gridsuite.useradmin.server.utils.TestConstants.*;
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
        createProfile(PROFILE_ONE, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null, null);
        // test with quotas
        createProfile(PROFILE_TWO, 10, 20, 2, 2, 1,
                1, 2, 1, 1,
                1, 1, 1, 1);
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
        createProfile(PROFILE_ONE, null, null, null,
                null, null, null, null,
                null, null, null,
                null, null, null);
        createUser(USER_SUB);
        associateProfileToUser(USER_SUB, PROFILE_ONE);

        assertTrue(getMaxAllowedBuilds(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedCases(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedLoadflow(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedSecurity(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedSensitivity(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedShortCircuit(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedVoltageInit(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedPccMin(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedStateEstimation(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedBalanceAdjustement(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedDynamicSimulation(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedDynamicSecurity(USER_SUB).isEmpty());
        assertTrue(getMaxAllowedDynamicMargin(USER_SUB).isEmpty());

        //profile with quotas
        createProfile(PROFILE_TWO, 10, 20, 2, 2, 1,
                1, 2, 1, 1,
                1, 1, 1, 1);
        createUser(USER_SUB_TWO);
        associateProfileToUser(USER_SUB_TWO, PROFILE_TWO);

        assertEquals("10", getMaxAllowedCases(USER_SUB_TWO));
        assertEquals("20", getMaxAllowedBuilds(USER_SUB_TWO));
        assertEquals("2", getMaxAllowedLoadflow(USER_SUB_TWO));
        assertEquals("2", getMaxAllowedSecurity(USER_SUB_TWO));
        assertEquals("1", getMaxAllowedSensitivity(USER_SUB_TWO));
        assertEquals("1", getMaxAllowedShortCircuit(USER_SUB_TWO));
        assertEquals("2", getMaxAllowedVoltageInit(USER_SUB_TWO));
        assertEquals("1", getMaxAllowedPccMin(USER_SUB_TWO));
        assertEquals("1", getMaxAllowedStateEstimation(USER_SUB_TWO));
        assertEquals("1", getMaxAllowedBalanceAdjustement(USER_SUB_TWO));
        assertEquals("1", getMaxAllowedDynamicSimulation(USER_SUB_TWO));
        assertEquals("1", getMaxAllowedDynamicSecurity(USER_SUB_TWO));
        assertEquals("1", getMaxAllowedDynamicMargin(USER_SUB_TWO));
    }

    private void createProfile(String profileName, Integer maxAllowedCases, Integer maxAllowedBuilds, Integer maxAllowedLoadflow,
                               Integer maxAllowedSecurity, Integer maxAllowedSensitivity, Integer maxAllowedShortCircuit,
                               Integer maxAllowedVoltageInit, Integer maxAllowedPccMin, Integer maxAllowedStateEstimation,
                               Integer maxAllowedBalanceAdjustement, Integer maxAllowedDynamicSimulation, Integer maxAllowedDynamicSecurity,
                               Integer maxAllowedDynamicMargin) throws Exception {
        UserProfile profileInfo = new UserProfile(null, profileName, null, null,
                null, null, null, null, false,
                maxAllowedCases, maxAllowedBuilds, maxAllowedLoadflow, maxAllowedSecurity, maxAllowedSensitivity, maxAllowedShortCircuit,
                maxAllowedVoltageInit, maxAllowedPccMin, maxAllowedStateEstimation, maxAllowedBalanceAdjustement, maxAllowedDynamicSimulation,
                maxAllowedDynamicSecurity, maxAllowedDynamicMargin, null, null, null);
        performPost(API_BASE_PATH + "/profiles", profileInfo);

        Optional<UserProfileEntity> createdProfile = userProfileRepository.findByName(profileName);
        assertTrue(createdProfile.isPresent());
        assertNull(createdProfile.get().getLoadFlowParameterId());
        assertNull(createdProfile.get().getSecurityAnalysisParameterId());
        assertNull(createdProfile.get().getSensitivityAnalysisParameterId());
        assertNull(createdProfile.get().getShortcircuitParameterId());
        assertNull(createdProfile.get().getPccminParameterId());
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
        UserInfos userInfos = new UserInfos(userSub, null, null, profileName, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null);
        performPut(API_BASE_PATH + "/users/" + userSub, userInfos);
    }

    private String getMaxAllowedBuilds(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-builds");
    }

    private String getMaxAllowedCases(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-cases");
    }

    private String getMaxAllowedLoadflow(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-loadflow");
    }

    private String getMaxAllowedSecurity(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-security");
    }

    private String getMaxAllowedSensitivity(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-sensitivity");
    }

    private String getMaxAllowedShortCircuit(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-shortcircuit");
    }

    private String getMaxAllowedVoltageInit(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-voltage-init");
    }

    private String getMaxAllowedPccMin(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-pcc-min");
    }

    private String getMaxAllowedStateEstimation(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-state-estimation");
    }

    private String getMaxAllowedBalanceAdjustement(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-balance-adjustement");
    }

    private String getMaxAllowedDynamicSimulation(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-dynamic-simulation");
    }

    private String getMaxAllowedDynamicSecurity(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-dynamic-security");
    }

    private String getMaxAllowedDynamicMargin(String userSub) throws Exception {
        return getMaxAllowedElement(userSub, "max-dynamic-margin");
    }

    private String getMaxAllowedElement(String userSub, String elementName) throws Exception {
        MvcResult result = performGet(API_BASE_PATH + "/users/" + userSub + "/profile/" + elementName);
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
