package com.stat.web.vo;

// import com.baomidou.mybatisplus.annotation.TableName;
// import com.baomidou.mybatisplus.annotation.IdType;
// import com.baomidou.mybatisplus.annotation.TableId;
// import com.baomidou.mybatisplus.annotation.TableLogic;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

import lombok.*;

/**
 * <p>
 * 方案信息
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
@Schema(description = "方案信息")
public class PlanInforVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键id")
    private Long id;

    /**
     * 申办单位
     */
    @Schema(description = "申办单位")
    private String applicationUnit;

    /**
     * 方案名称
     */
    @Schema(description = "方案名称")
    private String planName;

    /**
     * 方案编号
     */
    @Schema(description = "方案编号")
    private String planNumber;

    /**
     * 方案版本
     */
    @Schema(description = "方案版本")
    private String planVer;

    /**
     * 方案版本日期
     */
    @Schema(description = "方案版本日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date planVerDate;

    /**
     * CRF撰写单位
     */
    @Schema(description = "CRF撰写单位")
    private String crfUnit;

    /**
     * CRF语言
     */
    @Schema(description = "CRF语言")
    private String crfLanguage;

    /**
     * CRF版本
     */
    @Schema(description = "CRF版本")
    private String crfVer;

    /**
     * CRF版本日期
     */
    @Schema(description = "CRF版本日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date crfVerDate;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 项目ID
     */
    @Schema(description = "项目ID")
    private String projectId;

}
