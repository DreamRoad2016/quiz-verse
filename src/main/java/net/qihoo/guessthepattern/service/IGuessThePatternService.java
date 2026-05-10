package net.qihoo.guessthepattern.service;

import net.qihoo.guessthepattern.model.BoardModel;
import net.qihoo.guessthepattern.model.ComputerPlayer;
import net.qihoo.guessthepattern.model.PatternModel;

import java.util.List;

public interface IGuessThePatternService {
	List<String> createAllPattern(PatternModel patternModel) throws Exception;

	List<String> createAllPattern(PatternModel patternModel, BoardModel boardModel, int num) throws Exception;

	ComputerPlayer createComputerPlayer(PatternModel patternModel, BoardModel boardModel);

	ComputerPlayer createComputerPlayer2(PatternModel patternModel, BoardModel boardModel);

	String createComputerOptimalPosition(ComputerPlayer computerPlayer);

	String createComputerOptimalPosition2(ComputerPlayer computerPlayer);
}
