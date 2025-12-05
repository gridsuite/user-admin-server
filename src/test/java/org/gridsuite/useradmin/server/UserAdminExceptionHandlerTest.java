/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.gridsuite.useradmin.server.error.UserAdminExceptionHandler;
import org.gridsuite.useradmin.server.error.UserAdminException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class UserAdminExceptionHandlerTest {

    private TestUserAdminExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestUserAdminExceptionHandler();
    }

    @Test
    void mapsBusinessCodeToStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user-admin");
        UserAdminException exception = UserAdminException.forbidden();

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleDomainException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertEquals("useradmin.permissionDenied", response.getBody().getBusinessErrorCode());
    }

    private static final class TestUserAdminExceptionHandler extends UserAdminExceptionHandler {

        private TestUserAdminExceptionHandler() {
            super(() -> "user-admin-server");
        }

        ResponseEntity<PowsyblWsProblemDetail> invokeHandleDomainException(UserAdminException exception, MockHttpServletRequest request) {
            return super.handleDomainException(exception, request);
        }
    }
}
