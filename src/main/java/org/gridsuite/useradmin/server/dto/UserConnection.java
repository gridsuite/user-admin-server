package org.gridsuite.useradmin.server.dto;

import java.time.Instant;

public record UserConnection(String sub, Instant firstConnection, Instant lastConnection, boolean isAccepted) {
}
