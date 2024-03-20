/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import org.gridsuite.useradmin.server.repository.ConnectionRepository;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {UserAdminApplication.class})
@ActiveProfiles({"default", "noadmin"})
class NoAdminTest {
    private static final String USER_NOT_REGISTERED = "NOT_REGISTERED_USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAdminRepository userAdminRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @AfterEach
    public void cleanDB() {
        userAdminRepository.deleteAll();
        connectionRepository.deleteAll();
    }

    @Test
    void testNoAdmin() throws Exception {
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_NOT_REGISTERED))
                .andExpect(status().isOk())
                .andReturn();

        userAdminRepository.save(new UserInfosEntity("newUser"));

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_NOT_REGISTERED))
                .andExpect(status().isNoContent())
                .andReturn();

        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}/isAdmin", USER_NOT_REGISTERED))
                .andExpect(status().isForbidden())
                .andReturn();
    }
}
