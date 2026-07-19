package net.quizverse.match;

import net.quizverse.compare.CellResult;
import net.quizverse.compare.CompareEngine;
import net.quizverse.pack.PackRegistry;
import net.quizverse.pack.model.EntityBrief;
import net.quizverse.pack.model.LoadedPack;
import net.quizverse.pack.model.PackEntity;
import net.quizverse.pack.model.PackSchema;
import net.quizverse.web.dto.GuessRequest;
import net.quizverse.web.dto.GuessResponse;
import net.quizverse.web.dto.StartMatchRequest;
import net.quizverse.web.dto.StartMatchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final PackRegistry packs;
    private final CompareEngine compareEngine;
    private final MatchStore matchStore;

    public MatchService(PackRegistry packs, CompareEngine compareEngine, MatchStore matchStore) {
        this.packs = packs;
        this.compareEngine = compareEngine;
        this.matchStore = matchStore;
    }

    public StartMatchResponse start(StartMatchRequest request) {
        LoadedPack pack = packs.require(request.getPackId());
        List<PackEntity> entities = pack.getEntities();
        PackEntity answer = entities.get(ThreadLocalRandom.current().nextInt(entities.size()));

        MatchSession session = new MatchSession();
        session.setMatchId(UUID.randomUUID().toString());
        session.setPackId(pack.getId());
        session.setAnswerId(answer.getId());
        session.setGuessCount(0);
        session.setMaxGuesses(pack.getMeta().getMaxGuesses());
        session.setState("PLAYING");
        matchStore.save(session);

        StartMatchResponse resp = new StartMatchResponse();
        resp.setMatchId(session.getMatchId());
        resp.setPackId(pack.getId());
        resp.setMaxGuesses(session.getMaxGuesses());
        resp.setRemaining(session.getMaxGuesses());
        resp.setColumns(toColumnMeta(packs.tableColumns(pack.getId())));
        return resp;
    }

    public GuessResponse guess(String matchId, GuessRequest request) {
        MatchSession session = matchStore.find(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
        if (!"PLAYING".equals(session.getState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match already finished: " + session.getState());
        }

        LoadedPack pack = packs.require(session.getPackId());
        PackEntity guess = pack.findEntity(request.getEntityId());
        if (guess == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown entityId");
        }
        if (session.getGuessedIds().contains(guess.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already guessed this entity");
        }

        PackEntity answer = pack.findEntity(session.getAnswerId());
        Map<String, CellResult> cells = compareEngine.compare(pack, guess, answer);
        Map<String, String> display = compareEngine.display(pack, guess);

        session.getGuessedIds().add(guess.getId());
        session.setGuessCount(session.getGuessCount() + 1);

        GuessResponse resp = new GuessResponse();
        resp.setMatchId(matchId);
        resp.setGuessIndex(session.getGuessCount());
        resp.setDisplay(display);
        resp.setCells(cells);

        boolean hit = guess.getId().equals(answer.getId());
        resp.setHit(hit);

        if (hit) {
            session.setState("WON");
            resp.setGameStatus("WON");
            resp.setRemaining(session.getMaxGuesses() - session.getGuessCount());
            resp.setAnswerReveal(toBrief(answer));
        } else if (session.getGuessCount() >= session.getMaxGuesses()) {
            session.setState("LOST");
            resp.setGameStatus("LOST");
            resp.setRemaining(0);
            resp.setAnswerReveal(toBrief(answer));
        } else {
            resp.setGameStatus("PLAYING");
            resp.setRemaining(session.getMaxGuesses() - session.getGuessCount());
        }

        matchStore.save(session);
        return resp;
    }

    private static List<StartMatchResponse.ColumnMeta> toColumnMeta(List<PackSchema.ColumnDef> cols) {
        return cols.stream().map(c -> {
            StartMatchResponse.ColumnMeta m = new StartMatchResponse.ColumnMeta();
            m.setKey(c.getKey());
            m.setLabel(c.getLabel());
            m.setType(c.getType());
            return m;
        }).collect(Collectors.toList());
    }

    private static EntityBrief toBrief(PackEntity e) {
        return new EntityBrief(e.getId(), e.getName(), e.getAliases());
    }
}
