package com.stat.service.impl;

import com.stat.service.IProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 项目初始化服务
 * 系统启动时确保模板项目存在
 */
@Service
public class ProjectInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProjectInitializationService.class);

    @Resource
    private IProjectService projectService;

    private static final String TEMPLATE_PROJECT_ID = "P000_Demo";
    private static final String DEFAULT_PROJECT_ID = "DEFAULT";

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始初始化项目系统...");

        try {
            // 确保默认项目存在
            if (!projectService.projectExists(DEFAULT_PROJECT_ID)) {
                logger.info("创建默认项目: {}", DEFAULT_PROJECT_ID);
                projectService.createProject(DEFAULT_PROJECT_ID, "默认项目",
                    null, null, null, null, null, null, "system");
            }

            // 确保模板项目存在
            if (!projectService.projectExists(TEMPLATE_PROJECT_ID)) {
                logger.info("创建模板项目: {}", TEMPLATE_PROJECT_ID);
                projectService.createProject(TEMPLATE_PROJECT_ID, "模板项目",
                    null, null, null, null, null, null, "system");
            } else {
                logger.info("模板项目已存在: {}", TEMPLATE_PROJECT_ID);
                // 确保模板项目的文件夹结构存在
                projectService.createProjectDirectories(TEMPLATE_PROJECT_ID);
            }

            logger.info("项目系统初始化完成");

        } catch (Exception e) {
            logger.error("项目系统初始化失败", e);
            // 不抛出异常，避免影响系统启动
        }
    }
}