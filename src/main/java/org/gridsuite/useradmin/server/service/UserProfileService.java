/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import com.google.common.collect.Sets;
import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.gridsuite.useradmin.server.UserAdminException.Type.BAD_REQUEST;
import static org.gridsuite.useradmin.server.UserAdminException.Type.NOT_FOUND;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@Service
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final DirectoryService directoryService;
    private final AdminRightService adminRightService;

    public UserProfileService(final UserProfileRepository userProfileRepository,
                              final AdminRightService adminRightService,
                              final DirectoryService directoryService) {
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.adminRightService = Objects.requireNonNull(adminRightService);
        this.directoryService = Objects.requireNonNull(directoryService);
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getProfiles(String userId, boolean checkLinksValidity) {
        adminRightService.assertIsAdmin(userId);
        List<UserProfileEntity> profiles = userProfileRepository.findAll().stream().toList();
        if (profiles.isEmpty()) {
            return List.of();
        }

        if (!checkLinksValidity) {
            return profiles
                .stream()
                .map(this::toDto)
                .toList();
        }

        Set<UUID> allParametersUuidInAllProfiles = profiles
                .stream()
                .mapMulti((UserProfileEntity e, Consumer<UUID> consumer) ->
                    consumer.accept(e.getLoadFlowParameterId())
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<UUID> existingParametersUuids = directoryService.getExistingElements(allParametersUuidInAllProfiles);
        // relative complement will be used to check the elements validity (the missing set should be very small)
        Set<UUID> missingParametersUuids = Sets.difference(allParametersUuidInAllProfiles, existingParametersUuids);

        return profiles
                .stream()
                .map(p -> {
                    Boolean allParametersLinksValid = null;
                    if (p.getLoadFlowParameterId() != null) {
                        allParametersLinksValid = !missingParametersUuids.contains(p.getLoadFlowParameterId());
                    }
                    return toDto(p, allParametersLinksValid);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> getProfile(UUID profileUuid, String userId) {
        adminRightService.assertIsAdmin(userId);
        return getProfile(profileUuid);
    }

    @Transactional()
    public void updateProfile(UUID profileUuid, String userId, UserProfile userProfile) {
        adminRightService.assertIsAdmin(userId);
        UserProfileEntity profile = userProfileRepository.findById(profileUuid).orElseThrow(() -> new UserAdminException(NOT_FOUND));
        profile.setName(userProfile.name());
        profile.setLoadFlowParameterId(userProfile.loadFlowParameterId());
        profile.setMaxAllowedCases(userProfile.maxAllowedCases());
    }

    @Transactional
    public void createProfile(UserProfile userProfile, String userId) {
        adminRightService.assertIsAdmin(userId);
        UserProfileEntity userProfileEntity = toEntity(userProfile);
        if (userProfileEntity == null) {
            throw new UserAdminException(BAD_REQUEST, "Invalid user profile");
        }
        userProfileRepository.save(userProfileEntity);
    }

    @Transactional
    public long deleteProfiles(List<String> names, String userId) {
        adminRightService.assertIsAdmin(userId);
        return userProfileRepository.deleteAllByNameIn(names);
    }

    Optional<UserProfile> getProfile(UUID profileUuid) {
        return userProfileRepository.findById(profileUuid).map(this::toDto);
    }

    private UserProfile toDto(final UserProfileEntity entity) {
        return toDto(entity, null);
    }

    private UserProfile toDto(final UserProfileEntity entity, Boolean allParametersLinksValid) {
        if (entity == null) {
            return null;
        }
        return new UserProfile(entity.getId(), entity.getName(), entity.getLoadFlowParameterId(), allParametersLinksValid, entity.getMaxAllowedCases());
    }

    private UserProfileEntity toEntity(final UserProfile userProfile) {
        if (userProfile == null) {
            return null;
        }
        return new UserProfileEntity(UUID.randomUUID(), userProfile.name(), userProfile.loadFlowParameterId(), userProfile.maxAllowedCases());
    }
}
