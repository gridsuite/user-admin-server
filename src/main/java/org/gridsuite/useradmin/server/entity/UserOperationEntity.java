/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.useradmin.server.dto.QuotaType;

import java.util.UUID;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_quotas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sub", "operation_id"}),
        indexes = {@Index(name = "user_operation_sub_index", columnList = "sub")})
public class UserOperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub",
            referencedColumnName = "sub", nullable = false,
            foreignKey = @ForeignKey(name = "userOperation_userInfo_sub_fk_constraint"))
    private UserInfosEntity userInfos;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private QuotaType quotaType;

    public UserOperationEntity(UserInfosEntity userInfos, UUID operationId, QuotaType quotaType) {
        this.userInfos = userInfos;
        this.operationId = operationId;
        this.quotaType = quotaType;
    }
}
