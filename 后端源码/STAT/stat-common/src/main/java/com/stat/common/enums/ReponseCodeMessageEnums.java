package com.stat.common.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @description:
 * @author: yujq
 **/
public enum ReponseCodeMessageEnums {

    STA_3000("3000","英文名称必填"),
    STA_3001("3001","英文名称需唯一"),

    STA_3002("3002","EventOid必填"),
    STA_3003("3003","EventOid已存在"),
    STA_3004("3004","crfId必填"),
    STA_4001("4001","批量导入crf文档错误"),
    STA_4002("4002","EventOid已存在"),
    STA_4003("4003","导入参数错误"),
    ;
    /**
     * 编码
     */
    private final String code;
    /**
     * 说明
     */
    private final String des;

    ReponseCodeMessageEnums(String code, String des) {
        this.code = code;
        this.des = des;
    }


    private static final Map<String, String> REPONSECODEMESSAGEENUMS_INSTANCE = new HashMap<>();

    static {

        for (ReponseCodeMessageEnums e : ReponseCodeMessageEnums.values()) {
            REPONSECODEMESSAGEENUMS_INSTANCE.put(e.getCode(), e.getDes());
        }
    }
    public String getCode() {
        return this.code;
    }

    public String getDes() {
        return this.des;
    }



    public static String getByCode(String code) {
        if (Objects.isNull(code)) {
            return null;
        }
        return REPONSECODEMESSAGEENUMS_INSTANCE.get(code);
    }
}
