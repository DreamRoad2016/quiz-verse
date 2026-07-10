package net.qihoo.guessthepattern.work.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SaveCharacterRequest {
    private String displayName;
    private List<String> callNames;
    private Map<String, Object> attrs;
    private String status;
    private Boolean isActive;
    private Integer sortOrder;
}
