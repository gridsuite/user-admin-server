/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebInputException;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = { UserAdminException.class, TypeMismatchException.class })
    protected ResponseEntity<Object> handleException(RuntimeException exception) {
        if (exception instanceof UserAdminException) {
            UserAdminException userAdminException = (UserAdminException) exception;
            switch (userAdminException.getType()) {
                case FORBIDDEN:
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(userAdminException.getType());
                default:
            }
        } else if (exception instanceof ServerWebInputException) {
            ServerWebInputException serverWebInputException = (ServerWebInputException) exception;
            Throwable cause = serverWebInputException.getCause();
            if (cause instanceof TypeMismatchException && cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
                return ResponseEntity.status(serverWebInputException.getStatus()).body(cause.getMessage());
            }
        } else if (exception instanceof TypeMismatchException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getCause().getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
