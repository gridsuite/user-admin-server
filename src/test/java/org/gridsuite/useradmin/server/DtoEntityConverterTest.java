package org.gridsuite.useradmin.server;

import org.assertj.core.api.WithAssertions;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.gridsuite.useradmin.server.service.DtoConverter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Test conversion functions {@link org.gridsuite.useradmin.server.repository entities} to/from
 * {@link org.gridsuite.useradmin.server.dto DTOs}.
 *
 * @implNote Use full constructors and not {@link lombok.Builder builders} to detect when new field added and avoid default values.
 */
class DtoEntityConverterTest implements WithAssertions {
    @Test
    void testUserInfosEntityToDto() {
        final UUID id = UUID.randomUUID();
        assertThat(DtoConverter.toDto(new UserInfosEntity(id, "testUser"), sub -> false)).as("User infos DTO")
                .isEqualTo(new UserInfos("testUser", false));
        assertThat(DtoConverter.toDto(new UserInfosEntity(id, "user2"), sub -> true)).as("User infos DTO")
                .isEqualTo(new UserInfos("user2", true));
    }

    @Test
    void testUserConnectionEntityToDto() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime last = LocalDateTime.now().truncatedTo(ChronoUnit.HALF_DAYS);
        final LocalDateTime first = last.minusHours(1L).minusMinutes(1L);
        assertThat(DtoConverter.toDto(new ConnectionEntity(id, "testUser", first, last, false)))
                .as("User Connection DTO")
                .isEqualTo(new UserConnection("testUser", first.toInstant(ZoneOffset.UTC), last.toInstant(ZoneOffset.UTC), false));
        assertThat(DtoConverter.toDto(new ConnectionEntity(id, "user2", last, first, true)))
                .as("User Connection DTO")
                .isEqualTo(new UserConnection("user2", last.toInstant(ZoneOffset.UTC), first.toInstant(ZoneOffset.UTC), true));
    }
}
