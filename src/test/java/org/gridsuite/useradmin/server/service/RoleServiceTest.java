/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import jakarta.servlet.http.HttpServletRequest;
import org.gridsuite.useradmin.server.UserAdminApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.gridsuite.useradmin.server.service.RoleService.ROLES_HEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Achour Berrahma <achour.berrahma at rte-france.com>
 */
@SpringBootTest(classes = {UserAdminApplication.class})
class RoleServiceTest {

    @InjectMocks
    private RoleService roleService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletRequestAttributes attributes;

    @BeforeEach
    void setUp() {
        // Set up the RequestContextHolder with our mocked ServletRequestAttributes
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    @AfterEach
    void tearDown() {
        // Clear the RequestContextHolder after each test
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testHasRequiredRolesWithNoMatchingRolesAllRequired() {
        // Test when the user has none of the required roles (all required)
        when(request.getHeader(ROLES_HEADER)).thenReturn("VIEWER|GUEST");

        boolean hasRoles = roleService.hasRequiredRoles(Set.of("ADMIN", "USER"), true);

        assertFalse(hasRoles);
        verify(request).getHeader(ROLES_HEADER);
    }

    @Test
    void testCheckAccessWhenUserHasRequiredRoles() {
        // Test checkAccess when the user has the required roles
        when(request.getHeader(ROLES_HEADER)).thenReturn("ADMIN|USER");

        // Should not throw an exception
        assertDoesNotThrow(() -> roleService.checkAccess(Set.of("ADMIN"), false));
        verify(request).getHeader(ROLES_HEADER);
    }

    @Test
    void testCheckAccessWhenUserDoesNotHaveRequiredRoles() {
        // Test checkAccess when the user doesn't have the required roles
        when(request.getHeader(ROLES_HEADER)).thenReturn("USER|VIEWER");

        // Should throw a ResponseStatusException with FORBIDDEN status
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> roleService.checkAccess(Set.of("ADMIN"), false)
        );

        assertEquals(403, exception.getStatusCode().value());
        assertEquals("Insufficient privileges", exception.getReason());
        verify(request).getHeader(ROLES_HEADER);
    }

}
