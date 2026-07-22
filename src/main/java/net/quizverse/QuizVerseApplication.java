package net.quizverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Default match store is in-memory ({@code QUIZ_MATCH_STORE=memory}).
 * For Redis-backed matches: {@code QUIZ_MATCH_STORE=redis} plus spring.data.redis.*.
 */
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@ConfigurationPropertiesScan
public class QuizVerseApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizVerseApplication.class, args);
    }
}
