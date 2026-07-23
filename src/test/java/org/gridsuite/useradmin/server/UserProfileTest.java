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
import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.dto.UserProfile;
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

import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.gridsuite.useradmin.server.Utils.ROLES_HEADER;
import static org.gridsuite.useradmin.server.dto.QuotaType.*;
import static org.gridsuite.useradmin.server.utils.TestConstants.USER_ADMIN_ROLE;
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
        createProfile(PROFILE_1, ADMIN_USER, USER_ADMIN_ROLE, 10, 15, HttpStatus.CREATED);

        List<UserProfile> userProfiles = getProfileList(false);
        assertEquals(1, userProfiles.size());
        assertEquals(PROFILE_1, userProfiles.getFirst().getName());
        assertNull(userProfiles.getFirst().getLoadFlowParameterId());
        assertNull(userProfiles.getFirst().getSecurityAnalysisParameterId());
        assertNull(userProfiles.getFirst().getSensitivityAnalysisParameterId());
        assertNull(userProfiles.getFirst().getShortcircuitParameterId());
        assertNull(userProfiles.getFirst().getPccMinParameterId());
        assertNull(userProfiles.getFirst().getVoltageInitParameterId());
        assertNull(userProfiles.getFirst().getAllLinksValid());
        assertEquals(10, userProfiles.getFirst().getMaxOperationQuota().get(CASES));
        assertEquals(15, userProfiles.getFirst().getMaxOperationQuota().get(BUILD));
        assertNull(userProfiles.getFirst().getSpreadsheetConfigCollectionId());
        assertNull(userProfiles.getFirst().getNetworkVisualizationParameterId());

        createProfile(PROFILE_2, ADMIN_USER, USER_ADMIN_ROLE, null, null, HttpStatus.CREATED);
        createProfile(PROFILE_1, ADMIN_USER, USER_ADMIN_ROLE, null, null, HttpStatus.BAD_REQUEST);  // profile already exists
    }

    @Test
    void testCreateProfileForbidden() throws Exception {
        createProfile(PROFILE_1, NOT_ADMIN, "USER", 1, 0, HttpStatus.FORBIDDEN);
    }

    @Test
    void testDeleteExistingProfile() throws Exception {
        createProfile(PROFILE_1, ADMIN_USER, USER_ADMIN_ROLE, null, null, HttpStatus.CREATED);
        assertEquals(1, getProfileList(false).size());
        removeProfile(PROFILE_1, ADMIN_USER, USER_ADMIN_ROLE, HttpStatus.NO_CONTENT);
        assertEquals(0, getProfileList(false).size());
    }

    @Test
    void testDeleteProfileForbidden() throws Exception {
        removeProfile(PROFILE_1, NOT_ADMIN, "USER", HttpStatus.FORBIDDEN);
    }

    @Test
    void testDeleteProfileNotFound() throws Exception {
        removeProfile("noExist", ADMIN_USER, USER_ADMIN_ROLE, HttpStatus.NOT_FOUND);
    }

    @Test
    void testProfileUpdateNotFound() throws Exception {
        updateProfile(UserProfile.builder().id(UUID.randomUUID()).name(PROFILE_2).build(), ADMIN_USER, USER_ADMIN_ROLE, HttpStatus.NOT_FOUND);
    }

    @Test
    void testProfileUpdateForbidden() throws Exception {
        updateProfile(UserProfile.builder().id(UUID.randomUUID()).name(PROFILE_2).build(), NOT_ADMIN, "USER", HttpStatus.FORBIDDEN);
    }

    @Test
    void testProfileUpdateValidityOk() throws Exception {
        updateProfile(true);
    }

    @Test
    void testProfileUpdateValidityKo() throws Exception {
        updateProfile(false);
    }

    private void updateProfile(boolean validParameters) throws Exception {
        UUID loadFlowParametersUuid = UUID.fromString("11111111-9594-4e55-8ec7-07ea965d24eb");
        UUID securityAnalysisParametersUuid = UUID.fromString("22222222-9594-4e55-8ec7-07ea965d24eb");
        UUID sensitivityAnalysisParametersUuid = UUID.fromString("33333333-9594-4e55-8ec7-07ea965d24eb");
        UUID shortcircuitParametersUuid = UUID.fromString("44444444-9594-4e55-8ec7-07ea965d24eb");
        UUID pccminParametersUuid = UUID.fromString("55555555-9594-4e55-8ec7-07ea965d24eb");
        UUID voltageInitParametersUuid = UUID.fromString("66666666-9594-4e55-8ec7-07ea965d24eb");
        UUID spreadsheetConfigCollectionUuid = UUID.fromString("77777777-9594-4e55-8ec7-07ea965d24eb");
        UUID networkVisualizationParametersUuid = UUID.fromString("88888888-9594-4e55-8ec7-07ea965d24eb");
        UUID diagramConfigUuid = UUID.fromString("9999999-9594-4e55-8ec7-07ea965d24eb");
        List<UUID> elementsUuids = List.of(loadFlowParametersUuid, securityAnalysisParametersUuid,
            sensitivityAnalysisParametersUuid, shortcircuitParametersUuid, pccminParametersUuid, voltageInitParametersUuid,
                spreadsheetConfigCollectionUuid, networkVisualizationParametersUuid, diagramConfigUuid);

        // stub for parameters and spreadsheet config collection elements existence check
        final String urlPath = "/v1/elements";
        List<ElementAttributes> existingElements = validParameters ? List.of(
            new ElementAttributes(loadFlowParametersUuid, "loadFlowParams", "LOADFLOW_PARAMETERS"),
            new ElementAttributes(securityAnalysisParametersUuid, "securityAnalysisParams", "SECURITY_ANALYSIS_PARAMETERS"),
            new ElementAttributes(sensitivityAnalysisParametersUuid, "sensitivityAnalysisParams", "SENSITIVITY_PARAMETERS"),
            new ElementAttributes(shortcircuitParametersUuid, "shortcircuitParams", "SHORT_CIRCUIT_PARAMETERS"),
            new ElementAttributes(pccminParametersUuid, "pccminParams", "PCC_MIN_PARAMETERS"),
            new ElementAttributes(voltageInitParametersUuid, "voltageInitParams", "VOLTAGE_INIT_PARAMETERS"),
            new ElementAttributes(spreadsheetConfigCollectionUuid, "spreadsheetConfigCollection", "SPREADSHEET_CONFIG_COLLECTION"),
            new ElementAttributes(networkVisualizationParametersUuid, "networkVisualizationParams", "NETWORK_VISUALIZATION_PARAMETERS"),
            new ElementAttributes(diagramConfigUuid, "diagramConfig", "DIAGRAM_CONFIG")
            ) : List.of();
        UUID stubId = wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlPath + "\\?strictMode=false&ids=" + elementsUuids.stream().map(UUID::toString).collect(Collectors.joining(","))))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(existingElements))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))).getId();

        UUID profileUuid = createProfile(PROFILE_1, ADMIN_USER, USER_ADMIN_ROLE, null, 0, HttpStatus.CREATED);

        // udpate the profile: change name and set its parameters, maxAllowedCases, maxAllowedBuilds and spreadsheet config collection
        Map<QuotaType, Integer> maxAllowedValues = new EnumMap<>(QuotaType.class);
        maxAllowedValues.put(CASES, 10);
        maxAllowedValues.put(BUILD, 11);
        UserProfile userProfile = UserProfile.builder()
                .id(profileUuid).name(PROFILE_2)
                .loadFlowParameterId(loadFlowParametersUuid)
                .securityAnalysisParameterId(securityAnalysisParametersUuid)
                .sensitivityAnalysisParameterId(sensitivityAnalysisParametersUuid)
                .shortcircuitParameterId(shortcircuitParametersUuid)
                .pccMinParameterId(pccminParametersUuid)
                .voltageInitParameterId(voltageInitParametersUuid)
                .maxOperationQuota(maxAllowedValues)
                .spreadsheetConfigCollectionId(spreadsheetConfigCollectionUuid)
                .networkVisualizationParameterId(networkVisualizationParametersUuid)
                .workspaceId(diagramConfigUuid)
                .build();
        updateProfile(userProfile, ADMIN_USER, USER_ADMIN_ROLE, HttpStatus.OK);

        // profiles list (with validity flag)
        List<UserProfile> userProfiles = getProfileList(true);
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(elementsUuids), false, 1);
        assertEquals(1, userProfiles.size());
        assertEquals(loadFlowParametersUuid, userProfiles.get(0).getLoadFlowParameterId());
        assertEquals(securityAnalysisParametersUuid, userProfiles.get(0).getSecurityAnalysisParameterId());
        assertEquals(sensitivityAnalysisParametersUuid, userProfiles.get(0).getSensitivityAnalysisParameterId());
        assertEquals(shortcircuitParametersUuid, userProfiles.get(0).getShortcircuitParameterId());
        assertEquals(pccminParametersUuid, userProfiles.get(0).getPccMinParameterId());
        assertEquals(voltageInitParametersUuid, userProfiles.get(0).getVoltageInitParameterId());
        assertEquals(validParameters, userProfiles.get(0).getAllLinksValid());
        assertEquals(10, userProfiles.get(0).getMaxOperationQuota().get(CASES));
        assertEquals(11, userProfiles.get(0).getMaxOperationQuota().get(BUILD));
        assertEquals(spreadsheetConfigCollectionUuid, userProfiles.get(0).getSpreadsheetConfigCollectionId());
        assertEquals(networkVisualizationParametersUuid, userProfiles.get(0).getNetworkVisualizationParameterId());

        // profiles list (without validity flag)
        userProfiles = getProfileList(false);
        wireMockUtils.verifyGetRequest(stubId, urlPath, handleQueryParams(elementsUuids), false, 0);
        assertNull(userProfiles.get(0).getAllLinksValid());
    }

    private static Map<String, StringValuePattern> handleQueryParams(List<UUID> paramIds) {
        return Map.of("ids", WireMock.matching(paramIds.stream().map(uuid -> ".+").collect(Collectors.joining(","))));
    }

    private UUID createProfile(String profileName, String userName, String userRole, Integer maxAllowedCases, Integer maxAllowedBuilds, HttpStatusCode status) throws Exception {
        Map<QuotaType, Integer> maxAllowedValues = new EnumMap<>(QuotaType.class);
        maxAllowedValues.put(CASES, maxAllowedCases);
        maxAllowedValues.put(BUILD, maxAllowedBuilds);
        UserProfile profileInfo = UserProfile.builder().name(profileName).maxOperationQuota(maxAllowedValues).build();
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
            assertNull(profile1.get().getPccminParameterId()); // no pccmin params by dft
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
                                .header(ROLES_HEADER, USER_ADMIN_ROLE)
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
        mockMvc.perform(put("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", newData.getId())
                        .content(objectWriter.writeValueAsString(newData))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("userId", userName)
                        .header(ROLES_HEADER, userRole))
                .andExpect(status().is(status.value()));

        if (status == HttpStatus.OK) {
            // check access to updated profile
            UserProfile updatedProfile = objectMapper.readValue(
                    mockMvc.perform(get("/" + UserAdminApi.API_VERSION + "/profiles/{profileUuid}", newData.getId())
                                    .header("userId", userName)
                                    .header(ROLES_HEADER, userRole)
                                    .contentType(APPLICATION_JSON))
                            .andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString(),
                    new TypeReference<>() { });
            assertNotNull(updatedProfile);
            assertEquals(newData.getName(), updatedProfile.getName());
            assertEquals(newData.getLoadFlowParameterId(), updatedProfile.getLoadFlowParameterId());
            assertEquals(newData.getSecurityAnalysisParameterId(), updatedProfile.getSecurityAnalysisParameterId());
            assertEquals(newData.getSensitivityAnalysisParameterId(), updatedProfile.getSensitivityAnalysisParameterId());
            assertEquals(newData.getShortcircuitParameterId(), updatedProfile.getShortcircuitParameterId());
            assertEquals(newData.getPccMinParameterId(), updatedProfile.getPccMinParameterId());
            assertEquals(newData.getVoltageInitParameterId(), updatedProfile.getVoltageInitParameterId());
            assertEquals(newData.getMaxOperationQuota().get(BUILD), updatedProfile.getMaxOperationQuota().get(BUILD));
            assertNull(updatedProfile.getAllLinksValid()); // validity not set in this case
            assertEquals(newData.getSpreadsheetConfigCollectionId(), updatedProfile.getSpreadsheetConfigCollectionId());
            assertEquals(newData.getNetworkVisualizationParameterId(), updatedProfile.getNetworkVisualizationParameterId());
        }
    }
}
