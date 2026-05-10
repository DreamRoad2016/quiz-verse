package net.qihoo.guessthepattern.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 登录返回结果
 */
@Data
public class LoginResult {
    
    @ApiModelProperty(value = "登录令牌")
    private String token;
    
    @ApiModelProperty(value = "用户名")
    private String username;
    
    public static LoginResult of(String token, String username) {
        LoginResult result = new LoginResult();
        result.setToken(token);
        result.setUsername(username);
        return result;
    }
}

