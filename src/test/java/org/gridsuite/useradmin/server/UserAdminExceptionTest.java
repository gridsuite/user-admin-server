/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class UserAdminExceptionTest {

    @Test
    void staticFactoriesCoverAllPaths() {
        assertThat(UserAdminException.forbidden().getBusinessErrorCode())
            .isEqualTo(UserAdminBusinessErrorCode.USER_ADMIN_PERMISSION_DENIED);

        assertThat(UserAdminException.userAlreadyExists("subj").getMessage()).contains("subj");
        assertThat(UserAdminException.userNotFound("subj").getMessage()).contains("subj");

        assertThat(UserAdminException.profileAlreadyExists("profile").getBusinessErrorCode())
            .isEqualTo(UserAdminBusinessErrorCode.USER_ADMIN_PROFILE_ALREADY_EXISTS);

        UUID profileId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertThat(UserAdminException.profileNotFound(profileId).getMessage()).contains(profileId.toString());

        assertThat(UserAdminException.groupAlreadyExists("group").getBusinessErrorCode())
            .isEqualTo(UserAdminBusinessErrorCode.USER_ADMIN_GROUP_ALREADY_EXISTS);

        UUID groupId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertThat(UserAdminException.groupNotFound(groupId).getMessage()).contains(groupId.toString());
        assertThat(UserAdminException.groupNotFound("group").getMessage()).contains("group");

        Instant start = Instant.parse("2025-12-01T00:00:00Z");
        Instant end = Instant.parse("2025-12-02T00:00:00Z");
        assertThat(UserAdminException.announcementInvalidPeriod(start, end).getBusinessErrorCode())
            .isEqualTo(UserAdminBusinessErrorCode.USER_ADMIN_ANNOUNCEMENT_INVALID_PERIOD);
        assertThat(UserAdminException.announcementOverlap(start, end).getBusinessErrorCode())
            .isEqualTo(UserAdminBusinessErrorCode.USER_ADMIN_ANNOUNCEMENT_OVERLAP);

        UserAdminException formatted = UserAdminException.of(UserAdminBusinessErrorCode.USER_ADMIN_USER_NOT_FOUND,
            "User %s missing", "x");
        assertThat(formatted.getMessage()).isEqualTo("User x missing");
    }

    @Test
    void remoteErrorIsReturnedWhenPresent() {
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.BAD_GATEWAY)
            .server("directory")
            .detail("failure")
            .timestamp(Instant.parse("2025-12-03T00:00:00Z"))
            .path("/directory")
            .build();

        UserAdminException exception = new UserAdminException(UserAdminBusinessErrorCode.USER_ADMIN_REMOTE_ERROR,
            "wrapped", remote);

        assertThat(exception.getRemoteError()).contains(remote);
        assertThat(exception.getBusinessErrorCode())
            .isEqualTo(UserAdminBusinessErrorCode.USER_ADMIN_REMOTE_ERROR);
    }
}
