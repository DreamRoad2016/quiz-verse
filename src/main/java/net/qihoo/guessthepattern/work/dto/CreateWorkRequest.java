package net.qihoo.guessthepattern.work.dto;

import lombok.Data;

@Data
public class CreateWorkRequest {
    /** 作品 slug，如 zhenhuan_2011 */
    private String id;
    private String titleCn;
    /** 复制字段配置与 work_column，默认 zhenhuan_2011 */
    private String templateWorkId;
}
