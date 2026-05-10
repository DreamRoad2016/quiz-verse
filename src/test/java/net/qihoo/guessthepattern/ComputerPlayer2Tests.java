package net.qihoo.guessthepattern;

import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.ComputerPlayer;
import net.qihoo.guessthepattern.model.PatternModel;
import net.qihoo.guessthepattern.model.PlayProcess;
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
class ComputerPlayer2Tests {

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

		System.out.print("请输入结果：0代表未击中，1代表击中机身，2代表击落飞机\r\n猜测的点位是【" + computerOptimalPosition + "】：");
		Scanner input = new Scanner(System.in);
		boolean checkRes = false;
		String val = null;       // 记录输入度的字符串
		do {
			String pattern = "[0,1,2]";
			val = input.next();       // 等待输入值
			System.out.println("您输入的是：" + val);
			if (val.equals("#")) {
				print(computerPlayer.getProbability());
			} else if (val.equals("*")) {
				print(computerPlayer.getProcess());
			} else if (val.equals("res")) {
				System.out.println("未排除答案数量：" + computerPlayer.getUnfinishedAnswer().size());
			} else if (val.equals("resd")) {
				for (String answer : computerPlayer.getUnfinishedAnswer()) {
					System.out.print(answer + ",");
				}
			} else if (Pattern.matches(pattern, val)) {
				dealAnswer(computerPlayer, computerOptimalPosition, val);
				if (computerPlayer.getUnfinishedAnswer().size() > 16 ) {
					computerOptimalPosition = getComputerOptimalPosition1(computerPlayer);
				} else if(computerPlayer.getUnfinishedAnswer().size() > 1) {
					computerOptimalPosition = getComputerOptimalPosition2(computerPlayer);
				}
				if (computerPlayer.getUnfinishedAnswer().size() == 1) {
					checkRes = checkStandardAnswer(computerPlayer);
					if (checkRes) {
						System.out.println("哈哈，我猜出来了，你的谜底一定是：");
						print(computerPlayer, computerPlayer.getUnfinishedAnswer().get(0));
						System.out.println("其中三个关键位置是：");
						for (String answer : computerPlayer.getStandardAnswer()) {
							System.out.println(answer);
						}
						print(computerPlayer.getProcess());
					} else {
//						PlayProcess process = ad dPlayProcess(computerPlayer, computerOptimalPosition, val);
//						computerPlayer.getProcess().add(process);
						for (String answer : computerPlayer.getStandardAnswer()) {
							if (!computerPlayer.getYourAnswer().contains(answer)) {
								computerOptimalPosition = answer;
								computerPlayer.getYourAnswer().add(answer);
								System.out.println("下一个猜测的点位是【" + answer + "】：");
								break;
							}
						}
					}
				} else {
					System.out.println("请输入结果：0代表未击中，1代表击中机身，2代表击落飞机\r\n下一个猜测的点位是【" + computerOptimalPosition + "】：");
				}
			} else {
				System.out.println("格式错误，请重新输入__");
			}
		} while (!checkRes);
		input.close(); // 关闭资源
		System.out.println("");

	}

	private static boolean checkStandardAnswer(ComputerPlayer computerPlayer) {
		if (computerPlayer.getStandardAnswer().size() == 0) {
			Map<String, PatternModel> patternModelMap = PatternUtils.getBasePatternModel(computerPlayer.getPatten());
			String answer = computerPlayer.getUnfinishedAnswer().get(0);
			List<String> core = new ArrayList<>();
			String[] singletonPatterns = answer.split("_");
			for (String singletonPattern : singletonPatterns) {
				String corePosition = singletonPattern.substring(0, singletonPattern.length() - 1);
				String direction = singletonPattern.substring(singletonPattern.length() - 1);
				String[] coordinate = corePosition.split("-");
				int corex = Integer.parseInt(coordinate[0]);
				int corey = Integer.parseInt(coordinate[1]);
				PatternModel patternModelEntity = patternModelMap.get(direction);
				String oneAnswer = (corex + patternModelEntity.getCoreX()) + "-" + (corey + patternModelEntity.getCoreY());
				core.add(oneAnswer);
			}
			computerPlayer.setStandardAnswer(core);
		}
		if (computerPlayer.getYourAnswer().containsAll(computerPlayer.getStandardAnswer())) {
			return true;
		}
		return false;
	}
    private static String getComputerOptimalPosition1(ComputerPlayer computerPlayer){
		IGuessThePatternService guessThePatternService = new GuessThePatternServiceImpl();
		List<String> unfinishedAnswer = computerPlayer.getUnfinishedAnswer();
		int[][] probability = BoardUtils.getAllProbability(computerPlayer.getBoardModel(), computerPlayer.getPatten(), unfinishedAnswer);
		computerPlayer.setUnfinishedAnswer(unfinishedAnswer);
		computerPlayer.setProbability(probability);
		String computerOptimalPosition = guessThePatternService.createComputerOptimalPosition(computerPlayer);
		return computerOptimalPosition;
	}
    private static String getComputerOptimalPosition2(ComputerPlayer computerPlayer){
		IGuessThePatternService guessThePatternService = new GuessThePatternServiceImpl();
		List<String> unfinishedAnswer = computerPlayer.getUnfinishedAnswer();
		int[][] probability = BoardUtils.getAllProbability2(computerPlayer.getBoardModel(), computerPlayer.getPatten(), unfinishedAnswer);
		computerPlayer.setUnfinishedAnswer(unfinishedAnswer);
		computerPlayer.setProbability(probability);
		String computerOptimalPosition = guessThePatternService.createComputerOptimalPosition2(computerPlayer);
		return computerOptimalPosition;
	}
	private static void dealAnswer(ComputerPlayer computerPlayer, String position, String val) {
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
		PlayProcess process = addPlayProcess(computerPlayer, position, val);
		computerPlayer.getProcess().add(process);
		computerPlayer.getYourAnswer().add(position);
	}

	private static PlayProcess addPlayProcess(ComputerPlayer computerPlayer, String position, String val) {
		String[] realCoordinate = position.split("-");
		int x = Integer.parseInt(realCoordinate[0]);
		int y = Integer.parseInt(realCoordinate[1]);
		int realVal = Integer.parseInt(val);
		PlayProcess process = new PlayProcess();
		process.setStep(computerPlayer.getProcess().size() + 1);
		process.setRes(realVal);
		process.setX(x);
		process.setY(y);
		return process;
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
		System.out.println("   0  1  2  3  4  5  6  7  8  9  ");
		for (int i = 0; i < board.length; i++) {
			System.out.print(i + "  ");
			for (int j = 0; j < board[i].length; j++) {
				if (board[i][j] == 1) {
					System.out.print("*  ");
				} else {
					System.out.print("   ");
				}
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


}
