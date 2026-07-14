package com.stat.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一文件上传记录实体类
 * 替代原有的多个分类上传表：sas_acrf_upload, sas_xpt_upload, sas_project_spec_upload, sas_p21_spec_upload
 * 
 * @author System
 * @since 2025-01-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("file_upload_records")
public class FileUploadRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件唯一标识
     */
    @TableField("file_id")
    private String fileId;

    /**
     * 项目ID
     */
    @TableField("project_id")
    private String projectId;

    /**
     * 上传用户名
     */
    @TableField("username")
    private String username;

    /**
     * 文件类别：P21_SPEC, XPT, PROJECT_SPEC, ACRF, EDC_CODELIST, VLM, CODELIST
     */
    @TableField("file_category")
    private String fileCategory;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * 服务器存储文件名
     */
    @TableField("server_file_name")
    private String serverFileName;

    /**
     * 文件存储路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * projects 下当前有效工作副本路径
     */
    @TableField("workspace_file_path")
    private String workspaceFilePath;

    /**
     * 文件所属标准类型：SDTM、ADAM、SEND
     */
    @TableField("standard_type")
    private String standardType;

    /**
     * 文件大小(字节)
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件扩展名
     */
    @TableField("file_extension")
    private String fileExtension;

    /**
     * 文件MD5值
     */
    @TableField("file_md5")
    private String fileMd5;

    /**
     * 上传时间
     */
    @TableField("upload_time")
    private LocalDateTime uploadTime;

    /**
     * 上传状态：uploading, success, failed
     */
    @TableField("upload_status")
    private String uploadStatus;

    /**
     * 处理状态：pending, processing, completed, failed
     */
    @TableField("process_status")
    private String processStatus;

    /**
     * 处理时间
     */
    @TableField("process_time")
    private LocalDateTime processTime;

    /**
     * 处理耗时(毫秒)
     */
    @TableField("process_duration_ms")
    private Integer processDurationMs;

    /**
     * 处理结果文件路径
     */
    @TableField("output_file_path")
    private String outputFilePath;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志: 0-正常, 1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 备注信息
     */
    @TableField("remarks")
    private String remarks;

    /**
     * 当前版本号(每次替换+1)
     */
    @TableField("version_number")
    private Integer versionNumber;

    // 文件类别枚举常量
    public static class FileCategory {
        public static final String P21_SPEC = "P21_SPEC";
        public static final String XPT = "XPT";
        public static final String PROJECT_SPEC = "PROJECT_SPEC";
        public static final String ACRF = "ACRF";
        public static final String EDC_CODELIST = "EDC_CODELIST";
        public static final String VLM = "VLM";
        public static final String CODELIST = "CODELIST";
    }

    // 上传状态枚举常量
    public static class UploadStatus {
        public static final String UPLOADING = "uploading";
        public static final String SUCCESS = "success";
        public static final String FAILED = "failed";
    }

    // 处理状态枚举常量
    public static class ProcessStatus {
        public static final String PENDING = "pending";
        public static final String PROCESSING = "processing";
        public static final String COMPLETED = "completed";
        public static final String FAILED = "failed";
    }
}
