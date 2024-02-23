package org.gridsuite.useradmin.server;

import java.lang.annotation.*;

/**
 * Swagger metadata documentation of an endpoint restricted to certain roles or user types.
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(ApiRestriction.ApiRestrictions.class)
public @interface ApiRestriction {
    /**
     * List of <i>role</i> having access to this restricted ressource
     */
    String[] value() default {};

    /* *
     * Replace the list of roles in swagger description
     */
    //String description() default ""

    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public static @interface ApiRestrictions {
        ApiRestriction[] value() default {};
    }
}
