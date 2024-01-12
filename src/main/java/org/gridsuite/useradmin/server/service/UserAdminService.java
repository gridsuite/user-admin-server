/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

    public List<UserInfosEntity> getUsers(String userId) {
        assertIsAdmin(userId);
        return userAdminRepository.findAll();
    }

    public List<ConnectionEntity> getConnections(String userId) {
        assertIsAdmin(userId);
        return connectionsService.removeDuplicates();
    }

    public void createUser(String sub, String userId) {
        assertIsAdmin(userId);
        UserInfosEntity userInfosEntity = new UserInfosEntity(sub);
        userAdminRepository.save(userInfosEntity);
    }

    public void delete(UUID id, String userId) {
        assertIsAdmin(userId);
        userAdminRepository.deleteById(id);
    }

    public boolean subExists(String sub) {
        Boolean isAllowed = applicationProps.getAdmins().isEmpty() && userAdminRepository.count() == 0 || applicationProps.getAdmins().contains(sub) || !userAdminRepository.findAllBySub(sub).isEmpty();
        connectionsService.recordConnectionAttempt(sub, isAllowed);
        return isAllowed.booleanValue();
    }
}
