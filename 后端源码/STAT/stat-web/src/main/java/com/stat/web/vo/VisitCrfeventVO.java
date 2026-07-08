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
@Schema(description = "crf配置参数")
public class VisitCrfeventVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键id")
    private Long id;

    /**
     * 访视矩阵项ID
     */
    @Schema(description = "访视矩阵项ID")
    private Long eventId;

    /**
     * crf配置项OID
     */
    @Schema(description = "crf配置项OID")
    private List<String> formOid;
}
