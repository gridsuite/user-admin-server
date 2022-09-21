/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.gridsuite.useradmin.server.service.UserAdminService;
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
    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping(value = "/users")
    @Operation(summary = "get the users ids")
    @ApiResponse(responseCode = "200", description = "The users list")
    public ResponseEntity<List<UserInfosEntity>> getUsers() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getUsers());
    }

    @PostMapping(value = "/users/{sub}")
    @Operation(summary = "Create the user")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The user has been created")})
    public ResponseEntity<Void> createUser(@PathVariable("sub") String sub) {
        service.createUser(sub);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/users/{sub}")
    @Operation(summary = "delete the user")
    @ApiResponse(responseCode = "200", description = "User deleted")
    public ResponseEntity<Void> deleteUser(@PathVariable("sub") String sub) {
        service.delete(sub);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/users/{sub}", method = RequestMethod.HEAD)
    @Operation(summary = "Test if a sub exists")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "sub exists"),
            @ApiResponse(responseCode = "204", description = "sub does not exist"),
    })
    public ResponseEntity<Void> userExists(@PathVariable("sub") String sub) {
        return service.subExists(sub) ? ResponseEntity.ok().build() : ResponseEntity.noContent().build();
    }

}
