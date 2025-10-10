/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.powsybl.ws.commons.error.ServerNameProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Component
public class PropertyServerNameProvider implements ServerNameProvider {

    private final String name;

    public PropertyServerNameProvider(@Value("${server.name:user-admin-server}") String name) {
        this.name = name;
    }

    @Override
    public String serverName() {
        return name;
    }
}
