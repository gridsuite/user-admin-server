/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplication;
import org.gridsuite.useradmin.server.dto.QuotaState;
import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserOperationEntity;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.gridsuite.useradmin.server.repository.UserOperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userQuotaService, "self", userQuotaService);
    }

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
        Instant now = Instant.now();

        when(userOperationRepository.findBySub("user_A")).thenReturn(List.of(
                new UserOperationEntity("user_A", BUILD, now),
                new UserOperationEntity("user_A", BUILD, now),
                new UserOperationEntity("user_A", CASES, now)
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
    void getUserCurrentQuotaStateReturnsCurrentAndMaxForEachType() {
        Map<QuotaType, Integer> profileQuota = new EnumMap<>(QuotaType.class);
        profileQuota.put(CASES, 10);
        profileQuota.put(BUILD, 5);
        UserProfile profile = UserProfile.builder()
                .name("profile_A")
                .maxOperationQuota(profileQuota)
                .build();

        when(userProfileServiceMock.doGetUserProfile("user_A")).thenReturn(profile);

        Instant now = Instant.now();
        when(userOperationRepository.findBySub("user_A")).thenReturn(List.of(
                new UserOperationEntity("user_A", BUILD, now),
                new UserOperationEntity("user_A", BUILD, now)
        ));

        Map<QuotaType, QuotaState> result = userQuotaService.getUserCurrentQuotaState("user_A");

        assertNotNull(result);
        assertEquals(new QuotaState(2, 5), result.get(BUILD));
        assertEquals(new QuotaState(0, 10), result.get(CASES));
    }

    @Test
    void getUserCurrentQuotaStateReturnsZeroCurrentWhenNoOperations() {
        Map<QuotaType, Integer> defaultQuota = new EnumMap<>(QuotaType.class);
        defaultQuota.put(CASES, 20);
        defaultQuota.put(BUILD, 10);

        UserProfile profileWithNullQuota = mock(UserProfile.class);
        when(profileWithNullQuota.getMaxOperationQuota()).thenReturn(null);

        when(userProfileServiceMock.doGetUserProfile("user_B")).thenReturn(profileWithNullQuota);
        when(userProfileServiceMock.getDefaultMaxAllowedValues()).thenReturn(defaultQuota);

        Map<QuotaType, QuotaState> result = userQuotaService.getUserCurrentQuotaState("user_B");

        assertNotNull(result);
        assertEquals(new QuotaState(0, 20), result.get(CASES));
        assertEquals(new QuotaState(0, 10), result.get(BUILD));
    }

    @Test
    void consumeUserOperationSavesOperationWhenUnderQuota() {
        Map<QuotaType, Integer> profileQuota = new EnumMap<>(QuotaType.class);
        profileQuota.put(BUILD, 5);
        UserProfile profile = UserProfile.builder().name("profile_A").maxOperationQuota(profileQuota).build();
        when(userProfileServiceMock.doGetUserProfile("user_A")).thenReturn(profile);
        when(userOperationRepository.findBySubAndQuotaType("user_A", BUILD)).thenReturn(List.of());

        UUID generatedId = UUID.randomUUID();
        when(userOperationRepository.save(any())).thenAnswer(invocation -> {
            UserOperationEntity entity = invocation.getArgument(0);
            entity.setId(generatedId);
            return entity;
        });

        UUID quotaId = userQuotaService.consumeUserOperation("user_A", BUILD);

        assertEquals(generatedId, quotaId);
        ArgumentCaptor<UserOperationEntity> entityArgumentCaptor = ArgumentCaptor.forClass(UserOperationEntity.class);
        verify(userOperationRepository).save(entityArgumentCaptor.capture());
        assertEquals("user_A", entityArgumentCaptor.getValue().getSub());
        assertEquals(BUILD, entityArgumentCaptor.getValue().getQuotaType());
    }

    @Test
    void consumeUserOperationThrowsWhenQuotaExhausted() {
        Map<QuotaType, Integer> profileQuota = new EnumMap<>(QuotaType.class);
        profileQuota.put(BUILD, 1);
        UserProfile profile = UserProfile.builder().name("profile_A").maxOperationQuota(profileQuota).build();
        when(userProfileServiceMock.doGetUserProfile("user_A")).thenReturn(profile);
        when(userOperationRepository.findBySubAndQuotaType("user_A", BUILD))
                .thenReturn(List.of(new UserOperationEntity("user_A", BUILD, Instant.now())));

        assertThrows(UserAdminException.class, () -> userQuotaService.consumeUserOperation("user_A", BUILD));
        verify(userOperationRepository, never()).save(any());
    }

    @Test
    void releaseUserOperationRemovesMatchingOperation() {
        UUID quotaId = UUID.randomUUID();
        UserOperationEntity operation = new UserOperationEntity("user_A", BUILD, Instant.now());
        operation.setId(quotaId);
        when(userOperationRepository.findById(quotaId)).thenReturn(Optional.of(operation));

        userQuotaService.releaseUserOperation("user_A", quotaId);

        verify(userOperationRepository).delete(operation);
    }

    @Test
    void releaseUserOperationDoesNothingWhenSubDiffers() {
        UUID quotaId = UUID.randomUUID();
        UserOperationEntity operation = new UserOperationEntity("otherUser", BUILD, Instant.now());
        operation.setId(quotaId);
        when(userOperationRepository.findById(quotaId)).thenReturn(Optional.of(operation));

        userQuotaService.releaseUserOperation("user_A", quotaId);

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
