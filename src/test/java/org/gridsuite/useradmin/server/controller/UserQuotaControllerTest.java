/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.useradmin.server.dto.QuotaState;
import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.repository.UserOperationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.gridsuite.useradmin.server.Utils.ROLES_HEADER;
import static org.gridsuite.useradmin.server.dto.QuotaType.*;
import static org.gridsuite.useradmin.server.utils.TestConstants.API_BASE_PATH;
import static org.gridsuite.useradmin.server.utils.TestConstants.USER_ADMIN_ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@AutoConfigureMockMvc
@SpringBootTest
class UserQuotaControllerTest {

    private static final String ADMIN_USER = "admin1";
    private static final String USER_A = "userAQuota";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserOperationRepository userOperationRepository;

    @AfterEach
    void cleanDB() {
        userOperationRepository.deleteAll();
    }

    @Test
    void getUserProfileMaxQuotaReturnsDefaultQuotaWhenUserHasNoProfile() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH + "/users/{sub}/quota/state", USER_A)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<QuotaType, QuotaState> state = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() { });

        assertNotNull(state);
        // default values from application-default.yml
        assertEquals(20, state.get(CASES).max());
        assertEquals(10, state.get(BUILD).max());
        assertEquals(2, state.get(LOAD_FLOW).max());
        assertEquals(2, state.get(SECURITY_ANALYSIS).max());
        assertEquals(1, state.get(SENSITIVITY_ANALYSIS).max());
        assertEquals(1, state.get(SHORT_CIRCUIT).max());
        assertEquals(2, state.get(VOLTAGE_INITIALIZATION).max());
        assertEquals(1, state.get(PCC_MIN).max());
        assertEquals(1, state.get(STATE_ESTIMATION).max());
        assertEquals(1, state.get(BALANCE_ADJUSTMENT).max());
        assertEquals(1, state.get(DYNAMIC_SIMULATION).max());
        assertEquals(1, state.get(DYNAMIC_SECURITY_ANALYSIS).max());
        assertEquals(1, state.get(DYNAMIC_MARGIN_CALCULATION).max());
    }

    @Test
    void getUserCurrentQuotaUsageReturnsEmptyMapWhenNoOperationsRegistered() throws Exception {
        Map<QuotaType, Integer> usage = getCurrentQuota(USER_A);

        assertNotNull(usage);
        assertTrue(usage.isEmpty());
    }

    @Test
    void consumeUserOperationAddsOperationAndReturns200WithQuotaId() throws Exception {
        UUID quotaId = consumeOperation(USER_A, BUILD);

        assertNotNull(quotaId);
        Map<QuotaType, Integer> usage = getCurrentQuota(USER_A);
        assertEquals(1, usage.get(BUILD));
    }

    @Test
    void consumeUserOperationReturns429WhenQuotaExhausted() throws Exception {
        for (int i = 0; i < 10; i++) {
            consumeOperation(USER_A, BUILD);
        }
        assertEquals(10, getCurrentQuota(USER_A).get(BUILD));

        mockMvc.perform(post(API_BASE_PATH + "/users/{sub}/quota/{operation}/consume", USER_A, BUILD)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void releaseUserOperationRemovesOperationAndReturns200() throws Exception {
        UUID quotaId = consumeOperation(USER_A, BUILD);
        assertEquals(1, getCurrentQuota(USER_A).get(BUILD));

        releaseOperation(USER_A, quotaId);
        assertTrue(getCurrentQuota(USER_A).isEmpty());
    }

    @Test
    void resetUserCurrentQuotaUsageClearsAllOperationsWhenAdmin() throws Exception {
        consumeOperation(USER_A, BUILD);
        consumeOperation(USER_A, CASES);
        assertEquals(2, getCurrentQuota(USER_A).size());

        mockMvc.perform(post(API_BASE_PATH + "/users/{sub}/quota/reset", USER_A)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        assertTrue(getCurrentQuota(USER_A).isEmpty());
    }

    @Test
    void resetUserCurrentQuotaUsageReturns403WhenNotAdmin() throws Exception {
        mockMvc.perform(post(API_BASE_PATH + "/users/{sub}/quota/reset", USER_A)
                        .header("userId", "regularUser")
                        // no ADMIN role header
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private UUID consumeOperation(String sub, QuotaType operation) throws Exception {
        MvcResult result = mockMvc.perform(post(API_BASE_PATH + "/users/{sub}/quota/{operation}/consume", sub, operation)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);
    }

    private void releaseOperation(String sub, UUID quotaId) throws Exception {
        mockMvc.perform(post(API_BASE_PATH + "/users/{sub}/quota/{quotaId}/release", sub, quotaId)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private Map<QuotaType, Integer> getCurrentQuota(String sub) throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH + "/users/{sub}/quota/state", sub)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        Map<QuotaType, QuotaState> state = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() { });
        return state.entrySet().stream()
                .filter(entry -> entry.getValue().current() != 0)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().current()));
    }
}
