package net.quizverse.match;

import java.util.Optional;

public interface MatchStore {

    void save(MatchSession session);

    Optional<MatchSession> find(String matchId);
}
