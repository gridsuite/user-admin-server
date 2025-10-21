/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserGroup;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserGroupRepository;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class UserAdminService {
    private final UserInfosRepository userInfosRepository;
    private final UserProfileRepository userProfileRepository;
    private final ConnectionsService connectionsService;
    private final AdminRightService adminRightService;
    private final UserProfileService userProfileService;
    private final UserGroupService userGroupService;

    private final UserAdminApplicationProps applicationProps;
    private final UserGroupRepository userGroupRepository;

    public UserAdminService(final UserInfosRepository userInfosRepository,
                            final UserProfileRepository userProfileRepository,
                            final ConnectionsService connectionsService,
                            final AdminRightService adminRightService,
                            final UserProfileService userProfileService,
                            final UserGroupService userGroupService,
                            final UserAdminApplicationProps applicationProps,
                            final UserGroupRepository userGroupRepository) {
        this.userInfosRepository = Objects.requireNonNull(userInfosRepository);
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.connectionsService = Objects.requireNonNull(connectionsService);
        this.adminRightService = Objects.requireNonNull(adminRightService);
        this.userProfileService = Objects.requireNonNull(userProfileService);
        this.userGroupService = Objects.requireNonNull(userGroupService);
        this.applicationProps = Objects.requireNonNull(applicationProps);
        this.userGroupRepository = userGroupRepository;
    }

    private UserInfos toDtoUserInfo(final UserInfosEntity entity) {
        return UserInfosEntity.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<UserInfos> getUsers() {
        adminRightService.assertIsAdmin();
        return userInfosRepository.findAll().stream().map(this::toDtoUserInfo).toList();
    }

    @Transactional(readOnly = true)
    public List<UserConnection> getConnections() {
        adminRightService.assertIsAdmin();
        return connectionsService.removeDuplicates();
    }

    @Transactional
    public void createUser(String sub) {
        adminRightService.assertIsAdmin();
        if (userInfosRepository.existsBySub(sub)) {
            throw UserAdminException.userAlreadyExists(sub);
        }
        userInfosRepository.save(new UserInfosEntity(sub));
    }

    private static void removeUserFromGroups(UserInfosEntity entity) {
        if (entity.getGroups() != null) {
            entity.getGroups().forEach(group -> group.getUsers().removeIf(user -> user.getSub().equals(entity.getSub())));
        }
    }

    @Transactional
    public long delete(String sub) {
        adminRightService.assertIsAdmin();
        UserInfosEntity userInfosEntity = userInfosRepository.findBySub(sub).orElseThrow(() -> UserAdminException.userNotFound(sub));
        removeUserFromGroups(userInfosEntity);
        return userInfosRepository.deleteBySub(sub);
    }

    @Transactional
    public long delete(Collection<String> subs) {
        adminRightService.assertIsAdmin();
        subs.forEach(sub -> {
            UserInfosEntity userInfosEntity = userInfosRepository.findBySub(sub).orElseThrow(() -> UserAdminException.userNotFound(sub));
            removeUserFromGroups(userInfosEntity);
        });
        return userInfosRepository.deleteAllBySubIn(subs);
    }

    @Transactional()
    public void updateUser(String sub, UserInfos userInfos) {
        adminRightService.assertIsAdmin();
        UserInfosEntity user = userInfosRepository.findBySub(sub).orElseThrow(() -> UserAdminException.userNotFound(sub));
        Optional<UserProfileEntity> profile = userProfileRepository.findByName(userInfos.profileName());
        user.setSub(userInfos.sub());
        user.setProfile(profile.orElse(null));

        // remove user from all of his existing groups
        removeUserFromGroups(user);

        // set all new groups for user
        user.setGroups(userInfos.groups() == null ?
            null :
            userInfos.groups().stream().map(userGroupRepository::findByName)
                .filter(Optional::isPresent)
                .map(Optional::get).collect(Collectors.toSet()));

        // add user to all groups
        if (user.getGroups() != null) {
            user.getGroups().forEach(group -> {
                Set<UserInfosEntity> users = group.getUsers();
                if (users == null) {
                    users = new HashSet<>();
                }
                users.add(user);
            });
        }
    }

    @Transactional
    public void recordConnectionAttempt(String sub, boolean isConnectionAccepted) {
        connectionsService.recordConnectionAttempt(sub, isConnectionAccepted);
    }

    @Transactional(readOnly = true)
    public Optional<UserInfos> getUser(String sub) {
        adminRightService.assertIsAdmin();
        return userInfosRepository.findBySub(sub).map(this::toDtoUserInfo);
    }

    @Transactional(readOnly = true)
    public UserProfile getUserProfile(String sub) {
        return doGetUserProfile(sub);
    }

    private UserProfile doGetUserProfile(String sub) {
        // this method is not restricted to Admin because it is called by any user to retrieve its own profile
        Optional<UserInfosEntity> userOpt = userInfosRepository.findBySub(sub);

        if (userOpt.isEmpty()) {
            return createDefaultProfile();
        }

        UserInfosEntity user = userOpt.get();

        if (user.getProfile() == null) {
            return createDefaultProfile();
        }

        return userProfileService.getProfile(user.getProfile().getId())
                .orElseGet(this::createDefaultProfile);
    }

    @Transactional(readOnly = true)
    public List<UserGroup> getUserGroups(String sub) {
        // this method is not restricted to Admin because it is called by any user to retrieve its own groups
        Optional<UserInfosEntity> userOpt = userInfosRepository.findBySub(sub);

        if (userOpt.isEmpty()) {
            return List.of();
        }

        UserInfosEntity user = userOpt.get();

        if (user.getGroups() == null || user.getGroups().isEmpty()) {
            return List.of();
        }

        return user.getGroups().stream()
                .map(g -> userGroupService.getGroup(g.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    @Transactional(readOnly = true)
    public Integer getUserProfileMaxAllowedCases(String sub) {
        UserProfile profile = doGetUserProfile(sub);
        return Optional.ofNullable(profile.maxAllowedCases())
                .orElse(applicationProps.getDefaultMaxAllowedCases());
    }

    public Integer getCasesAlertThreshold() {
        return Optional.ofNullable(applicationProps.getCasesAlertThreshold()).orElse(90);
    }

    @Transactional(readOnly = true)
    public Integer getUserProfileMaxAllowedBuilds(String sub) {
        UserProfile profile = doGetUserProfile(sub);
        return Optional.ofNullable(profile.maxAllowedBuilds())
                .orElse(applicationProps.getDefaultMaxAllowedBuilds());
    }

    private UserProfile createDefaultProfile() {
        return UserProfile.createDefaultProfile(
                applicationProps.getDefaultMaxAllowedCases(),
                applicationProps.getDefaultMaxAllowedBuilds()
        );
    }
}
