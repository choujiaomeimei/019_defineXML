package com.stat.common.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @description:
 * @author: yujq
 **/
public enum BusLevelEnums {

    BUSLEVEL_1(1,"一级"),
    BUSLEVEL_2(2,"二级"),
    BUSLEVEL_3(3,"三级"),
    BUSLEVEL_4(4,"四级");
    /**
     * 编码
     */
    private final Integer code;
    /**
     * 说明
     */
    private final String des;

    BusLevelEnums(Integer code, String des) {
        this.code = code;
        this.des = des;
    }


    private static final Map<Integer, String> BUSLEVELENUMS_INSTANCE = new HashMap<>();

    static {

        for (BusLevelEnums e : BusLevelEnums.values()) {
            BUSLEVELENUMS_INSTANCE.put(e.getCode(), e.getDes());
        }
    }
    public Integer getCode() {
        return this.code;
    }

    public String getDes() {
        return this.des;
    }



    public static String getByCode(String code) {
        if (Objects.isNull(code)) {
            return null;
        }
        return BUSLEVELENUMS_INSTANCE.get(code);
    }
}
