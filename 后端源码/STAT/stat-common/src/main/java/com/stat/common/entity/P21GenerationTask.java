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
@TableName("p21_generation_task")
public class P21GenerationTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_name")
    private String taskName;

    @TableField("task_status")
    private TaskStatus taskStatus;

    // 输入文件信息
    @TableField("spec_file_path")
    private String specFilePath;

    @TableField("spec_file_name")
    private String specFileName;

    @TableField("template_file_path")
    private String templateFilePath;

    @TableField("template_file_name")
    private String templateFileName;

    // 配置信息
    @TableField("protocol_id")
    private String protocolId;

    @TableField("study_title")
    private String studyTitle;

    @TableField("config_json")
    private String configJson;

    // 输出信息
    @TableField("output_file_path")
    private String outputFilePath;

    @TableField("output_file_name")
    private String outputFileName;

    // 执行信息
    @TableField("progress_percentage")
    private Integer progressPercentage;

    @TableField("error_message")
    private String errorMessage;

    @TableField("python_process_id")
    private String pythonProcessId;

    // 时间信息
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("estimated_completion")
    private LocalDateTime estimatedCompletion;

    // 其他
    @TableField("created_by")
    private String createdBy;

    @TableField("remark")
    private String remark;

    public enum TaskStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}