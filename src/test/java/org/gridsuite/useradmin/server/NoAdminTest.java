/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {UserAdminApplication.class})
@ActiveProfiles("default, noadmin")
public class NoAdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserAdminRepository repository;

    private void cleanDB() {
        repository.deleteAll();
    }

    @Before
    public void setup() {
        cleanDB();
    }

    @Test
    public void testNoAdmin()throws Exception {
        mockMvc.perform(head("/" + UserAdminApi.API_VERSION + "/users/{sub}", "NOT_REGISTERED_USER"))
                .andExpect(status().isOk())
                .andReturn();
    }
}
