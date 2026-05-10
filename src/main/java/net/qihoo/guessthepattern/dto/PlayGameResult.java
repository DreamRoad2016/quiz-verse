package net.qihoo.guessthepattern.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import net.qihoo.guessthepattern.model.PlayProcess;

import java.util.List;

@Data
public class PlayGameResult {
	@ApiModelProperty(value = "结果类型 0：游戏结束 1：游戏中 2：游戏历史记录 3：答案")
	private int type;
	@ApiModelProperty(value = "结果 0：未击中目标 1：击中目标 2：击毁目标 -1：重复操作")
	private int result;
	@ApiModelProperty(value = "答案二维数组")
	private int[][] answer;
	@ApiModelProperty(value = "游戏过程，每一步的猜题操作")
	private List<PlayProcess> process;
	@ApiModelProperty(value = "排行榜排名：正数1-20表示上榜/刷新记录，负数-1到-20表示在榜但未刷新（绝对值是排名），0表示未上榜")
	private Integer rank;
}
