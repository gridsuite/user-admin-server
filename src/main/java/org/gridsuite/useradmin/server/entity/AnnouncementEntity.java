/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "announcement")
public class AnnouncementEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "startDate", nullable = false)
    private Instant startDate;

    @Column(name = "endDate", nullable = false)
    private Instant endDate;

    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AnnouncementSeverity severity;

    @Column(name = "notified")
    private boolean notified = false;

    public AnnouncementEntity(UUID id, Instant startDate, Instant endDate, String message, AnnouncementSeverity severity) {
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.message = message;
        this.severity = severity;
    }
}
