package com.stat.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 模板字段VO
 * @author system
 * @date 2024-12-19
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "模板字段VO")
public class TemplateFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字段ID")
    private Long id;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "字段编号")
    private String fieldOid;

    @Schema(description = "字段标签")
    private String fieldLabel;

    @Schema(description = "字段类型")
    private String fieldType;

    @Schema(description = "字段选项列表")
    private String fieldList;

    @Schema(description = "字段格式")
    private String fieldTypeFormat;

    @Schema(description = "字段单位")
    private String fieldTypeUnit;

    @Schema(description = "字段顺序")
    private Integer fieldOrder;

    @Schema(description = "字段属性")
    private String fieldAttribute;

    @Schema(description = "字段方向")
    private String fieldDirection;

    @Schema(description = "是否必填")
    private String fieldMust;

    @Schema(description = "创建用户")
    private String username;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @Schema(description = "删除标志")
    private Integer deleteFlag;

    @Schema(description = "是否上传")
    private Integer isUpload;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "表单OID")
    private String formOid;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "表单方向")
    private String formDirection;

    @Schema(description = "表单顺序")
    private Integer formOrder;
    
    @Schema(description = "格式化后的预览值")
    private String formattedValue;

    @Schema(description = "字段选项分隔符")
    private String fieldListSeparator;
}
