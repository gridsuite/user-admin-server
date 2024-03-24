/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.entity;

import jakarta.persistence.*;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.useradmin.server.dto.ParameterInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;

import java.util.UUID;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_profile")
public class UserProfileEntity {

    public UserProfileEntity(String name) {
        this(UUID.randomUUID(), name, null);
    }

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "loadflow_parameter_id", foreignKey = @ForeignKey(name = "loadflow_parameter_id_fk_constraint"))
    private ParameterEntity loadFlowParameter;

    public static UserProfile toDto(@Nullable final UserProfileEntity entity) {
        if (entity == null) {
            return null;
        }
        ParameterInfos lf = entity.getLoadFlowParameter() == null ? null : ParameterEntity.toDto(entity.getLoadFlowParameter());
        return new UserProfile(entity.getId(), entity.getName(), lf);
    }

    public void update(UserProfile userProfile) {
        this.setName(userProfile.name());
        if (userProfile.loadFlowParameter() == null) {
            this.setLoadFlowParameter(null);
        } else {
            loadFlowParameter = new ParameterEntity(userProfile.loadFlowParameter());
        }
    }
}

