package org.gridsuite.useradmin.server;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class Utils {
    private Utils() {
        throw new IllegalCallerException("Utility class can not be initialize.");
    }

    public static Instant convert(@Nullable final LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
