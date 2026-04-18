package com.ticks.user_service.config;

public final class CacheNames {

    private CacheNames() {};

    /** Caches {@link com.ticks.user_service.dto.response.UserResponseDTO} keyed by email */
    public static final String USER_BY_EMAIL = "users::by-email";

    /** Caches {@link com.ticks.user_service.dto.response.UserResponseDTO} keyed by UUID */
    public static final String USER_BY_ID = "users::by-id";

    /** Caches the Boolean result of {@code existsByEmail} */
    public static final String USER_EXISTS_BY_EMAIL = "users::exists-email";

    /** Caches the Boolean result of {@code existsByUsername} */
    public static final String USERS_EXISTS_BY_USERNAME = "users::exists-username";
}
