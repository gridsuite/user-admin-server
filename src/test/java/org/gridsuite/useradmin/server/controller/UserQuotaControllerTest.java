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
    void getUserMaxQuotaReturnsDefaultQuotaWhenUserHasNoProfile() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH + "/users/{sub}/quota/max", USER_A)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<QuotaType, Integer> quota = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() { });

        assertNotNull(quota);
        // default values from application-default.yml
        assertEquals(20, quota.get(CASES));
        assertEquals(10, quota.get(BUILD));
        assertEquals(2, quota.get(LOAD_FLOW));
        assertEquals(2, quota.get(SECURITY_ANALYSIS));
        assertEquals(1, quota.get(SENSITIVITY_ANALYSIS));
        assertEquals(1, quota.get(SHORT_CIRCUIT));
        assertEquals(2, quota.get(VOLTAGE_INITIALIZATION));
        assertEquals(1, quota.get(PCC_MIN));
        assertEquals(1, quota.get(STATE_ESTIMATION));
        assertEquals(1, quota.get(BALANCE_ADJUSTMENT));
        assertEquals(1, quota.get(DYNAMIC_SIMULATION));
        assertEquals(1, quota.get(DYNAMIC_SECURITY_ANALYSIS));
        assertEquals(1, quota.get(DYNAMIC_MARGIN_CALCULATION));
    }

    @Test
    void getUserCurrentQuotaUsageReturnsEmptyMapWhenNoOperationsRegistered() throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH + "/users/{sub}/quota/current", USER_A)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<QuotaType, Integer> usage = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() { });

        assertNotNull(usage);
        assertTrue(usage.isEmpty());
    }

    @Test
    void getUserCurrentQuotaUsageReturnsAggregatedCountsWhenOperationsExist() throws Exception {
        UUID buildOp1 = UUID.randomUUID();
        UUID buildOp2 = UUID.randomUUID();
        UUID casesOp1 = UUID.randomUUID();

        startOperation(USER_A, BUILD, buildOp1);
        startOperation(USER_A, BUILD, buildOp2);
        startOperation(USER_A, CASES, casesOp1);

        MvcResult result = mockMvc.perform(get(API_BASE_PATH + "/users/{sub}/quota/current", USER_A)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<QuotaType, Integer> usage = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() { });

        assertEquals(2, usage.get(BUILD));
        assertEquals(1, usage.get(CASES));
    }

    @Test
    void getUserCurrentQuotaStateReturnsCurrentAndMaxWhenNoOperations() throws Exception {
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
        assertEquals(new QuotaState(0, 20), state.get(CASES));
        assertEquals(new QuotaState(0, 10), state.get(BUILD));
    }

    @Test
    void getUserCurrentQuotaStateReturnsCurrentAndMaxWhenOperationsExist() throws Exception {
        startOperation(USER_A, BUILD, UUID.randomUUID());
        startOperation(USER_A, BUILD, UUID.randomUUID());
        startOperation(USER_A, CASES, UUID.randomUUID());

        MvcResult result = mockMvc.perform(get(API_BASE_PATH + "/users/{sub}/quota/state", USER_A)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<QuotaType, QuotaState> state = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() { });

        assertEquals(new QuotaState(2, 10), state.get(BUILD));
        assertEquals(new QuotaState(1, 20), state.get(CASES));
    }

    @Test
    void startUserOperationAddsOperationAndReturns200() throws Exception {
        UUID operationId = UUID.randomUUID();

        startOperation(USER_A, BUILD, operationId);

        // verify via the current-quota endpoint (avoids lazy-loading outside a session)
        Map<QuotaType, Integer> usage = getCurrentQuota(USER_A);
        assertEquals(1, usage.get(BUILD));
    }

    @Test
    void endUserOperationRemovesOperationAndReturns200() throws Exception {
        UUID operationId = UUID.randomUUID();

        startOperation(USER_A, BUILD, operationId);
        assertEquals(1, getCurrentQuota(USER_A).get(BUILD));

        endOperation(USER_A, BUILD, operationId);
        assertTrue(getCurrentQuota(USER_A).isEmpty());
    }

    @Test
    void endUserOperationDoesNotRemoveWhenTypeMismatch() throws Exception {
        UUID operationId = UUID.randomUUID();

        startOperation(USER_A, BUILD, operationId);

        // end with a different type — should be a no-op
        endOperation(USER_A, CASES, operationId);

        assertEquals(1, getCurrentQuota(USER_A).get(BUILD));
    }

    @Test
    void resetUserCurrentQuotaUsageClearsAllOperationsWhenAdmin() throws Exception {
        startOperation(USER_A, BUILD, UUID.randomUUID());
        startOperation(USER_A, CASES, UUID.randomUUID());
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

    private void startOperation(String sub, QuotaType operation, UUID operationId) throws Exception {
        mockMvc.perform(post(API_BASE_PATH + "/users/{sub}/quota/{operation}/{id}/start", sub, operation, operationId)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private void endOperation(String sub, QuotaType operation, UUID operationId) throws Exception {
        mockMvc.perform(post(API_BASE_PATH + "/users/{sub}/quota/{operation}/{id}/end", sub, operation, operationId)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private Map<QuotaType, Integer> getCurrentQuota(String sub) throws Exception {
        MvcResult result = mockMvc.perform(get(API_BASE_PATH + "/users/{sub}/quota/current", sub)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, USER_ADMIN_ROLE)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() { });
    }
}
