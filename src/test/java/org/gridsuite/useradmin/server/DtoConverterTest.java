package org.gridsuite.useradmin.server;

import org.assertj.core.api.WithAssertions;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.dto.UserProfile;
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
                    .isEqualTo(new UserInfos("sub_user", true, null));
            // with profile
            UserProfileEntity profile = new UserProfileEntity("a profile");
            assertThat(UserInfosEntity.toDto(new UserInfosEntity(uuid, "sub_user", profile), sub -> true))
                    .as("dto result")
                    .isEqualTo(new UserInfos("sub_user", true, "a profile"));
        }

        @Test
        void testConversionToDtoOfUserInfosAdminPredicate() {
            final UUID uuid = UUID.randomUUID();
            Predicate<String> isAdminTest = Mockito.mock(Predicate.class);
            Mockito.when(isAdminTest.test(Mockito.anyString())).thenReturn(false);
            assertThat(UserInfosEntity.toDto(new UserInfosEntity(uuid, "admin_user", null), isAdminTest))
                .as("dto result")
                .isEqualTo(new UserInfos("admin_user", false, null));
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
    class ConverterOfUserProfile {
        @Test
        void testConversionToDtoOfUserProfile() {
            final UUID uuid = UUID.randomUUID();
            final UUID parameterUuid = UUID.randomUUID();
            // no parameters
            assertThat(UserProfileEntity.toDto(new UserProfileEntity(uuid, "profil", null), null))
                    .as("dto result")
                    .isEqualTo(new UserProfile(uuid, "profil", null, null));
            assertThat(UserProfileEntity.toDto(new UserProfileEntity(uuid, "profil", null)))
                    .as("dto result")
                    .isEqualTo(new UserProfile(uuid, "profil", null, null));
            // with parameters, no validity check
            assertThat(UserProfileEntity.toDto(new UserProfileEntity(uuid, "profil", parameterUuid)))
                    .as("dto result")
                    .isEqualTo(new UserProfile(uuid, "profil", parameterUuid, null));
            // with parameters and validity ok
            assertThat(UserProfileEntity.toDto(new UserProfileEntity(uuid, "profil", parameterUuid), Boolean.TRUE))
                    .as("dto result")
                    .isEqualTo(new UserProfile(uuid, "profil", parameterUuid, Boolean.TRUE));
            // with parameters and validity ko
            assertThat(UserProfileEntity.toDto(new UserProfileEntity(uuid, "profil", parameterUuid), Boolean.FALSE))
                    .as("dto result")
                    .isEqualTo(new UserProfile(uuid, "profil", parameterUuid, Boolean.FALSE));
        }

        @Test
        void testConversionToDtoOfUserProfileNull() {
            assertThat(UserProfileEntity.toDto(null, null)).isNull();
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
