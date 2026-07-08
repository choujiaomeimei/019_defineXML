package com.stat.common.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "项目ID不能为空")
    @Size(max = 50, message = "项目ID不能超过50个字符")
    private String projectId;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称不能超过200个字符")
    private String projectName;

    private String protocolNumber;

    @NotBlank(message = "方案名称不能为空")
    private String protocolName;

    private String protocolVersion;
    private String protocolDate;
    private String sponsor;

    @NotEmpty(message = "请至少选择一种标准类型")
    private List<String> standardTypes;

    private String username;
}
