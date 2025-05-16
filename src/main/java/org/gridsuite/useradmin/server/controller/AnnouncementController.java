/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.gridsuite.useradmin.server.UserAdminApi;
import org.gridsuite.useradmin.server.dto.Announcement;
import org.gridsuite.useradmin.server.entity.AnnouncementSeverity;
import org.gridsuite.useradmin.server.service.AnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION + "/announcements")
@Tag(name = "AnnouncementController")
@ApiResponse(responseCode = "403", description = "The current user does not have right to ask these data")
@Validated
@AllArgsConstructor
public class AnnouncementController {
    private final AnnouncementService service;

    @GetMapping
    @Operation(summary = "get the list of announcements")
    @ApiResponse(responseCode = "200", description = "the list of announcements")
    @ApiResponse(responseCode = "403", description = "User is not an admin")
    public ResponseEntity<List<Announcement>> getAnnouncements() {
        return ResponseEntity.ok(service.getAnnouncements());
    }

    @GetMapping("/current")
    @Operation(summary = "get current announcement if it exists")
    @ApiResponse(responseCode = "200", description = "the current announcement")
    @ApiResponse(responseCode = "204", description = "there is no current announcement")
    @ApiResponse(responseCode = "403", description = "User is not an admin")
    public ResponseEntity<Announcement> getCurrentAnnouncement() {
        return service.getCurrentAnnouncement().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    @Operation(summary = "Create an announcement")
    @ApiResponse(responseCode = "200", description = "the created announcement")
    @ApiResponse(responseCode = "403", description = "User is not an admin")
    @ApiResponse(responseCode = "409", description = "There is a conflict in display time")
    public ResponseEntity<Announcement> createAnnouncement(@RequestParam("startDate") Instant startDate,
                                                           @RequestParam("endDate") @Future Instant endDate,
                                                           @RequestParam("severity") AnnouncementSeverity severity,
                                                           @RequestBody @NotBlank String message) {
        return ResponseEntity.ok(service.createAnnouncement(startDate, endDate, message, severity));
    }

    @DeleteMapping(value = "/{announcementId}")
    @Operation(summary = "Delete an announcement")
    @ApiResponse(responseCode = "200", description = "Announcement deleted")
    @ApiResponse(responseCode = "403", description = "User is not an admin")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable("announcementId") UUID announcementId) {
        service.deleteAnnouncement(announcementId);
        return ResponseEntity.ok().build();
    }
}
