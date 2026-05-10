package net.qihoo.guessthepattern.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 排行榜记录
 */
@Data
public class RankRecord {
    
    @ApiModelProperty(value = "排名")
    private int rank;
    
    @ApiModelProperty(value = "用户名")
    private String username;
    
    @ApiModelProperty(value = "步数")
    private int steps;
    
    @ApiModelProperty(value = "耗时（秒）")
    private long duration;
    
    @ApiModelProperty(value = "游戏时间")
    private String gameTime;
}

