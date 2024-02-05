package org.gridsuite.useradmin.server.service;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.WithAssertions;
import org.assertj.core.api.WithAssumptions;
import org.gridsuite.useradmin.server.UserAdminApplicationProps;
import org.gridsuite.useradmin.server.UserAdminException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.List;

import static org.gridsuite.useradmin.server.TestsTools.NOT_ADMIN;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public interface ProtectedServiceTest extends WithAssertions, WithAssumptions {
    UserAdminApplicationProps getUserAdminApplicationProps();

    List<ThrowingConsumer<String>> testAdminRestrictedFunctions();

    @DisplayName("Test methods restricted to admin (with non-admin user)")
    @SuppressWarnings("unused") //for IDEs who don't treat an interface containing tests
    @ParameterizedTest
    @MethodSource
    default void testAdminRestrictedFunctions(final ThrowingConsumer<String> fn) {
        //Mockito.reset(getUserAdminApplicationProps());
        assertThatThrownBy(() -> fn.accept(NOT_ADMIN))
                .asInstanceOf(InstanceOfAssertFactories.type(UserAdminException.class))
                .extracting(UserAdminException::getType)
                .isEqualTo(UserAdminException.Type.FORBIDDEN);
        Mockito.verify(getUserAdminApplicationProps()).getAdmins();
    }
}
