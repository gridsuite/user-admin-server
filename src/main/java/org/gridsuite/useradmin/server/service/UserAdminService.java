/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class UserAdminService {
    private UserAdminRepository userAdminRepository;

    private ConnectionsService connectionsService;

    @Autowired
    private UserAdminApplicationProps applicationProps;

    public UserAdminService(UserAdminRepository userAdminRepository, ConnectionsService connectionsService) {
        this.userAdminRepository = Objects.requireNonNull(userAdminRepository);
        this.connectionsService = Objects.requireNonNull(connectionsService);
    }

    public List<UserInfosEntity> getUsers(String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        return userAdminRepository.findAll();
    }

    public List<ConnectionEntity> getConnections(String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        return connectionsService.removeDuplicates();
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public void createUser(String sub, String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        UserInfosEntity userInfosEntity = new UserInfosEntity(sub);
        userAdminRepository.save(userInfosEntity);
    }

    public void delete(UUID id, String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        userAdminRepository.deleteById(id);
    }

    public boolean subExists(String sub) {
        Boolean isAllowed = (applicationProps.getAdmins().isEmpty() && userAdminRepository.count() == 0) || !userAdminRepository.findAllBySub(sub).isEmpty();
        connectionsService.recordConnectionAttempt(sub, isAllowed);
        return isAllowed.booleanValue();
    }

    private boolean isAdmin(String sub) {
        return applicationProps.getAdmins().contains(sub);
    }
}
