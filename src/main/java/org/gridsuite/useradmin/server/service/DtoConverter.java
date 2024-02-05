/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import jakarta.annotation.Nullable;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.function.Predicate;

public final class DtoConverter {
    private DtoConverter() {
        throw new IllegalCallerException("Utility class haven't instance.");
    }

    public static UserInfos toDto(@Nullable final UserInfosEntity entity, Predicate<String> isAdminFn) {
        return entity == null ? null : new UserInfos(entity.getSub(), isAdminFn.test(entity.getSub()));
    }

    public static UserConnection toDto(@Nullable final ConnectionEntity entity) {
        return entity == null ? null : new UserConnection(entity.getSub(), convert(entity.getFirstConnexionDate()),
                convert(entity.getLastConnexionDate()), Objects.requireNonNullElse(entity.getConnectionAccepted(), Boolean.FALSE));
    }

    public static Instant convert(@Nullable final LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
