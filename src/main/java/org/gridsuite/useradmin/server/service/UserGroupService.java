/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import jakarta.annotation.Nullable;
import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.dto.UserGroup;
import org.gridsuite.useradmin.server.entity.GroupInfosEntity;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.repository.UserGroupRepository;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gridsuite.useradmin.server.UserAdminException.Type.NOT_FOUND;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
public class UserGroupService {
    private final UserGroupService self;
    private final UserGroupRepository userGroupRepository;
    private final AdminRightService adminRightService;
    private final UserInfosRepository userInfosRepository;

    public UserGroupService(@Lazy final UserGroupService self,
                            final UserGroupRepository userGroupRepository,
                            final AdminRightService adminRightService,
                            UserInfosRepository userInfosRepository) {
        this.self = Objects.requireNonNull(self);
        this.userGroupRepository = Objects.requireNonNull(userGroupRepository);
        this.adminRightService = Objects.requireNonNull(adminRightService);
        this.userInfosRepository = userInfosRepository;
    }

    public UserGroup toDto(@Nullable final GroupInfosEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserGroup(entity.getId(),
            entity.getName(),
            entity.getUsers() == null ? null : entity.getUsers().stream().map(UserInfosEntity::getSub).collect(Collectors.toSet()));
    }

    @Transactional(readOnly = true)
    public Set<UserGroup> getGroups(String userId) {
        adminRightService.assertIsAdmin(userId);
        List<GroupInfosEntity> groups = userGroupRepository.findAll().stream().toList();
        return groups.stream().map(this::toDto).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Optional<UserGroup> getGroup(String group, String userId) {
        adminRightService.assertIsAdmin(userId);
        return getGroup(group);
    }

    @Transactional()
    public void updateGroup(UUID groupUuid, String userId, UserGroup userGroup) {
        adminRightService.assertIsAdmin(userId);
        GroupInfosEntity group = userGroupRepository.findById(groupUuid).orElseThrow(() -> new UserAdminException(NOT_FOUND));
        group.setName(userGroup.name());

        // remove group from all of his existing users
        if (group.getUsers() != null) {
            group.getUsers().forEach(user -> user.getGroups().removeIf(group2 -> group2.getName().equals(userGroup.name())));
        }

        // set all new users for group
        group.setUsers(userGroup.users() == null ?
            null :
            userGroup.users().stream().map(userInfosRepository::findBySub)
                .filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toSet()));

        // add group to all users
        if (group.getUsers() != null) {
            group.getUsers().forEach(user -> {
                Set<GroupInfosEntity> groups = user.getGroups();
                if (groups == null) {
                    groups = new HashSet<>();
                }
                groups.add(group);
            });
        }
    }

    @Transactional
    public void createGroup(String group, String userId) {
        adminRightService.assertIsAdmin(userId);
        userGroupRepository.save(new GroupInfosEntity(group));
    }

    @Transactional
    public long deleteGroups(List<String> names, String userId) {
        adminRightService.assertIsAdmin(userId);

        // remove group in the group's users
        names.forEach(name -> {
            GroupInfosEntity group = userGroupRepository.findByName(name).orElseThrow(() -> new UserAdminException(NOT_FOUND));
            if (group.getUsers() != null) {
                group.getUsers().forEach(user -> user.getGroups().removeIf(group2 -> group2.getName().equals(name)));
            }
        });
        return userGroupRepository.deleteAllByNameIn(names);
    }

    Optional<UserGroup> getGroup(UUID groupUuid) {
        return userGroupRepository.findById(groupUuid).map(this::toDto);
    }

    Optional<UserGroup> getGroup(String groupName) {
        Optional<GroupInfosEntity> groupInfosEntity = self.getGroupInfosEntity(groupName);
        return groupInfosEntity.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<GroupInfosEntity> getGroupInfosEntity(String name) {
        return userGroupRepository.findByName(name);
    }
}
