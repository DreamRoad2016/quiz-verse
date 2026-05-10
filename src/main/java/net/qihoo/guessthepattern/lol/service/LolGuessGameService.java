package net.qihoo.guessthepattern.lol.service;

import com.alibaba.fastjson.JSON;
import net.qihoo.guessthepattern.exception.BizException;
import net.qihoo.guessthepattern.lol.domain.LolPlayerRow;
import net.qihoo.guessthepattern.lol.dto.LolBriefDTO;
import net.qihoo.guessthepattern.lol.dto.LolColumnMetaDTO;
import net.qihoo.guessthepattern.lol.dto.LolGuessStartResponse;
import net.qihoo.guessthepattern.lol.dto.LolGuessTurnResponse;
import net.qihoo.guessthepattern.lol.model.LolGuessSession;
import net.qihoo.guessthepattern.lol.repo.LolPlayerJdbcRepository;
import net.qihoo.guessthepattern.lol.repo.LolPlayerJdbcRepository.LolBriefProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LolGuessGameService {

    private static final String REDIS_KEY = "lol_match:";
    private static final int DEFAULT_MAX = 8;
    private static final int TTL_HOURS = 3;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private LolPlayerJdbcRepository lolPlayerJdbcRepository;
    @Autowired
    private LolComparisonService lolComparisonService;

    public static List<LolColumnMetaDTO> columnMetas() {
        List<LolColumnMetaDTO> list = new ArrayList<>();
        list.add(new LolColumnMetaDTO("gameName", "ID/姓名"));
        list.add(new LolColumnMetaDTO("age", "年龄"));
        list.add(new LolColumnMetaDTO("region", "赛区"));
        list.add(new LolColumnMetaDTO("team", "战队"));
        list.add(new LolColumnMetaDTO("histTeams", "历史战队"));
        list.add(new LolColumnMetaDTO("leagues", "联赛"));
        list.add(new LolColumnMetaDTO("positions", "位置"));
        list.add(new LolColumnMetaDTO("birthplace", "出生地"));
        list.add(new LolColumnMetaDTO("champions", "擅长英雄"));
        list.add(new LolColumnMetaDTO("status", "是否退役"));
        list.add(new LolColumnMetaDTO("worlds", "世界赛"));
        list.add(new LolColumnMetaDTO("champs", "冠军"));
        return list;
    }

    public LolGuessStartResponse startNewGame() {
        int pool = lolPlayerJdbcRepository.countAll();
        if (pool == 0) {
            throw new BizException("题库为空，请先导入 demo_lol_player 数据");
        }
        UUID answerId = lolPlayerJdbcRepository.pickRandomAnswerId();
        String matchId = UUID.randomUUID().toString();

        LolGuessSession session = new LolGuessSession();
        session.setAnswerId(answerId.toString());
        session.setGuessCount(0);
        session.setMaxGuesses(DEFAULT_MAX);
        session.setState("PLAYING");

        redisTemplate.opsForValue().set(REDIS_KEY + matchId, JSON.toJSONString(session), TTL_HOURS, TimeUnit.HOURS);

        List<LolBriefDTO> briefs = lolPlayerJdbcRepository.listAllBriefs().stream()
                .map(LolGuessGameService::toBrief)
                .collect(Collectors.toList());

        return new LolGuessStartResponse(matchId, DEFAULT_MAX, pool, columnMetas(), briefs);
    }

    private static LolBriefDTO toBrief(LolBriefProjection p) {
        return new LolBriefDTO(p.id.toString(), p.gameId, p.realName);
    }

    public LolGuessTurnResponse guess(String matchId, String playerIdStr) {
        UUID guessId;
        try {
            guessId = UUID.fromString(playerIdStr);
        } catch (IllegalArgumentException e) {
            throw new BizException("playerId 必须是合法 UUID");
        }

        String raw = redisTemplate.opsForValue().get(REDIS_KEY + matchId);
        if (raw == null || "null".equals(raw)) {
            throw new BizException("对局不存在或已过期，请重新开始");
        }
        LolGuessSession session = JSON.parseObject(raw, LolGuessSession.class);
        if (!"PLAYING".equals(session.getState())) {
            throw new BizException("本局已结束");
        }

        if (!lolPlayerJdbcRepository.existsById(guessId)) {
            throw new BizException("所选选手不在题库中");
        }

        LolPlayerRow guess = lolPlayerJdbcRepository.findById(guessId)
                .orElseThrow(() -> new BizException("选手数据不存在"));
        UUID answerUuid = UUID.fromString(session.getAnswerId());
        LolPlayerRow answer = lolPlayerJdbcRepository.findById(answerUuid)
                .orElseThrow(() -> new BizException("答案数据异常"));

        session.setGuessCount(session.getGuessCount() + 1);
        int guessIndex = session.getGuessCount();
        int max = session.getMaxGuesses();

        boolean hit = guess.getId().equals(answer.getId());
        LolGuessTurnResponse resp = new LolGuessTurnResponse();
        resp.setMatchId(matchId);
        resp.setGuessIndex(guessIndex);
        resp.setHit(hit);
        resp.setGuessDisplay(lolComparisonService.buildGuessDisplay(guess));
        resp.setCells(lolComparisonService.compare(guess, answer));

        if (hit) {
            session.setState("WON");
            resp.setGameStatus("WON");
            resp.setRemaining(max - guessIndex);
            resp.setAnswerReveal(toBrief(answer));
            redisTemplate.delete(REDIS_KEY + matchId);
            return resp;
        }

        if (guessIndex >= max) {
            session.setState("LOST");
            resp.setGameStatus("LOST");
            resp.setRemaining(0);
            resp.setAnswerReveal(toBrief(answer));
            redisTemplate.delete(REDIS_KEY + matchId);
            return resp;
        }

        session.setState("PLAYING");
        resp.setGameStatus("PLAYING");
        resp.setRemaining(max - guessIndex);
        redisTemplate.opsForValue().set(REDIS_KEY + matchId, JSON.toJSONString(session), TTL_HOURS, TimeUnit.HOURS);
        return resp;
    }

    private static LolBriefDTO toBrief(LolPlayerRow row) {
        return new LolBriefDTO(row.getId().toString(), row.getGameId(), row.getRealName());
    }
}
