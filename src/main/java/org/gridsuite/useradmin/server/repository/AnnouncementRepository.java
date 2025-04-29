/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import org.gridsuite.useradmin.server.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, UUID> {

    /**
     * Check if an announcement overlap with the period asked.
     * @implSpec Two periods {@code [start1, end1)} and {@code [start2, end2)} overlap if: {@code start1 < end2 AND end1 > start2}.
     */
    boolean existsByStartDateLessThanAndEndDateGreaterThan(@NonNull Instant endDate, @NonNull Instant startDate);

    /**
     * Find announcements that isn't <strike>expired</strike> finished (current and future ones).
     */
    @Query("SELECT e FROM AnnouncementEntity e WHERE CURRENT_TIMESTAMP <= e.endDate ORDER BY e.startDate ASC, e.endDate ASC")
    Stream<AnnouncementEntity> findAnnouncements();

    @Query("SELECT e FROM AnnouncementEntity e WHERE e.startDate <= CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP <= e.endDate ORDER BY e.startDate ASC, e.endDate ASC LIMIT 1")
    Optional<AnnouncementEntity> findCurrentAnnouncement();

    @Transactional
    @Modifying
    @Query("DELETE FROM AnnouncementEntity e WHERE e.endDate < CURRENT_TIMESTAMP")
    long deleteExpiredAnnouncements();
}
