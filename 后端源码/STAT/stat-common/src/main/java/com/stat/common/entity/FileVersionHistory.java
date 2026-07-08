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
@TableName("file_version_history")
public class FileVersionHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("file_id")
    private String fileId;

    @TableField("project_id")
    private String projectId;

    @TableField("username")
    private String username;

    @TableField("file_category")
    private String fileCategory;

    @TableField("version_number")
    private Integer versionNumber;

    @TableField("original_name")
    private String originalName;

    @TableField("server_file_name")
    private String serverFileName;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_extension")
    private String fileExtension;

    @TableField("file_md5")
    private String fileMd5;

    @TableField("upload_time")
    private LocalDateTime uploadTime;

    @TableField("process_status")
    private String processStatus;

    @TableField("replaced_time")
    private LocalDateTime replacedTime;

    @TableField("replaced_by")
    private String replacedBy;
}
