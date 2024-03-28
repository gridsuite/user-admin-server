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
import org.gridsuite.useradmin.server.dto.ElementAttributes;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private UserProfileRepository userProfileRepository;

    private WireMockServer wireMockServer;

    private WireMockUtils wireMockUtils;

    private ObjectWriter objectWriter;

    @Before
    public void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        DirectoryService.setDirectoryServerBaseUri(wireMockServer.baseUrl());
        wireMockUtils = new WireMockUtils(wireMockServer);
        objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
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
    private static final String PROFILE_2 = "profile_2";

    @Test
    public void testUserProfile() throws Exception {
        // stub for parameters elements existence check (no ids to check here)
        final String urlPath = "/v1/elements";
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlPath + "\\?strictMode=false&ids="))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(List.of()))
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

    @Test
    public void testProfileUpdateValidityOk() throws Exception {
        updateProfile(true);
    }

    @Test
    public void testProfileUpdateValidityKo() throws Exception {
        updateProfile(false);
    }

    private void updateProfile(boolean validParameters) throws Exception {
        UUID lfParametersUuid = UUID.randomUUID();
        // stub for parameters elements existence check
        final String urlPath = "/v1/elements";
        List<ElementAttributes> existingElements = List.of(new ElementAttributes(lfParametersUuid, "name", "type"));
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlPath + "\\?strictMode=false&ids=" + lfParametersUuid))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(validParameters ? existingElements : List.of()))
                        .withHeader("Content-Type", "application/json"))).getId();

        // Create a profile
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/profiles/{profileName}", PROFILE_1)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isCreated())
                .andReturn();
        Optional<UserProfileEntity> profile1 = userProfileRepository.findByName(PROFILE_1);
        assertTrue(profile1.isPresent());
        assertNull(profile1.get().getLoadFlowParameterId()); // no LF params by dft

        UUID profileUuid = profile1.get().getId();

        // udpate the profile: change name and set its LF parameters
        UserProfile userProfile = new UserProfile(profileUuid, PROFILE_2, lfParametersUuid, null);
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", profileUuid)
                        .content(objectWriter.writeValueAsString(userProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER))
                .andExpect(status().isOk());

        userProfile = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", profileUuid)
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertNotNull(userProfile);
        assertEquals(PROFILE_2, userProfile.name());
        assertEquals(lfParametersUuid, userProfile.loadFlowParameterId());
        assertNull(userProfile.validity()); // validity not set in this case

        // profiles list (with validity flag)
        List<UserProfile> userProfiles = objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(List.of(lfParametersUuid)), false);
        assertEquals(1, userProfiles.size());
        assertEquals(lfParametersUuid, userProfiles.get(0).loadFlowParameterId());
        assertEquals(validParameters, userProfiles.get(0).validity());

        // bad update
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", UUID.randomUUID())
                        .content(objectWriter.writeValueAsString(userProfile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", ADMIN_USER))
                .andExpect(status().isNotFound());
    }

    private Map<String, StringValuePattern> handleQueryParams(List<UUID> paramIds) {
        return Map.of("ids", WireMock.matching(paramIds.stream().map(uuid -> ".+").collect(Collectors.joining(","))));
    }
}
