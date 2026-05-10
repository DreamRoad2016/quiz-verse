package net.qihoo.guessthepattern;

import net.qihoo.guessthepattern.model.*;
import net.qihoo.guessthepattern.service.IGuessThePatternService;
import net.qihoo.guessthepattern.service.impl.GuessThePatternServiceImpl;
import net.qihoo.guessthepattern.utils.BoardUtils;
import net.qihoo.guessthepattern.utils.CommonUtils;
import net.qihoo.guessthepattern.utils.PatternUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.regex.Pattern;

@SpringBootTest
class ComputerPlayerTests {

	@Test
	void contextLoads() {

	}

	public static void main(String[] args) throws Exception {
		IGuessThePatternService guessThePatternService = new GuessThePatternServiceImpl();
		PatternModel patternModel = new PatternModel();
		int[][] arr = {{0, 0, 1, 0, 0}, {1, 1, 1, 1, 1}, {0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}};
		patternModel.setPattern(arr);
		patternModel.setCoreX(0);
		patternModel.setCoreY(2);
		BoardModel boardModel = new BoardModel();
		boardModel.setX(10);
		boardModel.setY(10);
		ComputerPlayer computerPlayer = guessThePatternService.createComputerPlayer(patternModel, boardModel);
		String computerOptimalPosition = guessThePatternService.createComputerOptimalPosition(computerPlayer);

		System.out.print("猜测的点位是【" + computerOptimalPosition + "】：");
		Scanner input = new Scanner(System.in);
		String val = null;       // 记录输入度的字符串
		int step = 0;
		do {
			step++;
			String pattern = "[0,1,2]";
			val = input.next();       // 等待输入值
			System.out.println("您输入的是：" + val);
			if (val.equals("#")) {
				print(computerPlayer.getProbability());
			} else if (val.equals("*")) {
				print(computerPlayer.getProcess());
			} else if (Pattern.matches(pattern, val)) {
				computerOptimalPosition = dealAnswer(computerPlayer, computerOptimalPosition, val);
				if (computerPlayer.getUnfinishedAnswer().size() == 1) {
					System.out.println("哈哈，我花了"+step+"步猜出来了，你的谜底一定是：");
					print(computerPlayer, computerPlayer.getUnfinishedAnswer().get(0));
				}else{
					System.out.println("下一个猜测的点位是【" + computerOptimalPosition + "】：");
				}
			} else {
				System.out.println("格式错误，请重新输入__");
			}
		} while (computerPlayer.getUnfinishedAnswer().size() > 1);
		input.close(); // 关闭资源
		System.out.println("");

	}

	private static String dealAnswer(ComputerPlayer computerPlayer, String position, String val) {
		IGuessThePatternService guessThePatternService = new GuessThePatternServiceImpl();
		List<String> unfinishedAnswer = computerPlayer.getUnfinishedAnswer();
		Map<String, PatternModel> patternModelMap = PatternUtils.getBasePatternModel(computerPlayer.getPatten());
		String[] realCoordinate = position.split("-");
		int x = Integer.parseInt(realCoordinate[0]);
		int y = Integer.parseInt(realCoordinate[1]);
		int realVal = Integer.parseInt(val);
		if (realVal == 2) {
			Iterator<String> iterator = unfinishedAnswer.iterator();
			while (iterator.hasNext()) {
				String next = iterator.next();
				List<String> core = new ArrayList<>();
				String[] singletonPatterns = next.split("_");
				for (String singletonPattern : singletonPatterns) {
					String corePosition = singletonPattern.substring(0, singletonPattern.length() - 1);
					String direction = singletonPattern.substring(singletonPattern.length() - 1);
					String[] coordinate = corePosition.split("-");
					int corex = Integer.parseInt(coordinate[0]);
					int corey = Integer.parseInt(coordinate[1]);
					PatternModel patternModelEntity = patternModelMap.get(direction);
					String answer = (corex + patternModelEntity.getCoreX()) + "-" + (corey + patternModelEntity.getCoreY());
					core.add(answer);
				}
				if (!core.contains(position)) {
					iterator.remove();
				}
			}
		} else {
			Iterator<String> iterator = unfinishedAnswer.iterator();
			while (iterator.hasNext()) {
				String next = iterator.next();
				int[][] board = new int[computerPlayer.getBoardModel().getX()][computerPlayer.getBoardModel().getY()];
				String[] singletonPatterns = next.split("_");
				for (String singletonPattern : singletonPatterns) {
					String corePosition = singletonPattern.substring(0, singletonPattern.length() - 1);
					String direction = singletonPattern.substring(singletonPattern.length() - 1);
					String[] coordinate = corePosition.split("-");
					CommonUtils.completionBoard(coordinate, patternModelMap.get(direction), board);
				}
				if (!(board[x][y] == realVal)) {
					iterator.remove();
				}
			}
		}
		PlayProcess process = new PlayProcess();
		process.setStep(computerPlayer.getProcess().size() + 1);
		process.setRes(realVal);
		process.setX(x);
		process.setY(y);
		computerPlayer.getProcess().add(process);
		int[][] probability = BoardUtils.getAllProbability(computerPlayer.getBoardModel(), computerPlayer.getPatten(), unfinishedAnswer);
		computerPlayer.setUnfinishedAnswer(unfinishedAnswer);
		computerPlayer.setProbability(probability);
		String computerOptimalPosition = guessThePatternService.createComputerOptimalPosition(computerPlayer);
		return computerOptimalPosition;
	}


