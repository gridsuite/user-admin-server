/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.useradmin.server.repository;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Florent MILLOT <florent.millot at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "announcement")
public class AnnouncementEntity {

    public AnnouncementEntity(String message, Duration duration) {
        this.message = message;
        this.duration = duration;
    }

    @Id
    @GeneratedValue
    @Column
    private UUID id;

    @Column(nullable = false)
    private Instant creationDate = Instant.now();

    @Column(nullable = false)
    private String message;

    @Column
    private Duration duration;

}
