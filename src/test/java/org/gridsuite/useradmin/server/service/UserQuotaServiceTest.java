/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplication;
import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserOperationEntity;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.gridsuite.useradmin.server.repository.UserOperationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.gridsuite.useradmin.server.dto.QuotaType.BUILD;
import static org.gridsuite.useradmin.server.dto.QuotaType.CASES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@SpringBootTest(classes = {UserAdminApplication.class})
class UserQuotaServiceTest {

    @Mock
    private UserOperationRepository userOperationRepository;

    @Mock
    private AdminRightService adminRightServiceMock;

    @Mock
    private UserProfileService userProfileServiceMock;

    @InjectMocks
    private UserQuotaService userQuotaService;

    @Test
    void getUserMaxQuotaReturnsProfileQuotaWhenProfileHasQuota() {
        Map<QuotaType, Integer> profileQuota = new EnumMap<>(QuotaType.class);
        profileQuota.put(CASES, 10);
        profileQuota.put(BUILD, 5);
        UserProfile profile = UserProfile.builder()
                .name("profile_A")
                .maxOperationQuota(profileQuota)
                .build();

        when(userProfileServiceMock.doGetUserProfile("user_A")).thenReturn(profile);

        Map<QuotaType, Integer> result = userQuotaService.getUserMaxQuota("user_A");

        assertNotNull(result);
        assertEquals(10, result.get(CASES));
        assertEquals(5, result.get(BUILD));
    }

    @Test
    void getUserMaxQuotaReturnsDefaultQuotaWhenProfileHasNullQuota() {
        Map<QuotaType, Integer> defaultQuota = new EnumMap<>(QuotaType.class);
        defaultQuota.put(CASES, 20);
        defaultQuota.put(BUILD, 10);

        UserProfile profileWithNullQuota = mock(UserProfile.class);
        when(profileWithNullQuota.getMaxOperationQuota()).thenReturn(null);

        when(userProfileServiceMock.doGetUserProfile("user_B")).thenReturn(profileWithNullQuota);
        when(userProfileServiceMock.getDefaultMaxAllowedValues()).thenReturn(defaultQuota);

        Map<QuotaType, Integer> result = userQuotaService.getUserMaxQuota("user_B");

        assertNotNull(result);
        assertEquals(20, result.get(CASES));
        assertEquals(10, result.get(BUILD));
    }

    @Test
    void getUserCurrentQuotaUsageReturnsEmptyMapWhenNoOperations() {
        Map<QuotaType, Integer> result = userQuotaService.getUserCurrentQuotaUsage("user_A");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserCurrentQuotaUsageReturnsAggregatedCountsWhenOperationsExist() {
        UUID op1 = UUID.randomUUID();
        UUID op2 = UUID.randomUUID();
        UUID op3 = UUID.randomUUID();
        when(userOperationRepository.findBySub("user_A")).thenReturn(List.of(
                new UserOperationEntity("user_A", op1, BUILD),
                new UserOperationEntity("user_A", op2, BUILD),
                new UserOperationEntity("user_A", op3, CASES)
        ));

        Map<QuotaType, Integer> result = userQuotaService.getUserCurrentQuotaUsage("user_A");

        assertEquals(2, result.get(BUILD));
        assertEquals(1, result.get(CASES));
    }

    @Test
    void getUserCurrentQuotaUsageReturnsEmptyMapWhenUserDoesNotExist() {
        Map<QuotaType, Integer> result = userQuotaService.getUserCurrentQuotaUsage("unknown");
        assertTrue(result.isEmpty());
    }

    @Test
    void startUserOperationAddsOperationToUser() {
        UUID operationId = UUID.randomUUID();
        userQuotaService.startUserOperation("user_A", BUILD, operationId);

        ArgumentCaptor<UserOperationEntity> entityArgumentCaptor = ArgumentCaptor.forClass(UserOperationEntity.class);
        verify(userOperationRepository).save(entityArgumentCaptor.capture());

        UserOperationEntity value = entityArgumentCaptor.getValue();
        assertEquals("user_A", value.getSub());
        assertEquals(BUILD, value.getQuotaType());
        assertEquals(operationId, value.getOperationId());
    }

    @Test
    void endUserOperationRemovesMatchingOperation() {
        UUID operationId = UUID.randomUUID();
        UserOperationEntity operation1 = new UserOperationEntity("user_A", operationId, BUILD);
        UserOperationEntity operation2 = new UserOperationEntity("user_A", UUID.randomUUID(), CASES);

        when(userOperationRepository.findBySub("user_A")).thenReturn(List.of(operation1, operation2));

        userQuotaService.endUserOperation("user_A", BUILD, operationId);

        verify(userOperationRepository).delete(operation1);
    }

    @Test
    void endUserOperationDoesNotRemoveWhenOperationIdMatchesButTypeDiffers() {
        UUID operationId = UUID.randomUUID();
        UserOperationEntity operation1 = new UserOperationEntity("user_A", operationId, CASES);
        when(userOperationRepository.findBySub("user_A")).thenReturn(List.of(operation1));

        userQuotaService.endUserOperation("user_A", BUILD, operationId);

        verify(userOperationRepository, never()).delete(any());
    }

    @Test
    void resetUserCurrentQuotaUsageClearsOperationsWhenAdmin() {
        doNothing().when(adminRightServiceMock).assertIsAdmin();

        userQuotaService.resetUserCurrentQuotaUsage("user_A");

        verify(userOperationRepository).deleteBySub("user_A");
    }

    @Test
    void resetUserCurrentQuotaUsageThrowsForbiddenWhenNotAdmin() {
        doThrow(UserAdminException.forbidden()).when(adminRightServiceMock).assertIsAdmin();

        assertThrows(UserAdminException.class, () -> userQuotaService.resetUserCurrentQuotaUsage("user_A"));
        verify(userOperationRepository, never()).save(any());
    }
}
