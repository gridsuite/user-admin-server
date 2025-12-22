/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplication;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Anis Touri <anis.touri at rte-france.com>
 */

@SpringBootTest(classes = {UserAdminApplication.class})
class UserInfosServiceTest {

    @Mock
    private DirectoryService directoryServiceMock;

    @Mock
    private UserIdentityService userIdentityServiceMock;

    @Mock
    private UserAdminApplicationProps applicationPropsMock;

    @Mock
    private UserInfosRepository userInfosRepositoryMock;

    @InjectMocks
    private UserInfosService userInfosService;

    @Test
    void toDtoUserInfoTest() {
        // get number of cases used mock
        when(directoryServiceMock.getCasesCount("user_A")).thenReturn(3);
        // create user and profile
        UserProfileEntity profile = new UserProfileEntity(UUID.randomUUID(), "profile_A", null, null, null, null, null, 5, 6, null, null, null);
        UserInfosEntity user = new UserInfosEntity(UUID.randomUUID(), "user_A", profile, null);

        when(userInfosRepositoryMock.findBySub("user_A")).thenReturn(Optional.of(user));
        UserInfos userInfos = userInfosService.getUserInfo("user_A");
        assertNotNull(userInfos);
        assertEquals("user_A", userInfos.sub());
        assertEquals("profile_A", userInfos.profileName());
        assertEquals(5, userInfos.maxAllowedCases());
        assertEquals(3, userInfos.numberCasesUsed());
        assertEquals(6, userInfos.maxAllowedBuilds());
    }

    @Test
    void getUserInfoForNonExistentUser() {
        when(applicationPropsMock.getDefaultMaxAllowedCases()).thenReturn(20);
        when(applicationPropsMock.getDefaultMaxAllowedBuilds()).thenReturn(10);
        when(userInfosRepositoryMock.findBySub("nonExistent")).thenReturn(Optional.empty());
        when(userIdentityServiceMock.getIdentity("nonExistent")).thenReturn(Optional.empty());

        UserInfos userInfos = userInfosService.getUserInfo("nonExistent");
        assertNotNull(userInfos);
        assertEquals("nonExistent", userInfos.sub());
        assertNull(userInfos.firstName());
        assertNull(userInfos.lastName());
        assertNull(userInfos.profileName());
        assertEquals(20, userInfos.maxAllowedCases());
        assertEquals(0, userInfos.numberCasesUsed());
        assertEquals(10, userInfos.maxAllowedBuilds());
        assertTrue(userInfos.groups().isEmpty());
    }
}
