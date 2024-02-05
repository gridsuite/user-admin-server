package org.gridsuite.useradmin.server.dto;

public record UserInfos(
    String sub,
    boolean isAdmin
) { }
