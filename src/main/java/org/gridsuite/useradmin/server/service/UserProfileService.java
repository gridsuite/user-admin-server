/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
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

import static org.gridsuite.useradmin.server.UserAdminException.Type.NOT_FOUND;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@Service
public class UserProfileService {
    private final UserInfosRepository userInfosRepository;
    private final UserProfileRepository userProfileRepository;
    private final DirectoryService directoryService;
    private final UserAdminService userAdminService;

    public UserProfileService(final UserInfosRepository userInfosRepository,
                              final UserProfileRepository userProfileRepository,
                              final UserAdminService userAdminService,
                              final DirectoryService directoryService) {
        this.userInfosRepository = Objects.requireNonNull(userInfosRepository);
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.userAdminService = Objects.requireNonNull(userAdminService);
        this.directoryService = Objects.requireNonNull(directoryService);
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getProfiles(String sub) {
        List<UserProfileEntity> profiles;
        if (sub != null) {
            UserInfosEntity user = userInfosRepository.findBySub(sub).orElseThrow(() -> new UserAdminException(NOT_FOUND));
            profiles = user.getProfile() == null ? List.of() : userProfileRepository.findById(user.getProfile().getId()).stream().toList();
        } else {
            profiles = userProfileRepository.findAll().stream().toList();
        }
        if (profiles.isEmpty()) {
            return List.of();
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
        Set<UUID> missingParametersUuids = allParametersUuidInAllProfiles
                .stream()
                .filter(id -> !existingParametersUuids.contains(id))
                .collect(Collectors.toSet());

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
        userAdminService.assertIsAdmin(userId);
        return userProfileRepository.findById(profileUuid).map(this::toDto);
    }

    @Transactional()
    public void updateProfile(UUID profileUuid, String userId, UserProfile userProfile) {
        userAdminService.assertIsAdmin(userId);
        UserProfileEntity profile = userProfileRepository.findById(profileUuid).orElseThrow(() -> new UserAdminException(NOT_FOUND));
        profile.setName(userProfile.name());
        profile.setLoadFlowParameterId(userProfile.loadFlowParameterId());
    }

    @Transactional
    public void createProfile(String profileName, String userId) {
        userAdminService.assertIsAdmin(userId);
        userProfileRepository.save(new UserProfileEntity(profileName));
    }

    @Transactional
    public long deleteProfiles(List<String> names, String userId) {
        userAdminService.assertIsAdmin(userId);
        return userProfileRepository.deleteAllByNameIn(names);
    }

    private UserProfile toDto(final UserProfileEntity entity) {
        return toDto(entity, null);
    }

    private UserProfile toDto(final UserProfileEntity entity, Boolean allParametersLinksValid) {
        if (entity == null) {
            return null;
        }
        return new UserProfile(entity.getId(), entity.getName(), entity.getLoadFlowParameterId(), allParametersLinksValid);
    }
}
