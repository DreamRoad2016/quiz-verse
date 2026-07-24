package net.quizverse.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import net.quizverse.security.AuthService;
import net.quizverse.web.dto.AuthTokenResponse;
import net.quizverse.web.dto.WxLoginRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/wx-login")
    public AuthTokenResponse wxLogin(@RequestBody @Valid WxLoginRequest request) {
        return authService.loginWithWxCode(request.getCode());
    }

    @PostMapping("/guest")
    public AuthTokenResponse guest(HttpServletRequest request) {
        return authService.loginGuest(clientIp(request));
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }
}
