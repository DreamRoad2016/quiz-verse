package net.qihoo.guessthepattern.service;

import net.qihoo.guessthepattern.dto.PlayGameResult;
import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.ComputerPlayer;
import net.qihoo.guessthepattern.model.PatternModel;

import java.util.List;

public interface IStartGameService {
	String startGame();

	/**
	 * 开始游戏（带用户信息）
	 */
	String startGame(String username);

	PlayGameResult playGame(String gameId, String position) throws Exception;

	/**
	 * 游戏操作（带用户信息，用于记录战绩）
	 */
	PlayGameResult playGame(String gameId, String position, String username) throws Exception;
}
