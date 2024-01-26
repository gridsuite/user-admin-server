/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

class ConnectionRepositoryTest extends AbstractJpaCrudTest<ConnectionRepository, ConnectionEntity> {
    final Random random = new Random();

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionEntity generateEntity() {
        return new ConnectionEntity(RandomStringUtils.randomAlphanumeric(5, 15), LocalDateTime.now().minusSeconds(random.nextLong(100L, 9999L)),
                LocalDateTime.now(), random.nextBoolean());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionEntity generateEntityWithId(UUID id) {
        return new ConnectionEntity(id, RandomStringUtils.randomAlphanumeric(5, 15), LocalDateTime.now().minusSeconds(random.nextLong(100L, 9999L)),
                LocalDateTime.now(), random.nextBoolean());
    }

    @Test
    void testFindBySub() {
        final String sub = RandomStringUtils.randomAlphanumeric(5, 10);
        assertThat(this.repository.findBySub(sub)).as("Found entities in empty database").isEmpty();
        final ConnectionEntity entity = generateEntity();
        testEntityManager.persist(entity);
        assertThat(this.repository.findBySub(entity.getSub())).as("Found entity in repository").containsExactlyInAnyOrder(entity);
    }
}
