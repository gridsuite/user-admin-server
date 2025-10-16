/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class RestResponseEntityExceptionHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private TestRestResponseEntityExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TestRestResponseEntityExceptionHandler();
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

    @Test
    void propagatesRemoteDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user-admin/remote");
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.BAD_GATEWAY)
            .server("directory")
            .detail("failure")
            .timestamp(Instant.parse("2025-12-04T00:00:00Z"))
            .path("/directory")
            .build();
        UserAdminException exception = new UserAdminException(UserAdminBusinessErrorCode.USER_ADMIN_REMOTE_ERROR,
            "wrap", remote);

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleDomainException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getChain()).hasSize(1);
        assertEquals("user-admin-server", response.getBody().getChain().getFirst().getFromServer());
    }

    @Test
    void wrapsInvalidRemotePayloadWithDefaultCode() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/user-admin/remote");
        HttpClientErrorException exception = HttpClientErrorException.create(
            HttpStatus.BAD_GATEWAY,
            "bad gateway",
            null,
            "oops".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8
        );

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleRemoteException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertEquals("useradmin.remoteError", response.getBody().getBusinessErrorCode());
    }

    @Test
    void keepsRemoteStatusFromPayload() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user-admin/remote");
        PowsyblWsProblemDetail remote = PowsyblWsProblemDetail.builder(HttpStatus.BAD_REQUEST)
            .server("directory")
            .businessErrorCode("directory.remoteError")
            .detail("invalid")
            .timestamp(Instant.parse("2025-12-05T00:00:00Z"))
            .path("/directory")
            .build();

        byte[] payload = OBJECT_MAPPER.writeValueAsBytes(remote);
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad request",
            null, payload, StandardCharsets.UTF_8);

        ResponseEntity<PowsyblWsProblemDetail> response = handler.invokeHandleRemoteException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertEquals("directory.remoteError", response.getBody().getBusinessErrorCode());
    }

    private static final class TestRestResponseEntityExceptionHandler extends RestResponseEntityExceptionHandler {

        private TestRestResponseEntityExceptionHandler() {
            super(() -> "user-admin-server");
        }

        ResponseEntity<PowsyblWsProblemDetail> invokeHandleDomainException(UserAdminException exception, MockHttpServletRequest request) {
            return super.handleDomainException(exception, request);
        }

        ResponseEntity<PowsyblWsProblemDetail> invokeHandleRemoteException(HttpClientErrorException exception, MockHttpServletRequest request) {
            return super.handleRemoteException(exception, request);
        }
    }
}
