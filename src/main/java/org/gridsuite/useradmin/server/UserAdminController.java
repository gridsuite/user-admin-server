/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.service.ConnectionsService;
import org.gridsuite.useradmin.server.service.UserAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION)
@Tag(name = "User admin server")
@ApiResponse(responseCode = "403", description = "The current user haven't right to ask these data")
public class UserAdminController {
    private final UserAdminService service;
    private final ConnectionsService connService;

    public UserAdminController(UserAdminService userService, ConnectionsService connService) {
        this.service = userService;
        this.connService = connService;
    }

    @GetMapping(value = "/users", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get the users")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "200", description = "The users list")
    public ResponseEntity<List<UserInfos>> getUsers(@RequestHeader("userId") String userId,
                                                    @RequestParam(value = "search", required = false) Optional<String> searchTerm) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(searchTerm
                .map(term -> service.searchUsers(term, userId))
                .orElseGet(() -> service.getUsers(userId)));
    }

    @GetMapping(value = "/users/{sub}")
    @Operation(summary = "Get the user informations")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "200", description = "The user exist")
    @ApiResponse(responseCode = "404", description = "The user doesn't exist")
    public ResponseEntity<UserInfos> getUser(@PathVariable("sub") String sub, @RequestHeader("userId") String userId) {
        //TODO authorize if userId == sub (if user ask own data)
        return ResponseEntity.of(service.getUser(sub, userId));
    }

    @PutMapping(value = "/users/{sub}")
    @Operation(summary = "Create the user")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "201", description = "The user has been created")
    public ResponseEntity<Void> createUser(@PathVariable("sub") String sub, @RequestHeader("userId") String userId) {
        service.createUser(sub, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping(value = "/users/{sub}")
    @Operation(summary = "delete the user")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> deleteUser(@RequestHeader("userId") String userId, @PathVariable("sub") String sub) {
        if (service.delete(sub, userId) > 0L) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/users/{sub}", method = RequestMethod.HEAD)
    @Operation(summary = "Test if a user exists and record connection attempt")
    @ApiResponse(responseCode = "200", description = "sub exists")
    @ApiResponse(responseCode = "204", description = "sub does not exist")
    public ResponseEntity<Void> userExists(@PathVariable("sub") String sub) {
        return service.subExists(sub) ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/users/me/isElevatedUser", method = RequestMethod.HEAD)
    @Operation(summary = "Test if a user exists and is administrator (record connection attempt)")
    @ApiResponse(responseCode = "200", description = "user authorized and admin")
    public ResponseEntity<Void> userIsAdmin(@RequestHeader("userId") String userId) {
        return service.userIsAuthorizedAdmin(userId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping(value = "/connections/dedup", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get the connections", deprecated = true)
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "200", description = "The connections list")
    public ResponseEntity<List<UserConnection>> getConnections(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getConnections(userId));
    }

    @GetMapping(value = "/connections", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get the connections")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "200", description = "The connections list")
    public ResponseEntity<List<UserConnection>> getPageConnections(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(connService.getConnections(userId));
    }
}
