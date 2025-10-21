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
import org.gridsuite.useradmin.server.service.UserInfosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Anis TOURI <anis.touri at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION + "/users")
@Tag(name = "UserInfoController")
public class UserInfoController {

    private final UserInfosService userInfosService;

    public UserInfoController(UserInfosService userInfosService) {
        this.userInfosService = userInfosService;
    }

    @GetMapping(value = "/{sub}/detail", produces = "application/json")
    @Operation(summary = "get detailed user information")
    @ApiResponse(responseCode = "200", description = "The user exist")
    public ResponseEntity<UserInfos> getUserDetail(@PathVariable("sub") String sub) {
        return ResponseEntity.ok(userInfosService.getUserInfo(sub));
    }
}

