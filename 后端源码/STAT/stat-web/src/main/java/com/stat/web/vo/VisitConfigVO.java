package com.stat.web.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

import lombok.*;

/**
 * <p>
 * 访视矩阵项
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
@Schema(description = "访视矩阵项")
public class VisitConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键id")
    private Long id;

    /**
     * 访视矩阵编号
     */
    @Schema(description = "访视矩阵编号")
    private String eventOid;

    /**
     * 访视矩阵名字
     */
    @Schema(description = "访视矩阵名字")
    private String eventName;

    /**
     * 访视矩阵列名字
     */
    @Schema(description = "访视矩阵列名字")
    private String eventVisit;

    /**
     * 顺序
     */
    @Schema(description = "顺序")
    private Integer eventOrder;


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
}
