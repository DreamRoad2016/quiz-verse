package net.qihoo.guessthepattern.lol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LolGuessStartResponse {
    private String matchId;
    private int maxGuesses;
    private int poolSize;
    private List<LolColumnMetaDTO> columns;
    private List<LolBriefDTO> briefs;
}
