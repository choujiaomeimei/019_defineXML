package com.stat.common.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一文件上传记录DTO
 * 
 * @author System
 * @since 2025-01-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class FileUploadRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 文件唯一标识
     */
    private String fileId;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 上传用户名
     */
    private String username;

    /**
     * 文件类别：P21_SPEC, XPT, PROJECT_SPEC, ACRF, VLM, CODELIST
     */
    private String fileCategory;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 服务器存储文件名
     */
    private String serverFileName;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * 文件MD5值
     */
    private String fileMd5;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传状态：uploading, success, failed
     */
    private String uploadStatus;

    /**
     * 处理状态：pending, processing, completed, failed
     */
    private String processStatus;

    /**
     * 处理时间
     */
    private LocalDateTime processTime;

    /**
     * 处理耗时(毫秒)
     */
    private Integer processDurationMs;

    /**
     * 处理结果文件路径
     */
    private String outputFilePath;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志: 0-正常, 1-已删除
     */
    private Integer deleted;

    /**
     * 备注信息
     */
    private String remarks;

    /**
     * 当前版本号
     */
    private Integer versionNumber;

    // 文件类别枚举常量
    public static class FileCategory {
        public static final String P21_SPEC = "P21_SPEC";
        public static final String XPT = "XPT";
        public static final String PROJECT_SPEC = "PROJECT_SPEC";
        public static final String ACRF = "ACRF";
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
