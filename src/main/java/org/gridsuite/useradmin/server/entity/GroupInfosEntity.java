/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "group_infos", indexes = {@Index(name = "group_infos_name_index", columnList = "name")})
public class GroupInfosEntity {
    public GroupInfosEntity(String name) {
        this(UUID.randomUUID(), name, null);
    }

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany
    @JoinTable(name = "users_groups",
        joinColumns = @JoinColumn(name = "group_infos_id", foreignKey = @ForeignKey(name = "group_infos_id_fk_constraint")),
        inverseJoinColumns = @JoinColumn(name = "user_infos_id", foreignKey = @ForeignKey(name = "user_infos_id_fk_constraint")))
    private Set<UserInfosEntity> users;
}

