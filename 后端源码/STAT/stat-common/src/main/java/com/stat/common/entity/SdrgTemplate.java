package com.stat.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sdrg_template")
public class SdrgTemplate implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateName;
    private String templateType;
    private String filePath;
    private String sectionsConfig;
    private Boolean isDefault;
    private String createdBy;
    private Date createdTime;
}
