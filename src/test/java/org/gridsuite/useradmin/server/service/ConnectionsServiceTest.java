/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.service;

import lombok.Getter;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminConfiguration;
import org.gridsuite.useradmin.server.dto.UserConnection;
import org.gridsuite.useradmin.server.repository.ConnectionEntity;
import org.gridsuite.useradmin.server.repository.ConnectionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.gridsuite.useradmin.server.TestsTools.*;
import static org.gridsuite.useradmin.server.service.TestConfig.FIXED_CLOCK;

@ExtendWith({ MockitoExtension.class })
@SpringJUnitConfig(classes = {UserAdminConfiguration.class, TestConfig.class, ConnectionsService.class},
                   initializers = ConfigDataApplicationContextInitializer.class)
@TestPropertySource(locations = "classpath:application-default.yml")
class ConnectionsServiceTest implements ProtectedServiceTest {
    @Autowired
    ConnectionsService connectionsService;

    @MockBean
    ConnectionRepository connectionRepository;

    @Getter
    @SpyBean
    UserAdminApplicationProps userAdminApplicationProps;

    @AfterEach
    public void verifyMocks() {
        Mockito.verifyNoMoreInteractions(connectionRepository);
        //Mockito.verifyNoMoreInteractions(userAdminApplicationProps);
    }

    @Test
    void testGetConnectionsWhenIsAdmin() {
        final LocalDateTime time1 = LocalDateTime.of(2000, 1, 1, 10, 0);
        final LocalDateTime time2 = LocalDateTime.of(2000, 2, 3, 10, 0);
        final List<ConnectionEntity> list = List.of(
            new ConnectionEntity(USER_SUB, time1, time2, true),
            new ConnectionEntity(USER_SUB2, time1, time2, false)
        );
        Mockito.when(connectionRepository.findAll()).thenReturn(list);
        assertThat(connectionsService.getConnections(ADMIN_USER)).containsExactlyInAnyOrder(
            new UserConnection(USER_SUB, time1.toInstant(ZoneOffset.UTC), time2.toInstant(ZoneOffset.UTC), true),
            new UserConnection(USER_SUB2, time1.toInstant(ZoneOffset.UTC), time2.toInstant(ZoneOffset.UTC), false)
        );
        Mockito.verify(connectionRepository).findAll();
        Mockito.verify(userAdminApplicationProps).getAdmins();
    }

    @Disabled("test not implemented yet")
    @Test
    void testGetConnectionsDeduplicated() {
        //TODO
    }

    @Test
    void testRecordConnectionAttemptExistingUser() {
        ConnectionEntity entity = new ConnectionEntity();
        Mockito.when(connectionRepository.findBySub(USER_SUB)).thenReturn(List.of(entity));
        connectionsService.recordConnectionAttempt(USER_SUB, true);
        assertThat(entity)
                .hasFieldOrPropertyWithValue("lastConnexionDate", LocalDateTime.now(FIXED_CLOCK))
                .hasFieldOrPropertyWithValue("connectionAccepted", true);
        Mockito.verify(connectionRepository).findBySub(USER_SUB);
        Mockito.verify(connectionRepository).save(entity);
    }

    @Test
    void testRecordConnectionAttemptNotExistingUser() {
        final ArgumentCaptor<ConnectionEntity> entityArgumentCaptor = ArgumentCaptor.forClass(ConnectionEntity.class);
        Mockito.when(connectionRepository.findBySub(USER_SUB)).thenReturn(List.of());
        connectionsService.recordConnectionAttempt(USER_SUB, true);
        Mockito.verify(connectionRepository).findBySub(USER_SUB);
        Mockito.verify(connectionRepository).save(entityArgumentCaptor.capture());
        assertThat(entityArgumentCaptor.getValue())
                .hasFieldOrPropertyWithValue("lastConnexionDate", LocalDateTime.now(FIXED_CLOCK))
                .hasFieldOrPropertyWithValue("connectionAccepted", true);
    }

    @Override
    public List<ThrowingConsumer<String>> testAdminRestrictedFunctions() {
        return List.of(
            connectionsService::getConnections //testGetConnectionsWhenUserIsNotAdmin
        );
    }
}
