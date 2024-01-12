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

public final class DtoConverter {
    private DtoConverter() {
        throw new IllegalCallerException("Utility class haven't instance.");
    }

    public static UserInfos toDto(@Nullable final UserInfosEntity entity) {
        return entity == null ? null : new UserInfos(entity.getSub());
    }

    public static UserConnection toDto(@Nullable final ConnectionEntity entity) {
        return entity == null ? null : new UserConnection(entity.getSub(), convert(entity.getFirstConnexionDate()),
                convert(entity.getLastConnexionDate()), Objects.requireNonNullElse(entity.getConnectionAccepted(), Boolean.FALSE));
    }

    public static Instant convert(@Nullable final LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
