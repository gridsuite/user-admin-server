/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.repository.AnnouncementEntity;
import org.gridsuite.useradmin.server.repository.AnnouncementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Florent MILLOT <florent.millot at rte-france.com>
 */
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;

    public AnnouncementService(final AnnouncementRepository announcementRepository,
                               final NotificationService notificationService) {
        this.announcementRepository = Objects.requireNonNull(announcementRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
    }

    @Transactional
    public void sendAnnouncement(AnnouncementEntity announcement) {
        this.announcementRepository.deleteAll(); // for now, only one message at a time
        this.announcementRepository.save(announcement);
        if (announcement.getDuration() == null) {
            notificationService.emitMaintenanceMessage(announcement.getMessage());
        } else {
            notificationService.emitMaintenanceMessage(announcement.getMessage(), announcement.getDuration().toSeconds());
        }
    }

    @Transactional
    public void cancelAnnouncement(UUID announcementId) {
        if (!announcementRepository.existsById(announcementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find announcement");
        }
        this.announcementRepository.deleteById(announcementId);
        notificationService.emitCancelMaintenanceMessage();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementEntity> getAllAnnouncements() {
        return this.announcementRepository.findAll();
    }

}
