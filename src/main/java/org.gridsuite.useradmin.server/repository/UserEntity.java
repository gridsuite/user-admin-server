/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user", indexes = {@Index(name = "userEntity_sub_index", columnList = "sub")})
public class UserEntity {

    public UserEntity(String sub) {
        this(UUID.randomUUID(), sub);
    }

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "sub", nullable = false)
    private String sub;
}
