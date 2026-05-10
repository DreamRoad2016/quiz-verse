package net.qihoo.guessthepattern.model;

import lombok.Data;

@Data
public class PatternModel {
	private String patternName;
	private int coreX;
	private int coreY;
	private int[][] pattern;
}

