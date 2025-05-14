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

import java.util.Objects;
import java.util.Set;

/**
 * @author David Braquart <david.braquart at rte-france.com
 */
@Service
public class AdminRightService {

    private final RoleService roleService;
    private final UserAdminApplicationProps userAdminApplicationProps;

    public AdminRightService(final RoleService roleService,
                             final UserAdminApplicationProps userAdminApplicationProps) {
        this.roleService = Objects.requireNonNull(roleService);
        this.userAdminApplicationProps = Objects.requireNonNull(userAdminApplicationProps);
    }

    public void assertIsAdmin() throws UserAdminException {
        roleService.checkAccess(Set.of(userAdminApplicationProps.getAdminRole()), true);
    }
}
