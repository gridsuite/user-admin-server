/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class UserAdminService {
    private UserAdminRepository repository;

    public UserAdminService(UserAdminRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<UserEntity> getUsers() {
        return repository.findAll();
    }

    public void createUser(String userId) {
        UserEntity userEntity = new UserEntity(userId);
        repository.save(userEntity);
    }

    public void delete(String userId) {
        repository.deleteByUserId(userId);
    }

    public boolean userIdExists(String userId) {
        return !repository.findAllByUserId(userId).isEmpty();
    }
}
