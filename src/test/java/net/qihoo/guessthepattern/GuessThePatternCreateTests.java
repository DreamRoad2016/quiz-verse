package net.qihoo.guessthepattern;

import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.PatternModel;
import net.qihoo.guessthepattern.service.IGuessThePatternService;
import net.qihoo.guessthepattern.service.impl.GuessThePatternServiceImpl;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GuessThePatternCreateTests {

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
		guessThePatternService.createAllPattern(patternModel, boardModel, 3);

	}
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
