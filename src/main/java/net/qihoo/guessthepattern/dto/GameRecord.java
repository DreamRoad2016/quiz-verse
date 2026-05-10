package net.qihoo.guessthepattern.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 游戏战绩记录
 */
@Data
public class GameRecord {
    
    @ApiModelProperty(value = "游戏ID")
    private String gameId;
    
    @ApiModelProperty(value = "猜测步数")
    private int steps;
    
    @ApiModelProperty(value = "是否完成")
    private boolean win;
    
    @ApiModelProperty(value = "耗时（秒）")
    private long duration;
    
    @ApiModelProperty(value = "游戏时间")
    private String gameTime;
    
    @ApiModelProperty(value = "目标数量")
    private int targetCount;
}

