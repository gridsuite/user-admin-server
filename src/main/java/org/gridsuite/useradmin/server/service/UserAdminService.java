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
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;
import static org.gridsuite.useradmin.server.UserAdminException.Type.NOT_FOUND;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class UserAdminService {
    private final UserAdminRepository userAdminRepository;
    private final UserProfileRepository userProfileRepository;
    private final ConnectionsService connectionsService;
    private final UserAdminApplicationProps applicationProps;
    private final NotificationService notificationService;
    private final DirectoryService directoryService;

    public UserAdminService(final UserAdminApplicationProps applicationProps,
                            final UserAdminRepository userAdminRepository,
                            final UserProfileRepository userProfileRepository,
                            final ConnectionsService connectionsService,
                            final DirectoryService directoryService,
                            final NotificationService notificationService) {
        this.applicationProps = Objects.requireNonNull(applicationProps);
        this.userAdminRepository = Objects.requireNonNull(userAdminRepository);
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.connectionsService = Objects.requireNonNull(connectionsService);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.directoryService = Objects.requireNonNull(directoryService);
    }

    private boolean isAdmin(@lombok.NonNull String sub) {
        final List<String> admins = applicationProps.getAdmins();
        return admins.contains(sub);
    }

    private void assertIsAdmin(@lombok.NonNull String sub) throws UserAdminException {
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
        return userAdminRepository.findAll().stream().map(this::toDtoUserInfo).toList();
    }

    @Transactional(readOnly = true)
    public List<UserConnection> getConnections(String userId) {
        assertIsAdmin(userId);
        return connectionsService.removeDuplicates();
    }

    @Transactional
    public void createUser(String sub, String userId) {
        assertIsAdmin(userId);
        userAdminRepository.save(new UserInfosEntity(sub));
    }

    @Transactional
    public long delete(String sub, String userId) {
        assertIsAdmin(userId);
        return userAdminRepository.deleteBySub(sub);
    }

    @Transactional
    public long delete(List<String> subs, String userId) {
        assertIsAdmin(userId);
        return userAdminRepository.deleteAllBySubIn(subs);
    }

    @Transactional()
    public void updateUser(String sub, String userId, UserInfos userInfos) {
        assertIsAdmin(userId);
        UserInfosEntity user = userAdminRepository.findBySub(sub).orElseThrow(() -> new UserAdminException(NOT_FOUND));
        user.update(userInfos, userProfileRepository.findByName(userInfos.profileName()));
    }

    @Transactional
    public boolean subExists(String sub) {
        final List<String> admins = applicationProps.getAdmins();
        final boolean isAllowed = admins.isEmpty() && userAdminRepository.count() == 0L
                                || admins.contains(sub)
                                || userAdminRepository.existsBySub(sub);
        connectionsService.recordConnectionAttempt(sub, isAllowed);
        return isAllowed;
    }

    @Transactional(readOnly = true)
    public Optional<UserInfos> getUser(String sub, String userId) {
        assertIsAdmin(userId);
        return userAdminRepository.findBySub(sub).map(this::toDtoUserInfo);
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

    @Transactional(readOnly = true)
    public List<UserProfile> getProfiles(String userId) {
        assertIsAdmin(userId);
        List<UserProfileEntity> profiles = userProfileRepository.findAll().stream().toList();

        Set<UUID> allParametersUuidInAllProfiles = profiles
                .stream()
                .map(UserProfileEntity::getLoadFlowParameterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<UUID> existingParametersUuids = directoryService.getExistingElements(allParametersUuidInAllProfiles);
        // relative complement will be used to check the elements validity (the missing set should be very small)
        Set<UUID> missingParametersUuids = allParametersUuidInAllProfiles
                .stream()
                .filter(id -> !existingParametersUuids.contains(id))
                .collect(Collectors.toSet());

        return profiles
                .stream()
                .map(p -> UserProfileEntity.toDto(p, missingParametersUuids))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> getProfile(UUID profileUuid, String userId) {
        assertIsAdmin(userId);
        return userProfileRepository.findById(profileUuid).map(this::toDtoUserProfile);
    }

    @Transactional()
    public void updateProfile(UUID profileUuid, String userId, UserProfile userProfile) {
        assertIsAdmin(userId);
        UserProfileEntity profile = userProfileRepository.findById(profileUuid).orElseThrow(() -> new UserAdminException(NOT_FOUND));
        profile.update(userProfile);
    }

    @Transactional
    public void createProfile(String profileName, String userId) {
        assertIsAdmin(userId);
        userProfileRepository.save(new UserProfileEntity(profileName));
    }

    @Transactional
    public long deleteProfiles(List<String> names, String userId) {
        assertIsAdmin(userId);
        return userProfileRepository.deleteAllByNameIn(names);
    }

    private UserProfile toDtoUserProfile(final UserProfileEntity entity) {
        return UserProfileEntity.toDto(entity);
    }
}
