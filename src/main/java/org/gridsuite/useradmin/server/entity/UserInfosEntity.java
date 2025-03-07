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
import org.gridsuite.useradmin.server.dto.UserInfos;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        this(UUID.randomUUID(), sub, null, null);
    }

    @Id
    @Column(name = "id")
    private UUID id;

    // TODO rename to subject or userName
    @Column(name = "sub", nullable = false, unique = true)
    private String sub;

    @ManyToOne
    @JoinColumn(name = "profile_id", foreignKey = @ForeignKey(name = "profile_id_fk_constraint"))
    private UserProfileEntity profile;

    @ManyToMany(mappedBy = "users")
    private Set<GroupInfosEntity> groups;

    private UserInfos toUserInfos(Predicate<String> isAdminFn,
                                  Integer maxAllowedCases,
                                  Integer numberCasesUsed,
                                  Integer maxAllowedBuilds) {
        String profileName = getProfile() == null ? null : getProfile().getName();
        Set<String> groupNames = getGroups() == null ? null : getGroups().stream().map(GroupInfosEntity::getName).collect(Collectors.toSet());
        return new UserInfos(getSub(), isAdminFn.test(getSub()), profileName, maxAllowedCases, numberCasesUsed, maxAllowedBuilds, groupNames);
    }

    public static UserInfos toDto(@Nullable final UserInfosEntity entity, Predicate<String> isAdminFn) {
        return entity == null ? null : entity.toUserInfos(isAdminFn, null, null, null);
    }

    public static UserInfos toDtoWithDetail(@Nullable final UserInfosEntity entity, Predicate<String> isAdminFn, Integer maxAllowedCases, Integer numberCasesUsed, Integer maxAllowedBuilds) {
        return entity == null ? null : entity.toUserInfos(isAdminFn, maxAllowedCases, numberCasesUsed, maxAllowedBuilds);
    }
}
