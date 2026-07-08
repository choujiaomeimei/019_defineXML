package com.stat.web.vo;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

/**
 * <p>
 * crf配置参数
 * </p>
 *
 * @author jyq
 * @since 2024-03-13
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "crf配置参数")
public class CrfConfigFormFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    /**
     * 表单编号
     */
    @Schema(description = "表单编号")
    private String formOid;

    /**
     * 表单名字
     */
    @Schema(description = "表单名字")
    private String formName;

    /**
     * 表单默认全局  全局
     */
    @Schema(description = "表单默认全局  全局")
    private String formDirection;

    /**
     * 表单顺序
     */
    @Schema(description = "表单顺序")
    private Integer formOrder;

    /**
     * 字段编号
     */
    @Schema(description = "字段编号")
    private String fieldOid;

    /**
     * 字段名字
     */
    @Schema(description = "字段名字")
    private String fieldLabel;

    /**
     * 字段列表
     */
    @Schema(description = "字段列表")
    private String fieldList;

    /**
     * 默认全局  全局
     */
    @Schema(description = "默认全局  全局")
    private String fieldDirection;

    /**
     * 字段类型  默认值, 日期 , 日期时间 , 时间 , 数字,  文本  ,选项 , 标签
     */
    @Schema(description = "字段类型  默认值, 日期 , 日期时间 , 时间 , 数字,  文本  ,选项 , 标签")
    private String fieldType;

    /**
     * 字段单位  cm , kg,  kg/m2 , mmol/L , 个 , 周岁
     */
    @Schema(description = "字段单位  cm , kg,  kg/m2 , mmol/L , 个 , 周岁")
    private String fieldTypeUnit;

    /**
     * 字段格式 日期 YYYY年MM月DD日,   日期时间 YYYY年MM月DD日 HH时:MM分 , 时间 HH时:MM分 , 数字   , 文本 , 选项
     */
    @Schema(description = "字段格式 日期 YYYY年MM月DD日,   日期时间 YYYY年MM月DD日 HH时:MM分 , 时间 HH时:MM分 , 数字   , 文本 , 选项")
    private String fieldTypeFormat;

    /**
     * 字段属性   默认 , 多记录字段
     */
    @Schema(description = "字段属性   默认 , 多记录字段")
    private String fieldAttribute;

    /**
     * 字段是否必填   是
     */
    @Schema(description = "字段是否必填   是")
    private String fieldMust;

    /**
     * 顺序
     */
    @Schema(description = "顺序")
    private Integer fieldOrder;


    @Schema(description = "crfConfigId 必填")
    private Long crfConfigId;

    @Schema(description = "项目ID")
    private String projectId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 0 直接添加生效, 1 excel导入进来,需要添加才生效 2 导入后已添加 生效
     */
    @Schema(description = "是否上传 0-直接添加生效, 1-excel导入进来需要添加才生效, 2-导入后已添加生效")
    private Integer isUpload;

    /**
     * 字段选项分隔符，默认为英文逗号
     */
    @Schema(description = "字段选项分隔符")
    private String fieldListSeparator;

}
