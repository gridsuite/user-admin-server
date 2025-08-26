/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.dto;

import java.util.UUID;

public record UserProfile(
    UUID id,
    String name,
    UUID loadFlowParameterId,
    UUID securityAnalysisParameterId,
    UUID sensitivityAnalysisParameterId,
    UUID shortcircuitParameterId,
    UUID voltageInitParameterId,
    Boolean allLinksValid,
    Integer maxAllowedCases,
    Integer maxAllowedBuilds,
    UUID spreadsheetConfigCollectionId,
    UUID networkVisualizationParameterId,
    UUID diagramConfigId
) { }
