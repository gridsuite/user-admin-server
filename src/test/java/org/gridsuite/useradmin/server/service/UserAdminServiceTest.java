/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import lombok.Getter;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminConfiguration;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.UserAdminRepository;
import org.gridsuite.useradmin.server.repository.UserInfosEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.gridsuite.useradmin.server.TestsTools.*;
import static org.gridsuite.useradmin.server.service.TestConfig.FIXED_CLOCK;

@ExtendWith({ MockitoExtension.class })
@SpringJUnitConfig(classes = {UserAdminConfiguration.class, TestConfig.class, UserAdminService.class},
                   initializers = ConfigDataApplicationContextInitializer.class)
@TestPropertySource(locations = "classpath:application-default.yml")
class UserAdminServiceTest implements ProtectedServiceTest {
    @Autowired
    UserAdminService userAdminService;

    @MockBean
    ConnectionsService connectionsService;

    @MockBean
    UserAdminRepository userAdminRepository;

    @Getter
    @SpyBean
    UserAdminApplicationProps userAdminApplicationProps;

    /*private ConnectionsService getService(final boolean noAdminProfile) {
        return new ConnectionsService(this.connectionRepository, noAdminProfile
        ? new UserAdminApplicationProps()
        : new UserAdminApplicationProps(List.of(ADMIN_USER)));
    }*/

    @AfterEach
    public void verifyMocks() {
        Mockito.verifyNoMoreInteractions(userAdminRepository);
        Mockito.verifyNoMoreInteractions(connectionsService);
        Mockito.verifyNoMoreInteractions(userAdminApplicationProps);
    }

    @ParameterizedTest
    @MethodSource
    void testIfSubExists(final String sub, final boolean result, final boolean existsSub) {
        if (existsSub) {
            Mockito.when(userAdminRepository.existsBySub(sub)).thenReturn(result);
        }
        assertThat(userAdminService.subExists(sub)).isEqualTo(result);
        if (existsSub) {
            Mockito.verify(userAdminRepository).existsBySub(sub);
        }
        Mockito.verify(userAdminApplicationProps, Mockito.only()).getAdmins();
        Mockito.verify(connectionsService).recordConnectionAttempt(sub, result);
    }

    public List<Arguments> testIfSubExists() {
        return List.of(
                Arguments.arguments(USER_SUB, true, true),
                Arguments.arguments(USER_NOT_REGISTERED, false, true),
                Arguments.arguments(ADMIN_USER, true, false)
        );
    }

    @Test
    @DirtiesContext
    void testIfSubExistsWithNoUsersAndAdmins() {
        userAdminApplicationProps.setAdmins(List.of());
        Mockito.reset(userAdminApplicationProps);
        Mockito.when(userAdminRepository.count()).thenReturn(0L);
        assertThat(userAdminService.subExists(USER_SUB)).isTrue();
        Mockito.verify(userAdminRepository).count();
        Mockito.verify(connectionsService).recordConnectionAttempt(USER_SUB, true);
        Mockito.verify(userAdminApplicationProps, Mockito.only()).getAdmins();
    }

    @Test
    void testRetrieveAllUsers() {
        Mockito.when(userAdminRepository.findAll())
               .thenReturn(IntStream.range(1, 11).mapToObj(i -> new UserInfosEntity("user_" + i)).toList());
        assertThat(userAdminService.getUsers(ADMIN_USER)).containsExactlyInAnyOrder(IntStream.range(1, 11)
                .mapToObj(i -> new UserInfos("user_" + i, false)).toArray(UserInfos[]::new));
        Mockito.verify(userAdminRepository).findAll();
        Mockito.verify(userAdminApplicationProps, Mockito.times(11)).getAdmins();
    }

    @Test
    void testRetrieveAllUsersWithAdmin() {
        Mockito.when(userAdminRepository.findAll()).thenReturn(List.of(
                new UserInfosEntity(USER_SUB),
                new UserInfosEntity(USER_SUB2),
                new UserInfosEntity(ADMIN_USER),
                new UserInfosEntity(ADMIN_USER2)
        ));
        assertThat(userAdminService.getUsers(ADMIN_USER)).containsExactlyInAnyOrder(
                new UserInfos(USER_SUB, false),
                new UserInfos(USER_SUB2, false),
                new UserInfos(ADMIN_USER, true),
                new UserInfos(ADMIN_USER2, true)
        );
        Mockito.verify(userAdminRepository).findAll();
        Mockito.verify(userAdminApplicationProps, Mockito.times(5)).getAdmins();
    }

    @Test
    void testCreateUser() {
        final ArgumentCaptor<UserInfosEntity> argumentCaptor = ArgumentCaptor.forClass(UserInfosEntity.class);
        Mockito.when(userAdminRepository.save(Mockito.any())).thenAnswer(new ReturnsArgumentAt(0));
        userAdminService.createUser(USER_SUB, ADMIN_USER);
        Mockito.verify(userAdminRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).as("entity").satisfies(
                //entity -> assertThat(entity.getId()).as("id").isNotNull(),
                entity -> assertThat(entity.getSub()).as("sub").isEqualTo(USER_SUB)
        );
        Mockito.verify(userAdminApplicationProps).getAdmins();
    }

