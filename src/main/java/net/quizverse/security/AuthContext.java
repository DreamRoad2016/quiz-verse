package net.quizverse.security;

public final class AuthContext {

    private static final ThreadLocal<AuthPrincipal> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthPrincipal principal) {
        HOLDER.set(principal);
    }

    public static AuthPrincipal get() {
        return HOLDER.get();
    }

    public static String requireOwnerId() {
        AuthPrincipal p = HOLDER.get();
        if (p == null || p.getOwnerId() == null || p.getOwnerId().isBlank()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return p.getOwnerId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
