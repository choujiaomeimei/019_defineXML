package com.stat.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("project_snapshot")
public class ProjectSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private String projectId;

    @TableField("snapshot_name")
    private String snapshotName;

    @TableField("snapshot_type")
    private String snapshotType;

    @TableField("description")
    private String description;

    @TableField("spec_data_json")
    private String specDataJson;

    @TableField("vlm_data_json")
    private String vlmDataJson;

    @TableField("codelist_data_json")
    private String codelistDataJson;

    @TableField("config_json")
    private String configJson;

    @TableField("locked")
    private Boolean locked;

    @TableField("version_label")
    private String versionLabel;

    @TableField("created_by")
    private String createdBy;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
