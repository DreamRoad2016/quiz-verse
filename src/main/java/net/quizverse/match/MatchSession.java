package net.quizverse.match;

import java.util.ArrayList;
import java.util.List;

public class MatchSession {

    private String matchId;
    private String packId;
    private String answerId;
    private int guessCount;
    private int maxGuesses;
    /** PLAYING | WON | LOST */
    private String state = "PLAYING";
    private List<String> guessedIds = new ArrayList<>();

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getPackId() {
        return packId;
    }

    public void setPackId(String packId) {
        this.packId = packId;
    }

    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    public int getGuessCount() {
        return guessCount;
    }

    public void setGuessCount(int guessCount) {
        this.guessCount = guessCount;
    }

    public int getMaxGuesses() {
        return maxGuesses;
    }

    public void setMaxGuesses(int maxGuesses) {
        this.maxGuesses = maxGuesses;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getGuessedIds() {
        return guessedIds;
    }

    public void setGuessedIds(List<String> guessedIds) {
        this.guessedIds = guessedIds;
    }
}
