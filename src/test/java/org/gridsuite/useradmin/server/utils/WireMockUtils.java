/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.admin.model.ServeEventQuery;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WireMockUtils {

    private final WireMockServer wireMockServer;

    public WireMockUtils(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void verifyGetRequest(UUID stubId, String urlPath, Map<String, StringValuePattern> queryParams, boolean regexMatching, int count) {
        RequestPatternBuilder requestBuilder = regexMatching ? WireMock.getRequestedFor(WireMock.urlPathMatching(urlPath)) : WireMock.getRequestedFor(WireMock.urlPathEqualTo(urlPath));
        queryParams.forEach(requestBuilder::withQueryParam);
        wireMockServer.verify(count, requestBuilder);
        if (count > 0) {
            removeRequestForStub(stubId, 1);
        }
    }

    private void removeRequestForStub(UUID stubId, int nbRequests) {
        List<ServeEvent> serveEvents = wireMockServer.getServeEvents(ServeEventQuery.forStubMapping(stubId)).getServeEvents();
        assertEquals(nbRequests, serveEvents.size());
        for (ServeEvent serveEvent : serveEvents) {
            wireMockServer.removeServeEvent(serveEvent.getId());
        }
    }
}
