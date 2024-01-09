/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "connection", indexes = {@Index(name = "connection_sub_index", columnList = "sub")})
public class ConnectionEntity extends AbstractUserEntity {
    @Column(name = "firstConnectionDate", nullable = false)
    private LocalDateTime firstConnectionDate;

    @Column(name = "lastConnectionDate", nullable = false)
    private LocalDateTime lastConnectionDate;

    @Column(name = "connectionAccepted", nullable = false)
    private Boolean connectionAccepted;

    public ConnectionEntity(String sub, LocalDateTime firstConnectionDate, LocalDateTime lastConnectionDate, Boolean connectionAccepted) {
        super(sub);
        this.firstConnectionDate = firstConnectionDate;
        this.lastConnectionDate = lastConnectionDate;
        this.connectionAccepted = connectionAccepted;
    }
}
