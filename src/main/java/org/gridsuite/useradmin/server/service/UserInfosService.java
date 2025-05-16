package org.gridsuite.useradmin.server.service;

import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.dto.UserInfos;
import org.gridsuite.useradmin.server.entity.UserInfosEntity;
import org.gridsuite.useradmin.server.entity.UserProfileEntity;
import org.gridsuite.useradmin.server.repository.UserInfosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserInfosService {

    private final UserInfosRepository userInfosRepository;
    private final DirectoryService directoryService;
    private final UserAdminApplicationProps applicationProps;

    public UserInfosService(final UserInfosRepository userInfosRepository,
                            final DirectoryService directoryService,
                            final UserAdminApplicationProps applicationProps) {
        this.userInfosRepository = Objects.requireNonNull(userInfosRepository);
        this.directoryService = Objects.requireNonNull(directoryService);
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
    public Optional<UserInfos> getUserInfo(String sub) {
        Optional<UserInfosEntity> userInfosEntity = getUserInfosEntity(sub);
        if (userInfosEntity.isPresent()) {
            // get number of cases used
            Integer casesUsed = directoryService.getCasesCount(userInfosEntity.get().getSub());

            return Optional.of(toDtoUserInfo(userInfosEntity.get(), casesUsed));
        }
        return Optional.empty();
    }

    private Optional<UserInfosEntity> getUserInfosEntity(String sub) {
        return userInfosRepository.findBySub(sub);
    }
}
