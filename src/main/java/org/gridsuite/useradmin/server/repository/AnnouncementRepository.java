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

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, UUID> {

    boolean existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(@NonNull Instant endDate, @NonNull Instant startDate);

    @Query("SELECT e FROM AnnouncementEntity e WHERE e.startDate < :now AND e.endDate > :now")
    Optional<AnnouncementEntity> findCurrentAnnouncement(@NonNull Instant now);

    @Transactional
    @Modifying
    @Query("delete from AnnouncementEntity a where a.startDate < :now and a.endDate < :now")
    int deleteExpiredAnnouncements(Instant now);

}
