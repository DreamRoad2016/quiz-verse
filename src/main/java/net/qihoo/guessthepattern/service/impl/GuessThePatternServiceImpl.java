package net.qihoo.guessthepattern.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.ComputerPlayer;
import net.qihoo.guessthepattern.model.PatternModel;
import net.qihoo.guessthepattern.model.PlayProcess;
import net.qihoo.guessthepattern.service.IGuessThePatternService;
import net.qihoo.guessthepattern.utils.BoardUtils;
import net.qihoo.guessthepattern.utils.PatternUtils;
import net.qihoo.guessthepattern.utils.TempUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GuessThePatternServiceImpl implements IGuessThePatternService {

	@Override
	public List<String> createAllPattern(PatternModel patternModel) throws Exception {
		BoardModel boardModel = new BoardModel();
		boardModel.setX(10);
		boardModel.setY(10);
		return createAllPattern(patternModel, boardModel, 3);
	}

	@Override
	public List<String> createAllPattern(PatternModel patternModel, BoardModel boardModel, int num) throws Exception {
		Boolean res = PatternUtils.checkBoardPattern(patternModel, boardModel, num);
		if (res) {
			Map<String, PatternModel> patternModelMap = PatternUtils.getBasePatternModel(patternModel);
			List<String> allCombinations = BoardUtils.getAllCombinations(patternModel, boardModel, num);
			String allStr = String.join(",", allCombinations);
			log.info(allStr);
		} else {
			throw new Exception("参数不合法");
		}
		return null;
	}

	@Override
	public ComputerPlayer createComputerPlayer(PatternModel patternModel, BoardModel boardModel) {
		ComputerPlayer computerPlayer = new ComputerPlayer();
		List<String> list = TempUtils.getAll();
		computerPlayer.setBoardModel(boardModel);
		computerPlayer.setPatten(patternModel);
		int[][] probability = BoardUtils.getAllProbability(boardModel, patternModel, list);
		computerPlayer.setProbability(probability);
		computerPlayer.setProcess(new ArrayList<>());
		computerPlayer.setYourAnswer(new ArrayList<>());
		computerPlayer.setStandardAnswer(new ArrayList<>());
		computerPlayer.setUnfinishedAnswer(list);
		return computerPlayer;
	}

	@Override
	public String createComputerOptimalPosition(ComputerPlayer computerPlayer) {
		List<String> yourAnswer = computerPlayer.getYourAnswer();
		int optimal = (int) Math.ceil(computerPlayer.getUnfinishedAnswer().size() / 2.00);
		int gap = Integer.MAX_VALUE;
		List<String> resList = new ArrayList<>();
		int[][] probability = computerPlayer.getProbability();
		for (int i = 0; i < probability.length; i++) {
			for (int j = 0; j < probability[i].length; j++) {
				int cur = Math.abs(probability[i][j] - optimal);
				if (yourAnswer.contains(i + "-" + j)) {
					continue;
				}
				if (cur < gap) {
					gap = cur;
					resList = new ArrayList<>();
					resList.add(i + "-" + j);
				} else if (cur == gap) {
					resList.add(i + "-" + j);
				}
			}
		}
		int random = (int) Math.floor(Math.random() * resList.size());
		return resList.get(random);
	}

	@Override
	public ComputerPlayer createComputerPlayer2(PatternModel patternModel, BoardModel boardModel) {
		ComputerPlayer computerPlayer = new ComputerPlayer();
		List<String> list = TempUtils.getAll();
		computerPlayer.setBoardModel(boardModel);
		computerPlayer.setPatten(patternModel);
		int[][] probability = BoardUtils.getAllProbability2(boardModel, patternModel, list);
		computerPlayer.setProbability(probability);
		computerPlayer.setProcess(new ArrayList<>());
		computerPlayer.setYourAnswer(new ArrayList<>());
		computerPlayer.setStandardAnswer(new ArrayList<>());
		computerPlayer.setUnfinishedAnswer(list);
		return computerPlayer;
	}

	@Override
	public String createComputerOptimalPosition2(ComputerPlayer computerPlayer) {
		List<String> yourAnswer = computerPlayer.getYourAnswer();
		int optimal = (int) Math.ceil(computerPlayer.getUnfinishedAnswer().size() / 2.00);
		int gap = Integer.MIN_VALUE;
		List<String> resList = new ArrayList<>();
		int[][] probability = computerPlayer.getProbability();
		for (int i = 0; i < probability.length; i++) {
			for (int j = 0; j < probability[i].length; j++) {
				int cur = Math.abs(probability[i][j]);
				if (yourAnswer.contains(i + "-" + j)) {
					continue;
				}
				if (cur > gap) {
					gap = cur;
					resList = new ArrayList<>();
					resList.add(i + "-" + j);
				} else if (cur == gap) {
					resList.add(i + "-" + j);
				}
			}
		}
		int random = (int) Math.floor(Math.random() * resList.size());
		return resList.get(random);
	}


}
