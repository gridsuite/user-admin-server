/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.error;

import com.powsybl.ws.commons.error.BusinessErrorCode;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 *
 * Business error codes exposed by the user-admin server.
 */
public enum UserAdminBusinessErrorCode implements BusinessErrorCode {
    USER_ADMIN_PERMISSION_DENIED("useradmin.permissionDenied"),
    USER_ADMIN_USER_NOT_FOUND("useradmin.userNotFound"),
    USER_ADMIN_USER_ALREADY_EXISTS("useradmin.userAlreadyExists"),
    USER_ADMIN_PROFILE_NOT_FOUND("useradmin.profileNotFound"),
    USER_ADMIN_PROFILE_ALREADY_EXISTS("useradmin.profileAlreadyExists"),
    USER_ADMIN_GROUP_NOT_FOUND("useradmin.groupNotFound"),
    USER_ADMIN_GROUP_ALREADY_EXISTS("useradmin.groupAlreadyExists"),
    USER_ADMIN_ANNOUNCEMENT_INVALID_PERIOD("useradmin.announcementInvalidPeriod"),
    USER_ADMIN_ANNOUNCEMENT_OVERLAP("useradmin.announcementOverlap");

    private final String value;

    UserAdminBusinessErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
