/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import org.gridsuite.useradmin.server.UserAdminApi;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.service.UserAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 *
 * @implNote /!\ TO DEV: remember to maintain list access restricted in operations' description
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION)
@Tag(name = "UserAdminController", description = "User admin server")
@ApiResponse(responseCode = "403", description = "The current user does not have right to ask these data")
public class UserAdminController {
    private final UserAdminService service;

    public UserAdminController(UserAdminService userService) {
        this.service = userService;
    }

    @GetMapping(value = "/users", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get the users", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The users list")
    public ResponseEntity<List<UserInfos>> getUsers(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getUsers(userId));
    }

    @DeleteMapping(value = "/users", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "delete the users", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "204", description = "Users deleted")
    @ApiResponse(responseCode = "404", description = "One or more user(s) not found")
    public ResponseEntity<Void> deleteUser(@RequestHeader("userId") String userId, @RequestBody @NotEmpty List<String> subs) {
        if (service.delete(subs, userId) > 0L) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/users/{sub}")
    @Operation(summary = "Get the user informations", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The user exist")
    @ApiResponse(responseCode = "404", description = "The user doesn't exist")
    public ResponseEntity<UserInfos> getUser(@PathVariable("sub") String sub, @RequestHeader("userId") String userId) {
        return ResponseEntity.of(service.getUser(sub, userId));
    }

    @PostMapping(value = "/users/{sub}")
    @Operation(summary = "Create the user", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "201", description = "The user has been created")
    public ResponseEntity<Void> createUser(@PathVariable("sub") String sub, @RequestHeader("userId") String userId) {
        service.createUser(sub, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping(value = "/users/{sub}")
    @Operation(summary = "delete the user", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> deleteUser(@RequestHeader("userId") String userId, @PathVariable("sub") String sub) {
        if (service.delete(sub, userId) > 0L) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/users/{sub}")
    @Operation(summary = "update a user", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The user is updated")
    @ApiResponse(responseCode = "404", description = "The user does not exist")
    public ResponseEntity<UserProfile> updateUser(@PathVariable("sub") String sub,
                                                  @RequestHeader("userId") String userId,
                                                  @RequestBody UserInfos userInfos) {
        service.updateUser(sub, userId, userInfos);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/users/{sub}", method = RequestMethod.HEAD)
    @Operation(summary = "Test if a user exists and record connection attempt")
    @ApiResponse(responseCode = "200", description = "sub exists")
    @ApiResponse(responseCode = "204", description = "sub does not exist")
    public ResponseEntity<Void> userExists(@PathVariable("sub") String sub) {
        return service.subExists(sub) ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/users/{sub}/isAdmin", method = RequestMethod.HEAD)
    @Operation(summary = "Test if a user exists and is administrator (record connection attempt)")
    @ApiResponse(responseCode = "200", description = "user authorized and admin")
    public ResponseEntity<Void> userIsAdmin(@PathVariable("sub") String userId) {
        return service.userIsAdmin(userId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping(value = "/users/{sub}/profile")
    @Operation(summary = "Get the user's profile")
    @ApiResponse(responseCode = "200", description = "The user profile")
    @ApiResponse(responseCode = "404", description = "The user doesn't exist")
    public ResponseEntity<UserProfile> getUserProfile(@PathVariable("sub") String sub) {
        return ResponseEntity.of(service.getUserProfile(sub));
    }

    @GetMapping(value = "/users/{sub}/profile/max-cases")
    @Operation(summary = "Get the user's max allowed cases")
    @ApiResponse(responseCode = "200", description = "The user max allowed cases created")
    @ApiResponse(responseCode = "404", description = "The user doesn't exist")
    public ResponseEntity<Integer> getUserProfileMaxStudies(@PathVariable("sub") String sub) {
        return ResponseEntity.ok().body(service.getUserProfileMaxAllowedCases(sub));
    }

    @GetMapping(value = "/users/{sub}/profile/max-builds")
    @Operation(summary = "Get the user's max allowed builds")
    @ApiResponse(responseCode = "200", description = "The user max allowed builds")
    @ApiResponse(responseCode = "404", description = "The user doesn't exist")
    public ResponseEntity<Integer> getUserProfileMaxAllowedBuilds(@PathVariable("sub") String sub) {
        return ResponseEntity.ok().body(service.getUserProfileMaxAllowedBuilds(sub));
    }

    @GetMapping(value = "/connections", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get the connections", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The connections list")
    public ResponseEntity<List<UserConnection>> getConnections(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getConnections(userId));
    }

    @PostMapping(value = "/messages/maintenance")
    @Operation(summary = "send a message to all users connected")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "message sent"),
        @ApiResponse(responseCode = "403", description = "user is not an admin")
    })
    public ResponseEntity<Void> sendMaintenanceMessage(@RequestHeader("userId") String userId,
                                                       @Parameter(description = "the display time of the message in seconds") @RequestParam(value = "durationInSeconds", required = false) Integer duration,
                                                       @Parameter(description = "the message to display") @RequestBody String message) {
        service.sendMaintenanceMessage(userId, duration, message);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/messages/cancel-maintenance")
    @Operation(summary = "send a message to all users connected")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "message sent"),
        @ApiResponse(responseCode = "403", description = "user is not an admin")
    })
    public ResponseEntity<Void> sendCancelMaintenanceMessage(@RequestHeader("userId") String userId) {
        service.sendCancelMaintenanceMessage(userId);
        return ResponseEntity.ok().build();
    }
}
