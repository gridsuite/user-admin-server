/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.useradmin.server.dto.UserInfos;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_infos", indexes = {@Index(name = "user_infos_sub_index", columnList = "sub")})
public class UserInfosEntity {

    public UserInfosEntity(String sub) {
        this(UUID.randomUUID(), sub);
    }

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "sub", nullable = false, unique = true)
    private String sub;

    public static UserInfos toDto(@Nullable final UserInfosEntity entity, Predicate<String> isAdminFn) {
        return entity == null ? null : new UserInfos(entity.getSub(), isAdminFn.test(entity.getSub()));
    }
}
