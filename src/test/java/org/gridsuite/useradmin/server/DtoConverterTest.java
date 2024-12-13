package org.gridsuite.useradmin.server;

import org.assertj.core.api.WithAssertions;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.entity.ConnectionEntity;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.*;
import java.util.UUID;
import java.util.function.Predicate;

class DtoConverterTest implements WithAssertions {
    @Nested
    class ConverterOfUserInfos {
        @Test
        void testConversionToDtoOfUserInfos() {
            final UUID uuid = UUID.randomUUID();
            // no profile
            assertThat(UserInfosEntity.toDto(new UserInfosEntity(uuid, "sub_user", null), sub -> true))
                    .as("dto result")
                    .isEqualTo(new UserInfos("sub_user", true, null, null, null, null));
            // with profile
            UserProfileEntity profile = new UserProfileEntity(UUID.randomUUID(), "a profile", null, null, null, null, null, 5, 6);
            // Test mapping without quota
            assertThat(UserInfosEntity.toDto(new UserInfosEntity(uuid, "sub_user", profile), sub -> true))
                    .as("dto result")
                    .isEqualTo(new UserInfos("sub_user", true, "a profile", null, null, null));

            // Test mapping with quota
            assertThat(UserInfosEntity.toDtoWithDetail(new UserInfosEntity(uuid, "sub_user", profile), sub -> true, 5, 2, 6))
                    .as("dto result")
                    .isEqualTo(new UserInfos("sub_user", true, "a profile", 5, 2, 6));
        }

        @Test
        void testConversionToDtoOfUserInfosAdminPredicate() {
            final UUID uuid = UUID.randomUUID();
            Predicate<String> isAdminTest = Mockito.mock(Predicate.class);
            Mockito.when(isAdminTest.test(Mockito.anyString())).thenReturn(false);
            assertThat(UserInfosEntity.toDto(new UserInfosEntity(uuid, "admin_user", null), isAdminTest))
                .as("dto result")
                .isEqualTo(new UserInfos("admin_user", false, null, null, null, null));
            ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
            Mockito.verify(isAdminTest, Mockito.times(1)).test(argument.capture());
            assertThat(argument.getValue()).as("value predicate submitted").isEqualTo("admin_user");
        }

        @Test
        void testConversionToDtoOfUserInfosNull() {
            assertThat(UserInfosEntity.toDto(null, null)).isNull();
        }
    }

    @Nested
    class ConverterOfUserConnection {
        @Test
        void testConversionToDtoOfUserConnection() {
            final UUID uuid = UUID.randomUUID();
            final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
            assertThat(ConnectionEntity.toDto(new ConnectionEntity(uuid, "user1", LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC),
                                                               LocalDateTime.now(clock), false)))
                .as("dto result")
                .isEqualTo(new UserConnection("user1", Instant.EPOCH, Instant.now(clock), false));
        }

        @Test
        void testConversionToDtoOfUserConnectionNull() {
            assertThat(ConnectionEntity.toDto(null)).isNull();
        }
    }

    @Test
    void testInstantConversion() {
        assertThat(Utils.convert(LocalDateTime.of(2000, Month.JANUARY, 1, 1, 42, 42)))
                .as("instant result").isEqualTo(Instant.ofEpochSecond(946690962));
    }

    @Test
    void testInstantConversionNull() {
        assertThat(Utils.convert(null))
                .as("instant result").isNull();
    }
}
