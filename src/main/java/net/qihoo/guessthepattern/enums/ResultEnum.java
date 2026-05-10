package net.qihoo.guessthepattern.enums;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Description;

import java.util.Arrays;
import java.util.Map;

/**
 * Api返回状态码
 *
 * @author zhengweichao
 * @since 2018/03/13
 */
@Getter
@Description("Api返回状态码信息和相关描述，前端可根据状态码定位问题")
@AllArgsConstructor
public enum ResultEnum {
    SUCCESS("00000", "执行成功"),
    FAILURE("99999", "执行失败"),
    /****************************** 1错误来源于用户 ******************************/
    BIZ_EXCEPTION("A2000", "自定义错误"),
    HANDLE_BIND_EXCEPTION("A2001", "参数校验错误"),
    HANDLE_BINDSINGLE_EXCEPTION("A2002", "单个参数校验错误"),
    PARAMETER_MISSING_EXCEPTION("A2003", "缺少请求参数体"),
    RECORD_NOT_EXIST_EXCEPTION("A2004", "记录不存在"),
    RECORD_EXIST_EXCEPTION("A2005", "记录已存在"),
    ILLEGAL_CHARACTERS_IN_PARAM("A2006", "存在非法字符"),
    TOKEN_JWT_ERROR("A3000", "请重新登录"),
    REQUEST_ERROR_AUTHENTICATION("A4001", "Unauthorized"),
    REQUEST_NOT_AUTHENTICATION("A4003", "权限禁止"),
    REQUEST_VERIFY("A5000", "审核中"),
    MERCHANTS_VERIFY("A6000", "商户校验失败"),
    SETTLE_METHOD_EXIST_EXCEPTION("A7000", "结算方式已经存在"),
    SETTLE_ACCOUNT_EMPTY_EXCEPTION("A7001", "结算账号为空"),
    EXCEED_THE_MAXIMUM("A8000", "数量已达到上限"),

    SIGN_MISSING_EXCEPTION("A2010", "appId or Authorization or nonce is  or timestamp is null"),
    SIGN_APPID_MISSING_EXCEPTION("A2011", "未找到appId对应的appSecret"),
    SIGN_OUTTIME_EXCEPTION("A2012", "请求时间超过规定范围时间10分钟"),
    SIGN_EXCEPTION("A2013", "签名不一致"),
    /****************************** 2错误来源于当前系统 ******************************/
    NullPointerException("B2000","空指针"),

    /******************************3错误来源于第三方服务 ******************************/
    RESOURCE_ERROR("C2000", "请求资源异常");


    private final String code;
    private final String text;

    public static final ResultEnum DEFAULT = null;
    private static final Map<String, ResultEnum> CODE_ENUM_MAP =
            Maps.uniqueIndex(Arrays.asList(values()), ResultEnum::getCode);

    public static ResultEnum codeOf(String code) {
        return CODE_ENUM_MAP.get(code);
    }

    public boolean isSuccess() {
        return StringUtils.startsWith(String.valueOf(code), "00");
    }
}
