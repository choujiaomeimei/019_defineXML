package com.stat.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * aCRF上传记录DTO
 */
@Data
public class SasAcrfUploadDTO implements Serializable {

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
     * 上传时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    /**
     * 处理状态: 0-未处理, 1-处理成功, 2-处理失败
     */
    private Integer processStatus;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processTime;

    /**
     * 解析结果文件路径
     */
    private String outputFilePath;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 处理结果对象（用于前端显示）
     */
    private ProcessResult processResult;

    @Data
    public static class ProcessResult {
        private Boolean success;
        private String message;
        private String outputFile;
        private LocalDateTime processTime;
        private String error;
    }
}