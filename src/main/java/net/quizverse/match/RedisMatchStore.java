package net.quizverse.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.quizverse.config.QuizProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "quiz.match.store", havingValue = "redis")
public class RedisMatchStore implements MatchStore {

    private static final String KEY_PREFIX = "qv:match:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public RedisMatchStore(StringRedisTemplate redis, ObjectMapper mapper, QuizProperties properties) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttl = Duration.ofHours(properties.getMatch().getTtlHours());
    }

    @Override
    public void save(MatchSession session) {
        try {
            redis.opsForValue().set(KEY_PREFIX + session.getMatchId(),
                    mapper.writeValueAsString(session), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize match session", e);
        }
    }

    @Override
    public Optional<MatchSession> find(String matchId) {
        String json = redis.opsForValue().get(KEY_PREFIX + matchId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(json, MatchSession.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize match session", e);
        }
    }
}
