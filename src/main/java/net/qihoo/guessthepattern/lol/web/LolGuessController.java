package net.qihoo.guessthepattern.lol.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.qihoo.guessthepattern.lol.dto.LolGuessStartResponse;
import net.qihoo.guessthepattern.lol.dto.LolGuessTurnResponse;
import net.qihoo.guessthepattern.lol.service.LolGuessGameService;
import net.qihoo.guessthepattern.result.ResultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 单人猜 LoL 选手（题库：demo_lol_player）。
 */
@RestController
@Api(tags = "猜选手 Demo（单人）")
@RequestMapping("/api/lol-guess")
public class LolGuessController {

    @Resource
    private LolGuessGameService lolGuessGameService;

    @ApiOperation("开始新一局：随机答案，返回 matchId 与联想列表")
    @GetMapping("/start")
    public ResultResponse<LolGuessStartResponse> start() {
        return ResultResponse.success(lolGuessGameService.startNewGame());
    }

    @ApiOperation("提交一次猜测：playerId 为题库中选手 UUID")
    @GetMapping("/guess")
    public ResultResponse<LolGuessTurnResponse> guess(
            @ApiParam(value = "对局 id，来自 /start", required = true) @RequestParam String matchId,
            @ApiParam(value = "猜测的选手 UUID", required = true) @RequestParam String playerId) {
        return ResultResponse.success(lolGuessGameService.guess(matchId, playerId));
    }
}
