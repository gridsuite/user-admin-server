/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserOperationEntity;
import org.gridsuite.useradmin.server.repository.UserOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@Service
public class UserQuotaService {
    private final UserOperationRepository userOperationRepository;
    private final AdminRightService adminRightService;
    private final UserProfileService userProfileService;

    public UserQuotaService(final UserOperationRepository userOperationRepository,
                            final AdminRightService adminRightService,
                            final UserProfileService userProfileService) {
        this.userOperationRepository = userOperationRepository;
        this.adminRightService = Objects.requireNonNull(adminRightService);
        this.userProfileService = Objects.requireNonNull(userProfileService);
    }

    @Transactional(readOnly = true)
    public Map<QuotaType, Integer> getUserMaxQuota(String sub) {
        UserProfile profile = userProfileService.doGetUserProfile(sub);
        return Optional.ofNullable(profile.getMaxOperationQuota())
                .orElse(userProfileService.getDefaultMaxAllowedValues());
    }

    @Transactional()
    public void resetUserCurrentQuotaUsage(String sub) {
        adminRightService.assertIsAdmin();

        userOperationRepository.deleteBySub(sub);
    }

    @Transactional(readOnly = true)
    public Map<QuotaType, Integer> getUserCurrentQuotaUsage(String sub) {
        List<UserOperationEntity> userOperations = userOperationRepository.findBySub(sub);
        return userOperations.stream()
                .collect(Collectors.groupingBy(UserOperationEntity::getQuotaType, Collectors.summingInt(e -> 1)));
    }

    @Transactional()
    public void startUserOperation(String sub, QuotaType operation, UUID operationId) {
        UserOperationEntity operationEntity = new UserOperationEntity(sub, operationId, operation);

        userOperationRepository.save(operationEntity);
    }

    @Transactional()
    public void endUserOperation(String sub, QuotaType operation, UUID operationId) {
        List<UserOperationEntity> userOperations = userOperationRepository.findBySub(sub);

        userOperations.stream()
                .filter(userOperationEntity -> userOperationEntity.getSub().equals(sub) &&
                        userOperationEntity.getOperationId().equals(operationId) &&
                        userOperationEntity.getQuotaType().equals(operation))
                .forEach(userOperationRepository::delete);
    }
}
