package net.quizverse.web.dto;

import jakarta.validation.constraints.NotBlank;

public class WxLoginRequest {

    @NotBlank
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
