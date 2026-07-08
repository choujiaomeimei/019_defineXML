package com.stat.common.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 统一文件上传记录实体类
 * 替代原有的多个分类上传表
 *
 * @author system
 * @date 2025-01-10
 */
@Data
public class FileUploadRecord {

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
     * 文件类别
     */
    private FileCategory fileCategory;

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
     * 上传状态
     */
    private UploadStatus uploadStatus;

    /**
     * 处理状态
     */
    private ProcessStatus processStatus;

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
     * 文件类别枚举
     */
    public enum FileCategory {
        P21_SPEC("P21_SPEC", "P21空Spec文件"),
        XPT("XPT", "XPT数据文件"),
        PROJECT_SPEC("PROJECT_SPEC", "项目Spec文件"),
        ACRF("ACRF", "aCRF注释文件"),
        VLM("VLM", "VLM变量级元数据"),
        CODELIST("CODELIST", "CodeList代码列表");

        private final String code;
        private final String description;

        FileCategory(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static FileCategory fromCode(String code) {
            for (FileCategory category : values()) {
                if (category.code.equals(code)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("未知的文件类别: " + code);
        }
    }

    /**
     * 上传状态枚举
     */
    public enum UploadStatus {
        UPLOADING("uploading", "上传中"),
        SUCCESS("success", "上传成功"),
        FAILED("failed", "上传失败");

        private final String code;
        private final String description;

        UploadStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 处理状态枚举
     */
    public enum ProcessStatus {
        PENDING("pending", "待处理"),
        PROCESSING("processing", "处理中"),
        COMPLETED("completed", "处理完成"),
        FAILED("failed", "处理失败");

        private final String code;
        private final String description;

        ProcessStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 构造方法
     */
    public FileUploadRecord() {
        this.deleted = 0;
        this.uploadStatus = UploadStatus.UPLOADING;
        this.processStatus = ProcessStatus.PENDING;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 创建文件上传记录
     */
    public FileUploadRecord(String fileId, String projectId, String username,
                           FileCategory fileCategory, String originalName,
                           String filePath, Long fileSize) {
        this();
        this.fileId = fileId;
        this.projectId = projectId;
        this.username = username;
        this.fileCategory = fileCategory;
        this.originalName = originalName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.uploadTime = LocalDateTime.now();

        // 自动提取文件扩展名
        if (originalName != null && originalName.contains(".")) {
            this.fileExtension = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }

        // 生成服务器文件名
        this.serverFileName = generateServerFileName(originalName);
    }

    /**
     * 生成服务器存储文件名
     */
    private String generateServerFileName(String originalName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return timestamp + "_" + fileCategory.getCode() + extension;
    }

    /**
     * 标记上传成功
     */
    public void markUploadSuccess() {
        this.uploadStatus = UploadStatus.SUCCESS;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记上传失败
     */
    public void markUploadFailed(String errorMessage) {
        this.uploadStatus = UploadStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 开始处理
     */
    public void startProcessing() {
        this.processStatus = ProcessStatus.PROCESSING;
        this.processTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记处理完成
     */
    public void markProcessCompleted(String outputFilePath) {
        this.processStatus = ProcessStatus.COMPLETED;
        this.outputFilePath = outputFilePath;
        this.updateTime = LocalDateTime.now();

        // 计算处理耗时
        if (this.processTime != null) {
            this.processDurationMs = (int) java.time.Duration.between(this.processTime, this.updateTime).toMillis();
        }
    }

    /**
     * 标记处理失败
     */
    public void markProcessFailed(String errorMessage) {
        this.processStatus = ProcessStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updateTime = LocalDateTime.now();

        // 计算处理耗时
        if (this.processTime != null) {
            this.processDurationMs = (int) java.time.Duration.between(this.processTime, this.updateTime).toMillis();
        }
    }

    /**
     * 获取文件大小的可读格式
     */
    public String getReadableFileSize() {
        if (fileSize == null) return "0 B";

        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
        return String.format("%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }

    /**
     * 检查是否处理完成
     */
    public boolean isProcessed() {
        return processStatus == ProcessStatus.COMPLETED;
    }

    /**
     * 检查是否处理失败
     */
    public boolean isProcessFailed() {
        return processStatus == ProcessStatus.FAILED;
    }

    /**
     * 获取上传状态描述
     */
    public String getUploadStatusDescription() {
        return uploadStatus != null ? uploadStatus.getDescription() : "";
    }

    /**
     * 获取处理状态描述
     */
    public String getProcessStatusDescription() {
        return processStatus != null ? processStatus.getDescription() : "";
    }
}