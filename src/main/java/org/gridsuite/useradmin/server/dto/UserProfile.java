/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.dto;

import java.util.Map;
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
        Map<String, Integer> maxAllowValuesMap,
        UUID spreadsheetConfigCollectionId,
        UUID networkVisualizationParameterId,
        UUID workspaceId
) {
    public static final String DEFAULT_PROFILE_NAME = "default profile";
    public static final String MAX_ALLOWED_CASES = "maxAllowedCases";
    public static final String MAX_ALLOWED_BUILD = "maxAllowedBuilds";
    public static final String MAX_ALLOWED_LOADFLOW = "maxAllowedLoadflow";
    public static final String MAX_ALLOWED_SECURITY = "maxAllowedSecurity";
    public static final String MAX_ALLOWED_SENSITIVITY = "maxAllowedSensitivity";
    public static final String MAX_ALLOWED_SHORT_CIRCUIT = "maxAllowedShortCircuit";
    public static final String MAX_ALLOWED_VOLTAGE_INIT = "maxAllowedVoltageInit";
    public static final String MAX_ALLOWED_PCC_MIN = "maxAllowedPccMin";
    public static final String MAX_ALLOWED_STATE_ESTIMATION = "maxAllowedStateEstimation";
    public static final String MAX_ALLOWED_BALANCE_ADJUSTEMENT = "maxAllowedBalanceAdjustement";
    public static final String MAX_ALLOWED_DYNAMIC_SIMULATION = "maxAllowedDynamicSimulation";
    public static final String MAX_ALLOWED_DYNAMIC_SECURITY = "maxAllowedDynamicSecurity";
    public static final String MAX_ALLOWED_DYNAMIC_MARGIN = "maxAllowedDynamicMargin";

    public static UserProfile createDefaultProfile(Map<String, Integer> maxAllowValues) {
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
                maxAllowValues,
                null,
                null,
                null
        );
    }
}
