/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.service.DirectoryService;
import org.gridsuite.useradmin.server.utils.WireMockUtils;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserProfileTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserAdminRepository userAdminRepository;

    @Autowired
    private UserAdminRepository userProfileRepository;

    protected static WireMockServer wireMockServer;

    protected WireMockUtils wireMockUtils;

    @Before
    public void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        DirectoryService.setDirectoryServerBaseUri(wireMockServer.baseUrl());
        wireMockUtils = new WireMockUtils(wireMockServer);
    }

    @After
    public void tearOff() {
        userAdminRepository.deleteAll();
        userProfileRepository.deleteAll();

        try {
            wireMockServer.checkForUnmatchedRequests();
            Assert.assertEquals(0, wireMockServer.findAll(WireMock.anyRequestedFor(WireMock.anyUrl())).size());
        } finally {
            wireMockServer.shutdown();
        }
    }

    private static final String ADMIN_USER = "admin1";
    private static final String NOT_ADMIN = "notAdmin";
    private static final String PROFILE_1 = "profile_1";

    @Test
    public void testUserProfile() throws Exception {
        ObjectWriter objectWriter = objectMapper.writer().withDefaultPrettyPrinter();

        // stub for parameters elements existence check
        final String urlPath = "/v1/elements";
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlPath + "\\?strictMode=false&ids="))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(Set.of()))
                        .withHeader("Content-Type", "application/json"))).getId();

        // no existing profile
        List<UserProfile> userProfiles = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(0, userProfiles.size());
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(List.of()), false);

        // Create a profile
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/profiles/{profileName}", PROFILE_1)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();

        userProfiles = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(1, userProfiles.size());
        assertEquals(PROFILE_1, userProfiles.get(0).name());
        assertNull(userProfiles.get(0).loadFlowParameterId());
        assertNull(userProfiles.get(0).validity());
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(List.of()), false);

        // Remove the profile
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(List.of(PROFILE_1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isNoContent())
                .andReturn();

        userProfiles = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(0, userProfiles.size());
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(List.of()), false);

        // not allowed
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(List.of(PROFILE_1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", NOT_ADMIN)
                )
                .andExpect(status().isForbidden())
                .andReturn();

        // profile already deleted / not found
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(List.of(PROFILE_1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isNotFound())
                .andReturn();
    }

    private Map<String, StringValuePattern> handleQueryParams(List<UUID> paramIds) {
        return Map.of("ids", WireMock.matching(paramIds.stream().map(uuid -> ".+").collect(Collectors.joining(","))));
    }
}
