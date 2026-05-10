package net.qihoo.guessthepattern.model;

import lombok.Data;

import java.util.List;

@Data
public class ComputerPlayer {
	private BoardModel boardModel;
	private PatternModel patten;
	private int[][] probability;
	private List<String> unfinishedAnswer;
	private List<String> yourAnswer;
	private List<String> standardAnswer;
	private List<PlayProcess> process;
}
