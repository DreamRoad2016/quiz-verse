package net.qihoo.guessthepattern;

import net.qihoo.guessthepattern.model.BoardEntity;
import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.PatternModel;
import net.qihoo.guessthepattern.model.PlayProcess;
import net.qihoo.guessthepattern.service.IGuessThePatternService;
import net.qihoo.guessthepattern.service.impl.GuessThePatternServiceImpl;
import net.qihoo.guessthepattern.utils.CommonUtils;
import net.qihoo.guessthepattern.utils.PatternUtils;
import net.qihoo.guessthepattern.utils.TempUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

@SpringBootTest
class GuessThePatternApplicationTests {

	@Test
	void contextLoads() {

	}

	public static void main(String[] args) throws Exception {
		PatternModel patternModel = new PatternModel();
		int[][] arr = {{0, 0, 1, 0, 0}, {1, 1, 1, 1, 1}, {0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}};
		patternModel.setPattern(arr);
		patternModel.setCoreX(0);
		patternModel.setCoreY(2);
		BoardModel boardModel = new BoardModel();
		boardModel.setX(10);
		boardModel.setY(10);
		BoardEntity boardEntity = CommonUtils.createBoardEntity(boardModel, patternModel);

		System.out.print("请输入你要猜测的行与列，用[-]分割：");
		Scanner input = new Scanner(System.in);
		String val = null;       // 记录输入度的字符串
		do {
			String pattern = "[0-9]*-[0-9]*";
			val = input.next();       // 等待输入值
			System.out.println("您输入的是：" + val);
			if (val.equals("#")) {
				print(boardEntity.getBoardModel().getX(), boardEntity.getBoardModel().getY(), boardEntity.getProcess());
			} else if (val.equals("*")) {
				print(boardEntity.getProcess());
			} else if (val.equals("res")) {
				print(boardEntity.getBoard());
			} else if (Pattern.matches(pattern, val)) {
				int res = verifyAnswer(boardEntity, val);
				switch (res) {
					case -1: {
						System.out.println("此点位已被查看过，可查询历史记录确认");
						break;
					}
					case 0: {
						System.out.println("未命中飞机，请继续");
						break;
					}
					case 1: {
						System.out.println("成功命中机身，请继续");
						break;
					}
					case 2: {
						if (boardEntity.getUnfinishedAnswer().size() > 0) {
							System.out.println("恭喜你，击落一架飞机。请继续");
						} else {
							System.out.println("恭喜你成功击落三架飞机, 完成游戏");
							System.out.println("你一共花费"+boardEntity.getProcess().size()+"步通关游戏，详情如下：");
							print(boardEntity.getProcess());
							System.out.println("玩家视角图如下：");
							print(boardEntity.getBoardModel().getX(), boardEntity.getBoardModel().getY(), boardEntity.getProcess());
							System.out.println("标准答案图如下：");
							print(boardEntity.getBoard());
						}
						break;
					}
					default: {
						System.out.println("验证失败,请输入正确的行列数");
					}
				}
			} else {
				System.out.println("格式错误，请重新输入__");
			}
		} while (boardEntity.getUnfinishedAnswer().size() > 0);
		input.close(); // 关闭资源
		System.out.println("");

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
