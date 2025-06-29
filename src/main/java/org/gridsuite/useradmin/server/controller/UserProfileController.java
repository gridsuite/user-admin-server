/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import org.gridsuite.useradmin.server.UserAdminApi;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.service.UserProfileService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 *
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION + "/profiles")
@Tag(name = "UserProfileController")
@ApiResponse(responseCode = "403", description = "The current user does not have right to ask these data")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService userService) {
        this.service = userService;
    }

    @GetMapping(value = "", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get all the user profiles")
    @ApiResponse(responseCode = "200", description = "The profiles list")
    public ResponseEntity<List<UserProfile>> getProfiles(@RequestHeader("userId") String userId,
                                                         @Parameter(description = "To check if parameters and spreadsheet config collection links are still valid") @RequestParam(name = "checkLinksValidity", required = false, defaultValue = "true") boolean checkLinksValidity) {
        return ResponseEntity.ok().body(service.getProfiles(userId, checkLinksValidity));
    }

    @GetMapping(value = "/{profileUuid}")
    @Operation(summary = "get the profile information", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The profile exist")
    @ApiResponse(responseCode = "404", description = "The profile does not exist")
    public ResponseEntity<UserProfile> getProfile(@PathVariable("profileUuid") UUID profileUuid) {
        return ResponseEntity.of(service.getProfileIfAdmin(profileUuid));
    }

    @PutMapping(value = "/{profileUuid}")
    @Operation(summary = "update a profile", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The profile exists")
    @ApiResponse(responseCode = "404", description = "The profile does not exist")
    public ResponseEntity<UserProfile> updateProfile(@PathVariable("profileUuid") UUID profileUuid,
                                                     @RequestBody UserProfile userProfile) {
        service.updateProfile(profileUuid, userProfile);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "")
    @Operation(summary = "Create the profile", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "201", description = "The profile has been created")
    public ResponseEntity<Void> createProfile(@RequestBody UserProfile userProfile) {
        service.createProfile(userProfile);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping(value = "", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "delete the profiles", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "204", description = "Profiles deleted")
    @ApiResponse(responseCode = "404", description = "One or more profile(s) not found")
    @ApiResponse(responseCode = "422", description = "Integrity issue when a profile is still referenced by users")
    public ResponseEntity<Void> deleteProfiles(@RequestBody @NotEmpty List<String> names) {
        try {
            if (service.deleteProfiles(names) > 0L) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
