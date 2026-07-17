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
import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gridsuite.useradmin.server.dto.QuotaType.*;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@Service
public class UserProfileService {
    private final UserInfosRepository userInfosRepository;
    private final UserProfileRepository userProfileRepository;
    private final DirectoryService directoryService;
    private final AdminRightService adminRightService;
    private final UserAdminApplicationProps applicationProps;

    public UserProfileService(final UserInfosRepository userInfosRepository,
                              final UserProfileRepository userProfileRepository,
                              final AdminRightService adminRightService,
                              final DirectoryService directoryService,
                              final UserAdminApplicationProps applicationProps) {
        this.userInfosRepository = Objects.requireNonNull(userInfosRepository);
        this.userProfileRepository = Objects.requireNonNull(userProfileRepository);
        this.adminRightService = Objects.requireNonNull(adminRightService);
        this.directoryService = Objects.requireNonNull(directoryService);
        this.applicationProps = Objects.requireNonNull(applicationProps);
    }

    public UserProfile doGetUserProfile(String sub) {
        // this method is not restricted to Admin because it is called by any user to retrieve its own profile
        Optional<UserInfosEntity> userOpt = userInfosRepository.findBySub(sub);

        if (userOpt.isEmpty()) {
            return createDefaultProfile();
        }

        UserInfosEntity user = userOpt.get();

        if (user.getProfile() == null) {
            return createDefaultProfile();
        }

        return getProfile(user.getProfile().getId())
                .orElseGet(this::createDefaultProfile);
    }

    public UserProfile createDefaultProfile() {
        return UserProfile.createDefaultProfile(getDefaultMaxAllowedValues());
    }

