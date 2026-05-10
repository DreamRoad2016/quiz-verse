package net.qihoo.guessthepattern.lol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LolBriefDTO {
    private String id;
    private String gameId;
    private String realName;
}
