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
    UUID pccMinParameterId,
    UUID voltageInitParameterId,
    Boolean allLinksValid,
    Integer maxAllowedCases,
    Integer maxAllowedBuilds,
    Integer maxAllowedLoadflow,
    Integer maxAllowedSecurity,
    Integer maxAllowedSensitivity,
    Integer maxAllowedShortCircuit,
    Integer maxAllowedVoltageInit,
    Integer maxAllowedPccMin,
    Integer maxAllowedStateEstimation,
    Integer maxAllowedBalanceAdjustement,
    Integer maxAllowedDynamicSimulation,
    Integer maxAllowedDynamicSecurity,
    Integer maxAllowedDynamicMargin,
    UUID spreadsheetConfigCollectionId,
    UUID networkVisualizationParameterId,
    UUID workspaceId
) {
    public static final String DEFAULT_PROFILE_NAME = "default profile";

    public static UserProfile createDefaultProfile(Integer maxAllowedCases, Integer maxAllowedBuilds, Integer maxAllowedLoadflow,
                                                   Integer maxAllowedSecurity, Integer maxAllowedSensitivity, Integer maxAllowedShortCircuit,
                                                   Integer maxAllowedVoltageInit, Integer maxAllowedPccMin, Integer maxAllowedStateEstimation,
                                                   Integer maxAllowedBalanceAdjustement, Integer maxAllowedDynamicSimulation, Integer maxAllowedDynamicSecurity,
                                                   Integer maxAllowedDynamicMargin) {
        return new UserProfile(
                null,
                DEFAULT_PROFILE_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                maxAllowedCases,
                maxAllowedBuilds,
                maxAllowedLoadflow,
                maxAllowedSecurity,
                maxAllowedSensitivity,
                maxAllowedShortCircuit,
                maxAllowedVoltageInit,
                maxAllowedPccMin,
                maxAllowedStateEstimation,
                maxAllowedBalanceAdjustement,
                maxAllowedDynamicSimulation,
                maxAllowedDynamicSecurity,
                maxAllowedDynamicMargin,
                null,
                null,
                null
        );
    }
}
