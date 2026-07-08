package com.stat.web.controller;

import com.stat.common.entity.ProjectMember;
import com.stat.common.result.CommonResult;
import com.stat.common.dto.ProjectDTO;
import com.stat.common.entity.Project;
import com.stat.common.security.UserContext;
import com.stat.common.util.PasswordUtil;
import com.stat.dal.mapper.ProjectMemberMapper;
import com.stat.service.IProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/project-management")
public class ProjectManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectManagementController.class);

    @Resource
    private IProjectService projectService;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadBasePath;

    @Value("${app.projects.base-path:C:/Project_Web/019_defineXML/projects}")
    private String projectsBasePath;

    @PostMapping("/create")
    public CommonResult<ProjectDTO> createProject(@RequestBody Map<String, Object> request,
                                                  @RequestHeader(value = "username", required = false) String headerUsername) {
        String username = UserContext.getUsername();
        if (username == null) {
            username = headerUsername;
        }
        logger.info("接收创建项目请求: {}, 用户: {}", request, username);

        try {
            String projectId = (String) request.get("projectId");
            String projectName = (String) request.get("projectName");
            String protocolNumber = (String) request.get("protocolNumber");
            String protocolName = (String) request.get("protocolName");
            String protocolVersion = (String) request.get("protocolVersion");
            String protocolDateStr = (String) request.get("protocolDate");
            String sponsor = (String) request.get("sponsor");

            @SuppressWarnings("unchecked")
            List<String> standardTypes = (List<String>) request.get("standardTypes");

            String requestUsername = (String) request.get("username");
            if (requestUsername != null && !requestUsername.trim().isEmpty()) {
                username = requestUsername;
            }

            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.fail("400", "项目ID不能为空");
            }
            if (projectName == null || projectName.trim().isEmpty()) {
                return CommonResult.fail("400", "项目名称不能为空");
            }
            if (protocolName == null || protocolName.trim().isEmpty()) {
                return CommonResult.fail("400", "方案名称不能为空");
            }
            if (username == null || username.trim().isEmpty()) {
                return CommonResult.fail("401", "用户未登录");
            }
            if (standardTypes == null || standardTypes.isEmpty()) {
                return CommonResult.fail("400", "请至少选择一种标准类型");
            }

            Project project = projectService.createProject(
                projectId, projectName, protocolNumber, protocolName,
                protocolVersion, protocolDateStr, sponsor, standardTypes, username);

            // Auto-register creator as project owner
            ProjectMember member = new ProjectMember();
            member.setProjectId(project.getProjectId());
            member.setUsername(username);
            member.setRole("owner");
            projectMemberMapper.insert(member);

            ProjectDTO dto = projectService.getProjectDTO(project.getProjectId());

            logger.info("项目创建成功: {}, 用户: {}", project.getProjectId(), username);
            return CommonResult.success(dto);

        } catch (RuntimeException e) {
            logger.error("创建项目失败: {}", e.getMessage());
            return CommonResult.fail("400", e.getMessage());
        } catch (Exception e) {
            logger.error("创建项目异常", e);
            return CommonResult.fail("500", "创建项目失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目信息
     */
    @GetMapping("/info")
    public CommonResult<ProjectDTO> getProjectInfo(@RequestParam("projectId") String projectId) {
        logger.info("获取项目信息: {}", projectId);

        try {
            ProjectDTO dto = projectService.getProjectDTO(projectId);
            if (dto == null) {
                return CommonResult.fail("404", "项目不存在");
            }

            return CommonResult.success(dto);

        } catch (Exception e) {
            logger.error("获取项目信息失败", e);
            return CommonResult.fail("500", "获取项目信息失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有活跃项目
     */
    @GetMapping("/list/active")
    public CommonResult<List<ProjectDTO>> listActiveProjects() {
        logger.info("获取活跃项目列表");

        try {
            List<ProjectDTO> projects = projectService.listActiveProjects();
            logger.info("获取到 {} 个活跃项目", projects.size());
            return CommonResult.success(projects);

        } catch (Exception e) {
            logger.error("获取活跃项目列表失败", e);
            return CommonResult.fail("500", "获取项目列表失败: " + e.getMessage());
        }
    }

    /**
     * 列出当前用户的活跃项目
     */
    @GetMapping("/list/active/my")
    public CommonResult<List<ProjectDTO>> listMyActiveProjects(@RequestHeader(value = "username", required = false) String headerUsername) {
        String username = UserContext.getUsername();
        if (username == null) {
            username = headerUsername;
        }
        logger.info("获取用户活跃项目列表: {}", username);

        try {
            if (username == null || username.trim().isEmpty()) {
                return CommonResult.fail("401", "用户未登录");
            }

            List<ProjectDTO> projects = projectService.listActiveProjectsByUsername(username);
            logger.info("获取到用户 {} 的 {} 个活跃项目", username, projects.size());
            return CommonResult.success(projects);

        } catch (Exception e) {
            logger.error("获取用户活跃项目列表失败", e);
            return CommonResult.fail("500", "获取项目列表失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有项目
     */
    @GetMapping("/list/all")
    public CommonResult<List<ProjectDTO>> listAllProjects() {
        logger.info("获取所有项目列表");

        try {
            List<ProjectDTO> projects = projectService.listAllProjects();
            logger.info("获取到 {} 个项目", projects.size());
            return CommonResult.success(projects);

        } catch (Exception e) {
            logger.error("获取所有项目列表失败", e);
            return CommonResult.fail("500", "获取项目列表失败: " + e.getMessage());
        }
    }

    /**
     * 列出当前用户的所有项目
     */
    @GetMapping("/list/all/my")
    public CommonResult<List<ProjectDTO>> listMyAllProjects(@RequestHeader(value = "username", required = false) String headerUsername) {
        String username = UserContext.getUsername();
        if (username == null) {
            username = headerUsername;
        }
        logger.info("获取用户所有项目列表: {}", username);

        try {
            if (username == null || username.trim().isEmpty()) {
                return CommonResult.fail("401", "用户未登录");
            }

            List<ProjectDTO> projects = projectService.listProjectsByUsername(username);
            logger.info("获取到用户 {} 的 {} 个项目", username, projects.size());
            return CommonResult.success(projects);

        } catch (Exception e) {
            logger.error("获取用户所有项目列表失败", e);
            return CommonResult.fail("500", "获取项目列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新项目信息
     */
    @PutMapping("/update")
    public CommonResult<ProjectDTO> updateProject(@RequestBody Map<String, Object> request) {
        logger.info("接收更新项目请求: {}", request);

        try {
            String projectId = (String) request.get("projectId");
            String projectName = (String) request.get("projectName");
            String protocolNumber = (String) request.get("protocolNumber");
            String protocolName = (String) request.get("protocolName");
            String protocolVersion = (String) request.get("protocolVersion");
            String protocolDateStr = (String) request.get("protocolDate");
            String standardType = (String) request.get("standardType");
            String sponsor = (String) request.get("sponsor");
            String encoding = (String) request.get("encoding");
            String language = (String) request.get("language");
            String standardVersion = (String) request.get("standardVersion");

            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.fail("400", "项目ID不能为空");
            }

            Project project = projectService.updateProjectFull(projectId, projectName, protocolNumber, 
                protocolName, protocolVersion, protocolDateStr, standardType, sponsor,
                encoding, language, standardVersion);
            ProjectDTO dto = projectService.getProjectDTO(project.getProjectId());

            logger.info("项目更新成功: {}", project.getProjectId());
            return CommonResult.success(dto);

        } catch (RuntimeException e) {
            logger.error("更新项目失败: {}", e.getMessage());
            return CommonResult.fail("400", e.getMessage());
        } catch (Exception e) {
            logger.error("更新项目异常", e);
            return CommonResult.fail("500", "更新项目失败: " + e.getMessage());
        }
    }

    /**
     * 归档项目
     */
    @PutMapping("/archive")
    public CommonResult<Void> archiveProject(@RequestParam("projectId") String projectId) {
        logger.info("归档项目: {}", projectId);

        try {
            boolean success = projectService.archiveProject(projectId);
            if (success) {
                logger.info("项目归档成功: {}", projectId);
                return CommonResult.success();
            } else {
                return CommonResult.fail("404", "项目不存在或归档失败");
            }

        } catch (Exception e) {
            logger.error("归档项目失败", e);
            return CommonResult.fail("500", "归档项目失败: " + e.getMessage());
        }
    }

    /**
     * 激活项目
     */
    @PutMapping("/activate")
    public CommonResult<Void> activateProject(@RequestParam("projectId") String projectId) {
        logger.info("激活项目: {}", projectId);

        try {
            boolean success = projectService.activateProject(projectId);
            if (success) {
                logger.info("项目激活成功: {}", projectId);
                return CommonResult.success();
            } else {
                return CommonResult.fail("404", "项目不存在或激活失败");
            }

        } catch (Exception e) {
            logger.error("激活项目失败", e);
            return CommonResult.fail("500", "激活项目失败: " + e.getMessage());
        }
    }

    /**
     * 删除项目
     */
    @DeleteMapping("/delete")
    public CommonResult<Void> deleteProject(
            @RequestParam("projectId") String projectId,
            @RequestParam(value = "deleteFiles", required = false, defaultValue = "false") Boolean deleteFiles) {
        logger.info("删除项目: {}, 是否删除文件夹: {}", projectId, deleteFiles);

        try {
            boolean success = projectService.deleteProject(projectId, deleteFiles);
            if (success) {
                String message = deleteFiles ? "项目及文件夹删除成功" : "项目删除成功";
                logger.info(message + ": {}", projectId);
                return CommonResult.success();
            } else {
                return CommonResult.fail("404", "项目不存在或删除失败");
            }

        } catch (Exception e) {
            logger.error("删除项目失败", e);
            return CommonResult.fail("500", "删除项目失败: " + e.getMessage());
        }
    }

    /**
     * 检查项目是否存在
     */
    @GetMapping("/exists")
    public CommonResult<Boolean> projectExists(@RequestParam("projectId") String projectId) {
        try {
            boolean exists = projectService.projectExists(projectId);
            return CommonResult.success(exists);
        } catch (Exception e) {
            logger.error("检查项目存在性失败", e);
            return CommonResult.fail("500", "检查失败: " + e.getMessage());
        }
    }

    /**
     * 确保项目存在（如果不存在则创建）
     */
    @PostMapping("/ensure")
    public CommonResult<ProjectDTO> ensureProjectExists(@RequestBody Map<String, Object> request) {
        try {
            String projectId = (String) request.get("projectId");
            String projectName = (String) request.get("projectName");

            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.fail("400", "项目ID不能为空");
            }

            Project project = projectService.ensureProjectExists(projectId, projectName);
            ProjectDTO dto = projectService.getProjectDTO(project.getProjectId());

            return CommonResult.success(dto);

        } catch (Exception e) {
            logger.error("确保项目存在失败", e);
            return CommonResult.fail("500", "操作失败: " + e.getMessage());
        }
    }

    /**
     * 完整重置项目 —— 清空所有业务数据 + 删除本地文件
     * 等同于运行 reset_project.sql + 清理 uploads/ 和 projects/ 目录
     */
    @PostMapping("/reset-all")
    public CommonResult<Map<String, Object>> resetAll() {
        logger.warn("=== 开始完整项目重置 ===");
        Map<String, Object> result = new java.util.LinkedHashMap<>();

        try {
            // 1. 清空所有业务表数据（按外键依赖顺序）
            String[] tablesToClear = {
                "snapshot_files", "project_snapshot", "file_version_history",
                "file_upload_records",
                "sas_project_spec", "sas_vlm_data", "sas_codelist_data",
                "sas_methods_data", "sas_comments_data", "sas_pages_data",
                "sas_datasets_data",
                "define_sheet_data", "sdrg_content", "sdrg_template",
                "project_member", "project_config"
            };
            for (String table : tablesToClear) {
                try {
                    int count = jdbcTemplate.update("DELETE FROM `" + table + "` WHERE 1=1");
                    jdbcTemplate.execute("ALTER TABLE `" + table + "` AUTO_INCREMENT = 1");
                    result.put("cleared_" + table, count);
                } catch (Exception e) {
                    result.put("skip_" + table, e.getMessage());
                }
            }

            // 2. 重置 user 表 — 使用系统内置 PasswordUtil 生成正确格式密码
            try {
                jdbcTemplate.update("DELETE FROM `user` WHERE 1=1");
                jdbcTemplate.execute("ALTER TABLE `user` AUTO_INCREMENT = 1");
                String encodedPwd = PasswordUtil.encode("123456");
                jdbcTemplate.update(
                    "INSERT INTO `user` (username, password, email) VALUES (?, ?, ?)",
                    "admin", encodedPwd, "admin@system.local"
                );
                result.put("user_reset", "admin / 123456");
            } catch (Exception e) {
                result.put("user_error", e.getMessage());
            }

            // 3. 重置 project 表
            try {
                jdbcTemplate.update("DELETE FROM `project` WHERE 1=1");
                jdbcTemplate.execute("ALTER TABLE `project` AUTO_INCREMENT = 1");
                jdbcTemplate.update(
                    "INSERT INTO `project` (project_id, project_name, username, status, standard_type, created_time, updated_time, deleted) VALUES (?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                    "DEFAULT", "默认项目", "admin", 1, "SDTM"
                );
                jdbcTemplate.update(
                    "INSERT INTO `project` (project_id, project_name, username, status, standard_type, created_time, updated_time, deleted) VALUES (?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                    "P000_Demo", "模板项目", "admin", 1, "SDTM,ADAM"
                );
                result.put("project_reset", 2);
            } catch (Exception e) {
                result.put("project_error", e.getMessage());
            }

            // 4. 重建默认配置和成员
            try {
                jdbcTemplate.update(
                    "INSERT INTO project_config (project_id, username, encoding, language, standard_type, standard_version, ct_version, chinese_standard, english_standard, source_format, configuration, creator) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    "DEFAULT", "admin", "UTF-8", "CN", "SDTM", "SDTM-IG3.2", "CT2021-12-17", true, false, "SAS(XPT)", "SDTM-IG3.2(NMPA)", "admin"
                );
                jdbcTemplate.update(
                    "INSERT INTO project_config (project_id, username, encoding, language, standard_type, standard_version, ct_version, chinese_standard, english_standard, source_format, configuration, creator) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    "P000_Demo", "admin", "UTF-8", "CN", "SDTM", "SDTM-IG3.2", "CT2021-12-17", true, false, "SAS(XPT)", "SDTM-IG3.2(NMPA)", "admin"
                );
                jdbcTemplate.update("INSERT INTO project_member (project_id, username, role) VALUES (?, ?, ?)", "DEFAULT", "admin", "owner");
                jdbcTemplate.update("INSERT INTO project_member (project_id, username, role) VALUES (?, ?, ?)", "P000_Demo", "admin", "owner");
                result.put("config_member_reset", "ok");
            } catch (Exception e) {
                result.put("config_member_error", e.getMessage());
            }

            // 5. 清理本地文件 —— uploads/ 目录
            int uploadFilesDeleted = 0;
            File uploadsDir = new File(uploadBasePath);
            if (uploadsDir.exists() && uploadsDir.isDirectory()) {
                File[] projectDirs = uploadsDir.listFiles();
                if (projectDirs != null) {
                    for (File dir : projectDirs) {
                        uploadFilesDeleted += deleteDirectoryRecursive(dir);
                    }
                }
            }
            result.put("upload_files_deleted", uploadFilesDeleted);

            // 6. 清理本地文件 —— projects/ 目录
            int projectFilesDeleted = 0;
            File projectsDir = new File(projectsBasePath);
            if (projectsDir.exists() && projectsDir.isDirectory()) {
                File[] projectDirs = projectsDir.listFiles();
                if (projectDirs != null) {
                    for (File dir : projectDirs) {
                        projectFilesDeleted += deleteDirectoryRecursive(dir);
                    }
                }
            }
            result.put("project_files_deleted", projectFilesDeleted);

            logger.warn("=== 项目重置完成 === {}", result);
            return CommonResult.success(result);

        } catch (Exception e) {
            logger.error("项目重置失败", e);
            return CommonResult.fail("500", "重置失败: " + e.getMessage());
        }
    }

    private int deleteDirectoryRecursive(File file) {
        int count = 0;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    count += deleteDirectoryRecursive(child);
                }
            }
        }
        if (file.delete()) count++;
        return count;
    }
}