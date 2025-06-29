/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.entity.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.ConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Service
public class ConnectionsService {
    private final ConnectionRepository connectionRepository;

    public ConnectionsService(final ConnectionRepository connectionRepository) {
        this.connectionRepository = Objects.requireNonNull(connectionRepository);
    }

    @Transactional
    public void recordConnectionAttempt(String sub, boolean isConnectionAccepted) {
        ConnectionEntity connectionEntity = connectionRepository.findBySub(sub).stream().findFirst().orElse(null);
        if (connectionEntity == null) {
            //To avoid consistency issue, we truncate the time to microseconds since postgres and h2 can only store a precision of microseconds
            connectionEntity = new ConnectionEntity(sub, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
                                                    LocalDateTime.now().truncatedTo(ChronoUnit.MICROS), isConnectionAccepted);
        } else {
            connectionEntity.setLastConnexionDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
            connectionEntity.setConnectionAccepted(isConnectionAccepted);
        }
        connectionRepository.save(connectionEntity);
    }

    @Transactional
    public List<UserConnection> removeDuplicates() {
        Map<String, List<ConnectionEntity>> connectionsBySub = connectionRepository.findAll().stream().collect(Collectors.groupingBy(ConnectionEntity::getSub));
        connectionsBySub.keySet().forEach(sub ->
            connectionsBySub.get(sub).stream().skip(1).forEach(connectionEntity -> {
                ConnectionEntity groupedEntity = connectionsBySub.get(sub).get(0);
                if (connectionEntity.getLastConnexionDate().isAfter(groupedEntity.getLastConnexionDate())) {
                    groupedEntity.setLastConnexionDate(connectionEntity.getLastConnexionDate());
                }
                if (connectionEntity.getFirstConnexionDate().isBefore(groupedEntity.getFirstConnexionDate())) {
                    groupedEntity.setFirstConnexionDate(connectionEntity.getFirstConnexionDate());
                }
                connectionRepository.delete(connectionEntity);
            })
        );
        return connectionsBySub.values().stream().map(list -> list.get(0)).map(ConnectionEntity::toDto).toList();
    }
}
