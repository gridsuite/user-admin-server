/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.gridsuite.useradmin.server.dto.ElementAttributes;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */

@Service
public class DirectoryService {

    private static final String DIRECTORY_SERVER_API_VERSION = "v1";

    private static final String DELIMITER = "/";

    private static final String ELEMENTS_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER
            + "elements";

    private static final String USER_SERVER_ROOT_PATH = DELIMITER + DIRECTORY_SERVER_API_VERSION + DELIMITER
            + "users";

    private final RestTemplate restTemplate = new RestTemplate();

    private static String directoryServerBaseUri;

    public DirectoryService(@Value("${gridsuite.services.directory-server.base-uri:http://directory-server/}") String directoryServerBaseUri) {
        setDirectoryServerBaseUri(directoryServerBaseUri);
    }

    public static void setDirectoryServerBaseUri(String serverBaseUri) {
        DirectoryService.directoryServerBaseUri = serverBaseUri;
    }

    public Set<UUID> getExistingElements(Set<UUID> elementsUuids) {
        if (CollectionUtils.isEmpty(elementsUuids)) {
            return Set.of();
        }
        String path = UriComponentsBuilder.fromPath(ELEMENTS_SERVER_ROOT_PATH)
                .queryParam("strictMode", false) // no strict mode, to retrieve all elementsUuids, even if some of them don't exist
                .queryParam("ids", elementsUuids)
                .toUriString();
        List<ElementAttributes> existingElementList = restTemplate.exchange(directoryServerBaseUri + path, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ElementAttributes>>() {
                }).getBody();
        return existingElementList == null ? Set.of() : existingElementList.stream().map(ElementAttributes::getElementUuid).collect(Collectors.toSet());
    }

    public Integer getCasesCount(String userId) {
        String path = UriComponentsBuilder.fromPath(USER_SERVER_ROOT_PATH + "/{userId}/cases/count").buildAndExpand(userId).toUriString();
        return restTemplate.getForObject(directoryServerBaseUri + path, Integer.class);
    }
}