    @Test
    void testDeleteUser() {
        Mockito.when(userAdminRepository.deleteBySub(USER_SUB)).thenReturn(1L);
        assertThat(userAdminService.delete(USER_SUB, ADMIN_USER)).isOne();
        Mockito.verify(userAdminRepository).deleteBySub(USER_SUB);
        Mockito.verify(userAdminApplicationProps).getAdmins();
    }

    @Test
    void testGetUserExisting() {
        Mockito.when(userAdminRepository.findBySub(USER_SUB)).thenReturn(Optional.of(new UserInfosEntity(USER_SUB)));
        assertThat(userAdminService.getUser(USER_SUB, ADMIN_USER)).get(InstanceOfAssertFactories.type(UserInfos.class))
                .isEqualTo(new UserInfos(USER_SUB, false));
        Mockito.verify(userAdminRepository).findBySub(USER_SUB);
        Mockito.verify(userAdminApplicationProps, Mockito.times(2)).getAdmins();
    }

    @Test
    void testGetUserNotExisting() {
        Mockito.when(userAdminRepository.findBySub(USER_SUB)).thenReturn(Optional.empty());
        assertThat(userAdminService.getUser(USER_SUB, ADMIN_USER)).isNotPresent();
        Mockito.verify(userAdminRepository).findBySub(USER_SUB);
        Mockito.verify(userAdminApplicationProps).getAdmins();
    }

    @Test
    void testGetUserAndIsAdmin() {
        Mockito.when(userAdminRepository.findBySub(ADMIN_USER2)).thenReturn(Optional.of(new UserInfosEntity(ADMIN_USER2)));
        assertThat(userAdminService.getUser(ADMIN_USER2, ADMIN_USER)).get(InstanceOfAssertFactories.type(UserInfos.class))
                .isEqualTo(new UserInfos(ADMIN_USER2, true));
        Mockito.verify(userAdminRepository).findBySub(ADMIN_USER2);
        Mockito.verify(userAdminApplicationProps, Mockito.times(2)).getAdmins();
    }

    @Test
    void testSearchUser() {
        Mockito.when(userAdminRepository.findAllBySubContainsAllIgnoreCase("term")).thenReturn(List.of(new UserInfosEntity(USER_SUB)));
        assertThat(userAdminService.searchUsers("term", ADMIN_USER)).containsExactly(new UserInfos(USER_SUB, false));
        Mockito.verify(userAdminRepository).findAllBySubContainsAllIgnoreCase("term");
        Mockito.verify(userAdminApplicationProps, Mockito.times(2)).getAdmins();
    }

    @Test
    void testSearchUsers() {
        Mockito.when(userAdminRepository.findAllBySubContainsAllIgnoreCase("term"))
               .thenReturn(List.of(new UserInfosEntity(USER_SUB), new UserInfosEntity(ADMIN_USER)));
        assertThat(userAdminService.searchUsers("term", ADMIN_USER))
                .containsExactly(new UserInfos(USER_SUB, false), new UserInfos(ADMIN_USER, true));
        Mockito.verify(userAdminRepository).findAllBySubContainsAllIgnoreCase("term");
        Mockito.verify(userAdminApplicationProps, Mockito.times(3)).getAdmins();
    }

    @Test
    void testGetConnections() {
        final LocalDateTime now = LocalDateTime.ofInstant(FIXED_CLOCK.instant(), ZoneOffset.UTC);
        Mockito.when(connectionsService.removeDuplicates()).thenReturn(List.of(
                new ConnectionEntity(USER_SUB, now.minusHours(1), now, true),
                new ConnectionEntity(USER_SUB2, now, now, true)
        ));
        assertThat(userAdminService.getConnections(ADMIN_USER)).containsExactlyInAnyOrder(
                new UserConnection(USER_SUB, FIXED_CLOCK.instant().minus(1, ChronoUnit.HOURS), FIXED_CLOCK.instant(), true),
                new UserConnection(USER_SUB2, FIXED_CLOCK.instant(), FIXED_CLOCK.instant(), true)
        );
        Mockito.verify(connectionsService).removeDuplicates();
        Mockito.verify(userAdminApplicationProps).getAdmins();
    }

    @Override
    public List<ThrowingConsumer<String>> testAdminRestrictedFunctions() {
        return List.of(
                userId -> userAdminService.searchUsers("term", userId),
                userId -> userAdminService.getUser("sub", userId),
                userId -> userAdminService.createUser("sub", userId),
                userId -> userAdminService.getConnections(userId),
                userAdminService::getUsers,
                userId -> userAdminService.delete("sub", userId)
        );
    }
}
