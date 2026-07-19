package net.quizverse.match;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "quiz.match.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryMatchStore implements MatchStore {

    private final Map<String, MatchSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(MatchSession session) {
        sessions.put(session.getMatchId(), session);
    }

    @Override
    public Optional<MatchSession> find(String matchId) {
        return Optional.ofNullable(sessions.get(matchId));
    }
}
