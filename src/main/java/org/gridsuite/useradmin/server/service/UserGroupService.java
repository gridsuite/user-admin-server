/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import jakarta.annotation.Nullable;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.gridsuite.useradmin.server.dto.UserGroup;
import org.gridsuite.useradmin.server.entity.GroupInfosEntity;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.repository.UserGroupRepository;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
public class UserGroupService {
    private final UserGroupRepository userGroupRepository;
    private final AdminRightService adminRightService;
    private final UserInfosRepository userInfosRepository;

    public UserGroupService(final UserGroupRepository userGroupRepository,
                            final AdminRightService adminRightService,
                            UserInfosRepository userInfosRepository) {
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
    public Set<UserGroup> getGroups() {
        List<GroupInfosEntity> groups = userGroupRepository.findAll().stream().toList();
        return groups.stream().map(this::toDto).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Optional<UserGroup> getGroupIfAdmin(String group) {
        adminRightService.assertIsAdmin();
        return getGroup(group);
    }

    @Transactional()
    public void updateGroup(UUID groupUuid, UserGroup userGroup) {
        adminRightService.assertIsAdmin();
        GroupInfosEntity group = userGroupRepository.findById(groupUuid)
            .orElseThrow(() -> UserAdminException.groupNotFound(groupUuid));
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
    public void createGroup(String group) {
        adminRightService.assertIsAdmin();
        Optional<GroupInfosEntity> groupInfosEntity = userGroupRepository.findByName(group);
        if (groupInfosEntity.isPresent()) {
            throw UserAdminException.groupAlreadyExists(group);
        }
        userGroupRepository.save(new GroupInfosEntity(group));
    }

    @Transactional
    public long deleteGroups(List<String> names) {
        adminRightService.assertIsAdmin();

        // check if group contains users
        names.forEach(name -> {
            GroupInfosEntity group = userGroupRepository.findByName(name)
                .orElseThrow(() -> UserAdminException.groupNotFound(name));
            if (!CollectionUtils.isEmpty(group.getUsers())) {
                throw new DataIntegrityViolationException("Group " + name + " contains users !");
            }
        });
        return userGroupRepository.deleteAllByNameIn(names);
    }

    Optional<UserGroup> getGroup(UUID groupUuid) {
        return userGroupRepository.findById(groupUuid).map(this::toDto);
    }

    Optional<UserGroup> getGroup(String groupName) {
        Optional<GroupInfosEntity> groupInfosEntity = getGroupInfosEntity(groupName);
        return groupInfosEntity.map(this::toDto);
    }

    private Optional<GroupInfosEntity> getGroupInfosEntity(String name) {
        return userGroupRepository.findByName(name);
    }
}
