package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.dto.UserIdentity;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class UserInfosService {

    private final UserInfosRepository userInfosRepository;
    private final DirectoryService directoryService;
    private final UserIdentityService userIdentityService;
    private final UserAdminApplicationProps applicationProps;

    public UserInfosService(final UserInfosRepository userInfosRepository,
                            final DirectoryService directoryService,
                            final UserIdentityService userIdentityService,
                            final UserAdminApplicationProps applicationProps) {
        this.userInfosRepository = Objects.requireNonNull(userInfosRepository);
        this.directoryService = Objects.requireNonNull(directoryService);
        this.userIdentityService = Objects.requireNonNull(userIdentityService);
        this.applicationProps = Objects.requireNonNull(applicationProps);
    }

    public UserInfos toDtoUserInfo(final UserInfosEntity userInfosEntity, Integer casesUsed) {
        // get max allowed cases
        Integer maxAllowedCases = Optional.ofNullable(userInfosEntity.getProfile())
                .map(UserProfileEntity::getMaxAllowedCases)
                .orElse(applicationProps.getDefaultMaxAllowedCases());
        // get max allowed builds
        Integer maxAllowedBuilds = Optional.ofNullable(userInfosEntity.getProfile())
                .map(UserProfileEntity::getMaxAllowedBuilds)
                .orElse(applicationProps.getDefaultMaxAllowedBuilds());
        return UserInfosEntity.toDtoWithDetail(userInfosEntity, maxAllowedCases, casesUsed, maxAllowedBuilds);
    }

    @Transactional(readOnly = true)
    public UserInfos getUserInfo(String sub) {
        Optional<UserInfosEntity> userInfosEntity = getUserInfosEntity(sub);
        // get number of cases used
        Integer casesUsed = directoryService.getCasesCount(sub);

        UserInfos userInfos;
        if (userInfosEntity.isPresent()) {
            userInfos = toDtoUserInfo(userInfosEntity.get(), casesUsed);
        } else {
            userInfos = createDefaultUserInfo(sub, casesUsed);
        }

        // Enrich with identity information (firstName, lastName)
        return enrichWithIdentity(userInfos);
    }

    private UserInfos createDefaultUserInfo(String sub, Integer casesUsed) {
        return new UserInfos(
                sub,
                null,
                null,
                null,
                applicationProps.getDefaultMaxAllowedCases(),
                casesUsed,
                applicationProps.getDefaultMaxAllowedBuilds(),
                Set.of()
        );
    }

    private Optional<UserInfosEntity> getUserInfosEntity(String sub) {
        return userInfosRepository.findBySub(sub);
    }

    /**
     * Enriches user info with identity information (firstName, lastName).
     * Fails silently if identity cannot be fetched.
     */
    private UserInfos enrichWithIdentity(UserInfos userInfos) {
        if (userInfos == null) {
            return null;
        }
        Optional<UserIdentity> identity = userIdentityService.getIdentity(userInfos.sub());
        return identity.map(userInfos::withIdentity).orElse(userInfos);
    }
}
