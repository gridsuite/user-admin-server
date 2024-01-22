/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class UserAdminService extends AbstractCommonService {
    private final UserAdminRepository userAdminRepository;
    private final ConnectionsService connectionsService;

    public UserAdminService(UserAdminRepository userAdminRepository,
                            ConnectionsService connectionsService, UserAdminApplicationProps applicationProps) {
        super(applicationProps);
        this.userAdminRepository = Objects.requireNonNull(userAdminRepository);
        this.connectionsService = Objects.requireNonNull(connectionsService);
    }

    private UserInfos toDtoUserInfo(final UserInfosEntity entity) {
        return DtoConverter.toDto(entity, this::isAdmin);
    }

    public List<UserInfos> getUsers(@NonNull String userId) {
        assertIsAdmin(userId);
        return userAdminRepository.findAll().stream().map(this::toDtoUserInfo).toList();
    }

    public List<UserConnection> getConnections(String userId) {
        assertIsAdmin(userId);
        return connectionsService.removeDuplicates().stream().map(DtoConverter::toDto).toList();
    }

    public void createUser(String sub, String userId) {
        assertIsAdmin(userId);
        UserInfosEntity userInfosEntity = new UserInfosEntity(sub);
        userAdminRepository.save(userInfosEntity);
    }

    public long delete(String sub, String userId) {
        assertIsAdmin(userId);
        return userAdminRepository.deleteBySub(sub);
    }

    public boolean subExists(String sub) {
        final boolean isAllowed = applicationProps.getAdmins().isEmpty() && userAdminRepository.count() == 0
                || applicationProps.getAdmins().contains(sub)
                || !userAdminRepository.existsBySub(sub);
        connectionsService.recordConnectionAttempt(sub, isAllowed);
        return isAllowed;
    }

    public Optional<UserInfos> getUser(String sub, String userId) {
        assertIsAdmin(userId);
        return userAdminRepository.findBySub(sub).map(this::toDtoUserInfo);
    }

    public List<UserInfos> searchUsers(@NonNull String userId, @NonNull String term) {
        assertIsAdmin(userId);
        return userAdminRepository.findAllBySubContainsAllIgnoreCase(term).stream().map(this::toDtoUserInfo).toList();
    }

    public boolean userIsAuthorizedAdmin(@NonNull String userId) {
        return isAdmin(userId);
    }
}
