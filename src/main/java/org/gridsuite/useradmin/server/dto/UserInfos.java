/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.dto;

import java.util.Set;

public record UserInfos(
    String sub,
    String profileName,
    Integer maxAllowedCases,
    Integer numberCasesUsed,
    Integer maxAllowedBuilds,
    Set<String> groups
) { }
