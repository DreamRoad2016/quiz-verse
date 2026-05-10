package net.qihoo.guessthepattern.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.qihoo.guessthepattern.dto.GameRecord;
import net.qihoo.guessthepattern.dto.PlayGameResult;
import net.qihoo.guessthepattern.exception.BizException;
import net.qihoo.guessthepattern.model.BoardEntity;
import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.PatternModel;
import net.qihoo.guessthepattern.model.PlayProcess;
import net.qihoo.guessthepattern.service.IStartGameService;
import net.qihoo.guessthepattern.service.IUserService;
import net.qihoo.guessthepattern.utils.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class StartGameServiceImpl implements IStartGameService {

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Resource
	private IUserService userService;

	@Override
	public String startGame() {
		return startGame(null);
	}

	@Override
	public String startGame(String username) {
		PatternModel patternModel = new PatternModel();
		int[][] arr = {{0, 0, 1, 0, 0}, {1, 1, 1, 1, 1}, {0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}};
		patternModel.setPattern(arr);
		patternModel.setCoreX(0);
		patternModel.setCoreY(2);
		BoardModel boardModel = new BoardModel();
		boardModel.setX(10);
		boardModel.setY(10);
		BoardEntity boardEntity = CommonUtils.createBoardEntity(boardModel, patternModel);
		
		// 设置用户信息和开始时间
		boardEntity.setUsername(username);
		boardEntity.setStartTime(System.currentTimeMillis());
		
		long time = System.currentTimeMillis();
		String gameId = "game_guess:".concat(time+"");

		redisTemplate.opsForValue().set(gameId, JSONObject.toJSONString(boardEntity), 3, TimeUnit.HOURS);
		return gameId;
	}

	@Override
	public PlayGameResult playGame(String gameId, String position) throws Exception {
		return playGame(gameId, position, null);
	}

	@Override
	public PlayGameResult playGame(String gameId, String position, String username) throws Exception {
		String value = String.valueOf(redisTemplate.opsForValue().get(gameId));
		BoardEntity boardEntity = new BoardEntity();
		if (value.equals("null")) {
			throw new BizException("游戏已过期，请重新开始");
		} else {
			boardEntity = JSON.parseObject(value, BoardEntity.class);
		}
		PlayGameResult playGameResult = new PlayGameResult();
		String pattern = "[0-9]*-[0-9]*";
		String val = position;
		if (val.equals("*")) {
			playGameResult.setType(2);
			playGameResult.setProcess(boardEntity.getProcess());
		} else if (val.equals("res")) {
			playGameResult.setType(3);
			playGameResult.setAnswer(boardEntity.getBoard());
		} else if (Pattern.matches(pattern, val)) {
			playGameResult.setType(1);
			int res = verifyAnswer(boardEntity, val);
			playGameResult.setResult(res);
			if (res == 2){
				if (boardEntity.getUnfinishedAnswer().size() == 0) {
					playGameResult.setType(0);
					playGameResult.setProcess(boardEntity.getProcess());
					playGameResult.setAnswer(boardEntity.getBoard());
				}
			}


		} else {
			throw new Exception("位置格式错误");
		}
		
		// 游戏结束时记录战绩
		if (boardEntity.getUnfinishedAnswer().size() == 0) {
			redisTemplate.delete(gameId);
			// 如果有用户信息，记录战绩并尝试上榜
			String recordUsername = StringUtils.hasText(username) ? username : boardEntity.getUsername();
			if (StringUtils.hasText(recordUsername)) {
				int rank = saveGameRecordAndUpdateLeaderboard(gameId, boardEntity, recordUsername);
				playGameResult.setRank(rank);
			}
		} else {
			redisTemplate.opsForValue().set(gameId, JSONObject.toJSONString(boardEntity), 3, TimeUnit.HOURS);
		}
		return playGameResult;
	}

	/**
	 * 保存游戏战绩并尝试更新排行榜
	 * @return 排行榜排名（1-20），未上榜返回 -1
	 */
	private int saveGameRecordAndUpdateLeaderboard(String gameId, BoardEntity boardEntity, String username) {
		int steps = boardEntity.getYourAnswer().size();
		long duration = 0;
		
		// 计算耗时
		if (boardEntity.getStartTime() != null) {
			duration = (System.currentTimeMillis() - boardEntity.getStartTime()) / 1000;
		}
		
		// 保存战绩记录
		GameRecord record = new GameRecord();
		record.setGameId(gameId);
		record.setSteps(steps);
		record.setWin(true);
		record.setTargetCount(boardEntity.getStandardAnswer().size());
		record.setDuration(duration);
		record.setGameTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		userService.saveGameRecord(username, record);
		
		// 尝试更新排行榜
		return userService.tryUpdateLeaderboard(username, steps, duration);
	}

	private static int verifyAnswer(BoardEntity boardEntity, String yourAnswer) {
		int res = 0;
		if (boardEntity.getYourAnswer().contains(yourAnswer)) {
			return -1;
		}
		boardEntity.getYourAnswer().add(yourAnswer);
		PlayProcess process = new PlayProcess();
		String[] answer = yourAnswer.split("-");
		int x = Integer.parseInt(answer[0]);
		int y = Integer.parseInt(answer[1]);
		if (x < 0 || x > boardEntity.getBoardModel().getX() || y < 0 || y > boardEntity.getBoardModel().getY()) {
			return -100;
		}
		process.setX(x);
		process.setY(y);
		process.setStep(boardEntity.getYourAnswer().size());
		if (boardEntity.getUnfinishedAnswer().contains(yourAnswer)) {
			boardEntity.getUnfinishedAnswer().remove(yourAnswer);
			res = 2;
		} else {
			res = boardEntity.getBoard()[x][y];
		}
		process.setRes(res);
		boardEntity.getProcess().add(process);
		return res;

	}
}
