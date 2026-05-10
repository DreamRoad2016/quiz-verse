package net.qihoo.guessthepattern.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PlayProcess {
	@ApiModelProperty(value = "解答编号")
	private int step;
	@ApiModelProperty(value = "行数， 0--9")
	private int x;
	@ApiModelProperty(value = "列数， 0--9")
	private int y;
	@ApiModelProperty(value = "结果 0：未击中目标 1：击中目标 2：击毁目标")
	private int res;
}
