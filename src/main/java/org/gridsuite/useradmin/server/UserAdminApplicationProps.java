/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.gridsuite.useradmin.server.controller.UserAdminController;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.validation.annotation.Validated;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "useradmin")
public class UserAdminApplicationProps {

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedCases()} if {@code null}.
     */
    private Integer defaultMaxAllowedCases;

    /**
     * Value returned by {@link UserAdminController#getCasesAlertThreshold()}.
     * Default: {@code 90}
     */
    private Integer casesAlertThreshold;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedBuilds()} if {@code null}.
     */
    private Integer defaultMaxAllowedBuilds;

    /**
     * Cron jobs expression in UTC.
     */
    private Cron cron = new Cron();

    @Data
    public static class Cron {
        private String announcementCheck;
        private String announcementClean;

        @AssertTrue(message = "Invalide cron expression for \"announcementCheck\"")
        public boolean isValidAnnouncementCheck() {
            return this.announcementCheck == null || CronExpression.isValidExpression(this.announcementCheck);
        }

        @AssertTrue(message = "Invalide cron expression for \"announcementClean\"")
        public boolean isValidAnnouncementClean() {
            return this.announcementClean == null || CronExpression.isValidExpression(this.announcementClean);
        }
    }

    private String adminRole = "ADMIN";
}
