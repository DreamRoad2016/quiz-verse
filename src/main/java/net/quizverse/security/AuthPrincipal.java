package net.quizverse.security;

/**
 * 登录会话主体：微信 openid 或 guest:{ipHash}。
 */
public class AuthPrincipal {

    private final String ownerId;
    private final String kind;

    public AuthPrincipal(String ownerId, String kind) {
        this.ownerId = ownerId;
        this.kind = kind;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getKind() {
        return kind;
    }
}
