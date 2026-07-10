package net.qihoo.guessthepattern.work.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CharacterBriefDTO {
    private Long id;
    private String displayName;
    private List<String> callNames;
    private String status;
    private Boolean isActive;
}
