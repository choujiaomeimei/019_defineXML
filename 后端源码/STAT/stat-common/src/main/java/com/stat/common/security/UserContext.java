package com.stat.common.security;

/**
 * Thread-local holder for the authenticated user's identity.
 * Populated by JwtAuthenticationFilter on each request.
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();

    public static void set(UserInfo userInfo) {
        CURRENT_USER.set(userInfo);
    }

    public static UserInfo get() {
        return CURRENT_USER.get();
    }

    public static String getUsername() {
        UserInfo info = CURRENT_USER.get();
        return info != null ? info.getUsername() : null;
    }

    public static Long getUserId() {
        UserInfo info = CURRENT_USER.get();
        return info != null ? info.getUserId() : null;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }

    public static class UserInfo {
        private final Long userId;
        private final String username;

        public UserInfo(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }
}
