/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.useradmin.server.dto.UserIdentitiesResult;
import org.gridsuite.useradmin.server.dto.UserIdentity;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.repository.UserGroupRepository;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.gridsuite.useradmin.server.Utils.ROLES_HEADER;
import static org.gridsuite.useradmin.server.utils.TestConstants.USER_ADMIN_ROLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for user identity enrichment functionality.
 *
 * @author Achour Berrahma <achour.berrahma at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {UserAdminApplication.class})
class UserIdentityIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserInfosRepository userInfosRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private static final String ADMIN_USER = "admin1";
    private static final String USER_SUB_1 = "user1";
    private static final String USER_SUB_2 = "user2";

    @BeforeEach
    void setUp() {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
            wireMockServer.start();
        }
        wireMockServer.resetAll();

        // Inject the WireMock server URL into the service
        // This is done via @DynamicPropertySource
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
            wireMockServer.start();
        }
        registry.add("gridsuite.services.user-identity-server.base-uri", wireMockServer::baseUrl);
    }

    @AfterEach
    void cleanDB() {
        userGroupRepository.deleteAll();
        userInfosRepository.deleteAll();
        userProfileRepository.deleteAll();
    }

    @Test
    void testGetUsersWithIdentityEnrichment() throws Exception {
        // Create users in database
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_SUB_1, null, null));
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_SUB_2, null, null));

        // Mock user-identity-server response
        UserIdentity identity1 = new UserIdentity(USER_SUB_1, "John", "Doe");
        UserIdentity identity2 = new UserIdentity(USER_SUB_2, "Jane", "Smith");
        UserIdentitiesResult identitiesResult = new UserIdentitiesResult(
                Map.of(USER_SUB_1, identity1, USER_SUB_2, identity2),
                Map.of()
        );

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/v1/users/identities"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(identitiesResult))));

        // Call getUsers endpoint
        List<UserInfos> userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });

        assertEquals(2, userInfos.size());

        // Find and verify user1
        UserInfos user1Info = userInfos.stream()
                .filter(u -> USER_SUB_1.equals(u.sub()))
                .findFirst()
                .orElseThrow();
        assertEquals("John", user1Info.firstName());
        assertEquals("Doe", user1Info.lastName());

        // Find and verify user2
        UserInfos user2Info = userInfos.stream()
                .filter(u -> USER_SUB_2.equals(u.sub()))
                .findFirst()
                .orElseThrow();
        assertEquals("Jane", user2Info.firstName());
        assertEquals("Smith", user2Info.lastName());
    }

    @Test
    void testGetUserWithIdentityEnrichment() throws Exception {
        // Create user in database
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_SUB_1, null, null));

        // Mock user-identity-server response for single user
        UserIdentity identity = new UserIdentity(USER_SUB_1, "John", "Doe");

        wireMockServer.stubFor(WireMock.get(urlEqualTo("/v1/users/identities/" + USER_SUB_1))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(identity))));

        // Call getUser endpoint
        UserInfos userInfo = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB_1)
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });

        assertEquals(USER_SUB_1, userInfo.sub());
        assertEquals("John", userInfo.firstName());
        assertEquals("Doe", userInfo.lastName());
    }

    @Test
    void testGetUsersWhenIdentityServiceFails() throws Exception {
        // Create user in database
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_SUB_1, null, null));

        // Mock user-identity-server to return 500 error
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/v1/users/identities"))
                .willReturn(aResponse()
                        .withStatus(500)));

        // Call getUsers endpoint - should still succeed but without identity info
        List<UserInfos> userInfos = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users")
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });

        assertEquals(1, userInfos.size());
        assertEquals(USER_SUB_1, userInfos.getFirst().sub());
        assertNull(userInfos.getFirst().firstName());
        assertNull(userInfos.getFirst().lastName());
    }

    @Test
    void testGetUserWhenIdentityServiceReturns404() throws Exception {
        // Create user in database
        userInfosRepository.save(new UserInfosEntity(UUID.randomUUID(), USER_SUB_1, null, null));

        // Mock user-identity-server to return 404 (user not found in identity service)
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/v1/users/identities/" + USER_SUB_1))
                .willReturn(aResponse()
                        .withStatus(404)));

        // Call getUser endpoint - should still succeed but without identity info
        UserInfos userInfo = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}", USER_SUB_1)
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });

        assertEquals(USER_SUB_1, userInfo.sub());
        assertNull(userInfo.firstName());
        assertNull(userInfo.lastName());
    }

}
