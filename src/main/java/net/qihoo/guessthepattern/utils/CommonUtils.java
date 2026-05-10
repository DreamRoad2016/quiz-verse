package net.qihoo.guessthepattern.utils;

import lombok.extern.slf4j.Slf4j;
import net.qihoo.guessthepattern.model.BoardEntity;
import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.PatternModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CommonUtils {

	//动态生成所有组合
	public static void calculateCombination(Map<String, PatternModel> patternModelMap, BoardModel boardModel, List<List<String>> inputList, int beginIndex, List<String> res, String cur) {
		if (beginIndex == inputList.size()) {
			if (checkValid(patternModelMap, boardModel, cur.substring(0, cur.length() - 1))) {
				res.add(cur.substring(0, cur.length() - 1));
			}
			return;
		}
		for (String c : inputList.get(beginIndex)) {
			if (checkValid(patternModelMap, boardModel, cur + c)) {
				calculateCombination(patternModelMap, boardModel, inputList, beginIndex + 1, res, cur + c + "_");
			}
		}
	}

	private static boolean checkValid(Map<String, PatternModel> patternModelMap, BoardModel boardModel, String combination) {
		int[][] board = new int[boardModel.getX()][boardModel.getY()];
		String[] singletonPatterns = combination.split("_");
		for (String singletonPattern : singletonPatterns) {
			String position = singletonPattern.substring(0, singletonPattern.length() - 1);
			String direction = singletonPattern.substring(singletonPattern.length() - 1);
			String[] coordinate = position.split("-");
			completionBoard(coordinate, patternModelMap.get(direction), board);
		}
		for (int[] row : board) {
			for (int val : row) {
				if (val > 1) {
					log.info(combination + "不合法");
					return false;
				}
			}
		}
		return true;
	}

	public static BoardEntity createBoardEntity(BoardModel boardModel, PatternModel patternModel) {
		BoardEntity boardEntity = new BoardEntity();
		boardEntity.setPatten(patternModel);
		Map<String, PatternModel> patternModelMap = PatternUtils.getBasePatternModel(patternModel);
		String combination = TempUtils.getRandom();
		int[][] board = new int[boardModel.getX()][boardModel.getY()];
		String[] singletonPatterns = combination.split("_");
		List<String> standardAnswer = new ArrayList<>();
		for (String singletonPattern : singletonPatterns) {
			String position = singletonPattern.substring(0, singletonPattern.length() - 1);
			String direction = singletonPattern.substring(singletonPattern.length() - 1);
			String[] coordinate = position.split("-");
			int x = Integer.parseInt(coordinate[0]);
			int y = Integer.parseInt(coordinate[1]);
			PatternModel patternModelEntity = patternModelMap.get(direction);
			String answer = (x + patternModelEntity.getCoreX()) + "-" + (y + patternModelEntity.getCoreY());
			standardAnswer.add(answer);
			completionBoard(coordinate, patternModelEntity, board);
		}
		boardEntity.setBoard(board);
		boardEntity.setBoardModel(boardModel);
		boardEntity.setCombination(combination);
		boardEntity.setProcess(new ArrayList<>());
		boardEntity.setYourAnswer(new ArrayList<>());
		boardEntity.setStandardAnswer(standardAnswer);
		List<String> unfinishedAnswer = new ArrayList<>(standardAnswer);
		boardEntity.setUnfinishedAnswer(unfinishedAnswer);
		return boardEntity;
	}


	public static void completionBoard(String[] coordinate, PatternModel patternModel, int[][] board) {
		//x与y代表二维数组中的某一行某一列，并非严格的坐标系定义
		int x = Integer.parseInt(coordinate[0]);
		int y = Integer.parseInt(coordinate[1]);
		int[][] patternModelPattern = patternModel.getPattern();
		for (int i = 0; i < patternModelPattern.length; i++) {
			int[] row = patternModelPattern[i];
			for (int j = 0; j < row.length; j++) {
				board[x + i][y + j] = row[j] + board[x + i][y + j];
			}
		}
	}
}
