/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import java.util.Objects;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
public class UserAdminException extends RuntimeException {
    public enum Type {
        FORBIDDEN,
        NOT_FOUND,
        GROUP_ALREADY_EXISTS,
        USER_ALREADY_EXISTS,
        PROFILE_ALREADY_EXISTS,
        SEVERITY_DOES_NOT_EXIST,
        OVERLAPPING_ANNOUNCEMENTS,
        START_DATE_SAME_OR_AFTER_END_DATE
    }

    private final Type type;

    public UserAdminException(Type type) {
        super(Objects.requireNonNull(type.name()));
        this.type = type;
    }

    Type getType() {
        return type;
    }
}
