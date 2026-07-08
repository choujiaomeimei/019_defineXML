package com.stat.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * 模板预览字段VO - 支持普通字段和多记录字段表格
 * @author system
 * @date 2024-12-19
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "模板预览字段VO")
public class TemplatePreviewFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字段类型：normal-普通字段，table-多记录字段表格")
    private String fieldType;

    @Schema(description = "字段标签名称")
    private String fieldLabel;

    @Schema(description = "格式化后的值（普通字段使用）")
    private String formattedValue;

    @Schema(description = "字段单位")
    private String fieldUnit;

    @Schema(description = "是否为标签字段（1-是标签，0-普通字段）")
    private Integer isFieldLabel;

    @Schema(description = "纵向表格数据（表格类型使用）")
    private List<List<String>> tableData;

    @Schema(description = "字段顺序")
    private Integer fieldOrder;

    @Schema(description = "原始字段类型（如：数字、文本、日期等）")
    private String originalFieldType;

    @Schema(description = "字段格式（如：4.2、YYYY年MM月DD日等）")
    private String fieldTypeFormat;
}
