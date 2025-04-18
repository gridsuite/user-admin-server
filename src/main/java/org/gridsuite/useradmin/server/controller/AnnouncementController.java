/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.useradmin.server.UserAdminApi;
import org.gridsuite.useradmin.server.dto.Announcement;
import org.gridsuite.useradmin.server.service.AnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION + "/announcements")
@Tag(name = "AnnouncementController")
@ApiResponse(responseCode = "403", description = "The current user does not have right to ask these data")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService announcementService) {
        this.service = announcementService;
    }

    @GetMapping
    @Operation(summary = "get the list of announcements")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "the list of announcements"),
        @ApiResponse(responseCode = "403", description = "User is not an admin")
    })
    public ResponseEntity<List<Announcement>> getAnnouncements(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok().body(service.getAnnouncements(userId));
    }

    @GetMapping("/current")
    @Operation(summary = "get current announcement if it exists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "the current announcement"),
        @ApiResponse(responseCode = "204", description = "there is no current announcement"),
        @ApiResponse(responseCode = "403", description = "User is not an admin")
    })
    public ResponseEntity<Announcement> getCurrentAnnouncement(@RequestHeader("userId") String userId) {
        return service.getCurrentAnnouncement(userId).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping(value = "/{announcementId}")
    @Operation(summary = "Create an announcement")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "the created announcement"),
        @ApiResponse(responseCode = "403", description = "User is not an admin"),
        @ApiResponse(responseCode = "409", description = "There is a conflict in display time")
    })
    public ResponseEntity<Announcement> createAnnouncement(@RequestHeader("userId") String userId,
                                                   @PathVariable("announcementId") UUID announcementId,
                                                   @RequestParam("startDate") Instant startDate,
                                                   @RequestParam("endDate") Instant endDate,
                                                   @RequestParam("severity") String severity,
                                                   @RequestBody String message) {
        return ResponseEntity.ok().body(service.createAnnouncement(announcementId, startDate, endDate, message, severity, userId));
    }

    @DeleteMapping(value = "/{announcementId}")
    @Operation(summary = "Delete an announcement")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Announcement deleted"),
        @ApiResponse(responseCode = "403", description = "User is not an admin"),
    })
    public ResponseEntity<Void> deleteAnnouncement(@RequestHeader("userId") String userId,
                                                   @PathVariable("announcementId") UUID announcementId) {
        service.deleteAnnouncement(announcementId, userId);
        return ResponseEntity.ok().build();
    }
}
