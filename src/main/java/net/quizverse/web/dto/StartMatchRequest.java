package net.quizverse.web.dto;

import jakarta.validation.constraints.NotBlank;

public class StartMatchRequest {

    @NotBlank
    private String packId;

    public String getPackId() {
        return packId;
    }

    public void setPackId(String packId) {
        this.packId = packId;
    }
}
