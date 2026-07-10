package net.qihoo.guessthepattern.work.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class WorkRow {
    private String id;
    private String titleCn;
    private String category;
    private String poolType;
    private Integer schemaVersion;
    private String configDir;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
