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
import jakarta.validation.constraints.NotEmpty;
import org.gridsuite.useradmin.server.UserAdminApi;
import org.gridsuite.useradmin.server.dto.UserGroup;
import org.gridsuite.useradmin.server.service.UserGroupService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 *
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION + "/groups")
@Tag(name = "UserGroupController")
@ApiResponse(responseCode = "403", description = "The current user does not have right to ask these data")
public class UserGroupController {
    private final UserGroupService service;

    public UserGroupController(UserGroupService userService) {
        this.service = userService;
    }

    @GetMapping(value = "", produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "get all the groups")
    @ApiResponse(responseCode = "200", description = "The groups set")
    public ResponseEntity<Set<UserGroup>> getGroups() {
        return ResponseEntity.ok().body(service.getGroups());
    }

    @GetMapping(value = "/{group}")
    @Operation(summary = "Get the group informations", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The group exist")
    @ApiResponse(responseCode = "404", description = "The group doesn't exist")
    public ResponseEntity<UserGroup> getGroup(@PathVariable("group") String group) {
        return ResponseEntity.of(service.getGroupIfAdmin(group));
    }

    @PutMapping(value = "/{groupUuid}")
    @Operation(summary = "update a group", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The group exists")
    @ApiResponse(responseCode = "404", description = "The group does not exist")
    public ResponseEntity<Void> updateGroup(@PathVariable("groupUuid") UUID groupUuid,
                                            @RequestBody UserGroup userGroup) {
        service.updateGroup(groupUuid, userGroup);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{group}")
    @Operation(summary = "Create the group", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "201", description = "The group has been created")
    @ApiResponse(responseCode = "400", description = "The group already exists")
    public ResponseEntity<Void> createGroup(@PathVariable("group") String group) {
        service.createGroup(group);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping(value = "", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "delete the groups", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "204", description = "Groups deleted")
    @ApiResponse(responseCode = "404", description = "One or more group(s) not found")
    @ApiResponse(responseCode = "422", description = "Integrity issue when a group is still referenced by users")
    public ResponseEntity<Void> deleteGroups(@RequestBody @NotEmpty List<String> names) {
        try {
            if (service.deleteGroups(names) > 0L) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }
}
