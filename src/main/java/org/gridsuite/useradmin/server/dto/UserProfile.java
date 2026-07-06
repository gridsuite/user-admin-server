/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
@Getter
@Builder
public final class UserProfile {
    public static final String DEFAULT_PROFILE_NAME = "default profile";

    private final UUID id;
    private final String name;
    private final UUID loadFlowParameterId;
    private final UUID securityAnalysisParameterId;
    private final UUID sensitivityAnalysisParameterId;
    private final UUID shortcircuitParameterId;
    private final UUID pccMinParameterId;
    private final UUID voltageInitParameterId;
    private final Boolean allLinksValid;
    private final UUID spreadsheetConfigCollectionId;
    private final UUID networkVisualizationParameterId;
    private final UUID workspaceId;
    private final Map<QuotaType, Integer> maxOperationQuota;

    public UserProfile(
            UUID id,
            String name,
            UUID loadFlowParameterId,
            UUID securityAnalysisParameterId,
            UUID sensitivityAnalysisParameterId,
            UUID shortcircuitParameterId,
            UUID pccMinParameterId,
            UUID voltageInitParameterId,
            Boolean allLinksValid,
            UUID spreadsheetConfigCollectionId,
            UUID networkVisualizationParameterId,
            UUID workspaceId,
            Map<QuotaType, Integer> maxOperationQuota
    ) {
        this.id = id;
        this.name = name;
        this.loadFlowParameterId = loadFlowParameterId;
        this.securityAnalysisParameterId = securityAnalysisParameterId;
        this.sensitivityAnalysisParameterId = sensitivityAnalysisParameterId;
        this.shortcircuitParameterId = shortcircuitParameterId;
        this.pccMinParameterId = pccMinParameterId;
        this.voltageInitParameterId = voltageInitParameterId;
        this.allLinksValid = allLinksValid;
        this.spreadsheetConfigCollectionId = spreadsheetConfigCollectionId;
        this.networkVisualizationParameterId = networkVisualizationParameterId;
        this.workspaceId = workspaceId;
        this.maxOperationQuota = maxOperationQuota == null ? new HashMap<>() : new HashMap<>(maxOperationQuota);
    }

    public static UserProfile createDefaultProfile(Map<QuotaType, Integer> maxAllowValues) {
        return UserProfile.builder()
                .name(DEFAULT_PROFILE_NAME)
                .maxOperationQuota(maxAllowValues)
                .build();
    }

    public Map<QuotaType, Integer> getMaxOperationQuota() {
        return Collections.unmodifiableMap(maxOperationQuota);
    }

    public Integer getMaxOperationQuota(QuotaType quotaType) {
        return maxOperationQuota.get(quotaType);
    }

}
