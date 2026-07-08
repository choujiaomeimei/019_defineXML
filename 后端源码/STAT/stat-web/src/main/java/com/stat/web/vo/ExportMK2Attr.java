package com.stat.web.vo;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ExportMK2Attr implements Serializable {

    private static final long serialVersionUID = 1L;

    private String formOidName;

    private String formName;
    private String formOid;
    private Integer pageNumber;
    private Integer isGenPage=1;
    // 估计页数，默认为1
    private Integer estimatedPages = 1;

    List<ExportCrf2AttrField> exportCrf2AttrFields;
    List<VisitConfigVO> visitConfigs;
    //多记录字段特殊
    //默认 都是2即可
    private  Integer tableAllrow=2;
    //多记录字段特殊
    //默认 第二列  都是0即可tableAllrow-2=0
    private  Integer tableAllrow2=1;



    private  String tableAllrows="<w:gridSpan w:val=\"2\" />";
    private  String tableAllrow2s="<w:gridSpan w:val=\"1\" />";
}
