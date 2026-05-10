package net.qihoo.guessthepattern.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BoardEntity implements Serializable {
	private static final long serialVersionUID = -1377136021907810157L;
	private BoardModel boardModel;
	private PatternModel patten;
	private int[][] board;
	private String combination;
	private List<String> yourAnswer;
	private List<String> unfinishedAnswer;
	private List<String> standardAnswer;
	private List<PlayProcess> process;
	
	/** 用户名 */
	private String username;
	
	/** 游戏开始时间（时间戳） */
	private Long startTime;
}
