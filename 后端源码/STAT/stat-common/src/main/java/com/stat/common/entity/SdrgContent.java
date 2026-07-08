package com.stat.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sdrg_content")
public class SdrgContent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectId;
    private String sectionKey;
    private String sectionTitle;
    private Integer sectionOrder;
    private String contentText;
    private String contentJson;
    private String createdBy;
    private String updatedBy;
    private Date createdTime;
    private Date updatedTime;
}