	public static void print(ComputerPlayer computerPlayer, String combination) {
		Map<String, PatternModel> patternModelMap = PatternUtils.getBasePatternModel(computerPlayer.getPatten());
		int[][] board = new int[computerPlayer.getBoardModel().getX()][computerPlayer.getBoardModel().getY()];
		String[] singletonPatterns = combination.split("_");
		for (String singletonPattern : singletonPatterns) {
			String corePosition = singletonPattern.substring(0, singletonPattern.length() - 1);
			String direction = singletonPattern.substring(singletonPattern.length() - 1);
			String[] coordinate = corePosition.split("-");
			CommonUtils.completionBoard(coordinate, patternModelMap.get(direction), board);
		}
		for (int[] ints : board) {
			for (int anInt : ints) {
				System.out.print(anInt + " ");
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(int[][] arr) {
		for (int[] ints : arr) {
			for (int anInt : ints) {
				System.out.print(anInt + " ");
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(int x, int y, List<PlayProcess> processList) {
		String[][] yourAnswer = new String[x][y];
		for (int i = 0; i < yourAnswer.length; i++) {
			for (int j = 0; j < yourAnswer[i].length; j++) {
				yourAnswer[i][j] = "-";
			}
		}
		for (PlayProcess process : processList) {
			yourAnswer[process.getX()][process.getY()] = process.getRes() + "";
		}
		for (int i = 0; i < yourAnswer.length; i++) {
			for (int j = 0; j < yourAnswer[i].length; j++) {
				System.out.print(yourAnswer[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println();
	}

	public static void print(List<PlayProcess> processList) {
		for (PlayProcess process : processList) {
			System.out.println("第" + process.getStep() + "步： 【" + process.getX() + "，" + process.getY() + "】，结果为：" + process.getRes());
		}

	}

//	public static void main(String[] args) throws Exception {
//		IGuessThePatternService guessThePatternService = new GuessThePatternServiceImpl();
//		PatternModel patternModel = new PatternModel();
//		int[][] arr = {{0, 0, 1, 0, 0}, {1, 1, 1, 1, 1}, {0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}};
//		patternModel.setPattern(arr);
//		patternModel.setCoreX(0);
//		patternModel.setCoreY(2);
//		BoardModel boardModel = new BoardModel();
//		boardModel.setX(10);
//		boardModel.setY(10);
//		guessThePatternService.createAllPattern(patternModel, boardModel, 3);
//
//	}
//	public static void main(String[] args) {
//		int[][] arr = {{0, 0, 1, 0, 0}, {1, 1, 1, 1, 1}, {0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}};
//		PatternUtils.print(arr);
//		int[][] transpose = PatternUtils.transpose(arr);
//		PatternUtils.print(transpose);
//		int[][] transpose1 = PatternUtils.transpose(transpose);
//		PatternUtils.print(transpose1);
//		int[][] transpose2 = PatternUtils.transpose(transpose1);
//		PatternUtils.print(transpose2);
//
//	}

}
