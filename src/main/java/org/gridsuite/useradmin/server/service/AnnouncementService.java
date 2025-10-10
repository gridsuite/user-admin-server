/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import lombok.AllArgsConstructor;
import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.dto.Announcement;
import org.gridsuite.useradmin.server.entity.AnnouncementEntity;
import org.gridsuite.useradmin.server.entity.AnnouncementSeverity;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
@AllArgsConstructor
public class AnnouncementService {
    private final AdminRightService adminRightService;
    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;

    public Announcement createAnnouncement(Instant startDate,
                                           Instant endDate,
                                           String message,
                                           AnnouncementSeverity severity) {
        adminRightService.assertIsAdmin();
        if (!startDate.isBefore(endDate)) { // internally compare in seconds
            throw UserAdminException.announcementInvalidPeriod(startDate, endDate);
        }
        // Start is inclusive, End is exclusive — [start, end)
        if (announcementRepository.existsByStartDateLessThanAndEndDateGreaterThan(endDate, startDate)) {
            throw UserAdminException.announcementOverlap(startDate, endDate);
        }
        return announcementRepository.save(new AnnouncementEntity(startDate, endDate, message.trim(), severity)).toDto();
    }

    public void deleteAnnouncement(UUID announcementId) {
        adminRightService.assertIsAdmin();
        announcementRepository.findById(announcementId).ifPresent(announcement -> {
            final Instant now = Instant.now();
            announcementRepository.deleteById(announcement.getId());
            //it means that we are currently in the announcement time window
            if (announcement.getStartDate().isBefore(now) && announcement.getEndDate().isAfter(now)) {
                notificationService.emitCancelAnnouncementMessage();
            }
        });
    }

    public List<Announcement> getAnnouncements() {
        adminRightService.assertIsAdmin();
        return announcementRepository.findAnnouncements().stream().map(AnnouncementEntity::toDto).toList();
    }

    public Optional<Announcement> getCurrentAnnouncement() {
        return announcementRepository.findCurrentAnnouncement().map(AnnouncementEntity::toDto);
    }
}
