/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.dto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

public record UserInfos(
    String sub,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String firstName,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String lastName,
    String profileName,
    Integer maxAllowedCases,
    Integer numberCasesUsed,
    Integer maxAllowedBuilds,
    Set<String> groups
) {
    /**
     * Creates a new UserInfos with identity information (firstName and lastName).
     *
     * @param identity the user identity containing firstName and lastName
     * @return a new UserInfos with identity information merged
     */
    public UserInfos withIdentity(UserIdentity identity) {
        if (identity == null) {
            return this;
        }
        return new UserInfos(
                sub,
                identity.firstName(),
                identity.lastName(),
                profileName,
                maxAllowedCases,
                numberCasesUsed,
                maxAllowedBuilds,
                groups
        );
    }
}
