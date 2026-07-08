package com.stat.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "crf配置")
public class CrfConfigVO implements Serializable {

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
     * 是否模板库
     */
    @Schema(description = "是否模板库 0 否 1是  (默认 0 )")
    private Integer isUpload=0;

    /**
    * 项目ID
    */
   @Schema(description = "项目ID")
   private String projectId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 模板ID
     */
    @Schema(description = "模板ID")
    private Long templateId;

    /**
     * 字段列表
     */
    private List<CrfConfigFormFieldVO> fields;

}
