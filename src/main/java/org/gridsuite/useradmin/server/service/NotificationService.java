/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.entity.AnnouncementSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com
 */
@Service
public class NotificationService {

    public static final String GLOBAL_CONFIG_BINDING = "publishMessage-out-0";

    public static final String HEADER_MESSAGE_TYPE = "messageType";

    public static final String MESSAGE_TYPE_ANNOUNCEMENT = "announcement";

    public static final String MESSAGE_TYPE_CANCEL_ANNOUNCEMENT = "cancelAnnouncement";

    public static final String HEADER_DURATION = "duration";

    public static final String HEADER_ANNOUNCEMENT_ID = "announcementId";

    public static final String HEADER_SEVERITY = "severity";

    public static final String MESSAGE_LOG = "Sending message : {}";

    private static final String CATEGORY_BROKER_OUTPUT = UserAdminService.class.getName() + ".output-broker-messages";

    private static final Logger MESSAGE_OUTPUT_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    @Autowired
    private StreamBridge updatePublisher;

    private void sendMessage(Message<String> message, String bindingName) {
        MESSAGE_OUTPUT_LOGGER.debug(MESSAGE_LOG, message);
        updatePublisher.send(bindingName, message);
    }

    public void emitAnnouncementMessage(UUID announcementId, String message, long durationInMs, AnnouncementSeverity severity) {
        sendMessage(MessageBuilder.withPayload(message)
            .setHeader(HEADER_MESSAGE_TYPE, MESSAGE_TYPE_ANNOUNCEMENT)
            .setHeader(HEADER_ANNOUNCEMENT_ID, announcementId)
            .setHeader(HEADER_DURATION, durationInMs)
            .setHeader(HEADER_SEVERITY, severity)
            .build(), GLOBAL_CONFIG_BINDING);
    }

    public void emitCancelAnnouncementMessage() {
        sendMessage(MessageBuilder.withPayload("")
            .setHeader(HEADER_MESSAGE_TYPE, MESSAGE_TYPE_CANCEL_ANNOUNCEMENT)
            .build(), GLOBAL_CONFIG_BINDING);
    }
}
