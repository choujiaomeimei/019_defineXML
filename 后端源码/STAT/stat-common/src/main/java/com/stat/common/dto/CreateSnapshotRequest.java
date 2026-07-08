package com.stat.common.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CreateSnapshotRequest {

    @NotBlank(message = "项目ID不能为空")
    private String projectId;

    @NotBlank(message = "版本名称不能为空")
    private String snapshotName;

    private String description;
    private String username;
    private String versionLabel;
}
