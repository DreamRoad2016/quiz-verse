package net.qihoo.guessthepattern.exception;

import lombok.extern.slf4j.Slf4j;
import net.qihoo.guessthepattern.enums.ResultEnum;
import net.qihoo.guessthepattern.result.ResultResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static net.qihoo.guessthepattern.enums.ResultEnum.BIZ_EXCEPTION;


/**
 * @description: GlobalExceptionHandler
 * @author: qiruyu
 * @date: 2020/4/27
 **/
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    /**
     * -------- 通用异常处理方法 --------
     **/
    @ExceptionHandler(Exception.class)
    public ModelAndView error(HttpServletRequest req, HttpServletResponse response, Object handler,
                              Exception ex) {
        log.error("系统异常 请求路径uri: {},remoteHost: {}, 异常描述 {}", req.getRequestURI(), req.getRemoteHost(), ex);
        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", String.valueOf(ex.getClass()));
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("path", req.getRequestURI());
        mav.addObject("message", ex.getMessage());
        mav.setViewName("error");
        return mav;
    }

    /**
     * 自定义定异常处理方法
     *
     * @return
     */
    @ExceptionHandler(BizException.class)
    public ResultResponse BizException(BizException e) {
        String message = e.getMessage();
        if (e.getResultEnum() != null) {
            if (StringUtils.isNotBlank(message)) {
                return ResultResponse.fail(e.getResultEnum(), message);
            }
            return ResultResponse.fail(e.getResultEnum());
        }
        return ResultResponse.fail(BIZ_EXCEPTION, message);
    }

    /**
     * 一般的参数绑定时候抛出的异常
     *
     * @param e
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultResponse handleBindException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = null;
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        }
        return ResultResponse.fail(ResultEnum.HANDLE_BIND_EXCEPTION, message);
    }


    @ResponseBody
    @ExceptionHandler(value = BindException.class)
    public ResultResponse handleValidException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = null;
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getDefaultMessage();
            }
        }
        return ResultResponse.fail(ResultEnum.HANDLE_BIND_EXCEPTION, message);
    }

    /**
     * 单个参数校验
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResultResponse handleBindSingleException(ConstraintViolationException ex) {
        log.error("单个参数校验异常", ex);
        List<String> defaultMsg = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        return ResultResponse.fail(ResultEnum.HANDLE_BINDSINGLE_EXCEPTION, defaultMsg.get(0));
    }

    /**
     * 缺少请求体异常处理器
     *
     * @param e 缺少请求体异常
     * @return ResponseResult
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultResponse parameterBodyMissingExceptionHandler(HttpMessageNotReadableException e) {
        return ResultResponse.fail(ResultEnum.PARAMETER_MISSING_EXCEPTION);
    }

}
