/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Repository
public interface UserInfosRepository extends JpaRepository<UserInfosEntity, UUID> {
    boolean existsBySub(@NonNull String sub);

    Optional<UserInfosEntity> findBySub(@NonNull String sub);

    Long deleteBySub(@NonNull String sub);

    Long deleteAllBySubIn(@NonNull Collection<String> subs);
}
