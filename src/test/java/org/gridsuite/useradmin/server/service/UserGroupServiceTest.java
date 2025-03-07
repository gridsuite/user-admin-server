/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplication;
import org.gridsuite.useradmin.server.dto.UserGroup;
import org.gridsuite.useradmin.server.entity.GroupInfosEntity;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.repository.UserGroupRepository;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */

@SpringBootTest(classes = {UserAdminApplication.class})
class UserGroupServiceTest {

    @Mock
    private UserGroupService userGroupServiceSelfMock;

    @Mock
    private AdminRightService adminRightServiceMock;

    @Mock
    private UserInfosRepository userInfosRepositoryMock;

    @Mock
    private UserGroupRepository userGroupRepositoryMock;

    @InjectMocks
    private UserGroupService userGroupService;

    @Test
    void toDtoUserGroupTest() {
        // create user and group
        GroupInfosEntity group = new GroupInfosEntity("group_A");
        UserInfosEntity user = new UserInfosEntity(UUID.randomUUID(), "user_A", null, Set.of(group));
        group.setUsers(Set.of(user));

        when(userGroupRepositoryMock.findByName("group_A")).thenReturn(Optional.of(group));
        Optional<UserGroup> userGroup = userGroupService.getGroup("group_A");
        assertTrue(userGroup.isPresent());
        assertEquals("group_A", userGroup.get().name());
        assertEquals(1, userGroup.get().users().size());
        assertEquals("user_A", userGroup.get().users().iterator().next());
    }
}
