/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplication;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserOperationEntity;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.useradmin.server.dto.QuotaType.BUILD;
import static org.gridsuite.useradmin.server.dto.QuotaType.CASES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@SpringBootTest(classes = {UserAdminApplication.class})
class UserQuotaServiceTest {

    @Mock
    private UserInfosRepository userInfosRepositoryMock;

    @Mock
    private AdminRightService adminRightServiceMock;

    @Mock
    private UserProfileService userProfileServiceMock;

    @Mock
    private UserAdminApplicationProps applicationPropsMock;

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
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), "user_A", null, new ArrayList<>(), null);
        when(userInfosRepositoryMock.findBySub("user_A")).thenReturn(Optional.of(userInfosEntity));

        Map<QuotaType, Integer> result = userQuotaService.getUserCurrentQuotaUsage("user_A");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserCurrentQuotaUsageReturnsAggregatedCountsWhenOperationsExist() {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), "user_A", null, new ArrayList<>(), null);
        UUID op1 = UUID.randomUUID();
        UUID op2 = UUID.randomUUID();
        UUID op3 = UUID.randomUUID();
        userInfosEntity.getUserOperations().add(new UserOperationEntity(userInfosEntity, op1, BUILD));
        userInfosEntity.getUserOperations().add(new UserOperationEntity(userInfosEntity, op2, BUILD));
        userInfosEntity.getUserOperations().add(new UserOperationEntity(userInfosEntity, op3, CASES));

        when(userInfosRepositoryMock.findBySub("user_A")).thenReturn(Optional.of(userInfosEntity));

        Map<QuotaType, Integer> result = userQuotaService.getUserCurrentQuotaUsage("user_A");

        assertEquals(2, result.get(BUILD));
        assertEquals(1, result.get(CASES));
    }

    @Test
    void getUserCurrentQuotaUsageThrowsUserNotFoundWhenUserDoesNotExist() {
        when(userInfosRepositoryMock.findBySub("unknown")).thenReturn(Optional.empty());

        assertThrows(UserAdminException.class, () -> userQuotaService.getUserCurrentQuotaUsage("unknown"));
    }

    @Test
    void startUserOperationAddsOperationToUser() {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), "user_A", null, new ArrayList<>(), null);
        when(userInfosRepositoryMock.findBySub("user_A")).thenReturn(Optional.of(userInfosEntity));
        when(userInfosRepositoryMock.save(any())).thenReturn(userInfosEntity);

        UUID operationId = UUID.randomUUID();
        userQuotaService.startUserOperation("user_A", BUILD, operationId);

        assertEquals(1, userInfosEntity.getUserOperations().size());
        assertEquals(BUILD, userInfosEntity.getUserOperations().getFirst().getQuotaType());
        assertEquals(operationId, userInfosEntity.getUserOperations().getFirst().getOperationId());
        verify(userInfosRepositoryMock).save(userInfosEntity);
    }

    @Test
    void startUserOperationThrowsUserNotFoundWhenUserDoesNotExist() {
        when(userInfosRepositoryMock.findBySub("unknown")).thenReturn(Optional.empty());

        assertThrows(UserAdminException.class,
                () -> userQuotaService.startUserOperation("unknown", BUILD, UUID.randomUUID()));
    }

    @Test
    void endUserOperationRemovesMatchingOperation() {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), "user_A", null, new ArrayList<>(), null);
        UUID operationId = UUID.randomUUID();
        userInfosEntity.getUserOperations().add(new UserOperationEntity(userInfosEntity, operationId, BUILD));
        // add a different operation that must NOT be removed
        userInfosEntity.getUserOperations().add(new UserOperationEntity(userInfosEntity, UUID.randomUUID(), CASES));

        when(userInfosRepositoryMock.findBySub("user_A")).thenReturn(Optional.of(userInfosEntity));
        when(userInfosRepositoryMock.save(any())).thenReturn(userInfosEntity);

        userQuotaService.endUserOperation("user_A", BUILD, operationId);

        assertEquals(1, userInfosEntity.getUserOperations().size());
        assertEquals(CASES, userInfosEntity.getUserOperations().get(0).getQuotaType());
        verify(userInfosRepositoryMock).save(userInfosEntity);
    }

    @Test
    void endUserOperationDoesNotRemoveWhenOperationIdMatchesButTypeDiffers() {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), "user_A", null, new ArrayList<>(), null);
        UUID operationId = UUID.randomUUID();
        userInfosEntity.getUserOperations().add(new UserOperationEntity(userInfosEntity, operationId, BUILD));

        when(userInfosRepositoryMock.findBySub("user_A")).thenReturn(Optional.of(userInfosEntity));
        when(userInfosRepositoryMock.save(any())).thenReturn(userInfosEntity);

        // end a CASES operation with the same UUID — should NOT remove the BUILD one
        userQuotaService.endUserOperation("user_A", CASES, operationId);

        assertEquals(1, userInfosEntity.getUserOperations().size());
    }

    @Test
    void endUserOperationThrowsUserNotFoundWhenUserDoesNotExist() {
        when(userInfosRepositoryMock.findBySub("unknown")).thenReturn(Optional.empty());

        assertThrows(UserAdminException.class,
                () -> userQuotaService.endUserOperation("unknown", BUILD, UUID.randomUUID()));
    }

    @Test
    void resetUserCurrentQuotaUsageClearsOperationsWhenAdmin() {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), "user_A", null, new ArrayList<>(), null);
        userInfosEntity.getUserOperations().add(new UserOperationEntity(userInfosEntity, UUID.randomUUID(), BUILD));
        when(userInfosRepositoryMock.findBySub("user_A")).thenReturn(Optional.of(userInfosEntity));
        when(userInfosRepositoryMock.save(any())).thenReturn(userInfosEntity);
        doNothing().when(adminRightServiceMock).assertIsAdmin();

        userQuotaService.resetUserCurrentQuotaUsage("user_A");

        assertTrue(userInfosEntity.getUserOperations().isEmpty());
        verify(userInfosRepositoryMock).save(userInfosEntity);
    }

    @Test
    void resetUserCurrentQuotaUsageThrowsUserNotFoundWhenUserDoesNotExist() {
        doNothing().when(adminRightServiceMock).assertIsAdmin();
        when(userInfosRepositoryMock.findBySub("unknown")).thenReturn(Optional.empty());

        assertThrows(UserAdminException.class, () -> userQuotaService.resetUserCurrentQuotaUsage("unknown"));
    }

    @Test
    void resetUserCurrentQuotaUsageThrowsForbiddenWhenNotAdmin() {
        doThrow(UserAdminException.forbidden()).when(adminRightServiceMock).assertIsAdmin();

        assertThrows(UserAdminException.class, () -> userQuotaService.resetUserCurrentQuotaUsage("user_A"));
        verify(userInfosRepositoryMock, never()).save(any());
    }
}
