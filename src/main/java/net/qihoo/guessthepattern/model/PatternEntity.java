package net.qihoo.guessthepattern.model;

import lombok.Data;

@Data
public class PatternEntity {
	private int x;
	private int y;
	private PatternModel pattern;
}

