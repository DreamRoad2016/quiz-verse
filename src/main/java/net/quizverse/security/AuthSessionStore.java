package net.quizverse.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.quizverse.config.QuizProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthSessionStore {

    private static final String KEY_PREFIX = "qv:sess:";

    private final ObjectMapper mapper;
    private final Duration ttl;
    private final boolean useRedis;
    private final StringRedisTemplate redis;
    private final Map<String, TimedRecord> memory = new ConcurrentHashMap<>();

    public AuthSessionStore(ObjectMapper mapper,
                            QuizProperties properties,
                            ObjectProvider<StringRedisTemplate> redisProvider) {
        this.mapper = mapper;
        this.ttl = Duration.ofHours(properties.getSecurity().getSessionTtlHours());
        this.useRedis = "redis".equalsIgnoreCase(properties.getMatch().getStore());
        this.redis = useRedis ? redisProvider.getIfAvailable() : null;
        if (useRedis && this.redis == null) {
            throw new IllegalStateException("quiz.match.store=redis but StringRedisTemplate missing");
        }
    }

    public void save(AuthSessionRecord record) {
        if (useRedis) {
            try {
                redis.opsForValue().set(KEY_PREFIX + record.getToken(),
                        mapper.writeValueAsString(record), ttl);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize auth session", e);
            }
            return;
        }
        memory.put(record.getToken(), new TimedRecord(record, System.currentTimeMillis() + ttl.toMillis()));
    }

    public Optional<AuthSessionRecord> find(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        if (useRedis) {
            String json = redis.opsForValue().get(KEY_PREFIX + token);
            if (json == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(mapper.readValue(json, AuthSessionRecord.class));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to deserialize auth session", e);
            }
        }
        TimedRecord tr = memory.get(token);
        if (tr == null) {
            return Optional.empty();
        }
        if (tr.expireAtMs < System.currentTimeMillis()) {
            memory.remove(token);
            return Optional.empty();
        }
        return Optional.of(tr.record);
    }

    public Duration ttl() {
        return ttl;
    }

    private record TimedRecord(AuthSessionRecord record, long expireAtMs) {
    }
}
