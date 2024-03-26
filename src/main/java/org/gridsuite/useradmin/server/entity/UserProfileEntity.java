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
import org.gridsuite.useradmin.server.dto.UserProfile;

import java.util.Set;
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

    @Column(name = "loadFlowParameterId")
    private UUID loadFlowParameterId;

    public static UserProfile toDto(@Nullable final UserProfileEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserProfile(entity.getId(), entity.getName(), entity.getLoadFlowParameterId(), null);
    }

    public static UserProfile toDto(@Nullable final UserProfileEntity entity, Set<UUID> missingParameters) {
        if (entity == null) {
            return null;
        }
        Boolean globalValidity = null;
        if (entity.getLoadFlowParameterId() != null) {
            globalValidity = !missingParameters.contains(entity.getLoadFlowParameterId());
        }
        return new UserProfile(entity.getId(), entity.getName(), entity.getLoadFlowParameterId(), globalValidity);
    }

    public void update(UserProfile userProfile) {
        this.setName(userProfile.name());
        this.setLoadFlowParameterId(userProfile.loadFlowParameterId());
    }
}

