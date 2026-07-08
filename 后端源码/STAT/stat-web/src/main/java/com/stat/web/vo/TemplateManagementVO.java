package com.stat.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 模板管理VO
 * @author system
 * @date 2024-12-19
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "模板管理VO")
public class TemplateManagementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板编号")
    private String templateOid;

    @Schema(description = "模板类别")
    private String templateCategory;

    @Schema(description = "模板描述")
    private String description;

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

    @Schema(description = "模板顺序")
    private Integer templateOrder;

    @Schema(description = "是否公开")
    private Integer isPublic;

    @Schema(description = "使用次数")
    private Integer useCount;

    @Schema(description = "字段数量")
    private Integer fieldCount;
}
