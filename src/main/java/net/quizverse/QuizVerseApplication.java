package net.quizverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Redis auto-config is excluded by default so {@code quiz.match.store=memory} runs
 * without a local Redis. Enable Redis store via {@code QUIZ_MATCH_STORE=redis}
 * (see {@link net.quizverse.config.RedisMatchConfig}).
 */
@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@ConfigurationPropertiesScan
public class QuizVerseApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizVerseApplication.class, args);
    }
}
