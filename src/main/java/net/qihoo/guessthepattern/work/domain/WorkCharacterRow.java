package net.qihoo.guessthepattern.work.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkCharacterRow {
    private Long id;
    private String workId;
    private String displayName;
    private List<String> callNames;
    private Map<String, Object> attrs;
    private String status;
    private Boolean isActive;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}
