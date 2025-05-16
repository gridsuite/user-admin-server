package org.gridsuite.useradmin.server;

import org.assertj.core.api.WithAssertions;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.entity.ConnectionEntity;
import org.gridsuite.useradmin.server.entity.GroupInfosEntity;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Set;
import java.util.UUID;

class DtoConverterTest implements WithAssertions {
    @Nested
    class ConverterOfUserInfos {
        @Test
        void testConversionToDtoOfUserInfos() {
            final UUID uuid = UUID.randomUUID();

            // no profile and no group
            assertThat(UserInfosEntity.toDto(new UserInfosEntity(uuid, "sub_user", null, null)))
                    .as("dto result")
                    .isEqualTo(new UserInfos("sub_user", null, null, null, null, null));

            // with profile but without group
            UserProfileEntity profile = new UserProfileEntity(UUID.randomUUID(), "a profile", null, null, null, null, null, 5, 6, null, null);
            //     Test mapping without quota
            assertThat(UserInfosEntity.toDto(new UserInfosEntity(uuid, "sub_user", profile, null)))
                    .as("dto result")
                    .isEqualTo(new UserInfos("sub_user", "a profile", null, null, null, null));
            //     Test mapping with quota
            assertThat(UserInfosEntity.toDtoWithDetail(new UserInfosEntity(uuid, "sub_user", profile, null), 5, 2, 6))
                    .as("dto result")
                    .isEqualTo(new UserInfos("sub_user", "a profile", 5, 2, 6, null));

            // with profile and groups
            GroupInfosEntity group1 = new GroupInfosEntity(UUID.randomUUID(), "group1", Set.of());
            GroupInfosEntity group2 = new GroupInfosEntity(UUID.randomUUID(), "group2", Set.of());
            //     Test mapping without quota
            assertThat(UserInfosEntity.toDto(new UserInfosEntity(uuid, "sub_user", profile, Set.of(group1, group2))))
                .as("dto result")
                .isEqualTo(new UserInfos("sub_user", "a profile", null, null, null, Set.of("group1", "group2")));
            //     Test mapping with quota
            assertThat(UserInfosEntity.toDtoWithDetail(new UserInfosEntity(uuid, "sub_user", profile, Set.of(group1, group2)), 5, 2, 6))
                .as("dto result")
                .isEqualTo(new UserInfos("sub_user", "a profile", 5, 2, 6, Set.of("group1", "group2")));
        }

        @Test
        void testConversionToDtoOfUserInfosNull() {
            assertThat(UserInfosEntity.toDto(null)).isNull();
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
