package net.qihoo.guessthepattern.work.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkListItemDTO {
    private String id;
    private String titleCn;
    private String configDir;
    private Boolean enabled;
    private int characterCount;
    private int activeCharacterCount;
    private boolean hasFieldConfig;
}
