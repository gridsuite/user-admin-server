/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gridsuite.useradmin.server.springdoc;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * The interface Pageable as query param.
 */
@Parameter(name = "pageable", hidden = true)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@PageableAsQueryParam.PageParameter
@PageableAsQueryParam.SizeParameter
@PageableAsQueryParam.SortParameter
public @interface PageableAsQueryParam {
    //TODO found how to not duplicate "default" values

    @AliasFor(annotation = PageParameter.class, attribute = "schema")
    Schema defaultPage() default @Schema(type = "integer", defaultValue = "0");

    @AliasFor(annotation = SizeParameter.class, attribute = "schema")
    Schema defaultSize() default @Schema(type = "integer", defaultValue = "20");

    @AliasFor(annotation = SortParameter.class, attribute = "array")
    ArraySchema defaultSort() default @ArraySchema(schema = @Schema(type = "string"));

    @Parameter
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface PageParameter {
        @AliasFor(annotation = Parameter.class, attribute = "in")
        ParameterIn in() default ParameterIn.QUERY;
        @AliasFor(annotation = Parameter.class, attribute = "name")
        String name() default "page";
        @AliasFor(annotation = Parameter.class, attribute = "description")
        String description() default "Zero-based page index (0..N)";
        @AliasFor(annotation = Parameter.class, attribute = "schema")
        Schema schema() default @Schema(type = "integer", defaultValue = "0");
    }

    @Parameter
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface SizeParameter {
        @AliasFor(annotation = Parameter.class, attribute = "in")
        ParameterIn in() default ParameterIn.QUERY;
        @AliasFor(annotation = Parameter.class, attribute = "name")
        String name() default "size";
        @AliasFor(annotation = Parameter.class, attribute = "description")
        String description() default "The size of the page to be returned";
        @AliasFor(annotation = Parameter.class, attribute = "schema")
        Schema schema() default @Schema(type = "integer", defaultValue = "20");
    }

    @Parameter
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface SortParameter {
        @AliasFor(annotation = Parameter.class, attribute = "in")
        ParameterIn in() default ParameterIn.QUERY;
        @AliasFor(annotation = Parameter.class, attribute = "name")
        String name() default "sort";
        @AliasFor(annotation = Parameter.class, attribute = "description")
        String description() default "Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.";
        @AliasFor(annotation = Parameter.class, attribute = "array")
        ArraySchema array() default @ArraySchema(schema = @Schema(type = "string"));
    }
}
