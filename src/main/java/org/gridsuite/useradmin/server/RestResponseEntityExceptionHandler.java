/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.powsybl.ws.commons.error.AbstractBaseRestExceptionHandler;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import com.powsybl.ws.commons.error.ServerNameProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import java.util.Optional;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 *
 * Handle exception catch from the {@link org.gridsuite.useradmin.server.controller controllers}.
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler
    extends AbstractBaseRestExceptionHandler<UserAdminException, UserAdminBusinessErrorCode> {

    public RestResponseEntityExceptionHandler(ServerNameProvider serverNameProvider) {
        super(serverNameProvider);
    }

    @Override
    protected Optional<PowsyblWsProblemDetail> getRemoteError(UserAdminException ex) {
        return ex.getRemoteError();
    }

    @Override
    protected Optional<UserAdminBusinessErrorCode> getBusinessCode(UserAdminException ex) {
        return ex.getErrorCode();
    }

    @Override
    protected HttpStatus mapStatus(UserAdminBusinessErrorCode errorCode) {
        return switch (errorCode) {
            case USER_ADMIN_PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case USER_ADMIN_USER_NOT_FOUND,
                 USER_ADMIN_PROFILE_NOT_FOUND,
                 USER_ADMIN_GROUP_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USER_ADMIN_REMOTE_ERROR -> HttpStatus.BAD_GATEWAY;
            case USER_ADMIN_USER_ALREADY_EXISTS,
                 USER_ADMIN_PROFILE_ALREADY_EXISTS,
                 USER_ADMIN_GROUP_ALREADY_EXISTS,
                 USER_ADMIN_ANNOUNCEMENT_INVALID_PERIOD,
                 USER_ADMIN_ANNOUNCEMENT_OVERLAP -> HttpStatus.BAD_REQUEST;
        };
    }

    @Override
    protected UserAdminBusinessErrorCode defaultRemoteErrorCode() {
        return UserAdminBusinessErrorCode.USER_ADMIN_REMOTE_ERROR;
    }

    @Override
    protected UserAdminException wrapRemote(PowsyblWsProblemDetail remoteError) {
        return new UserAdminException(
            UserAdminBusinessErrorCode.USER_ADMIN_REMOTE_ERROR,
            remoteError.getDetail(),
            remoteError
        );
    }
}
