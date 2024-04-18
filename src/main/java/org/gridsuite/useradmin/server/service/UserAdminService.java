/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
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
    private final UserAdminApplicationProps applicationProps;
    private final NotificationService notificationService;

    public UserAdminService(final UserAdminApplicationProps applicationProps,
                            final UserInfosRepository userInfosRepository,
                            final UserProfileRepository userProfileRepository,
                            final ConnectionsService connectionsService,
                            final NotificationService notificationService) {
        this.applicationProps = Objects.requireNonNull(applicationProps);
        this.userInfosRepository = Objects.requireNonNull(userInfosRepository);
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.connectionsService = Objects.requireNonNull(connectionsService);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    private boolean isAdmin(@lombok.NonNull String sub) {
        final List<String> admins = applicationProps.getAdmins();
        return admins.contains(sub);
    }

    public void assertIsAdmin(@lombok.NonNull String sub) throws UserAdminException {
        if (!this.isAdmin(sub)) {
            throw new UserAdminException(FORBIDDEN);
        }
    }

    private UserInfos toDtoUserInfo(final UserInfosEntity entity) {
        return UserInfosEntity.toDto(entity, this::isAdmin);
    }

    @Transactional(readOnly = true)
    public List<UserInfos> getUsers(String userId) {
        assertIsAdmin(userId);
        return userInfosRepository.findAll().stream().map(this::toDtoUserInfo).toList();
    }

    @Transactional(readOnly = true)
    public List<UserConnection> getConnections(String userId) {
        assertIsAdmin(userId);
        return connectionsService.removeDuplicates();
    }

    @Transactional
    public void createUser(String sub, String userId) {
        assertIsAdmin(userId);
        userInfosRepository.save(new UserInfosEntity(sub));
    }

    @Transactional
    public long delete(String sub, String userId) {
        assertIsAdmin(userId);
        return userInfosRepository.deleteBySub(sub);
    }

    @Transactional
    public long delete(Collection<String> subs, String userId) {
        assertIsAdmin(userId);
        return userInfosRepository.deleteAllBySubIn(subs);
    }

    @Transactional()
    public void updateUser(String sub, String userId, UserInfos userInfos) {
        assertIsAdmin(userId);
        UserInfosEntity user = userInfosRepository.findBySub(sub).orElseThrow(() -> new UserAdminException(NOT_FOUND));
        Optional<UserProfileEntity> profile = userProfileRepository.findByName(userInfos.profileName());
        user.setSub(userInfos.sub());
        user.setProfile(profile.orElse(null));
    }

    @Transactional
    public boolean subExists(String sub) {
        final List<String> admins = applicationProps.getAdmins();
        final boolean isAllowed = admins.isEmpty() && userInfosRepository.count() == 0L
                                || admins.contains(sub)
                                || userInfosRepository.existsBySub(sub);
        connectionsService.recordConnectionAttempt(sub, isAllowed);
        return isAllowed;
    }

    @Transactional(readOnly = true)
    public Optional<UserInfos> getUser(String sub, String userId) {
        assertIsAdmin(userId);
        return userInfosRepository.findBySub(sub).map(this::toDtoUserInfo);
    }

    @Transactional(readOnly = true)
    public boolean userIsAdmin(@NonNull String userId) {
        return isAdmin(userId);
    }

    public void sendMaintenanceMessage(String userId, Integer durationInSeconds, String message) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        if (durationInSeconds == null) {
            notificationService.emitMaintenanceMessage(message);
        } else {
            notificationService.emitMaintenanceMessage(message, durationInSeconds);
        }
    }

    public void sendCancelMaintenanceMessage(String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        notificationService.emitCancelMaintenanceMessage();
    }
}
