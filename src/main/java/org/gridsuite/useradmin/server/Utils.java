package org.gridsuite.useradmin.server;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Set;

public final class Utils {
    public static final String ROLES_HEADER = "roles";
    public static final String ROLE_DELIMITER = "\\|";

    private Utils() {
        throw new IllegalCallerException("Utility class can not be initialize.");
    }

    public static Instant convert(@Nullable final LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * Gets the current user's roles from the request headers.
     *
     * @return A set of the user's roles
     */
    public static Set<String> getCurrentUserRoles() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Collections.emptySet();
        }

        HttpServletRequest request = attributes.getRequest();
        String rolesHeader = request.getHeader(ROLES_HEADER);

        if (rolesHeader == null || rolesHeader.trim().isEmpty()) {
            return Collections.emptySet();
        }

        return Set.of(rolesHeader.split(ROLE_DELIMITER));
    }
}
