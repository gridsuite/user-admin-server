/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.dto.UserIdentitiesResult;
import org.gridsuite.useradmin.server.dto.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Service for fetching user identity information (firstName, lastName) from user-identity-server.
 * All operations fail silently to ensure that identity enrichment doesn't break core functionality.
 *
 * @author Achour Berrahma {@literal <achour.berrahma at rte-france.com>}
 */
@Service
public class UserIdentityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserIdentityService.class);
    private static final String USER_IDENTITY_API_VERSION = "v1";
    private static final String IDENTITIES_PATH = "/" + USER_IDENTITY_API_VERSION + "/users/identities";

    private final RestTemplate restTemplate;
    private final String userIdentityServerBaseUri;

    public UserIdentityService(
            RestTemplate restTemplate,
            @Value("${gridsuite.services.user-identity-server.base-uri:http://user-identity-server/}") String userIdentityServerBaseUri) {
        this.restTemplate = restTemplate;
        this.userIdentityServerBaseUri = userIdentityServerBaseUri;
    }

    /**
     * Fetches identity information for a single user.
     * Fails silently if the service is unavailable or a user is not found.
     *
     * @param sub the user's subject identifier
     * @return Optional containing UserIdentity if found, empty otherwise
     */
    public Optional<UserIdentity> getIdentity(String sub) {
        if (sub == null || sub.isBlank()) {
            return Optional.empty();
        }

        try {
            String url = userIdentityServerBaseUri + IDENTITIES_PATH + "/" + sub;
            UserIdentity identity = restTemplate.getForObject(url, UserIdentity.class);
            return Optional.ofNullable(identity);
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch identity for user '{}': {}", sub, e.getMessage());
            LOGGER.debug("Identity fetch error details", e);
            return Optional.empty();
        }
    }

    /**
     * Fetches identity information for multiple users in a single request.
     * Fails silently if the service is unavailable.
     *
     * @param subs collection of user subject identifiers
     * @return Map of sub to UserIdentity for successfully fetched identities
     */
    public Map<String, UserIdentity> getIdentities(Collection<String> subs) {
        if (CollectionUtils.isEmpty(subs)) {
            return Map.of();
        }

        try {
            String url = UriComponentsBuilder.fromUriString(userIdentityServerBaseUri + IDENTITIES_PATH)
                    .queryParam("subs", String.join(",", subs))
                    .toUriString();

            UserIdentitiesResult result = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<UserIdentitiesResult>() { }
            ).getBody();

            if (result == null || result.data() == null) {
                return Map.of();
            }

            if (!result.errors().isEmpty()) {
                LOGGER.debug("Some user identities could not be fetched: {}", result.errors().keySet());
            }

            return result.data();
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch identities for {} users: {}", subs.size(), e.getMessage());
            LOGGER.debug("Batch identity fetch error details", e);
            return Map.of();
        }
    }

}
