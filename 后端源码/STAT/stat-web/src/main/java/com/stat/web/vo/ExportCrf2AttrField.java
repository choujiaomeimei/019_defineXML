package com.stat.web.vo;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ExportCrf2AttrField implements Serializable {

    private static final long serialVersionUID = 1L;
    private String fieldLabelName;

    private String fieldLabelAttr;
    //是否是标签 =默认 0 否 1是 2 是否是多记录字段
    private Integer isfieldLabel=0;

//    //是否是多记录字段 0 否 1是
//    private Integer isAttrMultipleRecords=0;
    //只支持 多字段列在一起
    //多记录字段 多个列 多个值
    // nam1 name2 name3
    // 1     2      3
    private  List<List<String>> attrKeyValues;

    //多记录字段特殊
    //默认 都是2即可
    private  Integer tableAllrow=2;
    //多记录字段特殊
    //默认 第二列  都是0即可tableAllrow-2=0
    private  Integer tableAllrow2=1;

    /**
     * 字段单位  cm , kg,  kg/m2 , mmol/L , 个 , 周岁
     */
    private String fieldTypeUnit;

}
