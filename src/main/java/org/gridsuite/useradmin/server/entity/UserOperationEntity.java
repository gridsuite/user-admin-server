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

import java.time.Instant;
import java.util.UUID;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_operation",
        uniqueConstraints = @UniqueConstraint(name = "user_operation_sub_operation_id_uk", columnNames = {"sub", "operation_id"}),
        indexes = {@Index(name = "user_operation_sub_index", columnList = "sub")})
public class UserOperationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "sub", nullable = false)
    private String sub;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private QuotaType quotaType;

    @Column(name = "quota_date", nullable = false)
    private Instant quotaDate;

    public UserOperationEntity(String sub, UUID operationId, QuotaType quotaType, Instant quotaDate) {
        this.sub = sub;
        this.operationId = operationId;
        this.quotaType = quotaType;
        this.quotaDate = quotaDate;
    }
}
