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
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import org.gridsuite.useradmin.server.dto.ElementAttributes;
import org.gridsuite.useradmin.server.dto.UserProfile;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.gridsuite.useradmin.server.repository.UserProfileRepository;
import org.gridsuite.useradmin.server.service.DirectoryService;
import org.gridsuite.useradmin.server.utils.WireMockUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserProfileTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserInfosRepository userInfosRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserAdminApplicationProps userAdminApplicationProps;

    private WireMockServer wireMockServer;

    private WireMockUtils wireMockUtils;

    private ObjectWriter objectWriter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        DirectoryService.setDirectoryServerBaseUri(wireMockServer.baseUrl());
        wireMockUtils = new WireMockUtils(wireMockServer);
        objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
    }

    @AfterEach
    void tearOff() {
        userInfosRepository.deleteAll();
        userProfileRepository.deleteAll();

        try {
            wireMockServer.checkForUnmatchedRequests();
            assertEquals(0, wireMockServer.findAll(WireMock.anyRequestedFor(WireMock.anyUrl())).size());
        } finally {
            wireMockServer.shutdown();
        }
    }

    private static final String ADMIN_USER = "admin1";
    private static final String NOT_ADMIN = "notAdmin";
    private static final String PROFILE_1 = "profile_1";
    private static final String PROFILE_2 = "profile_2";

    @Test
    void testEmptyProfileList() throws Exception {
        // no existing profile in empty db
        assertEquals(0, getProfileList().size());
    }

    @Test
    void testCreateProfile() throws Exception {
        createProfile(PROFILE_1, ADMIN_USER, 10, 15, HttpStatus.CREATED);

        List<UserProfile> userProfiles = getProfileList();
        assertEquals(1, userProfiles.size());
        assertEquals(PROFILE_1, userProfiles.get(0).name());
        assertNull(userProfiles.get(0).loadFlowParameterId());
        assertNull(userProfiles.get(0).allParametersLinksValid());
        assertEquals(10, userProfiles.get(0).maxAllowedCases());
        assertEquals(15, userProfiles.get(0).maxAllowedBuilds());

        createProfile(PROFILE_2, ADMIN_USER, null, null, HttpStatus.CREATED);
    }

    @Test
    void testCreateProfileForbidden() throws Exception {
        createProfile(PROFILE_1, NOT_ADMIN, 1, 0, HttpStatus.FORBIDDEN);
    }

    @Test
    void testDeleteExistingProfile() throws Exception {
        createProfile(PROFILE_1, ADMIN_USER, null, null, HttpStatus.CREATED);
        assertEquals(1, getProfileList().size());
        removeProfile(PROFILE_1, ADMIN_USER, HttpStatus.NO_CONTENT);
        assertEquals(0, getProfileList().size());
    }

    @Test
    void testDeleteProfileForbidden() throws Exception {
        removeProfile(PROFILE_1, NOT_ADMIN, HttpStatus.FORBIDDEN);
    }

    @Test
    void testDeleteProfileNotFound() throws Exception {
        removeProfile("noExist", ADMIN_USER, HttpStatus.NOT_FOUND);
    }

    @Test
    void testProfileUpdateNotFound() throws Exception {
        updateProfile(new UserProfile(UUID.randomUUID(), PROFILE_2, null, null, null, null), ADMIN_USER, HttpStatus.NOT_FOUND);
    }

    @Test
    void testProfileUpdateForbidden() throws Exception {
        updateProfile(new UserProfile(UUID.randomUUID(), PROFILE_2, null, null, null, null), NOT_ADMIN, HttpStatus.FORBIDDEN);
    }

    @Test
    void testProfileUpdateValidityOk() throws Exception {
        updateProfile(true);
    }

    @Test
    void testProfileUpdateValidityKo() throws Exception {
        updateProfile(false);
    }

    @Test
    void testGetProfileMaxAllowedCases() throws Exception {
        UserProfileEntity userProfileEntity = new UserProfileEntity(UUID.randomUUID(), "profileName", null, 15, null);
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), ADMIN_USER, userProfileEntity);
        userProfileRepository.save(userProfileEntity);
        userInfosRepository.save(userInfosEntity);

        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-cases", ADMIN_USER)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("15", result.getResponse().getContentAsString());
    }

    @Test
    void testGetProfileMaxAllowedBuilds() throws Exception {
        UserProfileEntity userProfileEntity = new UserProfileEntity(UUID.randomUUID(), "profileName", null, null, 15);
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), ADMIN_USER, userProfileEntity);
        userProfileRepository.save(userProfileEntity);
        userInfosRepository.save(userInfosEntity);

        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-builds", ADMIN_USER)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("15", result.getResponse().getContentAsString());
    }

    @Test
    void testGetProfileMaxAllowedCasesWithNoProfileSet() throws Exception {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), ADMIN_USER, null);
        userInfosRepository.save(userInfosEntity);

        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-cases", ADMIN_USER)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isOk())
                .andReturn();
        String defaultMaxAllowedCases = String.valueOf(userAdminApplicationProps.getDefaultMaxAllowedCases());
        assertEquals(defaultMaxAllowedCases, result.getResponse().getContentAsString());
    }

    @Test
    void testGetProfileMaxAllowedBuildsWithNoProfileSet() throws Exception {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), ADMIN_USER, null);
        userInfosRepository.save(userInfosEntity);

        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-builds", ADMIN_USER)
                        .header("userId", ADMIN_USER)
                )
                .andExpect(status().isOk())
                .andReturn();
        String defaultMaxAllowedBuilds = String.valueOf(userAdminApplicationProps.getDefaultMaxAllowedBuilds());
        assertEquals(defaultMaxAllowedBuilds, result.getResponse().getContentAsString());
    }

    private void updateProfile(boolean validParameters) throws Exception {
        UUID lfParametersUuid = UUID.randomUUID();
        // stub for parameters elements existence check
        final String urlPath = "/v1/elements";
        List<ElementAttributes> existingElements = validParameters ? List.of(new ElementAttributes(lfParametersUuid, "name", "type")) : List.of();
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlPath + "\\?strictMode=false&ids=" + lfParametersUuid))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(existingElements))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))).getId();

        UUID profileUuid = createProfile(PROFILE_1, ADMIN_USER, null, 0, HttpStatus.CREATED);

        // udpate the profile: change name and set its LF parameters and maxAllowedCases
        UserProfile userProfile = new UserProfile(profileUuid, PROFILE_2, lfParametersUuid, null, 10, 11);
        updateProfile(userProfile, ADMIN_USER, HttpStatus.OK);

        // profiles list (with validity flag)
        List<UserProfile> userProfiles = getProfileList();
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(List.of(lfParametersUuid)), false);
        assertEquals(1, userProfiles.size());
        assertEquals(lfParametersUuid, userProfiles.get(0).loadFlowParameterId());
        assertEquals(validParameters, userProfiles.get(0).allParametersLinksValid());
        assertEquals(10, userProfiles.get(0).maxAllowedCases());
        assertEquals(11, userProfiles.get(0).maxAllowedBuilds());
    }

    private static Map<String, StringValuePattern> handleQueryParams(List<UUID> paramIds) {
        return Map.of("ids", WireMock.matching(paramIds.stream().map(uuid -> ".+").collect(Collectors.joining(","))));
    }

    private UUID createProfile(String profileName, String userName, Integer maxAllowedCases, Integer maxAllowedBuilds, HttpStatusCode status) throws Exception {
        UserProfile profileInfo = new UserProfile(null, profileName, null, false, maxAllowedCases, maxAllowedBuilds);
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(profileInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", userName)
                )
                .andExpect(status().is(status.value()))
                .andReturn();
        if (status == HttpStatus.CREATED) {
            // check repository
            Optional<UserProfileEntity> profile1 = userProfileRepository.findByName(profileName);
            assertTrue(profile1.isPresent());
            assertEquals(
                    Optional.ofNullable(maxAllowedCases).orElse(userAdminApplicationProps.getDefaultMaxAllowedCases()),
                    profile1.get().getMaxAllowedCases()
            );
            assertEquals(
                    Optional.ofNullable(maxAllowedBuilds).orElse(userAdminApplicationProps.getDefaultMaxAllowedBuilds()),
                    profile1.get().getMaxAllowedBuilds()
            );
            assertNull(profile1.get().getLoadFlowParameterId()); // no LF params by dft
            return profile1.get().getId();
        }
        return null;
    }

    private List<UserProfile> getProfileList() throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles")
                                .header("userId", ADMIN_USER)
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
    }

    private void removeProfile(String profileName, String userName, HttpStatusCode status) throws Exception {
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(List.of(profileName)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", userName)
                )
                .andExpect(status().is(status.value()))
                .andReturn();
    }

    private void updateProfile(UserProfile newData, String userName, HttpStatusCode status) throws Exception {
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", newData.id())
                        .content(objectWriter.writeValueAsString(newData))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", userName))
                .andExpect(status().is(status.value()));

        if (status == HttpStatus.OK) {
            // check access to updated profile
            UserProfile updatedProfile = objectMapper.readValue(
                    mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", newData.id())
                                    .header("userId", userName)
                                    .contentType(APPLICATION_JSON))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    new TypeReference<>() { });
            assertNotNull(updatedProfile);
            assertEquals(newData.name(), updatedProfile.name());
            assertEquals(newData.loadFlowParameterId(), updatedProfile.loadFlowParameterId());
            assertEquals(newData.maxAllowedCases(), updatedProfile.maxAllowedCases());
            assertNull(updatedProfile.allParametersLinksValid()); // validity not set in this case
        }
    }
}
