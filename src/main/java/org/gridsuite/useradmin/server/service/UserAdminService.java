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
import org.springframework.transaction.annotation.Transactional;

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

    public UserAdminService(final UserAdminApplicationProps applicationProps,
                            final UserAdminRepository userAdminRepository,
                            final ConnectionsService connectionsService) {
        super(applicationProps);
        this.userAdminRepository = Objects.requireNonNull(userAdminRepository);
        this.connectionsService = Objects.requireNonNull(connectionsService);
    }

    private UserInfos toDtoUserInfo(final UserInfosEntity entity) {
        return DtoConverter.toDto(entity, this::isAdmin);
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
    public boolean userIsAuthorizedAdmin(@NonNull String userId) {
        return isAdmin(userId);
    }
}
