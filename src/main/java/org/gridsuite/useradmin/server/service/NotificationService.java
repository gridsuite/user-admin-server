/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com
 */
@Service
public class NotificationService {

    public static final String HEADER_MESSAGE_TYPE = "messageType";

    public static final String MESSAGE_TYPE_MAINTENANCE = "maintenance";

    public static final String MESSAGE_TYPE_CANCEL_MAINTENANCE = "cancelMaintenance";

    public static final String HEADER_DURATION = "duration";

    public static final String MESSAGE_LOG = "Sending message : {}";

    private static final String CATEGORY_BROKER_OUTPUT = UserAdminService.class.getName() + ".output-broker-messages";

    private static final Logger MESSAGE_OUTPUT_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    @Autowired
    private StreamBridge updatePublisher;

    private void sendMessage(Message<String> message) {
        MESSAGE_OUTPUT_LOGGER.debug(MESSAGE_LOG, message);
        updatePublisher.send("publishMessage-out-0", message);
    }

    public void emitMaintenanceMessage(String message, long duration) {
        sendMessage(MessageBuilder.withPayload(message)
                .setHeader(HEADER_MESSAGE_TYPE, MESSAGE_TYPE_MAINTENANCE)
                .setHeader(HEADER_DURATION, duration)
                .build());
    }

    public void emitMaintenanceMessage(String message) {
        sendMessage(MessageBuilder.withPayload(message)
                .setHeader(HEADER_MESSAGE_TYPE, MESSAGE_TYPE_MAINTENANCE)
                .build());
    }

    public void emitCancelMaintenanceMessage() {
        sendMessage(MessageBuilder.withPayload("")
                .setHeader(HEADER_MESSAGE_TYPE, MESSAGE_TYPE_CANCEL_MAINTENANCE)
                .build());
    }
}
