/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@NoArgsConstructor
//@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_infos", indexes = {@Index(name = "user_infos_sub_index", columnList = "sub")})
public class UserInfosEntity extends AbstractUserEntity {
    public UserInfosEntity(String sub) {
        super(sub);
    }
}
