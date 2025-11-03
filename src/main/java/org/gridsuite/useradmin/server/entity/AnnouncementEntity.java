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
import org.gridsuite.useradmin.server.dto.Announcement;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "announcement", indexes = @Index(name = "start_end_date_index", columnList = "startDate, endDate"))
public class AnnouncementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Column(name = "notified", nullable = false)
    private boolean notified = false;

    public AnnouncementEntity(Instant startDate, Instant endDate, String message, AnnouncementSeverity severity) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.message = message;
        this.severity = severity;
    }

    public Announcement toDto() {
        return new Announcement(
                        this.getId(),
                        this.getStartDate(),
                        this.getEndDate(),
                        this.getMessage(),
                        this.getSeverity()
                );
    }
}
