package net.quizverse.security;

import net.quizverse.config.QuizProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简易滑动窗口限流：Redis INCR+EXPIRE，或内存计数。
 */
@Component
public class RateLimitService {

    private final boolean useRedis;
    private final StringRedisTemplate redis;
    private final QuizProperties.Security security;
    private final Map<String, Window> memory = new ConcurrentHashMap<>();

    public RateLimitService(QuizProperties properties,
                            ObjectProvider<StringRedisTemplate> redisProvider) {
        this.security = properties.getSecurity();
        this.useRedis = "redis".equalsIgnoreCase(properties.getMatch().getStore());
        this.redis = useRedis ? redisProvider.getIfAvailable() : null;
    }

    public void checkGuestIp(String ip) {
        check("guest:ip:" + ip, security.getGuestPerIpPerHour());
    }

    public void checkStart(String ownerId) {
        check("start:" + ownerId, security.getStartPerSessionPerHour());
    }

    public void checkGuess(String ownerId) {
        check("guess:" + ownerId, security.getGuessPerSessionPerHour());
    }

    public void checkBriefs(String ownerId) {
        check("briefs:" + ownerId, security.getBriefsPerSessionPerHour());
    }

    private void check(String key, int limitPerHour) {
        if (limitPerHour <= 0) {
            return;
        }
        if (useRedis && redis != null) {
            String redisKey = "qv:rl:" + key;
            Long n = redis.opsForValue().increment(redisKey);
            if (n != null && n == 1L) {
                redis.expire(redisKey, Duration.ofHours(1));
            }
            if (n != null && n > limitPerHour) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
            }
            return;
        }
        long now = System.currentTimeMillis();
        Window w = memory.compute(key, (k, old) -> {
            if (old == null || now - old.windowStartMs > Duration.ofHours(1).toMillis()) {
                return new Window(now, new AtomicInteger(1));
            }
            old.count.incrementAndGet();
            return old;
        });
        if (w.count.get() > limitPerHour) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }

    private static final class Window {
        private final long windowStartMs;
        private final AtomicInteger count;

        private Window(long windowStartMs, AtomicInteger count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }
}
