/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.gridsuite.useradmin.server.Utils.ROLES_HEADER;
import static org.gridsuite.useradmin.server.dto.QuotaType.*;
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
        createProfile();
        // test with quotas
        createProfile(PROFILE_TWO, getMaxAllowedCases());
    }

    @Test
    void testUserCreationWithoutProfile() throws Exception {
        createUser(USER_SUB);

        assertNull(getMaxAllowedBuilds(USER_SUB));
        assertNull(getMaxAllowedCases(USER_SUB));
    }

    @Test
    void testUserCreationWithProfile() throws Exception {
        //profile with no quotas
        createProfile();
        createUser(USER_SUB);
        associateProfileToUser(USER_SUB, PROFILE_ONE);

        assertNull(getMaxAllowedBuilds(USER_SUB));
        assertNull(getMaxAllowedCases(USER_SUB));

        // check all types via the new unified /quota/max endpoint
        Map<QuotaType, Integer> quota = getMaxQuotaMap(USER_SUB);
        assertTrue(quota.values().stream().allMatch(v -> v == null));

        Map<QuotaType, Integer> maxAllowedCases = getMaxAllowedCases();

        //profile with quotas
        createProfile(PROFILE_TWO, maxAllowedCases);
        createUser(USER_SUB_TWO);
        associateProfileToUser(USER_SUB_TWO, PROFILE_TWO);

        // check all types via the new unified /quota/max endpoint
        Map<QuotaType, Integer> quotaTwo = getMaxQuotaMap(USER_SUB_TWO);
        assertEquals(10, quotaTwo.get(CASES));
        assertEquals(20, quotaTwo.get(BUILD));
        assertEquals(2, quotaTwo.get(LOAD_FLOW));
        assertEquals(2, quotaTwo.get(SECURITY_ANALYSIS));
        assertEquals(1, quotaTwo.get(SENSITIVITY_ANALYSIS));
        assertEquals(1, quotaTwo.get(SHORT_CIRCUIT));
        assertEquals(2, quotaTwo.get(VOLTAGE_INITIALIZATION));
        assertEquals(1, quotaTwo.get(PCC_MIN));
        assertEquals(1, quotaTwo.get(STATE_ESTIMATION));
        assertEquals(1, quotaTwo.get(BALANCE_ADJUSTMENT));
        assertEquals(1, quotaTwo.get(DYNAMIC_SIMULATION));
        assertEquals(1, quotaTwo.get(DYNAMIC_SECURITY_ANALYSIS));
        assertEquals(1, quotaTwo.get(DYNAMIC_MARGIN_CALCULATION));
    }

    private static @NonNull Map<QuotaType, Integer> getMaxAllowedCases() {
        Map<QuotaType, Integer> maxAllowedCases = new EnumMap<>(QuotaType.class);
        maxAllowedCases.put(CASES, 10);
        maxAllowedCases.put(BUILD, 20);
        maxAllowedCases.put(LOAD_FLOW, 2);
        maxAllowedCases.put(SECURITY_ANALYSIS, 2);
        maxAllowedCases.put(SENSITIVITY_ANALYSIS, 1);
        maxAllowedCases.put(SHORT_CIRCUIT, 1);
        maxAllowedCases.put(VOLTAGE_INITIALIZATION, 2);
        maxAllowedCases.put(PCC_MIN, 1);
        maxAllowedCases.put(STATE_ESTIMATION, 1);
        maxAllowedCases.put(BALANCE_ADJUSTMENT, 1);
        maxAllowedCases.put(DYNAMIC_SIMULATION, 1);
        maxAllowedCases.put(DYNAMIC_SECURITY_ANALYSIS, 1);
        maxAllowedCases.put(DYNAMIC_MARGIN_CALCULATION, 1);
        return maxAllowedCases;
    }

    private void createProfile() throws Exception {
        createProfile(NoQuotaTest.PROFILE_ONE, new EnumMap<>(QuotaType.class));
    }

    private void createProfile(String profileName, Map<QuotaType, Integer> maxAllowedValues) throws Exception {
        UserProfile profileInfo = UserProfile.builder().name(profileName).allLinksValid(false).maxOperationQuota(maxAllowedValues).build();
        performPost(API_BASE_PATH + "/profiles", profileInfo);

        Optional<UserProfileEntity> createdProfile = userProfileRepository.findByName(profileName);
        assertTrue(createdProfile.isPresent());
        assertNull(createdProfile.get().getLoadFlowParameterId());
        assertNull(createdProfile.get().getSecurityAnalysisParameterId());
        assertNull(createdProfile.get().getSensitivityAnalysisParameterId());
        assertNull(createdProfile.get().getShortcircuitParameterId());
        assertNull(createdProfile.get().getPccminParameterId());
        assertNull(createdProfile.get().getVoltageInitParameterId());
        assertEquals(maxAllowedValues.get(BUILD), createdProfile.get().getMaxAllowedBuilds());
        assertEquals(maxAllowedValues.get(CASES), createdProfile.get().getMaxAllowedCases());
        assertEquals(maxAllowedValues.get(LOAD_FLOW), createdProfile.get().getMaxAllowedLoadflow());
        assertEquals(maxAllowedValues.get(SECURITY_ANALYSIS), createdProfile.get().getMaxAllowedSecurity());
        assertEquals(maxAllowedValues.get(SENSITIVITY_ANALYSIS), createdProfile.get().getMaxAllowedSensitivity());
        assertEquals(maxAllowedValues.get(SHORT_CIRCUIT), createdProfile.get().getMaxAllowedShortCircuit());
        assertEquals(maxAllowedValues.get(VOLTAGE_INITIALIZATION), createdProfile.get().getMaxAllowedVoltageInit());
        assertEquals(maxAllowedValues.get(PCC_MIN), createdProfile.get().getMaxAllowedPccMin());
        assertEquals(maxAllowedValues.get(STATE_ESTIMATION), createdProfile.get().getMaxAllowedStateEstimation());
        assertEquals(maxAllowedValues.get(BALANCE_ADJUSTMENT), createdProfile.get().getMaxAllowedBalanceAdjustement());
        assertEquals(maxAllowedValues.get(DYNAMIC_SIMULATION), createdProfile.get().getMaxAllowedDynamicSimulation());
        assertEquals(maxAllowedValues.get(DYNAMIC_SECURITY_ANALYSIS), createdProfile.get().getMaxAllowedDynamicSecurity());
        assertEquals(maxAllowedValues.get(DYNAMIC_MARGIN_CALCULATION), createdProfile.get().getMaxAllowedDynamicMargin());
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

    private Map<QuotaType, Integer> getMaxQuotaMap(String userSub) throws Exception {
        MvcResult result = performGet(API_BASE_PATH + "/users/" + userSub + "/quota/max");
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() { });
    }

    private Integer getMaxAllowedBuilds(String userSub) throws Exception {
        return getMaxQuotaMap(userSub).get(BUILD);
    }

    private Integer getMaxAllowedCases(String userSub) throws Exception {
        return getMaxQuotaMap(userSub).get(CASES);
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
