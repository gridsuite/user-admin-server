package org.gridsuite.useradmin.server.service;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.WithAssertions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.gridsuite.useradmin.server.TestsTools.*;

class CommonServiceTest implements WithAssertions {
    private static final CommonService CS_ADMINS = new CommonService(new UserAdminApplicationProps(List.of(ADMIN_USER, ADMIN_USER2)));
    private static final CommonService CS_EMPTY = new CommonService(new UserAdminApplicationProps(List.of()));

    @ParameterizedTest
    @MethodSource
    void testFnIsAdmin(final CommonService service, final String sub, final boolean result) {
        assertThat(service.isAdmin(sub)).isEqualTo(result);
    }

    static List<Arguments> testFnIsAdmin() {
        return List.of(
            Arguments.of(CS_EMPTY, NOT_ADMIN, true),
            Arguments.of(CS_ADMINS, ADMIN_USER, true),
            Arguments.of(CS_ADMINS, NOT_ADMIN, false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFnAssertIsAdminAuthorized(final CommonService service, final String sub) {
        assertThatNoException().isThrownBy(() -> service.assertIsAdmin(sub));
    }

    static List<Arguments> testFnAssertIsAdminAuthorized() {
        return List.of(
                Arguments.of(CS_EMPTY, NOT_ADMIN),
                Arguments.of(CS_ADMINS, ADMIN_USER)
        );
    }

    @Test
    void testFnAssertIsAdminNotAuthorized() {
        assertThatThrownBy(() -> CS_ADMINS.assertIsAdmin(NOT_ADMIN))
                .asInstanceOf(InstanceOfAssertFactories.type(UserAdminException.class))
                .extracting(UserAdminException::getType)
                .isEqualTo(UserAdminException.Type.FORBIDDEN);
    }

    static class CommonService extends AbstractCommonService {
        protected CommonService(@NonNull UserAdminApplicationProps applicationProps) {
            super(applicationProps);
        }

        @Override
        public boolean isAdmin(@NonNull String sub) {
            return super.isAdmin(sub);
        }

        @Override
        public void assertIsAdmin(@NonNull String sub) throws UserAdminException {
            super.assertIsAdmin(sub);
        }

        @Override
        public String toString() {
            return "Srv{" + applicationProps + '}';
        }
    }
}
