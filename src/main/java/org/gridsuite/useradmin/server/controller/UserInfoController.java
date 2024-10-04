/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.useradmin.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.useradmin.server.UserAdminApi;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Anis TOURI <anis.touri at rte-france.com>
 *
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION + "/users")
@Tag(name = "UserInfoController")
public class UserInfoController {

    private final UserProfileService service;

    public UserInfoController(UserProfileService userService) {
        this.service = userService;
    }

    @GetMapping(value = "/{sub}/detail", produces = "application/json")
    @Operation(summary = "get detailed user information")
    @ApiResponse(responseCode = "200", description = "The user exist")
    @ApiResponse(responseCode = "404", description = "The user doesn't exist")
    public ResponseEntity<UserInfos> getUser(@PathVariable("sub") String sub) {
        return ResponseEntity.of(service.getUserInfo(sub));
    }
}

