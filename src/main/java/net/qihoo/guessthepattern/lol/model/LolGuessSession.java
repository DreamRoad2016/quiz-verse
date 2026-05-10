package net.qihoo.guessthepattern.lol.model;

import lombok.Data;

/**
 * Redis 中保存的单局状态（JSON）。
 */
@Data
public class LolGuessSession {
    private String answerId;
    private int guessCount;
    private int maxGuesses;
    /** PLAYING | WON | LOST */
    private String state;
}
