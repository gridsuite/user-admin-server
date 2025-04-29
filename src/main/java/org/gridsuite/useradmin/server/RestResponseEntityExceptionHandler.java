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

import static org.gridsuite.useradmin.server.UserAdminException.Type.*;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {
    @ExceptionHandler(value = {UserAdminException.class})
    protected ResponseEntity<UserAdminException.Type> handleException(UserAdminException userAdminException) {
        return switch (userAdminException.getType()) {
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(userAdminException.getType());
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(userAdminException.getType());
            case GROUP_ALREADY_EXISTS, USER_ALREADY_EXISTS, PROFILE_ALREADY_EXISTS, SEVERITY_DOES_NOT_EXIST,
                 OVERLAPPING_ANNOUNCEMENTS, START_DATE_SAME_OR_AFTER_END_DATE ->
                    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(userAdminException.getType());
        };
    }
}
