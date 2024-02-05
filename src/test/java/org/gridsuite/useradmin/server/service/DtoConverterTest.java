package org.gridsuite.useradmin.server.service;

import org.assertj.core.api.WithAssertions;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class DtoConverterTest implements WithAssertions {
    @Nested
    class ConverterOfUserInfos {
        @Test
        void testConversionToDtoOfUserInfos() {
            final UUID uuid = UUID.randomUUID();
            assertThat(DtoConverter.toDto(new UserInfosEntity(uuid, "sub_user"), sub -> true))
                    .as("dto result")
                    .isEqualTo(new UserInfos("sub_user", true));
        }

        @Test
        void testConversionToDtoOfUserInfosAdminPredicate() {
            final UUID uuid = UUID.randomUUID();
            final AtomicReference<String> valuePredicateSubmitted = new AtomicReference<>(null);
            assertThat(DtoConverter.toDto(new UserInfosEntity(uuid, "admin_user"), sub -> {
                valuePredicateSubmitted.set(sub);
                return false;
            }))
                .as("dto result")
                .isEqualTo(new UserInfos("admin_user", false));
            assertThat(valuePredicateSubmitted).as("value predicate submitted").hasValue("admin_user");
        }

        @Test
        void testConversionToDtoOfUserInfosNull() {
            assertThat(DtoConverter.toDto((UserInfosEntity) null, null)).isNull();
        }
    }

    @Nested
    class ConverterOfUserConnection {
        @Test
        void testConversionToDtoOfUserConnection() {
            final UUID uuid = UUID.randomUUID();
            final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
            assertThat(DtoConverter.toDto(new ConnectionEntity(uuid, "user1", LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC),
                                                               LocalDateTime.now(clock), false)))
                .as("dto result")
                .isEqualTo(new UserConnection("user1", Instant.EPOCH, Instant.now(clock), false));
        }

        @Test
        void testConversionToDtoOfUserConnectionNull() {
            assertThat(DtoConverter.toDto((ConnectionEntity) null)).isNull();
        }
    }

    @Test
    void testInstantConversion() {
        assertThat(DtoConverter.convert(LocalDateTime.of(2000, Month.JANUARY, 1, 1, 42, 42)))
                .as("instant result").isEqualTo(Instant.ofEpochSecond(946690962));
    }

    @Test
    void testInstantConversionNull() {
        assertThat(DtoConverter.convert(null))
                .as("instant result").isNull();
    }
}
