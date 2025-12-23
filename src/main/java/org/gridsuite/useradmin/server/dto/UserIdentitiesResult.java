/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * DTO representing the batch response from user-identity-server.
 *
 * @author Achour Berrahma <achour.berrahma at rte-france.com>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserIdentitiesResult(
        Map<String, UserIdentity> data,
        Map<String, Object> errors
) {
    public UserIdentitiesResult {
        data = data != null ? data : Map.of();
        errors = errors != null ? errors : Map.of();
    }
}
