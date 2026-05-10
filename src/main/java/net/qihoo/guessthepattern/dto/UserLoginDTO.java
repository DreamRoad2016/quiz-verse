package net.qihoo.guessthepattern.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 用户登录/注册请求
 */
@Data
public class UserLoginDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5-]{2,20}$", message = "用户名只能包含中英文、数字和中横线，长度2-20")
    @ApiModelProperty(value = "用户名", required = true)
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @ApiModelProperty(value = "密码", required = true)
    private String password;
}

