/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class UserAdminService {
    private static final String UNAUTHORIZED = "Unauthorized";
    private UserAdminRepository repository;

    @Autowired
    private UserAdminApplicationProps applicationProps;

    public UserAdminService(UserAdminRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<UserInfosEntity> getUsers(String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        return repository.findAll();
    }

    public void createUser(String sub, String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        UserInfosEntity userInfosEntity = new UserInfosEntity(sub);
        repository.save(userInfosEntity);
    }

    public void delete(UUID id, String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        repository.deleteById(id);
    }

    public boolean subExists(String sub) {
        return applicationProps.getAdmins().isEmpty() || !repository.findAllBySub(sub).isEmpty();
    }

    private boolean isAdmin(String sub) {
        return applicationProps.getAdmins().contains(sub);
    }
}
