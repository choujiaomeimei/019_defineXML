package com.stat.web.controller;

import com.stat.common.entity.Project;
import com.stat.common.result.CommonResult;
import com.stat.service.IProjectService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 只读 Study API：从 project 表派生研究级元数据。
 */
@RestController
@RequestMapping("/study")
public class StudyController {

    private static final Logger logger = LoggerFactory.getLogger(StudyController.class);

    @Resource
    private IProjectService projectService;

    @GetMapping("/data")
    public CommonResult<Map<String, Object>> getStudyData(@RequestParam("projectId") String projectId) {
        logger.info("获取 Study 元数据, projectId: {}", projectId);

        try {
            Project project = projectService.getProject(projectId);
            if (project == null) {
                return CommonResult.fail("404", "项目不存在");
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("StudyName", project.getProjectName());
            data.put("StudyDescription", project.getProtocolName());
            data.put("ProtocolName", project.getProtocolNumber());
            data.put("StandardName", project.getStandardType());
            String standardVersion = project.getStandardVersion();
            data.put("StandardVersion", standardVersion != null ? standardVersion : "");
            String language = project.getLanguage();
            data.put("Language", language != null ? language : "CN");

            return CommonResult.success(data);
        } catch (Exception e) {
            logger.error("获取 Study 元数据失败", e);
            return CommonResult.fail("500", "获取 Study 数据失败: " + e.getMessage());
        }
    }
}
