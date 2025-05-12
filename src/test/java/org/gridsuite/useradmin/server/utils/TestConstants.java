/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.utils;

import org.gridsuite.useradmin.server.UserAdminApi;

/**
 * @author Achour Berrahma <achour.berrahma at rte-france.com>
 */
public final class TestConstants {

    public static final String API_BASE_PATH = "/" + UserAdminApi.API_VERSION;
    public static final String RECORD_CONNECTION_URL = "/" + UserAdminApi.API_VERSION + "/users/{sub}/record-connection";
    public static final String IS_CONNECTION_ACCEPTED_PARAM = "isConnectionAccepted";

}
