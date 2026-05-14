package net.qihoo.guessthepattern.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用数据管理：密钥 + 允许 JDBC 访问的表白名单。
 * <p>默认密钥仅用于本地开发；生产环境务必通过 {@code QUIZ_ADMIN_API_KEY} 或 {@code quiz.admin.api-key} 覆盖，
 * 且勿将真实密钥提交到版本库。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "quiz.admin")
public class QuizAdminProperties {

    /**
     * 与请求头 {@code X-Admin-Key} 比对；未在配置中指定时使用此默认值。
     */
    private String apiKey = "QV-DEV-LOLDATA-9f3c1e8b";

    /**
     * 可在管理页/API 中操作的表名（仅小写、数字、下划线）。新表建好后在此追加即可，无需再写专用页面。
     */
    private List<String> tables = new ArrayList<>();
}
