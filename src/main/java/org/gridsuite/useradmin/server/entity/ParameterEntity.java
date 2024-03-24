/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
import org.gridsuite.useradmin.server.dto.ParameterInfos;

import java.util.UUID;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "parameter")
public class ParameterEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID parameterId;

    @Column(nullable = false)
    private String fullName;

    public ParameterEntity(ParameterInfos parameterInfos) {
        parameterId = parameterInfos.parameterId();
        fullName = parameterInfos.fullName();
    }

    public static ParameterInfos toDto(@Nullable final ParameterEntity entity) {
        return entity == null ? null : new ParameterInfos(entity.getId(), entity.getParameterId(), entity.fullName);
    }
}
