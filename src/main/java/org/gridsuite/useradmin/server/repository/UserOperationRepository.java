/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server.repository;

import org.gridsuite.useradmin.server.dto.QuotaType;
import org.gridsuite.useradmin.server.entity.UserOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @author Ghiles Abdellah {@literal <ghiles.abdellah at rte-france.com>}
 */
@Repository
public interface UserOperationRepository extends JpaRepository<UserOperationEntity, UUID> {

    List<UserOperationEntity> findBySub(@NonNull String sub);

    long deleteBySub(@NonNull String sub);

    /**
     * We could add a db lock (pessimistic/otpimistic lock on the user's rows for this operation type: serializes concurrent
     * consume attempts for the same (sub, operation) pair, closing the check-then-insert race window)
     * For now this is considered as a race condition fix since the operation are fast and the window is very small.
     * Moreover, the business impact is none...
     */
    List<UserOperationEntity> findBySubAndQuotaType(@NonNull String sub, @NonNull QuotaType quotaType);
}