    public Map<QuotaType, Integer> getDefaultMaxAllowedValues() {
        Map<QuotaType, Integer> maxAllowedValuesMap = new EnumMap<>(QuotaType.class);
        maxAllowedValuesMap.put(CASES, applicationProps.getDefaultMaxAllowedCases());
        maxAllowedValuesMap.put(BUILD, applicationProps.getDefaultMaxAllowedBuilds());
        maxAllowedValuesMap.put(LOAD_FLOW, applicationProps.getDefaultMaxAllowedLoadflow());
        maxAllowedValuesMap.put(SECURITY_ANALYSIS, applicationProps.getDefaultMaxAllowedSecurity());
        maxAllowedValuesMap.put(SENSITIVITY_ANALYSIS, applicationProps.getDefaultMaxAllowedSensitivity());
        maxAllowedValuesMap.put(SHORT_CIRCUIT, applicationProps.getDefaultMaxAllowedShortCircuit());
        maxAllowedValuesMap.put(VOLTAGE_INITIALIZATION, applicationProps.getDefaultMaxAllowedVoltageInit());
        maxAllowedValuesMap.put(PCC_MIN, applicationProps.getDefaultMaxAllowedPccMin());
        maxAllowedValuesMap.put(STATE_ESTIMATION, applicationProps.getDefaultMaxAllowedStateEstimation());
        maxAllowedValuesMap.put(BALANCE_ADJUSTMENT, applicationProps.getDefaultMaxAllowedBalanceAdjustement());
        maxAllowedValuesMap.put(DYNAMIC_SIMULATION, applicationProps.getDefaultMaxAllowedDynamicSimulation());
        maxAllowedValuesMap.put(DYNAMIC_SECURITY_ANALYSIS, applicationProps.getDefaultMaxAllowedDynamicSecurity());
        maxAllowedValuesMap.put(DYNAMIC_MARGIN_CALCULATION, applicationProps.getDefaultMaxAllowedDynamicMargin());
        return maxAllowedValuesMap;
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
        profile.setMaxAllowedCases(userProfile.getMaxOperationQuota(QuotaType.CASES));
        profile.setMaxAllowedBuilds(userProfile.getMaxOperationQuota(QuotaType.BUILD));
        profile.setMaxAllowedLoadflow(userProfile.getMaxOperationQuota(QuotaType.LOAD_FLOW));
        profile.setMaxAllowedSecurity(userProfile.getMaxOperationQuota(QuotaType.SECURITY_ANALYSIS));
        profile.setMaxAllowedSensitivity(userProfile.getMaxOperationQuota(QuotaType.SENSITIVITY_ANALYSIS));
        profile.setMaxAllowedShortCircuit(userProfile.getMaxOperationQuota(QuotaType.SHORT_CIRCUIT));
        profile.setMaxAllowedVoltageInit(userProfile.getMaxOperationQuota(QuotaType.VOLTAGE_INITIALIZATION));
        profile.setMaxAllowedPccMin(userProfile.getMaxOperationQuota(QuotaType.PCC_MIN));
        profile.setMaxAllowedStateEstimation(userProfile.getMaxOperationQuota(QuotaType.STATE_ESTIMATION));
        profile.setMaxAllowedBalanceAdjustement(userProfile.getMaxOperationQuota(QuotaType.BALANCE_ADJUSTMENT));
        profile.setMaxAllowedDynamicSimulation(userProfile.getMaxOperationQuota(QuotaType.DYNAMIC_SIMULATION));
        profile.setMaxAllowedDynamicSecurity(userProfile.getMaxOperationQuota(QuotaType.DYNAMIC_SECURITY_ANALYSIS));
        profile.setMaxAllowedDynamicMargin(userProfile.getMaxOperationQuota(QuotaType.DYNAMIC_MARGIN_CALCULATION));
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
        Map<QuotaType, Integer> maxAllowedValues = new EnumMap<>(QuotaType.class);
        maxAllowedValues.put(QuotaType.CASES, entity.getMaxAllowedCases());
        maxAllowedValues.put(QuotaType.BUILD, entity.getMaxAllowedBuilds());
        maxAllowedValues.put(QuotaType.LOAD_FLOW, entity.getMaxAllowedLoadflow());
        maxAllowedValues.put(QuotaType.SECURITY_ANALYSIS, entity.getMaxAllowedSecurity());
        maxAllowedValues.put(QuotaType.SENSITIVITY_ANALYSIS, entity.getMaxAllowedSensitivity());
        maxAllowedValues.put(QuotaType.SHORT_CIRCUIT, entity.getMaxAllowedShortCircuit());
        maxAllowedValues.put(QuotaType.VOLTAGE_INITIALIZATION, entity.getMaxAllowedVoltageInit());
        maxAllowedValues.put(QuotaType.PCC_MIN, entity.getMaxAllowedPccMin());
        maxAllowedValues.put(QuotaType.STATE_ESTIMATION, entity.getMaxAllowedStateEstimation());
        maxAllowedValues.put(QuotaType.BALANCE_ADJUSTMENT, entity.getMaxAllowedBalanceAdjustement());
        maxAllowedValues.put(QuotaType.DYNAMIC_SIMULATION, entity.getMaxAllowedDynamicSimulation());
        maxAllowedValues.put(QuotaType.DYNAMIC_SECURITY_ANALYSIS, entity.getMaxAllowedDynamicSecurity());
        maxAllowedValues.put(QuotaType.DYNAMIC_MARGIN_CALCULATION, entity.getMaxAllowedDynamicMargin());

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
                .maxOperationQuota(maxAllowedValues)
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
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.CASES)).orElse(applicationProps.getDefaultMaxAllowedCases()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.BUILD)).orElse(applicationProps.getDefaultMaxAllowedBuilds()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.LOAD_FLOW)).orElse(applicationProps.getDefaultMaxAllowedLoadflow()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.SECURITY_ANALYSIS)).orElse(applicationProps.getDefaultMaxAllowedSecurity()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.SENSITIVITY_ANALYSIS)).orElse(applicationProps.getDefaultMaxAllowedSensitivity()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.SHORT_CIRCUIT)).orElse(applicationProps.getDefaultMaxAllowedShortCircuit()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.VOLTAGE_INITIALIZATION)).orElse(applicationProps.getDefaultMaxAllowedVoltageInit()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.PCC_MIN)).orElse(applicationProps.getDefaultMaxAllowedPccMin()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.STATE_ESTIMATION)).orElse(applicationProps.getDefaultMaxAllowedStateEstimation()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.BALANCE_ADJUSTMENT)).orElse(applicationProps.getDefaultMaxAllowedBalanceAdjustement()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.DYNAMIC_SIMULATION)).orElse(applicationProps.getDefaultMaxAllowedDynamicSimulation()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.DYNAMIC_SECURITY_ANALYSIS)).orElse(applicationProps.getDefaultMaxAllowedDynamicSecurity()),
                Optional.ofNullable(userProfile.getMaxOperationQuota(QuotaType.DYNAMIC_MARGIN_CALCULATION)).orElse(applicationProps.getDefaultMaxAllowedDynamicMargin()),
                userProfile.getSpreadsheetConfigCollectionId(),
                userProfile.getNetworkVisualizationParameterId(),
                userProfile.getWorkspaceId()
        );
    }
}
