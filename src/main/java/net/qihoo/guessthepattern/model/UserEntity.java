package net.qihoo.guessthepattern.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户实体
 */
@Data
public class UserEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码（MD5加密）
     */
    private String password;
    
    /**
     * 创建时间
     */
    private String createTime;
}

