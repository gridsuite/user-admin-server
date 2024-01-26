/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class UserAdminRepositoryTest extends AbstractJpaCrudTest<UserAdminRepository, UserInfosEntity> {
    /**
     * {@inheritDoc}
     */
    @Override
    protected UserInfosEntity generateEntity() {
        return new UserInfosEntity(RandomStringUtils.randomAlphanumeric(5, 15));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UserInfosEntity generateEntityWithId(UUID id) {
        return new UserInfosEntity(id, RandomStringUtils.randomAlphanumeric(5, 15));
    }

    @Test
    void testExistsBySub/*IgnoreCase*/() {
        assertThat(this.repository.existsBySub(RandomStringUtils.randomAlphanumeric(5, 10))).as("Exists entity in empty database").isFalse();
        final UserInfosEntity entity = testEntityManager.persist(generateEntity());
        assertThat(this.repository.existsBySub(entity.getSub())).as("Exists entity in repository").isTrue();
    }

    @Test
    void testFindBySub/*IgnoreCase*/() {
        assertThat(this.repository.findBySub(RandomStringUtils.randomAlphanumeric(5, 10))).as("Found entity in empty database").isNotPresent();
        final UserInfosEntity entity = testEntityManager.persist(generateEntity());
        assertThat(this.repository.findBySub(entity.getSub())).as("Found entity in repository").isPresent();
    }

    @Test
    void testDeleteBySub/*IgnoreCase*/() {
        final UUID id = UUID.randomUUID();
        final UserInfosEntity entity = testEntityManager.persist(generateEntity());
        assertThat(this.repository.deleteBySub(entity.getSub())).as("Number of deleted entities").isOne();
        assertThat(testEntityManager.find(entity.getClass(), id)).as("Entity deleted in database").isNull();
    }

    @Test
    void testFindAllBySubContainsAllIgnoreCase() {
        assertThat(this.repository.findAllBySubContainsAllIgnoreCase(RandomStringUtils.randomAlphanumeric(5, 10)))
                .as("Found entity in empty database").isEmpty();
        final UserInfosEntity entity1 = testEntityManager.persist(new UserInfosEntity("AdminUser123"));
        final UserInfosEntity entity2 = testEntityManager.persist(new UserInfosEntity("SuperAdminUser"));
        final UserInfosEntity entity3 = testEntityManager.persist(new UserInfosEntity("lambda-user"));
        final UserInfosEntity entity4 = testEntityManager.persist(new UserInfosEntity("normal-id"));
        assertThat(this.repository.findAllBySubContainsAllIgnoreCase("admin")).as("Found entities").containsExactly(entity1, entity2);
    }
}
