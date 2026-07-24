package net.quizverse.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.quizverse.config.QuizProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class WechatCode2SessionClient {

    private static final Logger log = LoggerFactory.getLogger(WechatCode2SessionClient.class);

    private final QuizProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public WechatCode2SessionClient(QuizProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    public String exchangeCodeForOpenId(String code) {
        String appId = properties.getWechat().getAppId();
        String secret = properties.getWechat().getAppSecret();
        if (appId == null || appId.isBlank() || secret == null || secret.isBlank()) {
            throw new UnauthorizedException("WeChat app credentials not configured");
        }
        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session"
                    + "?appid=" + enc(appId)
                    + "&secret=" + enc(secret)
                    + "&js_code=" + enc(code)
                    + "&grant_type=authorization_code";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(resp.body());
            if (node.hasNonNull("errcode") && node.get("errcode").asInt() != 0) {
                log.warn("WeChat code2session failed errcode={}", node.get("errcode").asInt());
                throw new UnauthorizedException("WeChat login failed");
            }
            String openid = node.path("openid").asText(null);
            if (openid == null || openid.isBlank()) {
                throw new UnauthorizedException("WeChat login failed");
            }
            return openid;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("WeChat code2session error: {}", e.toString());
            throw new UnauthorizedException("WeChat login failed");
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
