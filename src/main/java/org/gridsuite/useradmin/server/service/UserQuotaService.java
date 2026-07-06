/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserOperationEntity;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@Service
public class UserQuotaService {
    private final UserInfosRepository userInfosRepository;
    private final AdminRightService adminRightService;
    private final UserProfileService userProfileService;

    public UserQuotaService(final UserInfosRepository userInfosRepository,
                            final AdminRightService adminRightService,
                            final UserProfileService userProfileService) {
        this.userInfosRepository = Objects.requireNonNull(userInfosRepository);
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
        Optional<UserInfosEntity> userOpt = userInfosRepository.findBySub(sub);

        UserInfosEntity userInfosEntity = userOpt.orElseThrow(() -> UserAdminException.userNotFound(sub));
        userInfosEntity.getUserOperations().clear();
        userInfosRepository.save(userInfosEntity);
    }

    @Transactional(readOnly = true)
    public Map<QuotaType, Integer> getUserCurrentQuotaUsage(String sub) {
        Optional<UserInfosEntity> userOpt = userInfosRepository.findBySub(sub);

        UserInfosEntity userInfosEntity = userOpt.orElseThrow(() -> UserAdminException.userNotFound(sub));
        return userInfosEntity.getQuotaUsageMap();
    }

    @Transactional()
    public void startUserOperation(String sub, QuotaType operation, UUID operationId) {
        Optional<UserInfosEntity> userOpt = userInfosRepository.findBySub(sub);

        UserInfosEntity userInfosEntity = userOpt.orElseThrow(() -> UserAdminException.userNotFound(sub));
        userInfosEntity.getUserOperations().add(new UserOperationEntity(userInfosEntity, operationId, operation));
        userInfosRepository.save(userInfosEntity);
    }

    @Transactional()
    public void endUserOperation(String sub, QuotaType operation, UUID operationId) {
        Optional<UserInfosEntity> userOpt = userInfosRepository.findBySub(sub);

        UserInfosEntity userInfosEntity = userOpt.orElseThrow(() -> UserAdminException.userNotFound(sub));
        userInfosEntity.getUserOperations().removeIf(userOperationEntity -> userOperationEntity.getOperationId().equals(operationId) && userOperationEntity.getQuotaType().equals(operation));
        userInfosRepository.save(userInfosEntity);
    }
}
