package com.stat.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.io.Serializable;
import java.util.Map;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "导出配置")
public class ExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "项目ID")
    private String projectId;

    @Schema(description = "是否生成目录  0 否 1 是 (默认是)")
    private Integer isGenDir=1;
    @Schema(description = "是否生成页眉页脚  0 否 1 是 (默认是)")
    private Integer isGenHeaderFooter=1;
    @Schema(description = "是否unique表单  0 否 1 是 (默认 否)")
    private Integer isGenUnique=0;
    @Schema(description = "是否分页  0 否 1 是 (默认是)")
    private Integer isGenPage=1;
    @Schema(description = "是否横排版 0 否 1 是 (默认是)")
    private Integer isGenHorizontal=1;

    @Schema(description = "是否访视导出 0 否 模块导出 1 是 (默认是)  unique为1 则不起作用")
    private Integer isGenVisit=1;


    @Schema(description = "是否Spec导出 0 否 默认  1 是   ")
    private Integer isGenSpec=0;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "列宽配置，key为列索引，value为宽度像素值")
    private Map<Integer, Integer> columnWidths;

    @Schema(description = "是否纵向纸张 0 横向 1 纵向 (默认横向)")
    private Integer isPaperPortrait = 0;

    @Schema(description = "页眉样式 1 默认样式 2 表格样式 (默认1)")
    private String headerStyle = "1";

}
