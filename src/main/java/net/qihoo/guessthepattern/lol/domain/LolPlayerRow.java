package net.qihoo.guessthepattern.lol.domain;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * 与表 demo_lol_player 一行对应（用于比对）。
 */
@Value
@Builder
public class LolPlayerRow {
    UUID id;
    String gameId;
    String realName;
    Integer age;
    String currentTeam;
    String[] historicalTeams;
    String region;
    String[] identityRegions;
    String[] positions;
    String birthplace;
    String[] champions;
    String status;
    int worldsCount;
    int championshipsCount;
}
