/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.service.ConnectionsService;
import org.gridsuite.useradmin.server.service.UserAdminService;
import org.gridsuite.useradmin.server.springdoc.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION)
@Tag(name = "User admin server")
public class UserAdminController {
    private static final int PAGE_DEFAULT_SIZE = 25;
    private static final String PAGE_DEFAULT_SIZE_DOC = "" + PAGE_DEFAULT_SIZE;

    private final UserAdminService service;
    private final ConnectionsService connService;

    public UserAdminController(UserAdminService userService, ConnectionsService connService) {
        this.service = userService;
        this.connService = connService;
    }

    @GetMapping(value = "/users", params = "!search", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get the users")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "200", description = "The users list")
    @PageableAsQueryParam(defaultSize = @Schema(type = "integer", defaultValue = PAGE_DEFAULT_SIZE_DOC),
                          defaultSort = @ArraySchema(schema = @Schema(type = "string", defaultValue = "sub")))
    public ResponseEntity<Page<UserInfos>> getUsers(@RequestHeader("userId") String userId,
                                                    @PageableDefault(size = PAGE_DEFAULT_SIZE, sort = {"sub"}) Pageable pageable) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getUsers(userId, pageable));
    }

    @GetMapping(value = "/users", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "search users")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "200", description = "The users search list")
    @PageableAsQueryParam(defaultSize = @Schema(type = "integer", defaultValue = PAGE_DEFAULT_SIZE_DOC),
                          defaultSort = @ArraySchema(schema = @Schema(type = "string", defaultValue = "sub")))
    public ResponseEntity<Page<UserInfos>> searchUsers(@RequestHeader("userId") String userId,
                                                       @RequestParam("search") String searchTerm,
                                                       @PageableDefault(size = PAGE_DEFAULT_SIZE, sort = {"sub"}) Pageable pageable) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.searchUsers(userId, searchTerm, pageable));
    }

    @PutMapping(value = "/users/{sub}")
    @Operation(summary = "Create the user")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "The user has been created")})
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
        if (service.delete(userId, sub) > 0L) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = "/users/{sub}", method = RequestMethod.HEAD)
    @Operation(summary = "Test if a user exists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "sub exists"),
        @ApiResponse(responseCode = "204", description = "sub does not exist"),
    })
    public ResponseEntity<Void> userExists(@PathVariable("sub") String sub) {
        return service.subExists(sub) ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/connections/full", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get the connections", deprecated = true)
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "200", description = "The connections list")
    public ResponseEntity<List<UserConnection>> getConnections(@RequestHeader("userId") String userId) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getConnections(userId));
    }

    @GetMapping(value = "/connections", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get the connections")
    @SecurityRequirement(name = "userType", scopes = {"admin"})
    @ApiResponse(responseCode = "200", description = "The connections paged list")
    @PageableAsQueryParam(defaultSize = @Schema(type = "integer", defaultValue = PAGE_DEFAULT_SIZE_DOC),
                          defaultSort = @ArraySchema(schema = @Schema(type = "string", defaultValue = "sub")))
    public ResponseEntity<Page<UserConnection>> getPageConnections(@RequestHeader("userId") String userId,
                                                                   @PageableDefault(size = PAGE_DEFAULT_SIZE, sort = {"sub"}) Pageable pageable) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(connService.getConnections(userId, pageable));
    }
}
