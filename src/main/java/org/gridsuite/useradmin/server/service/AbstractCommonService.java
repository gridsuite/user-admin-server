/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminException;

import java.util.List;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
abstract class AbstractCommonService {
    @NonNull protected final UserAdminApplicationProps applicationProps;

    protected boolean isAdmin(@NonNull String sub) {
        final List<String> admins = applicationProps.getAdmins();
        return admins.contains(sub);
    }

    protected void assertIsAdmin(@NonNull String sub) throws UserAdminException {
        if (!this.isAdmin(sub)) {
            throw new UserAdminException(FORBIDDEN);
        }
    }
}
