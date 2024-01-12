package org.gridsuite.useradmin.server.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminException;

import static org.gridsuite.useradmin.server.UserAdminException.Type.FORBIDDEN;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
abstract class AbstractCommonService {
    @NonNull protected final UserAdminApplicationProps applicationProps;

    protected boolean isAdmin(@NonNull String sub) {
        return applicationProps.getAdmins().contains(sub);
    }

    protected void assertIsAdmin(@NonNull String sub) throws UserAdminException {
        if (!this.isAdmin(sub)) {
            throw new UserAdminException(FORBIDDEN);
        }
    }
}
