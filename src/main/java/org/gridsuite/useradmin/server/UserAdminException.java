/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.powsybl.ws.commons.error.AbstractBusinessException;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.useradmin.server.UserAdminBusinessErrorCode.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 *
 * User admin specific runtime exception enriched with a business error code.
 */
public class UserAdminException extends AbstractBusinessException {

    private final UserAdminBusinessErrorCode errorCode;
    private final PowsyblWsProblemDetail remoteError;

    public UserAdminException(UserAdminBusinessErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public UserAdminException(UserAdminBusinessErrorCode errorCode, String message, PowsyblWsProblemDetail remoteError) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.remoteError = remoteError;
    }

    public static UserAdminException forbidden() {
        return new UserAdminException(USER_ADMIN_PERMISSION_DENIED, "User is not allowed to perform this action");
    }

    public static UserAdminException userAlreadyExists(String sub) {
        return new UserAdminException(USER_ADMIN_USER_ALREADY_EXISTS, String.format("User '%s' already exists", sub));
    }

    public static UserAdminException userNotFound(String sub) {
        return new UserAdminException(USER_ADMIN_USER_NOT_FOUND, String.format("User '%s' was not found", sub));
    }

    public static UserAdminException profileAlreadyExists(String name) {
        return new UserAdminException(USER_ADMIN_PROFILE_ALREADY_EXISTS, String.format("Profile '%s' already exists", name));
    }

    public static UserAdminException profileNotFound(UUID profileId) {
        return new UserAdminException(USER_ADMIN_PROFILE_NOT_FOUND, String.format("Profile '%s' was not found", profileId));
    }

    public static UserAdminException groupAlreadyExists(String name) {
        return new UserAdminException(USER_ADMIN_GROUP_ALREADY_EXISTS, String.format("Group '%s' already exists", name));
    }

    public static UserAdminException groupNotFound(UUID groupId) {
        return new UserAdminException(USER_ADMIN_GROUP_NOT_FOUND, String.format("Group '%s' was not found", groupId));
    }

    public static UserAdminException groupNotFound(String name) {
        return new UserAdminException(USER_ADMIN_GROUP_NOT_FOUND, String.format("Group '%s' was not found", name));
    }

    public static UserAdminException announcementInvalidPeriod(Instant startDate, Instant endDate) {
        return new UserAdminException(USER_ADMIN_ANNOUNCEMENT_INVALID_PERIOD,
            String.format("Announcement end date '%s' must be after start date '%s'", endDate, startDate));
    }

    public static UserAdminException announcementOverlap(Instant startDate, Instant endDate) {
        return new UserAdminException(USER_ADMIN_ANNOUNCEMENT_OVERLAP,
            String.format("Announcement period [%s, %s) overlaps with an existing announcement", startDate, endDate));
    }

    public static UserAdminException of(UserAdminBusinessErrorCode errorCode, String message, Object... args) {
        return new UserAdminException(errorCode, args.length == 0 ? message : String.format(message, args));
    }

    @Override
    public UserAdminBusinessErrorCode getBusinessErrorCode() {
        return errorCode;
    }

    public Optional<PowsyblWsProblemDetail> getRemoteError() {
        return Optional.ofNullable(remoteError);
    }
}
