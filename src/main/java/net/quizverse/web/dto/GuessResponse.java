package net.quizverse.web.dto;

import net.quizverse.compare.CellResult;
import net.quizverse.pack.model.EntityBrief;

import java.util.LinkedHashMap;
import java.util.Map;

public class GuessResponse {

    private String matchId;
    private int guessIndex;
    private boolean hit;
    private String gameStatus;
    private int remaining;
    private Map<String, String> display = new LinkedHashMap<>();
    private Map<String, CellResult> cells = new LinkedHashMap<>();
    private EntityBrief answerReveal;
    /** 揭晓答案时的表格列展示值（与 columns 同 key） */
    private Map<String, String> answerDisplay = new LinkedHashMap<>();

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public int getGuessIndex() {
        return guessIndex;
    }

    public void setGuessIndex(int guessIndex) {
        this.guessIndex = guessIndex;
    }

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public Map<String, String> getDisplay() {
        return display;
    }

    public void setDisplay(Map<String, String> display) {
        this.display = display;
    }

    public Map<String, CellResult> getCells() {
        return cells;
    }

    public void setCells(Map<String, CellResult> cells) {
        this.cells = cells;
    }

    public EntityBrief getAnswerReveal() {
        return answerReveal;
    }

    public void setAnswerReveal(EntityBrief answerReveal) {
        this.answerReveal = answerReveal;
    }

    public Map<String, String> getAnswerDisplay() {
        return answerDisplay;
    }

    public void setAnswerDisplay(Map<String, String> answerDisplay) {
        this.answerDisplay = answerDisplay;
    }
}
