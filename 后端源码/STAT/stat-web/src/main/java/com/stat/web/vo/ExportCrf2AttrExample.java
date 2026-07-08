package com.stat.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 这是一个示例文件，展示如何在ExportCrf2Attr类中添加level属性
 * 请将level属性添加到原有的ExportCrf2Attr类中
 */
@Data
@Schema(description = "ExportCrf2Attr示例：用于Word导出的CRF属性，添加了level属性")
public class ExportCrf2AttrExample {

    @Schema(description = "表单OID和名称")
    private String formOidName;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "表单OID")
    private String formOid;

    @Schema(description = "页码")
    private String pageNumber;

    @Schema(description = "目录级别")
    private Integer level;  // 新增属性：目录级别，1=一级目录，2=二级目录，3=三级目录

    @Schema(description = "表格总列数")
    private int tableAllrow = 8;

    @Schema(description = "表格第二部分总列数")
    private int tableAllrow2 = 4;

    @Schema(description = "表格总列数XML格式")
    private String tableAllrows = "<w:gridSpan w:val=\"8\" />";

    @Schema(description = "表格第二部分总列数XML格式")
    private String tableAllrow2s = "<w:gridSpan w:val=\"4\" />";

    @Schema(description = "CRF表单字段属性列表")
    private List<ExportCrf2AttrField> exportCrf2AttrField;
}
