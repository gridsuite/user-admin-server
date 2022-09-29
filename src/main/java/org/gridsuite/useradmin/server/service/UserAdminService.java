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
import org.gridsuite.useradmin.server.repository.ConnectionRepository;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class UserAdminService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAdminService.class);
    private UserAdminRepository userAdminRepository;

    private ConnectionRepository connectionRepository;

    @Autowired
    private UserAdminApplicationProps applicationProps;

    public UserAdminService(UserAdminRepository userAdminRepository, ConnectionRepository connectionRepository) {
        this.userAdminRepository = Objects.requireNonNull(userAdminRepository);
        this.connectionRepository = Objects.requireNonNull(connectionRepository);
    }

    public List<UserInfosEntity> getUsers(String userId) {
        if (!isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        return userAdminRepository.findAll();
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
        recordConnectionAttempt(sub, isAllowed);
        return isAllowed.booleanValue();
    }

    public void recordConnectionAttempt(String sub, Boolean isAllowed) {
        ConnectionEntity connectionEntity =  connectionRepository.findBySub(sub);
        if (connectionEntity == null) {
            connectionEntity = new ConnectionEntity(sub, LocalDateTime.now(), LocalDateTime.now(), isAllowed);
            try {
                connectionRepository.save(connectionEntity);
            } catch (DataIntegrityViolationException e) {
                LOGGER.info("User connection already recorded.");
            }
        } else {
            connectionEntity.setLastConnexionDate(LocalDateTime.now());
            connectionEntity.setConnectionAccepted(isAllowed);
            connectionRepository.save(connectionEntity);
        }
    }

    private boolean isAdmin(String sub) {
        return applicationProps.getAdmins().contains(sub);
    }
}
