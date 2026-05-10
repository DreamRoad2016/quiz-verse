package net.qihoo.guessthepattern.lol.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class LolGuessTurnResponse {
    private String matchId;
    private int guessIndex;
    private boolean hit;
    private int remaining;
    /** PLAYING | WON | LOST */
    private String gameStatus;
    private Map<String, String> guessDisplay = new LinkedHashMap<>();
    private Map<String, LolCellResultDTO> cells = new LinkedHashMap<>();
    private LolBriefDTO answerReveal;
}
