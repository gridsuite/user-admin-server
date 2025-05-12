/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Achour Berrahma <achour.berrahma at rte-france.com>
 * Service for checking user roles.
 */
@Service
public class RoleService {
    public static final String ROLES_HEADER = "roles";
    public static final String ROLE_DELIMITER = "\\|";

    /**
     * Checks if the current user has the required roles.
     *
     * @param requiredRoles    The roles required for access
     * @param allRolesRequired If true, all roles are required; if false, any one role is sufficient
     * @return True if the user has the required roles, false otherwise
     */
    public boolean hasRequiredRoles(Set<String> requiredRoles, boolean allRolesRequired) {
        Set<String> userRoles = getCurrentUserRoles();

        if (userRoles.isEmpty()) {
            return false;
        }

        if (allRolesRequired) {
            // User must have all required roles
            return userRoles.containsAll(requiredRoles);
        } else {
            // User must have at least one of the required roles
            for (String role : requiredRoles) {
                if (userRoles.contains(role)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Gets the current user's roles from the request headers.
     *
     * @return A set of the user's roles
     */
    public Set<String> getCurrentUserRoles() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Collections.emptySet();
        }

        HttpServletRequest request = attributes.getRequest();
        String rolesHeader = request.getHeader(ROLES_HEADER);

        if (rolesHeader == null || rolesHeader.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> roles = new HashSet<>();
        String[] roleParts = rolesHeader.split(ROLE_DELIMITER);
        Collections.addAll(roles, roleParts);

        return roles;
    }

    /**
     * Checks if the user has required roles and throws an exception if not.
     *
     * @param requiredRoles The roles to check for
     * @param allRolesRequired Whether all roles are required
     * @throws ResponseStatusException with 403 status if the user doesn't have the required roles
     */
    public void checkAccess(Set<String> requiredRoles, boolean allRolesRequired) {
        if (!hasRequiredRoles(requiredRoles, allRolesRequired)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient privileges");
        }
    }

}