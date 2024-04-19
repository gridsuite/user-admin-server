/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;
import static org.gridsuite.useradmin.server.UserAdminException.Type.NOT_FOUND;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class UserAdminService {
    private final UserInfosRepository userInfosRepository;
    private final UserProfileRepository userProfileRepository;
    private final ConnectionsService connectionsService;
    private final NotificationService notificationService;
    private final AdminRightService adminRightService;
    private final UserProfileService userProfileService;

    public UserAdminService(final UserInfosRepository userInfosRepository,
                            final UserProfileRepository userProfileRepository,
                            final ConnectionsService connectionsService,
                            final AdminRightService adminRightService,
                            final NotificationService notificationService,
                            final UserProfileService userProfileService) {
        this.userInfosRepository = Objects.requireNonNull(userInfosRepository);
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.connectionsService = Objects.requireNonNull(connectionsService);
        this.adminRightService = Objects.requireNonNull(adminRightService);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.userProfileService = Objects.requireNonNull(userProfileService);
    }

    private UserInfos toDtoUserInfo(final UserInfosEntity entity) {
        return UserInfosEntity.toDto(entity, adminRightService::isAdmin);
    }

    @Transactional(readOnly = true)
    public List<UserInfos> getUsers(String userId) {
        adminRightService.assertIsAdmin(userId);
        return userInfosRepository.findAll().stream().map(this::toDtoUserInfo).toList();
    }

    @Transactional(readOnly = true)
    public List<UserConnection> getConnections(String userId) {
        adminRightService.assertIsAdmin(userId);
        return connectionsService.removeDuplicates();
    }

    @Transactional
    public void createUser(String sub, String userId) {
        adminRightService.assertIsAdmin(userId);
        userInfosRepository.save(new UserInfosEntity(sub));
    }

    @Transactional
    public long delete(String sub, String userId) {
        adminRightService.assertIsAdmin(userId);
        return userInfosRepository.deleteBySub(sub);
    }

    @Transactional
    public long delete(Collection<String> subs, String userId) {
        adminRightService.assertIsAdmin(userId);
        return userInfosRepository.deleteAllBySubIn(subs);
    }

    @Transactional()
    public void updateUser(String sub, String userId, UserInfos userInfos) {
        adminRightService.assertIsAdmin(userId);
        UserInfosEntity user = userInfosRepository.findBySub(sub).orElseThrow(() -> new UserAdminException(NOT_FOUND));
        Optional<UserProfileEntity> profile = userProfileRepository.findByName(userInfos.profileName());
        user.setSub(userInfos.sub());
        user.setProfile(profile.orElse(null));
    }

    @Transactional
    public boolean subExists(String sub) {
        final List<String> admins = adminRightService.getAdmins();
        final boolean isAllowed = admins.isEmpty() && userInfosRepository.count() == 0L
                                || admins.contains(sub)
                                || userInfosRepository.existsBySub(sub);
        connectionsService.recordConnectionAttempt(sub, isAllowed);
        return isAllowed;
    }

    @Transactional(readOnly = true)
    public Optional<UserInfos> getUser(String sub, String userId) {
        adminRightService.assertIsAdmin(userId);
        return userInfosRepository.findBySub(sub).map(this::toDtoUserInfo);
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> getUserProfile(String sub) {
        // this method is not restricted to Admin because it is called by any user to retrieve its own profile
        UserInfosEntity user = userInfosRepository.findBySub(sub).orElseThrow(() -> new UserAdminException(NOT_FOUND));
        return user.getProfile() == null ? Optional.empty() : userProfileService.getProfile(user.getProfile().getId());
    }

    @Transactional(readOnly = true)
    public boolean userIsAdmin(@NonNull String userId) {
        return adminRightService.isAdmin(userId);
    }

    public void sendMaintenanceMessage(String userId, Integer durationInSeconds, String message) {
        if (!adminRightService.isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        if (durationInSeconds == null) {
            notificationService.emitMaintenanceMessage(message);
        } else {
            notificationService.emitMaintenanceMessage(message, durationInSeconds);
        }
    }

    public void sendCancelMaintenanceMessage(String userId) {
        if (!adminRightService.isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        notificationService.emitCancelMaintenanceMessage();
    }
}
