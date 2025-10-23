/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Handle exceptions catch from the {@link org.gridsuite.useradmin.server.controller controllers}.
 */
@RestControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler({UserAdminException.class})
    protected ResponseEntity<Object> handleException(@NotNull final UserAdminException userAdminException) {
        return switch (userAdminException.getType()) {
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(userAdminException.getType());
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(userAdminException.getType());
            case GROUP_ALREADY_EXISTS, USER_ALREADY_EXISTS, PROFILE_ALREADY_EXISTS, SEVERITY_DOES_NOT_EXIST,
                 OVERLAPPING_ANNOUNCEMENTS, START_DATE_SAME_OR_AFTER_END_DATE ->
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(userAdminException.getType());
        };
    }

    /**
     * {@link org.springframework.validation.annotation.Validated @Validated} errors handler.
     */
    @ExceptionHandler({ConstraintViolationException.class}) // @Validated errors
    public ProblemDetail handleConstraintViolation(@NotNull final ConstraintViolationException exception, WebRequest request) {
        final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Entity validation error");
        problemDetail.setType(URI.create("http://gridsuite.org/errors/constraint-violation"));
        problemDetail.setProperty("violations", exception.getConstraintViolations().stream().collect(Collectors.toUnmodifiableMap(ConstraintViolation::getPropertyPath, ConstraintViolation::getMessage)));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
