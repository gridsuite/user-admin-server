/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;

/**
 * @author David Braquart <david.braquart at rte-france.com
 */
@Service
public class AdminRightService {

    private final UserAdminApplicationProps applicationProps;

    public AdminRightService(final UserAdminApplicationProps applicationProps) {
        this.applicationProps = Objects.requireNonNull(applicationProps);
    }

    public List<String> getAdmins() {
        return applicationProps.getAdmins();
    }

    public boolean isAdmin(@lombok.NonNull String sub) {
        return this.getAdmins().contains(sub);
    }

    public void assertIsAdmin(@lombok.NonNull String sub) throws UserAdminException {
        if (!this.isAdmin(sub)) {
            throw new UserAdminException(FORBIDDEN);
        }
    }
}
