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
import java.util.List;

import lombok.*;

/**
 * <p>
 * 访视矩阵项crf配置
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
public class VisitCrfConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;


    private List<VisitCrfeventVO> visitCrfevent;



}
