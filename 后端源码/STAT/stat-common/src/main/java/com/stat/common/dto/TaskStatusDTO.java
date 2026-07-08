package com.stat.common.dto;

import com.stat.common.entity.P21GenerationTask;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskStatusDTO {

    private Long taskId;
    private String taskName;
    private P21GenerationTask.TaskStatus taskStatus;
    private Integer progressPercentage;
    private String errorMessage;

    // 时间信息
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime estimatedCompletion;

    // 文件信息
    private String specFileName;
    private String templateFileName;
    private String outputFileName;
    private String outputFilePath;

    // 配置信息
    private String protocolId;
    private String studyTitle;

    // 文件路径信息 (用于兼容性)
    private String specFilePath;
    private String templateFilePath;

    // 用户信息
    private String createdBy;
    private String remark;

    /**
     * 判断任务是否已完成
     */
    public boolean isCompleted() {
        return taskStatus == P21GenerationTask.TaskStatus.COMPLETED;
    }

    /**
     * 判断任务是否失败
     */
    public boolean isFailed() {
        return taskStatus == P21GenerationTask.TaskStatus.FAILED;
    }

    /**
     * 判断任务是否正在处理
     */
    public boolean isProcessing() {
        return taskStatus == P21GenerationTask.TaskStatus.PROCESSING;
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        switch (taskStatus) {
            case PENDING:
                return "等待处理";
            case PROCESSING:
                return "正在处理";
            case COMPLETED:
                return "处理完成";
            case FAILED:
                return "处理失败";
            default:
                return "未知状态";
        }
    }
}