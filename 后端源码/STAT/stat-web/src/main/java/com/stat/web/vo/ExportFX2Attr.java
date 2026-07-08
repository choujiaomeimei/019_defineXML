package com.stat.web.vo;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ExportFX2Attr implements Serializable {

    private static final long serialVersionUID = 1L;


    private String eventName;
    private Integer pageNumber;
    private Integer isGenPage=1;
    private Integer estimatedPages = 1;
    private List<ExportCrf2Attr> exportCrf2Attrs;
}
