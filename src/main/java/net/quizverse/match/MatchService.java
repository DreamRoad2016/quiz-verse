package net.quizverse.match;

import net.quizverse.compare.CellResult;
import net.quizverse.compare.CompareEngine;
import net.quizverse.config.QuizProperties;
import net.quizverse.pack.PackRegistry;
import net.quizverse.pack.model.EntityBrief;
import net.quizverse.pack.model.LoadedPack;
import net.quizverse.pack.model.PackEntity;
import net.quizverse.pack.model.PackSchema;
import net.quizverse.security.AuthContext;
import net.quizverse.security.RateLimitService;
import net.quizverse.web.dto.EntityDetailResponse;
import net.quizverse.web.dto.GuessRequest;
import net.quizverse.web.dto.GuessResponse;
import net.quizverse.web.dto.StartMatchRequest;
import net.quizverse.web.dto.StartMatchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
    private final RateLimitService rateLimit;
    private final QuizProperties properties;

    public MatchService(PackRegistry packs,
                        CompareEngine compareEngine,
                        MatchStore matchStore,
                        RateLimitService rateLimit,
                        QuizProperties properties) {
        this.packs = packs;
        this.compareEngine = compareEngine;
        this.matchStore = matchStore;
        this.rateLimit = rateLimit;
        this.properties = properties;
    }

    public StartMatchResponse start(StartMatchRequest request) {
        String ownerId = currentOwnerId();
        if (properties.getSecurity().isEnabled()) {
            rateLimit.checkStart(ownerId);
        }

        LoadedPack pack = packs.require(request.getPackId());
        List<PackEntity> entities = pack.getEntities();
        PackEntity answer = entities.get(ThreadLocalRandom.current().nextInt(entities.size()));

        MatchSession session = new MatchSession();
        session.setMatchId(UUID.randomUUID().toString());
        session.setPackId(pack.getId());
        session.setAnswerId(answer.getId());
        session.setOwnerId(ownerId);
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
        MatchSession session = requireOwnedPlaying(matchId);
        String ownerId = currentOwnerId();
        if (properties.getSecurity().isEnabled()) {
            rateLimit.checkGuess(ownerId);
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
            attachAnswer(resp, pack, answer);
        } else if (session.getGuessCount() >= session.getMaxGuesses()) {
            session.setState("LOST");
            resp.setGameStatus("LOST");
            resp.setRemaining(0);
            attachAnswer(resp, pack, answer);
        } else {
            resp.setGameStatus("PLAYING");
            resp.setRemaining(session.getMaxGuesses() - session.getGuessCount());
        }

        matchStore.save(session);
        return resp;
    }

    /** 主动揭晓答案；本局记为 GIVEN_UP，不消耗猜次行。 */
    public GuessResponse giveUp(String matchId) {
        MatchSession session = requireOwnedPlaying(matchId);
        String ownerId = currentOwnerId();
        if (properties.getSecurity().isEnabled()) {
            rateLimit.checkGuess(ownerId);
        }

        LoadedPack pack = packs.require(session.getPackId());
        PackEntity answer = pack.findEntity(session.getAnswerId());
        if (answer == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Answer entity missing");
        }

        session.setState("GIVEN_UP");
        matchStore.save(session);

        GuessResponse resp = new GuessResponse();
        resp.setMatchId(matchId);
        resp.setGuessIndex(session.getGuessCount());
        resp.setHit(false);
        resp.setGameStatus("GIVEN_UP");
        resp.setRemaining(session.getMaxGuesses() - session.getGuessCount());
        attachAnswer(resp, pack, answer);
        return resp;
    }

    public EntityDetailResponse entityDetail(String packId, String entityId) {
        String ownerId = currentOwnerId();
        if (properties.getSecurity().isEnabled()) {
            rateLimit.checkBriefs(ownerId);
        }
        LoadedPack pack = packs.require(packId);
        PackEntity entity = pack.findEntity(entityId);
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
        }
        Map<String, String> display = compareEngine.display(pack, entity);
        EntityDetailResponse resp = new EntityDetailResponse();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setAliases(entity.getAliases() != null ? entity.getAliases() : List.of());
        resp.setSummary(entity.getSummary());
        resp.setPackId(pack.getId());
        resp.setDisplay(display);
        List<EntityDetailResponse.Field> fields = new ArrayList<>();
        for (PackSchema.ColumnDef col : packs.tableColumns(packId)) {
            if ("identity".equalsIgnoreCase(col.getType())) {
                continue;
            }
            String value = display.getOrDefault(col.getKey(), "—");
            fields.add(new EntityDetailResponse.Field(col.getKey(), col.getLabel(), value));
        }
        resp.setFields(fields);
        return resp;
    }

    public List<EntityBrief> briefs(String packId) {
        String ownerId = currentOwnerId();
        if (properties.getSecurity().isEnabled()) {
            rateLimit.checkBriefs(ownerId);
        }
        return packs.briefs(packId);
    }

    private MatchSession requireOwnedPlaying(String matchId) {
        MatchSession session = matchStore.find(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
        assertOwner(session);
        if (!"PLAYING".equals(session.getState())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match already finished: " + session.getState());
        }
        return session;
    }

    private void assertOwner(MatchSession session) {
        if (!properties.getSecurity().isEnabled()) {
            return;
        }
        String ownerId = currentOwnerId();
        if (session.getOwnerId() == null || !session.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Match not owned by current session");
        }
    }

    private static String currentOwnerId() {
        var p = AuthContext.get();
        if (p == null || p.getOwnerId() == null || p.getOwnerId().isBlank()) {
            return "anon";
        }
        return p.getOwnerId();
    }

    private void attachAnswer(GuessResponse resp, LoadedPack pack, PackEntity answer) {
        resp.setAnswerReveal(toBrief(answer));
        resp.setAnswerDisplay(compareEngine.display(pack, answer));
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
