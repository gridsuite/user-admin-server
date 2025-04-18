/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.dto;

import org.gridsuite.useradmin.server.entity.AnnouncementEntity;

import java.time.Duration;

public final class AnnouncementMapper {

    private AnnouncementMapper() {
        // Could not be instantiated
    }

    public static Announcement fromEntity(AnnouncementEntity entity) {
        return entity == null ? null :
            new Announcement(
                entity.getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getMessage(),
                entity.getSeverity(),
                Duration.between(entity.getStartDate(), entity.getEndDate()).toMillis()
            );
    }
}
