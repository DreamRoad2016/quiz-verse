package net.quizverse.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.quizverse.config.QuizProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final QuizProperties properties;

    public AuthInterceptor(AuthService authService, QuizProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!properties.getSecurity().isEnabled()) {
            AuthContext.set(new AuthPrincipal("anon", "anon"));
            return true;
        }
        String auth = request.getHeader("Authorization");
        AuthPrincipal principal = authService.resolveBearer(auth);
        AuthContext.set(principal);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }
}
