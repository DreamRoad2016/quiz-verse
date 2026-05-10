package net.qihoo.guessthepattern.exception;

import lombok.Getter;
import net.qihoo.guessthepattern.enums.ResultEnum;

/**
 * 业务异常基类
 *
 * @author qiruyu
 * @since 2018/03/13
 */
public class BizException extends RuntimeException {

    @Getter
    private ResultEnum resultEnum;

    public BizException() {
        super(ResultEnum.BIZ_EXCEPTION.getText());
        this.resultEnum = ResultEnum.BIZ_EXCEPTION;
    }

    public BizException(ResultEnum resultEnum) {
        super(resultEnum.getText());
        this.resultEnum = resultEnum;
    }


    public BizException(String message) {
        super(message);
        this.resultEnum = ResultEnum.BIZ_EXCEPTION;
    }

    public BizException(String message, ResultEnum resultEnum) {
        super(message);
        this.resultEnum = resultEnum;

    }

    public BizException(String message, Throwable cause, ResultEnum resultEnum) {
        super(message, cause);
        this.resultEnum = resultEnum;
    }
}
