package net.quizverse.security;

import net.quizverse.config.QuizProperties;
import net.quizverse.web.dto.AuthTokenResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthSessionStore sessions;
    private final WechatCode2SessionClient wechat;
    private final RateLimitService rateLimit;
    private final QuizProperties properties;

    public AuthService(AuthSessionStore sessions,
                       WechatCode2SessionClient wechat,
                       RateLimitService rateLimit,
                       QuizProperties properties) {
        this.sessions = sessions;
        this.wechat = wechat;
        this.rateLimit = rateLimit;
        this.properties = properties;
    }

    public AuthTokenResponse loginWithWxCode(String code) {
        if (code == null || code.isBlank()) {
            throw new UnauthorizedException("code required");
        }
        String openid = wechat.exchangeCodeForOpenId(code.trim());
        return issueToken(openid, "wx");
    }

    public AuthTokenResponse loginGuest(String clientIp) {
        String ip = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp.trim();
        rateLimit.checkGuestIp(ip);
        // 每个 guest 会话独立 owner，避免同 IP 串改他人对局；限流仍按 IP
        String ownerId = "guest:" + UUID.randomUUID().toString().replace("-", "");
        return issueToken(ownerId, "guest");
    }

    public AuthPrincipal resolveBearer(String authorizationHeader) {
        String token = extractBearer(authorizationHeader);
        AuthSessionRecord record = sessions.find(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));
        return new AuthPrincipal(record.getOwnerId(), record.getKind());
    }

    private AuthTokenResponse issueToken(String ownerId, String kind) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        AuthSessionRecord record = new AuthSessionRecord(token, ownerId, kind, System.currentTimeMillis());
        sessions.save(record);
        AuthTokenResponse resp = new AuthTokenResponse();
        resp.setSessionToken(token);
        resp.setExpiresIn(sessions.ttl().toSeconds());
        resp.setKind(kind);
        return resp;
    }

    public static String extractBearer(String header) {
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Missing Authorization");
        }
        String h = header.trim();
        if (h.length() > 7 && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = h.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        throw new UnauthorizedException("Invalid Authorization");
    }

    public static String shortHash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig).substring(0, 16);
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    public boolean securityEnabled() {
        return properties.getSecurity().isEnabled();
    }
}
