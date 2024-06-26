/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.useradmin.server.Utils;
import org.gridsuite.useradmin.server.dto.UserConnection;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "connection", indexes = {@Index(name = "connection_sub_index", columnList = "sub")})
public class ConnectionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "sub", nullable = false)
    private String sub;

    @Column(name = "firstConnexionDate", nullable = false)
    private LocalDateTime firstConnexionDate;

    @Column(name = "lastConnexionDate", nullable = false)
    private LocalDateTime lastConnexionDate;

    @Column(name = "connectionAccepted", nullable = false)
    private Boolean connectionAccepted;

    public ConnectionEntity(String sub, LocalDateTime firstConnexionDate, LocalDateTime lastConnexionDate, Boolean connectionAccepted) {
        this(UUID.randomUUID(), sub, firstConnexionDate, lastConnexionDate, connectionAccepted);
    }

    public static UserConnection toDto(@Nullable final ConnectionEntity entity) {
        return entity == null ? null : new UserConnection(entity.getSub(), Utils.convert(entity.getFirstConnexionDate()),
                Utils.convert(entity.getLastConnexionDate()), Objects.requireNonNullElse(entity.getConnectionAccepted(), Boolean.FALSE));
    }
}
