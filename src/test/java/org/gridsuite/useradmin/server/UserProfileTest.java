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
import static org.gridsuite.useradmin.server.Utils.ROLES_HEADER;
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
        assertEquals(0, getProfileList(false).size());
    }

    @Test
    void testCreateProfile() throws Exception {
        createProfile(PROFILE_1, ADMIN_USER, userAdminApplicationProps.getAdminRole(), 10, 15, HttpStatus.CREATED);

        List<UserProfile> userProfiles = getProfileList(false);
        assertEquals(1, userProfiles.size());
        assertEquals(PROFILE_1, userProfiles.get(0).name());
        assertNull(userProfiles.get(0).loadFlowParameterId());
        assertNull(userProfiles.get(0).securityAnalysisParameterId());
        assertNull(userProfiles.get(0).sensitivityAnalysisParameterId());
        assertNull(userProfiles.get(0).shortcircuitParameterId());
        assertNull(userProfiles.get(0).voltageInitParameterId());
        assertNull(userProfiles.get(0).allLinksValid());
        assertEquals(10, userProfiles.get(0).maxAllowedCases());
        assertEquals(15, userProfiles.get(0).maxAllowedBuilds());
        assertNull(userProfiles.get(0).spreadsheetConfigCollectionId());
        assertNull(userProfiles.get(0).networkVisualizationParameterId());

        createProfile(PROFILE_2, ADMIN_USER, userAdminApplicationProps.getAdminRole(), null, null, HttpStatus.CREATED);
        createProfile(PROFILE_1, ADMIN_USER, userAdminApplicationProps.getAdminRole(), null, null, HttpStatus.BAD_REQUEST);  // profile already exists
    }

    @Test
    void testCreateProfileForbidden() throws Exception {
        createProfile(PROFILE_1, NOT_ADMIN, "USER", 1, 0, HttpStatus.FORBIDDEN);
    }

    @Test
    void testDeleteExistingProfile() throws Exception {
        createProfile(PROFILE_1, ADMIN_USER, userAdminApplicationProps.getAdminRole(), null, null, HttpStatus.CREATED);
        assertEquals(1, getProfileList(false).size());
        removeProfile(PROFILE_1, ADMIN_USER, userAdminApplicationProps.getAdminRole(), HttpStatus.NO_CONTENT);
        assertEquals(0, getProfileList(false).size());
    }

    @Test
    void testDeleteProfileForbidden() throws Exception {
        removeProfile(PROFILE_1, NOT_ADMIN, "USER", HttpStatus.FORBIDDEN);
    }

    @Test
    void testDeleteProfileNotFound() throws Exception {
        removeProfile("noExist", ADMIN_USER, userAdminApplicationProps.getAdminRole(), HttpStatus.NOT_FOUND);
    }

    @Test
    void testProfileUpdateNotFound() throws Exception {
        updateProfile(new UserProfile(UUID.randomUUID(), PROFILE_2, null, null, null, null, null, null, null, null, null, null),
                ADMIN_USER,
                userAdminApplicationProps.getAdminRole(),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void testProfileUpdateForbidden() throws Exception {
        updateProfile(new UserProfile(UUID.randomUUID(), PROFILE_2, null, null, null, null, null, null, null, null, null, null),
                NOT_ADMIN,
                "USER",
                HttpStatus.FORBIDDEN);
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
        UserProfileEntity userProfileEntity = new UserProfileEntity(UUID.randomUUID(), "profileName", null, null, null, null, null, 15, null, null, null);
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), ADMIN_USER, userProfileEntity, null);
        userProfileRepository.save(userProfileEntity);
        userInfosRepository.save(userInfosEntity);

        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-cases", ADMIN_USER)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, userAdminApplicationProps.getAdminRole())
                )
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("15", result.getResponse().getContentAsString());
    }

    @Test
    void testGetProfileMaxAllowedBuilds() throws Exception {
        UserProfileEntity userProfileEntity = new UserProfileEntity(UUID.randomUUID(), "profileName", null, null, null, null, null, null, 15, null, null);
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), ADMIN_USER, userProfileEntity, null);
        userProfileRepository.save(userProfileEntity);
        userInfosRepository.save(userInfosEntity);

        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-builds", ADMIN_USER)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, userAdminApplicationProps.getAdminRole())
                )
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("15", result.getResponse().getContentAsString());
    }

    @Test
    void testGetProfileMaxAllowedCasesWithNoProfileSet() throws Exception {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), ADMIN_USER, null, null);
        userInfosRepository.save(userInfosEntity);

        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-cases", ADMIN_USER)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, userAdminApplicationProps.getAdminRole())
                )
                .andExpect(status().isOk())
                .andReturn();
        String defaultMaxAllowedCases = String.valueOf(userAdminApplicationProps.getDefaultMaxAllowedCases());
        assertEquals(defaultMaxAllowedCases, result.getResponse().getContentAsString());
    }

    @Test
    void testGetProfileMaxAllowedBuildsWithNoProfileSet() throws Exception {
        UserInfosEntity userInfosEntity = new UserInfosEntity(UUID.randomUUID(), ADMIN_USER, null, null);
        userInfosRepository.save(userInfosEntity);

        MvcResult result = mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/users/{sub}/profile/max-builds", ADMIN_USER)
                        .header("userId", ADMIN_USER)
                        .header(ROLES_HEADER, userAdminApplicationProps.getAdminRole())
                )
                .andExpect(status().isOk())
                .andReturn();
        String defaultMaxAllowedBuilds = String.valueOf(userAdminApplicationProps.getDefaultMaxAllowedBuilds());
        assertEquals(defaultMaxAllowedBuilds, result.getResponse().getContentAsString());
    }

    private void updateProfile(boolean validParameters) throws Exception {
        UUID loadFlowParametersUuid = UUID.fromString("11111111-9594-4e55-8ec7-07ea965d24eb");
        UUID securityAnalysisParametersUuid = UUID.fromString("22222222-9594-4e55-8ec7-07ea965d24eb");
        UUID sensitivityAnalysisParametersUuid = UUID.fromString("33333333-9594-4e55-8ec7-07ea965d24eb");
        UUID shortcircuitParametersUuid = UUID.fromString("44444444-9594-4e55-8ec7-07ea965d24eb");
        UUID voltageInitParametersUuid = UUID.fromString("55555555-9594-4e55-8ec7-07ea965d24eb");
        UUID spreadsheetConfigCollectionUuid = UUID.fromString("66666666-9594-4e55-8ec7-07ea965d24eb");
        UUID networkVisualizationParametersUuid = UUID.fromString("77777777-9594-4e55-8ec7-07ea965d24eb");
        List<UUID> elementsUuids = List.of(loadFlowParametersUuid, securityAnalysisParametersUuid,
            sensitivityAnalysisParametersUuid, shortcircuitParametersUuid, voltageInitParametersUuid, spreadsheetConfigCollectionUuid, networkVisualizationParametersUuid);

        // stub for parameters and spreadsheet config collection elements existence check
        final String urlPath = "/v1/elements";
        List<ElementAttributes> existingElements = validParameters ? List.of(
            new ElementAttributes(loadFlowParametersUuid, "loadFlowParams", "LOADFLOW_PARAMETERS"),
            new ElementAttributes(securityAnalysisParametersUuid, "securityAnalysisParams", "SECURITY_ANALYSIS_PARAMETERS"),
            new ElementAttributes(sensitivityAnalysisParametersUuid, "sensitivityAnalysisParams", "SENSITIVITY_PARAMETERS"),
            new ElementAttributes(shortcircuitParametersUuid, "shortcircuitParams", "SHORT_CIRCUIT_PARAMETERS"),
            new ElementAttributes(voltageInitParametersUuid, "voltageInitParams", "VOLTAGE_INIT_PARAMETERS"),
            new ElementAttributes(spreadsheetConfigCollectionUuid, "spreadsheetConfigCollection", "SPREADSHEET_CONFIG_COLLECTION"),
            new ElementAttributes(networkVisualizationParametersUuid, "networkVisualizationParams", "NETWORK_VISUALIZATION_PARAMETERS")
            ) : List.of();
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlPath + "\\?strictMode=false&ids=" + elementsUuids.stream().map(UUID::toString).collect(Collectors.joining(","))))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(existingElements))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))).getId();

        UUID profileUuid = createProfile(PROFILE_1, ADMIN_USER, userAdminApplicationProps.getAdminRole(), null, 0, HttpStatus.CREATED);

        // udpate the profile: change name and set its parameters, maxAllowedCases, maxAllowedBuilds and spreadsheet config collection
        UserProfile userProfile = new UserProfile(profileUuid, PROFILE_2, loadFlowParametersUuid, securityAnalysisParametersUuid,
            sensitivityAnalysisParametersUuid, shortcircuitParametersUuid, voltageInitParametersUuid, null, 10, 11, spreadsheetConfigCollectionUuid, networkVisualizationParametersUuid);
        updateProfile(userProfile, ADMIN_USER, userAdminApplicationProps.getAdminRole(), HttpStatus.OK);

        // profiles list (with validity flag)
        List<UserProfile> userProfiles = getProfileList(true);
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(elementsUuids), false, 1);
        assertEquals(1, userProfiles.size());
        assertEquals(loadFlowParametersUuid, userProfiles.get(0).loadFlowParameterId());
        assertEquals(securityAnalysisParametersUuid, userProfiles.get(0).securityAnalysisParameterId());
        assertEquals(sensitivityAnalysisParametersUuid, userProfiles.get(0).sensitivityAnalysisParameterId());
        assertEquals(shortcircuitParametersUuid, userProfiles.get(0).shortcircuitParameterId());
        assertEquals(voltageInitParametersUuid, userProfiles.get(0).voltageInitParameterId());
        assertEquals(validParameters, userProfiles.get(0).allLinksValid());
        assertEquals(10, userProfiles.get(0).maxAllowedCases());
        assertEquals(11, userProfiles.get(0).maxAllowedBuilds());
        assertEquals(spreadsheetConfigCollectionUuid, userProfiles.get(0).spreadsheetConfigCollectionId());
        assertEquals(networkVisualizationParametersUuid, userProfiles.get(0).networkVisualizationParameterId());

        // profiles list (without validity flag)
        userProfiles = getProfileList(false);
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(elementsUuids), false, 0);
        assertNull(userProfiles.get(0).allLinksValid());
    }

    private static Map<String, StringValuePattern> handleQueryParams(List<UUID> paramIds) {
        return Map.of("ids", WireMock.matching(paramIds.stream().map(uuid -> ".+").collect(Collectors.joining(","))));
    }

    private UUID createProfile(String profileName, String userName, String userRole, Integer maxAllowedCases, Integer maxAllowedBuilds, HttpStatusCode status) throws Exception {
        UserProfile profileInfo = new UserProfile(null, profileName, null, null, null, null, null, false, maxAllowedCases, maxAllowedBuilds, null, null);
        mockMvc.perform(post("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(profileInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", userName)
                        .header(ROLES_HEADER, userRole)
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
            assertNull(profile1.get().getLoadFlowParameterId()); // no loadflow params by dft
            assertNull(profile1.get().getSecurityAnalysisParameterId()); // no security analysis params by dft
            assertNull(profile1.get().getSensitivityAnalysisParameterId()); // no sensitivity analysis params by dft
            assertNull(profile1.get().getShortcircuitParameterId()); // no shortcircuit params by dft
            assertNull(profile1.get().getVoltageInitParameterId()); // no voltage init params by dft
            assertNull(profile1.get().getSpreadsheetConfigCollectionId()); // no spreadsheet config collection by dft
            assertNull(profile1.get().getNetworkVisualizationParameterId()); // no network visualization params by dft
            return profile1.get().getId();
        }
        return null;
    }

    private List<UserProfile> getProfileList(boolean checkLinksValidity) throws Exception {
        return objectMapper.readValue(
                mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles?checkLinksValidity=" + checkLinksValidity)
                                .header("userId", ADMIN_USER)
                                .header(ROLES_HEADER, userAdminApplicationProps.getAdminRole())
                                .contentType(APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() { });
    }

    private void removeProfile(String profileName, String userName, String userRole, HttpStatusCode status) throws Exception {
        mockMvc.perform(delete("/" + UserAdminApi.API_VERSION + "/profiles")
                        .content(objectWriter.writeValueAsString(List.of(profileName)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", userName)
                        .header(ROLES_HEADER, userRole)
                )
                .andExpect(status().is(status.value()))
                .andReturn();
    }

    private void updateProfile(UserProfile newData, String userName, String userRole, HttpStatusCode status) throws Exception {
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", newData.id())
                        .content(objectWriter.writeValueAsString(newData))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", userName)
                        .header(ROLES_HEADER, userRole))
                .andExpect(status().is(status.value()));

        if (status == HttpStatus.OK) {
            // check access to updated profile
            UserProfile updatedProfile = objectMapper.readValue(
                    mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", newData.id())
                                    .header("userId", userName)
                                    .header(ROLES_HEADER, userRole)
                                    .contentType(APPLICATION_JSON))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    new TypeReference<>() { });
            assertNotNull(updatedProfile);
            assertEquals(newData.name(), updatedProfile.name());
            assertEquals(newData.loadFlowParameterId(), updatedProfile.loadFlowParameterId());
            assertEquals(newData.securityAnalysisParameterId(), updatedProfile.securityAnalysisParameterId());
            assertEquals(newData.sensitivityAnalysisParameterId(), updatedProfile.sensitivityAnalysisParameterId());
            assertEquals(newData.shortcircuitParameterId(), updatedProfile.shortcircuitParameterId());
            assertEquals(newData.voltageInitParameterId(), updatedProfile.voltageInitParameterId());
            assertEquals(newData.maxAllowedCases(), updatedProfile.maxAllowedCases());
            assertNull(updatedProfile.allLinksValid()); // validity not set in this case
            assertEquals(newData.spreadsheetConfigCollectionId(), updatedProfile.spreadsheetConfigCollectionId());
            assertEquals(newData.networkVisualizationParameterId(), updatedProfile.networkVisualizationParameterId());
        }
    }
}
