package net.qihoo.guessthepattern.web;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.qihoo.guessthepattern.dto.PlayGameResult;
import net.qihoo.guessthepattern.result.ResultResponse;
import net.qihoo.guessthepattern.service.IStartGameService;
import net.qihoo.guessthepattern.service.IUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;

/**
 * 游戏游玩
 *
 * @author zhouqingji
 */
@RestController
@Api(tags = "游戏游玩")
@RequestMapping("/game")
public class GameController {

	@Resource
	IStartGameService startGameService;

	@Resource
	IUserService userService;

	@ApiOperation("接口测试")
	@GetMapping("/hello")
	public ResultResponse token() {
		String str = "489465";
		return ResultResponse.success(str);
	}

	@ApiOperation("开始游戏（无需登录）")
	@GetMapping("/start")
	public ResultResponse startGame() {
		String gameId = startGameService.startGame();
		return ResultResponse.success(gameId);
	}

	@ApiOperation("开始游戏（需登录，带token）")
	@GetMapping("/startWithUser")
	public ResultResponse startGameWithUser(@RequestParam(value = "token") String token) {
		String username = userService.validateToken(token);
		String gameId = startGameService.startGame(username);
		return ResultResponse.success(gameId);
	}

	@ApiOperation("猜猜猜（无需登录）")
	@GetMapping("/play")
	public ResultResponse<PlayGameResult> playGame(@NotBlank @RequestParam(value = "gameId") String gameId,
												   @NotBlank @RequestParam(value = "position") String position) throws Exception {
		PlayGameResult playGameResult = startGameService.playGame(gameId, position);
		return ResultResponse.success(playGameResult);
	}

	@ApiOperation("猜猜猜（需登录，带token，完成后记录战绩）")
	@GetMapping("/playWithUser")
	public ResultResponse<PlayGameResult> playGameWithUser(@NotBlank @RequestParam(value = "gameId") String gameId,
														   @NotBlank @RequestParam(value = "position") String position,
														   @RequestParam(value = "token") String token) throws Exception {
		String username = userService.validateToken(token);
		PlayGameResult playGameResult = startGameService.playGame(gameId, position, username);
		return ResultResponse.success(playGameResult);
	}
}
