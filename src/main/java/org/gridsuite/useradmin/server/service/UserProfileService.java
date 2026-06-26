/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.BooleanUtils;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gridsuite.useradmin.server.dto.UserProfile.*;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@Service
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final DirectoryService directoryService;
    private final AdminRightService adminRightService;
    private final UserAdminApplicationProps applicationProps;

    public UserProfileService(final UserProfileRepository userProfileRepository,
                              final AdminRightService adminRightService,
                              final DirectoryService directoryService,
                              final UserAdminApplicationProps applicationProps) {
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.adminRightService = Objects.requireNonNull(adminRightService);
        this.directoryService = Objects.requireNonNull(directoryService);
        this.applicationProps = Objects.requireNonNull(applicationProps);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("checkstyle:LambdaBodyLength")
    public List<UserProfile> getProfiles(String userId, boolean checkLinksValidity) {
        adminRightService.assertIsAdmin();
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

        Set<UUID> allUuidsInAllProfiles = profiles
            .stream()
            .flatMap(e -> Stream.of(
                e.getLoadFlowParameterId(),
                e.getSecurityAnalysisParameterId(),
                e.getSensitivityAnalysisParameterId(),
                e.getShortcircuitParameterId(),
                e.getPccminParameterId(),
                e.getVoltageInitParameterId(),
                e.getSpreadsheetConfigCollectionId(),
                e.getNetworkVisualizationParameterId(),
                e.getWorkspaceId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<UUID> existingUuids = directoryService.getExistingElements(allUuidsInAllProfiles, userId);
        // relative complement will be used to check the elements validity (the missing set should be very small)
        Set<UUID> missingUuids = Sets.difference(allUuidsInAllProfiles, existingUuids);

        return profiles
                .stream()
                .map(p -> {
                    Boolean allLinksValid = null;
                    if (p.getLoadFlowParameterId() != null) {
                        allLinksValid = !missingUuids.contains(p.getLoadFlowParameterId());
                    }
                    if (BooleanUtils.toBooleanDefaultIfNull(allLinksValid, true) && p.getSecurityAnalysisParameterId() != null) {
                        allLinksValid = !missingUuids.contains(p.getSecurityAnalysisParameterId());
                    }
                    if (BooleanUtils.toBooleanDefaultIfNull(allLinksValid, true) && p.getSensitivityAnalysisParameterId() != null) {
                        allLinksValid = !missingUuids.contains(p.getSensitivityAnalysisParameterId());
                    }
                    if (BooleanUtils.toBooleanDefaultIfNull(allLinksValid, true) && p.getShortcircuitParameterId() != null) {
                        allLinksValid = !missingUuids.contains(p.getShortcircuitParameterId());
                    }
                    if (BooleanUtils.toBooleanDefaultIfNull(allLinksValid, true) && p.getPccminParameterId() != null) {
                        allLinksValid = !missingUuids.contains(p.getPccminParameterId());
                    }
                    if (BooleanUtils.toBooleanDefaultIfNull(allLinksValid, true) && p.getVoltageInitParameterId() != null) {
                        allLinksValid = !missingUuids.contains(p.getVoltageInitParameterId());
                    }
                    if (BooleanUtils.toBooleanDefaultIfNull(allLinksValid, true) && p.getSpreadsheetConfigCollectionId() != null) {
                        allLinksValid = !missingUuids.contains(p.getSpreadsheetConfigCollectionId());
                    }
                    if (BooleanUtils.toBooleanDefaultIfNull(allLinksValid, true) && p.getNetworkVisualizationParameterId() != null) {
                        allLinksValid = !missingUuids.contains(p.getNetworkVisualizationParameterId());
                    }
                    if (BooleanUtils.toBooleanDefaultIfNull(allLinksValid, true) && p.getWorkspaceId() != null) {
                        allLinksValid = !missingUuids.contains(p.getWorkspaceId());
                    }
                    return toDto(p, allLinksValid);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> getProfileIfAdmin(UUID profileUuid) {
        adminRightService.assertIsAdmin();
        return getProfile(profileUuid);
    }

    @Transactional()
    public void updateProfile(UUID profileUuid, UserProfile userProfile) {
        adminRightService.assertIsAdmin();
        UserProfileEntity profile = userProfileRepository.findById(profileUuid)
            .orElseThrow(() -> UserAdminException.profileNotFound(profileUuid));
        profile.setName(userProfile.getName());
        profile.setLoadFlowParameterId(userProfile.getLoadFlowParameterId());
        profile.setSecurityAnalysisParameterId(userProfile.getSecurityAnalysisParameterId());
        profile.setSensitivityAnalysisParameterId(userProfile.getSensitivityAnalysisParameterId());
        profile.setShortcircuitParameterId(userProfile.getShortcircuitParameterId());
        profile.setPccminParameterId(userProfile.getPccMinParameterId());
        profile.setVoltageInitParameterId(userProfile.getVoltageInitParameterId());
        profile.setMaxAllowedCases(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_CASES));
        profile.setMaxAllowedBuilds(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_BUILD));
        profile.setMaxAllowedLoadflow(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_LOADFLOW));
        profile.setMaxAllowedSecurity(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_SECURITY));
        profile.setMaxAllowedSensitivity(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_SENSITIVITY));
        profile.setMaxAllowedShortCircuit(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_SHORT_CIRCUIT));
        profile.setMaxAllowedVoltageInit(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_VOLTAGE_INIT));
        profile.setMaxAllowedPccMin(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_PCC_MIN));
        profile.setMaxAllowedStateEstimation(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_STATE_ESTIMATION));
        profile.setMaxAllowedBalanceAdjustement(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_BALANCE_ADJUSTEMENT));
        profile.setMaxAllowedDynamicSimulation(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_DYNAMIC_SIMULATION));
        profile.setMaxAllowedDynamicSecurity(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_DYNAMIC_SECURITY));
        profile.setMaxAllowedDynamicMargin(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_DYNAMIC_MARGIN));
        profile.setSpreadsheetConfigCollectionId(userProfile.getSpreadsheetConfigCollectionId());
        profile.setNetworkVisualizationParameterId(userProfile.getNetworkVisualizationParameterId());
        profile.setWorkspaceId(userProfile.getWorkspaceId());
    }

    @Transactional
    public void createProfile(UserProfile userProfile) {
        adminRightService.assertIsAdmin();
        if (userProfileRepository.findByName(userProfile.getName()).isPresent()) {
            throw UserAdminException.profileAlreadyExists(userProfile.getName());
        }
        UserProfileEntity userProfileEntity = toEntity(userProfile);
        userProfileRepository.save(userProfileEntity);
    }

    @Transactional
    public long deleteProfiles(List<String> names) {
        adminRightService.assertIsAdmin();
        return userProfileRepository.deleteAllByNameIn(names);
    }

    Optional<UserProfile> getProfile(UUID profileUuid) {
        return userProfileRepository.findById(profileUuid).map(this::toDto);
    }

    private UserProfile toDto(final UserProfileEntity entity) {
        return toDto(entity, null);
    }

    private UserProfile toDto(final UserProfileEntity entity, Boolean allLinksValid) {
        if (entity == null) {
            return null;
        }
        Map<String, Integer> maxAllowedValues = new HashMap<>();
        maxAllowedValues.put(MAX_ALLOWED_CASES, entity.getMaxAllowedCases());
        maxAllowedValues.put(MAX_ALLOWED_BUILD, entity.getMaxAllowedBuilds());
        maxAllowedValues.put(MAX_ALLOWED_LOADFLOW, entity.getMaxAllowedLoadflow());
        maxAllowedValues.put(MAX_ALLOWED_SECURITY, entity.getMaxAllowedSecurity());
        maxAllowedValues.put(MAX_ALLOWED_SENSITIVITY, entity.getMaxAllowedSensitivity());
        maxAllowedValues.put(MAX_ALLOWED_SHORT_CIRCUIT, entity.getMaxAllowedShortCircuit());
        maxAllowedValues.put(MAX_ALLOWED_VOLTAGE_INIT, entity.getMaxAllowedVoltageInit());
        maxAllowedValues.put(MAX_ALLOWED_PCC_MIN, entity.getMaxAllowedPccMin());
        maxAllowedValues.put(MAX_ALLOWED_STATE_ESTIMATION, entity.getMaxAllowedStateEstimation());
        maxAllowedValues.put(MAX_ALLOWED_BALANCE_ADJUSTEMENT, entity.getMaxAllowedBalanceAdjustement());
        maxAllowedValues.put(MAX_ALLOWED_DYNAMIC_SIMULATION, entity.getMaxAllowedDynamicSimulation());
        maxAllowedValues.put(MAX_ALLOWED_DYNAMIC_SECURITY, entity.getMaxAllowedDynamicSecurity());
        maxAllowedValues.put(MAX_ALLOWED_DYNAMIC_MARGIN, entity.getMaxAllowedDynamicMargin());
        return UserProfile.builder()
                .id(entity.getId())
                .name(entity.getName())
                .loadFlowParameterId(entity.getLoadFlowParameterId())
                .securityAnalysisParameterId(entity.getSecurityAnalysisParameterId())
                .sensitivityAnalysisParameterId(entity.getSensitivityAnalysisParameterId())
                .shortcircuitParameterId(entity.getShortcircuitParameterId())
                .pccMinParameterId(entity.getPccminParameterId())
                .voltageInitParameterId(entity.getVoltageInitParameterId())
                .allLinksValid(allLinksValid)
                .maxAllowValuesMap(maxAllowedValues)
                .spreadsheetConfigCollectionId(entity.getSpreadsheetConfigCollectionId())
                .networkVisualizationParameterId(entity.getNetworkVisualizationParameterId())
                .workspaceId(entity.getWorkspaceId())
                .build();
    }

    private UserProfileEntity toEntity(final UserProfile userProfile) {
        Objects.requireNonNull(userProfile);
        return new UserProfileEntity(
            UUID.randomUUID(),
            userProfile.getName(),
            userProfile.getLoadFlowParameterId(),
            userProfile.getSecurityAnalysisParameterId(),
            userProfile.getSensitivityAnalysisParameterId(),
            userProfile.getShortcircuitParameterId(),
            userProfile.getPccMinParameterId(),
            userProfile.getVoltageInitParameterId(),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_CASES)).orElse(applicationProps.getDefaultMaxAllowedCases()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_BUILD)).orElse(applicationProps.getDefaultMaxAllowedBuilds()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_LOADFLOW)).orElse(applicationProps.getDefaultMaxAllowedLoadflow()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_SECURITY)).orElse(applicationProps.getDefaultMaxAllowedSecurity()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_SENSITIVITY)).orElse(applicationProps.getDefaultMaxAllowedSensitivity()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_SHORT_CIRCUIT)).orElse(applicationProps.getDefaultMaxAllowedShortCircuit()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_VOLTAGE_INIT)).orElse(applicationProps.getDefaultMaxAllowedVoltageInit()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_PCC_MIN)).orElse(applicationProps.getDefaultMaxAllowedPccMin()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_STATE_ESTIMATION)).orElse(applicationProps.getDefaultMaxAllowedStateEstimation()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_BALANCE_ADJUSTEMENT)).orElse(applicationProps.getDefaultMaxAllowedBalanceAdjustement()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_DYNAMIC_SIMULATION)).orElse(applicationProps.getDefaultMaxAllowedDynamicSimulation()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_DYNAMIC_SECURITY)).orElse(applicationProps.getDefaultMaxAllowedDynamicSecurity()),
            Optional.ofNullable(userProfile.getMaxAllowValuesMap().get(MAX_ALLOWED_DYNAMIC_MARGIN)).orElse(applicationProps.getDefaultMaxAllowedDynamicMargin()),
            userProfile.getSpreadsheetConfigCollectionId(),
            userProfile.getNetworkVisualizationParameterId(),
            userProfile.getWorkspaceId()
        );
    }
}
