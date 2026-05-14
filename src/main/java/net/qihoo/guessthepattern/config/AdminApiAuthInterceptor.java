package net.qihoo.guessthepattern.config;

import com.alibaba.fastjson.JSON;
import net.qihoo.guessthepattern.enums.ResultEnum;
import net.qihoo.guessthepattern.result.ResultResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 管理类 API：校验 {@code X-Admin-Key} 与配置的密钥一致。
 */
@Component
public class AdminApiAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_ADMIN_KEY = "X-Admin-Key";

    @Resource
    private QuizAdminProperties quizAdminProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String expected = quizAdminProperties.getApiKey();
        if (!StringUtils.hasText(expected)) {
            writeUnauthorized(response, "管理接口未配置密钥");
            return false;
        }
        String provided = request.getHeader(HEADER_ADMIN_KEY);
        if (!StringUtils.hasText(provided)) {
            writeUnauthorized(response, "缺少请求头 " + HEADER_ADMIN_KEY);
            return false;
        }
        if (!constantTimeEquals(expected.trim(), provided.trim())) {
            writeUnauthorized(response, "密钥无效");
            return false;
        }
        return true;
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        return MessageDigest.isEqual(x, y);
    }

    private static void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ResultResponse<Void> body = ResultResponse.fail(ResultEnum.REQUEST_ERROR_AUTHENTICATION, msg);
        response.getWriter().write(JSON.toJSONString(body));
    }
}
