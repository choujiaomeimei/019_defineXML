package com.stat.common.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

@Data
public class P21GenerationRequestDTO {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @NotNull(message = "项目Spec文件不能为空")
    private MultipartFile specFile;

    @NotNull(message = "P21模板文件不能为空")
    private MultipartFile templateFile;

    @NotBlank(message = "协议号不能为空")
    private String protocolId;

    @NotBlank(message = "研究标题不能为空")
    private String studyTitle;

    // 其他配置参数
    private String ctSheetName; // CT表单名称
    private String outputDir;   // 输出目录
    private String remark;      // 备注信息

    // 用户信息
    private String createdBy;
}