/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;
import static org.gridsuite.useradmin.server.UserAdminException.Type.GROUP_ALREADY_EXISTS;
import static org.gridsuite.useradmin.server.UserAdminException.Type.NOT_FOUND;
import static org.gridsuite.useradmin.server.UserAdminException.Type.PROFILE_ALREADY_EXISTS;
import static org.gridsuite.useradmin.server.UserAdminException.Type.USER_ALREADY_EXISTS;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(value = { UserAdminException.class })
    protected ResponseEntity<Object> handleException(RuntimeException exception) {
        if (exception instanceof UserAdminException userAdminException) {
            if (userAdminException.getType().equals(FORBIDDEN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(userAdminException.getType());
            } else if (userAdminException.getType().equals(NOT_FOUND)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(userAdminException.getType());
            } else if (userAdminException.getType().equals(GROUP_ALREADY_EXISTS) ||
                       userAdminException.getType().equals(USER_ALREADY_EXISTS) ||
                       userAdminException.getType().equals(PROFILE_ALREADY_EXISTS)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(userAdminException.getType());
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
