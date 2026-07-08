package com.stat.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * 模板预览VO
 * @author system
 * @date 2024-12-19
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "模板预览VO")
public class TemplatePreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板编号")
    private String templateOid;

    @Schema(description = "模板类别")
    private String templateCategory;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "表单方向")
    private String formDirection;

    @Schema(description = "表单顺序")
    private Integer formOrder;

    @Schema(description = "预览字段列表（支持普通字段和多记录字段表格）")
    private List<TemplatePreviewFieldVO> previewFields;
}
