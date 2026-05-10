package net.qihoo.guessthepattern.result;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import net.qihoo.guessthepattern.enums.ResultEnum;

/**
 * 重构Result支持状态码
 *
 * @author qiruyu
 * @since 2020/04/23
 */
@Getter
public class ResultResponse<R> extends Result<R> {

    /**
     * 返回结果状态码
     */
    @ApiModelProperty(notes = "结果码", required = true)
    private String resultCode;


    public void setResultStatusEnum(ResultEnum resultEnum) {
        this.setResultCode(resultEnum.getCode());
        this.setResultMsg(resultEnum.getText());
    }

    /****************************** 私有化构造函数 ******************************/
    private ResultResponse(ResultEnum resultEnum) {
        this.setResultCode(resultEnum.getCode());
        this.setResultMsg(resultEnum.getText());
    }

    private ResultResponse(ResultEnum resultEnum, R resultData) {
        this.setResultCode(resultEnum.getCode());
        this.setResultMsg(resultEnum.getText());
        this.setResultData(resultData);
    }

    private ResultResponse() {
    }

    private void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    /****************************** 执行成功 ******************************/

    public static <R> ResultResponse<R> success() {
        ResultResponse<R> res = new ResultResponse<>(ResultEnum.SUCCESS);
        res.setIsSuccess(true);
        return res;
    }

    public static <R> ResultResponse<R> success(R resultData) {
        ResultResponse<R> res = new ResultResponse<>(ResultEnum.SUCCESS, resultData);
        res.setIsSuccess(true);
        return res;
    }

    public static <R> ResultResponse<R> success(ResultEnum resultEnum) {
        ResultResponse<R> res = new ResultResponse<>(resultEnum);
        res.setIsSuccess(true);
        return res;
    }

    public static <R> ResultResponse<R> success(ResultEnum resultEnum, R resultData) {
        ResultResponse<R> res = new ResultResponse<>(resultEnum, resultData);
        res.setIsSuccess(true);
        return res;
    }

    /****************************** 执行失败
     * @param ******************************/

    public static <R> ResultResponse<R> fail() {
        ResultResponse<R> res = new ResultResponse<>(ResultEnum.FAILURE);
        res.setIsSuccess(false);
        return res;
    }

    public static <R> ResultResponse<R> fail(R resultData) {
        ResultResponse<R> res = new ResultResponse<>(ResultEnum.FAILURE, resultData);
        res.setIsSuccess(false);
        return res;
    }

    public static <R> ResultResponse<R> fail(ResultEnum resultEnum) {
        ResultResponse<R> res =  new ResultResponse<>(resultEnum);
        res.setIsSuccess(false);
        return res;
    }

    public static <R> ResultResponse<R> fail(ResultEnum resultEnum, R resultData) {
        ResultResponse<R> res = new ResultResponse<>(resultEnum, resultData);
        res.setIsSuccess(false);
        return res;
    }


    public static <R> ResultResponse<R> fail(ResultEnum resultEnum, String resultMsg) {
        ResultResponse<R> res =  new ResultResponse<>(resultEnum);
        res.setIsSuccess(false);
        res.setResultMsg(resultMsg);
        return res;
    }

}
