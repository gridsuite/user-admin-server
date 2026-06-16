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
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedLoadflow()} if {@code null}.
     */
    private Integer defaultMaxAllowedLoadflow;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedSecurity()} if {@code null}.
     */
    private Integer defaultMaxAllowedSecurity;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedSensitivity()} if {@code null}.
     */
    private Integer defaultMaxAllowedSensitivity;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedShortCircuit()} if {@code null}.
     */
    private Integer defaultMaxAllowedShortCircuit;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedVoltageInit()} if {@code null}.
     */
    private Integer defaultMaxAllowedVoltageInit;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedPccMin()} if {@code null}.
     */
    private Integer defaultMaxAllowedPccMin;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedStateEstimation()} if {@code null}.
     */
    private Integer defaultMaxAllowedStateEstimation;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedBalanceAdjustement()} if {@code null}.
     */
    private Integer defaultMaxAllowedBalanceAdjustement;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedDynamicSimulation()} if {@code null}.
     */
    private Integer defaultMaxAllowedDynamicSimulation;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedDynamicSecurity()} if {@code null}.
     */
    private Integer defaultMaxAllowedDynamicSecurity;

    /**
     * Default value of {@link org.gridsuite.useradmin.server.dto.UserInfos#maxAllowedDynamicMargin()} if {@code null}.
     */
    private Integer defaultMaxAllowedDynamicMargin;

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
