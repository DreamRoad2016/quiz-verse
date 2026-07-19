package net.quizverse.web.dto;

import jakarta.validation.constraints.NotBlank;

public class GuessRequest {

    @NotBlank
    private String entityId;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
