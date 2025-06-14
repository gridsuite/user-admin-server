/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.dto;

import org.gridsuite.useradmin.server.entity.AnnouncementSeverity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record Announcement(UUID id, Instant startDate, Instant endDate, String message, AnnouncementSeverity severity) {
    public long remainingTimeMs() {
        return Duration.between(Instant.now(), this.endDate()).toMillis();
    }
}
