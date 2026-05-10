package net.qihoo.guessthepattern.result;


import com.alibaba.fastjson.JSON;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import net.qihoo.guessthepattern.exception.BizException;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * 返回消息
 * @author qiruyu
 */
@ApiModel
public class Result<T> implements Serializable {

    @ApiModelProperty(notes = "返回数据",required = true)
    private T resultData;

    @ApiModelProperty(notes = "成功标识",example = "true",required = true)
    private boolean isSuccess = false;

    @ApiModelProperty(notes = "结果信息",example = "执行成功！",required = true)
    private String resultMsg = "执行失败!";

    public Result(){

    }

    public static <T> Result<T> fail(){
        return new Result<>();
    }

    public static <T> Result<T> fail(String msg){
        Result<T> result = fail();
        result.setResultMsg(msg);
        return result;
    }

//    public static <T> Result<T> success(String msg){
//        Result<T> result = success();
//        result.setResultMsg(msg);
//        return result;
//    }


    public static <T> Result<T> success(){
        Result<T> result = new Result<>();
        result.setResultMsg("执行成功！");
        result.setIsSuccess(true);
        return result;
    }

    public static <T> Result<T> success(T t){
        Result<T> result = success();
        result.setResultData(t);
        return result;
    }

    public static <T> Result<T> success(Result<T> result){
        if(result == null){
            return success();
        }
        result.setResultMsg("执行成功！");
        result.setIsSuccess(true);
        return result;
    }
    public T getResultData() {
        return resultData;
    }

    public void setResultData(T resultData) {
        this.resultData = resultData;
    }

    public boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getResultMsg() {
        return resultMsg;
    }



    public void setResultMsg(String resultMsg) {
        this.resultMsg = resultMsg;
    }

    @Override
    public String toString(){
        return JSON.toJSONString(this);
    }

    /**
     * 添加返回数据
     * @param key key
     * @param value value
     * @return Result
     */
    @SuppressWarnings("unchecked")
    public Result addData(String key, Object value) {
        Map<String,Object> map;
        if(this.resultData == null){
            map = new HashMap<>();
        }else if(this.resultData instanceof Map){
            map = (Map<String, Object>) this.resultData;
        }else {
            throw new BizException("not support");
        }
        map.put(key,value);
        setResultData((T) map);
        return this;
    }

    /**
     * 删除数据
     * @param keys
     * @return
     */
    @SuppressWarnings("unchecked")
    public Result removeData(String... keys) {
        if(this.resultData==null || !(this.resultData instanceof Map)){
            return this;
        }
        Map<String,Object> map = (Map<String, Object>) this.resultData;
        for (String key : keys) {
            map.remove(key);
        }
        return this;
    }

    /**
     * 清空返回数据
     * @return Result
     */
    public Result clearData() {
        this.resultData = null;
        return this;
    }

    /**
     * 获取resultDataMap 的值
     * @param key key
     * @return Object
     */
    @SuppressWarnings("unchecked")
    public Object get(String key){
        if(this.resultData==null || StringUtils.isBlank(key) || !(this.resultData instanceof Map)){
            return null;
        }
        Map<String,Object> map = (Map<String, Object>) this.resultData;
        return map.get(key);
    }
}
