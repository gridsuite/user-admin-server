/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.useradmin.server;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Admin API")
                        .description("This is the documentation of the user admin REST API")
                        .version(UserAdminApi.API_VERSION))
                .specVersion(SpecVersion.V31);
    }

    /**
     * Recognize and transform {@link ApiRestriction} to swagger documentation.
     */
    @Bean
    public OperationCustomizer restrictedEndpointsOpenApiCustomizer() {
        return (operation, handlerMethod) -> {
            // get annotation(s) from controller (class) and/or operation (method)
            final ApiRestriction[] restrictions = ArrayUtils.addAll(
                handlerMethod.getBeanType().getDeclaredAnnotationsByType(ApiRestriction.class),
                handlerMethod.getMethod().getDeclaredAnnotationsByType(ApiRestriction.class)
            );
            final String[] roles = Arrays.stream(restrictions)
                .map(ApiRestriction::value)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(String[]::new);
            // disable for not overcharge swagger-ui
            //for final String str : roles) operation addTagsItem("role_" + str)
            if (roles.length > 0) {
                operation.addExtension("x-restricted-to", roles);
                operation.setDescription((StringUtils.isBlank(operation.getDescription()) ? "" : operation.getDescription() + "\n\n")
                    + "Access restricted to users of type: "
                    + Arrays.stream(roles).map(str -> StringUtils.wrapIfMissing(str, '`')).collect(Collectors.joining(", ")));
            }
            return operation;
        };
    }
}
