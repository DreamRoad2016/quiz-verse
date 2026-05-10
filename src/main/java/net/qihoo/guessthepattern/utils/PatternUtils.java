package net.qihoo.guessthepattern.utils;

import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.PatternModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PatternUtils {

	public static Map<String, PatternModel> getBasePatternModel(PatternModel patternModel) {
		Map<String, PatternModel> result = new HashMap<>();
		result.put("N", patternModel);
		PatternModel patternModelE = getNewPattern(patternModel);
		result.put("E", patternModelE);
		PatternModel patternModelS = getNewPattern(patternModelE);
		result.put("S", patternModelS);
		PatternModel patternModelW = getNewPattern(patternModelS);
		result.put("W", patternModelW);
		return result;
	}

	private static PatternModel getNewPattern(PatternModel patternModel) {
		PatternModel patternModelNew = new PatternModel();
		int[][] pattern = patternModel.getPattern();
		int row = pattern.length;
		int col = pattern[0].length;
		int[][] patternNew = new int[col][row];
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				patternNew[j][row - i - 1] = pattern[i][j];
				if (i == patternModel.getCoreX() && j == patternModel.getCoreY()) {
					patternModelNew.setCoreX(j);
					patternModelNew.setCoreY(row - i - 1);
				}
			}
		}
		patternModelNew.setPattern(patternNew);
		return patternModelNew;
	}

	private static Boolean checkPatternValid(PatternModel patternModel) {
		int[][] pattern = patternModel.getPattern();
		if (Objects.isNull(patternModel.getCoreX()) || Objects.isNull(patternModel.getCoreY())) {
			return false;
		}
		if (Objects.isNull(pattern)) {
			return false;
		}
		int row = pattern.length;
		if (row == 0) {
			return false;
		}
		int col = pattern[0].length;
		if (col == 0) {
			return false;
		}
		if (patternModel.getCoreX() < 0 || patternModel.getCoreX() >= row || patternModel.getCoreY() < 0 || patternModel.getCoreY() >= col) {
			return false;
		}
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				int patternVal = pattern[i][j];
				if (Objects.isNull(patternVal)) {
					return false;
				}
				if (i == 0 || i == row - 1 | j == 0 | j == col - 1) {
					if (patternVal == 0) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public static int[][] transpose(int[][] arr) {
		int row = arr.length;
		int col = arr[0].length;
		int[][] newArr = new int[col][row];
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				newArr[j][row - i - 1] = arr[i][j];
			}
		}
		return newArr;
	}

	public static Boolean checkBoardPattern(PatternModel patternModel, BoardModel boardModel, int num) {
		if (checkPatternValid(patternModel)) {
			return false;
		}
		if (patternModel.getPattern().length > boardModel.getX() || patternModel.getPattern().length > boardModel.getY() ||
				patternModel.getPattern()[0].length > boardModel.getX() || patternModel.getPattern()[0].length > boardModel.getY()) {
			return false;
		}
		if (num > 5 || num < 1) {
			return false;
		}
		return true;
	}
}
