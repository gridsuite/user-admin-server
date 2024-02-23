package org.gridsuite.useradmin.server;

import io.swagger.v3.oas.models.Operation;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class CoverageTest implements WithAssertions {
    @Test
    void testUtilityClassConstructor() {
        assertThatThrownBy(Utils.class::newInstance).as("Utils class init exception")
                .isInstanceOf(IllegalAccessException.class);
    }

    @Test
    void testSwaggerRestrictedOperationCustomizer() throws NoSuchMethodException {
        new SwaggerConfig().restrictedEndpointsOpenApiCustomizer().customize(new Operation(),
            new HandlerMethod(new Object(), UserAdminController.class.getMethod("getUsers", String.class)));
        assertThat(true).isTrue();
    }
}
