/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.useradmin.server.UserAdminApi;
import org.gridsuite.useradmin.server.dto.*;
import org.gridsuite.useradmin.server.service.UserQuotaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@RestController
@RequestMapping(value = "/" + UserAdminApi.API_VERSION)
@Tag(name = "UserQuotaController")
@ApiResponse(responseCode = "403", description = "The current user does not have right to ask these data")
public class UserQuotaController {
    private final UserQuotaService userQuotaService;

    public UserQuotaController(UserQuotaService userQuotaService) {
        this.userQuotaService = userQuotaService;
    }

    @GetMapping(value = "/users/{sub}/quota/max")
    @Operation(summary = "Get the user's quota")
    @ApiResponse(responseCode = "200", description = "The user max quota")
    public ResponseEntity<Map<QuotaType, Integer>> getUserProfileMaxQuota(@PathVariable("sub") String sub) {
        Map<QuotaType, Integer> userMaxQuota = userQuotaService.getUserMaxQuota(sub);
        return ResponseEntity.ok().body(userMaxQuota);
    }

    @GetMapping(value = "/users/{sub}/quota/current")
    @Operation(summary = "Get the user's current quota usage")
    @ApiResponse(responseCode = "200", description = "The user current quota usage")
    public ResponseEntity<Map<QuotaType, Integer>> getUserCurrentQuotaUsage(@PathVariable("sub") String sub) {
        Map<QuotaType, Integer> userCurrentQuotaUsage = userQuotaService.getUserCurrentQuotaUsage(sub);
        return ResponseEntity.ok().body(userCurrentQuotaUsage);
    }

    @GetMapping(value = "/users/{sub}/quota/state")
    @Operation(summary = "Get the user's current quota state : usage and max")
    @ApiResponse(responseCode = "200", description = "The user current quota state")
    public ResponseEntity<Map<QuotaType, QuotaState>> getUserCurrentQuotaState(@PathVariable("sub") String sub) {
        Map<QuotaType, QuotaState> userCurrentQuotaStat = userQuotaService.getUserCurrentQuotaState(sub);
        return ResponseEntity.ok().body(userCurrentQuotaStat);
    }

    @PostMapping(value = "/users/{sub}/quota/reset")
    @Operation(summary = "Reset the user's current quota usage", description = "Access restricted to users of type: `admin`")
    @ApiResponse(responseCode = "200", description = "The user current quota usage has been reseted")
    public ResponseEntity<Void> resetUserCurrentQuotaUsage(@PathVariable("sub") String sub) {
        userQuotaService.resetUserCurrentQuotaUsage(sub);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/users/{sub}/quota/{operation}/{operation_id}/start")
    @Operation(summary = "Increase the user's current quota usage for a given operation")
    @ApiResponse(responseCode = "200", description = "The user current quota usage has been updated")
    public ResponseEntity<Void> startUserOperation(@PathVariable("sub") String sub,
                                                   @PathVariable("operation") QuotaType operation,
                                                   @PathVariable("operation_id") UUID operationId) {
        userQuotaService.startUserOperation(sub, operation, operationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/users/{sub}/quota/{operation}/{operation_id}/end")
    @Operation(summary = "Decrease the user's current quota usage for a given operation")
    @ApiResponse(responseCode = "200", description = "The user current quota usage has been updated")
    public ResponseEntity<Void> endUserOperation(@PathVariable("sub") String sub,
                                                 @PathVariable("operation") QuotaType operation,
                                                 @PathVariable("operation_id") UUID operationId) {
        userQuotaService.endUserOperation(sub, operation, operationId);
        return ResponseEntity.ok().build();
    }
}
