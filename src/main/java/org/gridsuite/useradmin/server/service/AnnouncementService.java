/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminException;
import org.gridsuite.useradmin.server.dto.Announcement;
import org.gridsuite.useradmin.server.dto.AnnouncementMapper;
import org.gridsuite.useradmin.server.entity.AnnouncementEntity;
import org.gridsuite.useradmin.server.entity.AnnouncementSeverity;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.useradmin.server.UserAdminException.Type.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class AnnouncementService {

    private final AdminRightService adminRightService;

    private final AnnouncementRepository announcementRepository;

    private final NotificationService notificationService;

    public AnnouncementService(AdminRightService adminRightService, AnnouncementRepository announcementRepository, NotificationService notificationService) {
        this.adminRightService = adminRightService;
        this.announcementRepository = announcementRepository;
        this.notificationService = notificationService;
    }

    public Announcement createAnnouncement(Instant startDate,
                                           Instant endDate,
                                           String message,
                                           String stringSeverity,
                                           String userId) {
        if (!adminRightService.isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }

        if (startDate.equals(endDate)) {
            throw new UserAdminException(SAME_START_END_DATE);
        }

        if (startDate.isAfter(endDate)) {
            throw new UserAdminException(START_DATE_AFTER_END_DATE);
        }

        if (announcementRepository.existsByStartDateLessThanEqualAndEndDateGreaterThanEqual(startDate, endDate)) {
            throw new UserAdminException(OVERLAPPING_ANNOUNCEMENTS);
        }

        AnnouncementSeverity severity;
        try {
            severity = AnnouncementSeverity.valueOf(stringSeverity);
        } catch (IllegalArgumentException e) {
            throw new UserAdminException(SEVERITY_DOES_NOT_EXIST);
        }
        return AnnouncementMapper.fromEntity(announcementRepository.save(new AnnouncementEntity(startDate, endDate, message, severity)));
    }

    public void deleteAnnouncement(UUID announcementId, String userId) {
        if (!adminRightService.isAdmin(userId)) {
            throw new UserAdminException(FORBIDDEN);
        }
        announcementRepository.findById(announcementId).ifPresent(announcement -> {
            Instant now = Instant.now();
            announcementRepository.deleteById(announcement.getId());
            //it means that we are currently in the announcement time window
            if (announcement.getStartDate().isBefore(now) && announcement.getEndDate().isAfter(now)) {
                notificationService.emitCancelAnnouncementMessage();
            }
        });
    }

    public List<Announcement> getAnnouncements(String userId) {
        adminRightService.assertIsAdmin(userId);
        return announcementRepository.findAll().stream().map(AnnouncementMapper::fromEntity).toList();
    }

    public Optional<Announcement> getCurrentAnnouncement() {
        return announcementRepository.findCurrentAnnouncement().map(AnnouncementMapper::fromEntity);
    }
}
