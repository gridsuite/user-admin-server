/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
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

    public List<UserInfosEntity> getUsers() {
        return repository.findAll();
    }

    public void createUser(String sub) {
        UserInfosEntity userInfosEntity = new UserInfosEntity(sub);
        repository.save(userInfosEntity);
    }

    public void delete(String sub) {
        repository.deleteBySub(sub);
    }

    public boolean subExists(String sub) {
        return !repository.findAllBySub(sub).isEmpty();
    }
}
