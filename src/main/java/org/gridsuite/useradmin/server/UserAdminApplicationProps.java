/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "useradmin")
public class UserAdminApplicationProps {

    private Integer defaultMaxAllowedCases;

    private Integer casesAlertThreshold;

    private Integer defaultMaxAllowedBuilds;

    @Value("${gridsuite.user-roles.admin:ADMIN}")
    private String adminRole;

    @Value("${gridsuite.user-roles.user:UTILISATEURS}")
    private String userRole;
}
