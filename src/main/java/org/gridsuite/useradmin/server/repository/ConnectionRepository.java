/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Repository
public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID> {
    @NonNull
    Optional<ConnectionEntity> findBySub/*IgnoreCase*/(@NonNull String sub);

    @Transactional()
    @Modifying
    default void recordNewConnection(@NonNull final String sub, final boolean connectionAccepted) {
        this.findBySub/*IgnoreCase*/(sub).ifPresentOrElse(
            conn -> this.save(conn.setLastConnexionDate(LocalDateTime.now()).setConnectionAccepted(connectionAccepted)),
            () -> this.save(new ConnectionEntity(sub, LocalDateTime.now(), LocalDateTime.now(), connectionAccepted))
        );
    }
}
