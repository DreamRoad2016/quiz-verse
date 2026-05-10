package net.qihoo.guessthepattern.utils;


import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.PatternModel;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class BoardUtils {

	public static List<String> getAllCombinations(PatternModel patternModel, BoardModel boardModel, int num) {
		//获取所有单个模型
		List<String> singleton = getSingletonList(patternModel.getPattern().length, patternModel.getPattern()[0].length, boardModel.getX(), boardModel.getY());
		List<List<String>> sourceData = new ArrayList<>();
		int round = num;
		while (round > 0) {
			round--;
			sourceData.add(singleton);
		}
		List<String> combinationRes = new ArrayList<>();
		//获取四个方向得二维分布图
		Map<String, PatternModel> patternModelMap = PatternUtils.getBasePatternModel(patternModel);
		CommonUtils.calculateCombination(patternModelMap, boardModel, sourceData, 0, combinationRes, "");
		Set<Set<String>> validCombinations = new HashSet<>();
		for (String combinationRe : combinationRes) {
			String[] patternArr = combinationRe.split("_");
			Set<String> patternSet = new HashSet<>(Arrays.asList(patternArr));
			if (patternSet.size() != num || validCombinations.contains(patternSet)) {
				continue;
			}
			validCombinations.add(patternSet);
		}
		List<String> result = new ArrayList<>();
		for (Set<String> validCombination : validCombinations) {
			String validRes = StringUtils.join(validCombination.toArray(), "_");
			result.add(validRes);
		}
		return result;
	}

	private static List<String> getSingletonList(int patternX, int patternY, int boardX, int boardY) {
		List<String> singleton = new ArrayList<>();
		//南北向
		int nsXMax = boardX - patternX;
		int nsYMax = boardY - patternY;
		//东西向
		int ewXMax = boardX - patternY;
		int ewYMax = boardY - patternX;
		for (int i = 0; i <= nsXMax; i++) {
			for (int j = 0; j <= nsYMax; j++) {

				singleton.add(i + "-" + j + "N");
				singleton.add(i + "-" + j + "S");
			}
		}
		for (int i = 0; i <= ewXMax; i++) {
			for (int j = 0; j <= ewYMax; j++) {

				singleton.add(i + "-" + j + "E");
				singleton.add(i + "-" + j + "W");
			}
		}
		return singleton;
	}

	public static int[][] getAllProbability(BoardModel boardModel, PatternModel patternModel, List<String> list) {
		int[][] res = new int[boardModel.getX()][boardModel.getY()];
		Map<String, PatternModel> patternModelMap = PatternUtils.getBasePatternModel(patternModel);
		for (String combination : list) {
			String[] singletonPatterns = combination.split("_");
			for (String singletonPattern : singletonPatterns) {
				String position = singletonPattern.substring(0, singletonPattern.length() - 1);
				String direction = singletonPattern.substring(singletonPattern.length() - 1);
				String[] coordinate = position.split("-");
				CommonUtils.completionBoard(coordinate, patternModelMap.get(direction), res);
			}
		}
		return res;
	}

	public static int[][] getAllProbability2(BoardModel boardModel, PatternModel patternModel, List<String> list) {
		int[][] res = new int[boardModel.getX()][boardModel.getY()];
		Map<String, PatternModel> patternModelMap = PatternUtils.getBasePatternModel(patternModel);
		for (String combination : list) {
			String[] singletonPatterns = combination.split("_");
			for (String singletonPattern : singletonPatterns) {
				String position = singletonPattern.substring(0, singletonPattern.length() - 1);
				String direction = singletonPattern.substring(singletonPattern.length() - 1);
				PatternModel patternModel1 = patternModelMap.get(direction);
				String[] coordinate = position.split("-");
				int x = Integer.parseInt(coordinate[0]);
				int y = Integer.parseInt(coordinate[1]);
				res[x + patternModel1.getCoreX()][y + patternModel1.getCoreY()] += 1;
			}
		}
		return res;
	}
}
