package com.stat.service.impl;

import com.stat.common.dto.ProjectDTO;
import com.stat.common.entity.Project;
import com.stat.dal.mapper.*;
import com.stat.service.IProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目服务实现类
 */
@Service
public class ProjectServiceImpl implements IProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private SasP21SpecUploadMapper sasP21SpecUploadMapper;

    @Resource
    private SasXptUploadMapper sasXptUploadMapper;

    @Resource
    private SasProjectSpecUploadMapper sasProjectSpecUploadMapper;

    @Resource
    private SasAcrfUploadMapper sasAcrfUploadMapper;

    @Value("${app.projects.base-path:E:/JAVAPROJ/008_defineXML/projects}")
    private String projectsBasePath;

    private static final String TEMPLATE_PROJECT_ID = "P000_Demo";

    @Override
    @Transactional
    public Project createProject(String projectId, String projectName, String protocolNumber,
                                String protocolName, String protocolVersion, String protocolDateStr,
                                String sponsor, List<String> standardTypes, String username) {
        logger.info("创建项目: projectId={}, projectName={}, standardTypes={}, username={}",
                    projectId, projectName, standardTypes, username);

        // 检查项目是否已存在
        if (projectExists(projectId)) {
            throw new RuntimeException("项目ID已存在: " + projectId);
        }

        // 创建项目实体
        Project project = new Project(projectId, projectName);
        project.setProtocolNumber(protocolNumber);
        project.setProtocolName(protocolName);
        project.setProtocolVersion(protocolVersion);

        // 处理日期
        if (protocolDateStr != null && !protocolDateStr.trim().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date = LocalDate.parse(protocolDateStr, formatter);
                project.setProtocolDate(date);
            } catch (Exception e) {
                logger.warn("日期格式解析失败: {}", protocolDateStr);
            }
        }

        project.setSponsor(sponsor);
        
        // 将标准类型列表转换为逗号分隔的字符串保存到数据库
        if (standardTypes != null && !standardTypes.isEmpty()) {
            String standardTypeStr = String.join(",", standardTypes);
            project.setStandardType(standardTypeStr);
        }
        
        project.setUsername(username);

        // 保存到数据库
        int result = projectMapper.insert(project);
        if (result <= 0) {
            throw new RuntimeException("创建项目失败");
        }

        // 根据标准类型创建项目文件夹结构
        if (!createProjectDirectoriesWithStandardTypes(projectId, standardTypes)) {
            logger.warn("创建项目文件夹结构失败，但项目已保存到数据库: {}", projectId);
        }

        logger.info("项目创建成功: {}", project);
        return project;
    }

    @Override
    public Project ensureProjectExists(String projectId, String projectName) {
        Project project = getProject(projectId);
        if (project == null) {
            // 如果项目名称为空，使用项目ID作为名称
            if (projectName == null || projectName.trim().isEmpty()) {
                projectName = projectId;
            }
            project = createProject(projectId, projectName, null, null, null, null, null, 
                                   java.util.Arrays.asList("SDTM"), "system");
        }
        return project;
    }

    @Override
    public Project getProject(String projectId) {
        return projectMapper.selectByProjectId(projectId);
    }

    @Override
    public ProjectDTO getProjectDTO(String projectId) {
        Project project = getProject(projectId);
        if (project == null) {
            return null;
        }

        ProjectDTO dto = convertToDTO(project);
        // 填充文件统计信息
        fillFileStats(dto, project.getProjectId());
        return dto;
    }

    @Override
    public List<ProjectDTO> listActiveProjects() {
        List<Project> projects = projectMapper.selectActiveProjects();
        return projects.stream()
                .map(this::convertToDTO)
                .peek(dto -> fillFileStats(dto, dto.getProjectId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectDTO> listAllProjects() {
        List<Project> projects = projectMapper.selectAllValid();
        return projects.stream()
                .map(this::convertToDTO)
                .peek(dto -> fillFileStats(dto, dto.getProjectId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectDTO> listActiveProjectsByUsername(String username) {
        List<Project> projects = projectMapper.selectActiveProjectsByUsername(username);
        return projects.stream()
                .map(this::convertToDTO)
                .peek(dto -> fillFileStats(dto, dto.getProjectId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectDTO> listProjectsByUsername(String username) {
        List<Project> projects = projectMapper.selectProjectsByUsername(username);
        return projects.stream()
                .map(this::convertToDTO)
                .peek(dto -> fillFileStats(dto, dto.getProjectId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Project updateProject(String projectId, String projectName, String description) {
        Project project = getProject(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在: " + projectId);
        }

        project.setProjectName(projectName);
        project.setUpdatedTime(LocalDateTime.now());

        int result = projectMapper.updateById(project);
        if (result <= 0) {
            throw new RuntimeException("更新项目失败");
        }

        return project;
    }

    @Override
    @Transactional
    public Project updateProjectFull(String projectId, String projectName, String protocolNumber, 
                                   String protocolName, String protocolVersion, String protocolDateStr, 
                                   String standardType, String sponsor,
                                   String encoding, String language, String standardVersion) {
        Project project = getProject(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在: " + projectId);
        }

        project.setProjectName(projectName);
        project.setProtocolNumber(protocolNumber);
        project.setProtocolName(protocolName);
        project.setProtocolVersion(protocolVersion);
        project.setStandardType(standardType);
        project.setSponsor(sponsor);
        project.setEncoding(encoding);
        project.setLanguage(language);
        project.setStandardVersion(standardVersion);
        project.setUpdatedTime(LocalDateTime.now());

        // 处理日期
        if (protocolDateStr != null && !protocolDateStr.trim().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate date = LocalDate.parse(protocolDateStr, formatter);
                project.setProtocolDate(date);
            } catch (Exception e) {
                logger.warn("日期格式解析失败: {}", protocolDateStr);
            }
        }

        int result = projectMapper.updateById(project);
        if (result <= 0) {
            throw new RuntimeException("更新项目失败");
        }

        logger.info("项目完整更新成功: {}", project);
        return project;
    }

    @Override
    @Transactional
    public boolean archiveProject(String projectId) {
        return projectMapper.archiveProject(projectId) > 0;
    }

    @Override
    @Transactional
    public boolean activateProject(String projectId) {
        return projectMapper.activateProject(projectId) > 0;
    }

    @Override
    @Transactional
    public boolean deleteProject(String projectId) {
        return deleteProject(projectId, false);
    }

    @Override
    @Transactional
    public boolean deleteProject(String projectId, boolean deleteFiles) {
        logger.warn("删除项目: {}, 删除文件夹: {}", projectId, deleteFiles);
        
        // 删除数据库记录
        boolean dbDeleted = projectMapper.deleteByProjectId(projectId) > 0;
        
        if (!dbDeleted) {
            logger.error("删除项目数据库记录失败: {}", projectId);
            return false;
        }
        
        // 如果需要删除文件夹
        if (deleteFiles) {
            try {
                Path projectDir = Paths.get(projectsBasePath, projectId);
                if (Files.exists(projectDir)) {
                    logger.info("开始删除项目文件夹: {}", projectDir);
                    deleteDirectory(projectDir);
                    logger.info("项目文件夹删除成功: {}", projectDir);
                } else {
                    logger.warn("项目文件夹不存在: {}", projectDir);
                }
            } catch (IOException e) {
                logger.error("删除项目文件夹失败: " + projectId, e);
                // 文件夹删除失败不影响数据库删除的结果
            }
        }
        
        return true;
    }

    /**
     * 递归删除目录及其内容
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // 倒序，确保先删除文件再删除目录
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.debug("删除文件/文件夹: {}", path);
                    } catch (IOException e) {
                        logger.error("删除文件/文件夹失败: " + path, e);
                    }
                });
        }
    }


    @Override
    public boolean createProjectDirectories(String projectId) {
        try {
            Path projectDir = Paths.get(projectsBasePath, projectId);
            Path templateDir = Paths.get(projectsBasePath, TEMPLATE_PROJECT_ID);

            // 如果模板项目存在且当前项目不是模板项目，则从模板复制
            if (!projectId.equals(TEMPLATE_PROJECT_ID) && Files.exists(templateDir)) {
                logger.info("从模板项目复制文件夹结构: {} -> {}", TEMPLATE_PROJECT_ID, projectId);
                copyDirectoryStructure(templateDir, projectDir);
                logger.info("从模板项目复制完成: {}", projectId);
                return true;
            } else {
                // 手动创建文件夹结构（用于模板项目或模板不存在的情况）
                return createDirectoriesManually(projectDir, projectId);
            }

        } catch (Exception e) {
            logger.error("创建项目文件夹结构失败: {}", projectId, e);
            return false;
        }
    }

    /**
     * 手动创建项目文件夹结构
     */
    private boolean createDirectoriesManually(Path projectDir, String projectId) throws IOException {
        // 创建项目主目录
        Files.createDirectories(projectDir);
        logger.info("创建项目目录: {}", projectDir);

        // 创建uploads子目录
        String[] uploadDirs = {"p21-spec", "xpt", "project-spec", "acrf"};
        for (String dir : uploadDirs) {
            Path uploadDir = projectDir.resolve("uploads").resolve(dir);
            Files.createDirectories(uploadDir);
            logger.debug("创建上传目录: {}", uploadDir);
        }

        // 创建define子目录
        String[] defineDirs = {"p21空spec", "sdtm define package", "项目Spec", "SDTM注释CRF"};
        for (String dir : defineDirs) {
            Path defineDir = projectDir.resolve("define").resolve(dir);
            Files.createDirectories(defineDir);
            logger.debug("创建define目录: {}", defineDir);
        }

        // 创建output子目录
        String[] outputDirs = {"define", "reports"};
        for (String dir : outputDirs) {
            Path outputDir = projectDir.resolve("output").resolve(dir);
            Files.createDirectories(outputDir);
            logger.debug("创建输出目录: {}", outputDir);
        }

        logger.info("项目文件夹结构手动创建成功: {}", projectId);
        return true;
    }

    /**
     * 复制目录结构和文件
     */
    private void copyDirectoryStructure(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));

                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                    logger.debug("创建目录: {}", targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.debug("复制文件: {} -> {}", sourcePath, targetPath);
                }
            } catch (IOException e) {
                logger.warn("复制失败: {} -> {}", sourcePath, target.resolve(source.relativize(sourcePath)), e);
            }
        });
    }

    @Override
    public String getProjectPath(String projectId, String fileType, String subType) {
        Path projectDir = Paths.get(projectsBasePath, projectId);

        if (subType != null && !subType.isEmpty()) {
            return projectDir.resolve(fileType).resolve(subType).toString();
        } else {
            return projectDir.resolve(fileType).toString();
        }
    }

    @Override
    public boolean projectExists(String projectId) {
        return projectMapper.existsByProjectId(projectId) > 0;
    }

    /**
     * 填充文件统计信息
     */
    private void fillFileStats(ProjectDTO dto, String projectId) {
        try {
            ProjectDTO.FileStats stats = new ProjectDTO.FileStats();

            // 这里可以根据实际需要查询各类文件的数量
            // 暂时设置为0，后续可以扩展
            stats.setP21SpecCount(0);
            stats.setXptCount(0);
            stats.setProjectSpecCount(0);
            stats.setAcrfCount(0);

            dto.setFileStats(stats);
        } catch (Exception e) {
            logger.warn("获取项目文件统计失败: {}", projectId, e);
            dto.setFileStats(new ProjectDTO.FileStats());
        }
    }

    @Override
    public boolean hasProjectPermission(String projectId, String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        Project project = getProject(projectId);
        if (project == null) {
            return false;
        }

        // 检查用户名是否匹配
        return username.equals(project.getUsername());
    }

    /**
     * 根据标准类型创建项目文件夹结构
     * @param projectId 项目ID
     * @param standardTypes 标准类型列表（SDTM、ADAM、SEND）
     * @return 是否成功
     */
    public boolean createProjectDirectoriesWithStandardTypes(String projectId, List<String> standardTypes) {
        try {
            Path projectDir = Paths.get(projectsBasePath, projectId);
            
            // 创建项目主目录
            Files.createDirectories(projectDir);
            logger.info("创建项目目录: {}", projectDir);

            // 为每个标准类型创建子文件夹并复制模板内容
            for (String standardType : standardTypes) {
                String templateProjectId = TEMPLATE_PROJECT_ID + "_" + standardType;
                Path templateDir = Paths.get(projectsBasePath, templateProjectId);
                Path targetStandardDir = projectDir.resolve(standardType);

                if (Files.exists(templateDir)) {
                    logger.info("从模板 {} 复制文件夹结构到 {}/{}", templateProjectId, projectId, standardType);
                    copyDirectoryStructure(templateDir, targetStandardDir);
                    logger.info("标准类型 {} 的文件夹复制完成", standardType);
                } else {
                    logger.warn("模板项目不存在: {}，跳过复制", templateProjectId);
                    // 如果模板不存在，创建空的标准类型文件夹
                    createStandardTypeDirectoriesManually(targetStandardDir, standardType);
                }
            }

            logger.info("项目文件夹结构创建成功: {}, 标准类型: {}", projectId, standardTypes);
            return true;

        } catch (Exception e) {
            logger.error("创建项目文件夹结构失败: {}, 标准类型: {}", projectId, standardTypes, e);
            return false;
        }
    }

    /**
     * 手动创建标准类型文件夹结构（当模板不存在时）
     */
    private boolean createStandardTypeDirectoriesManually(Path standardDir, String standardType) throws IOException {
        // 创建标准类型目录
        Files.createDirectories(standardDir);
        logger.info("创建标准类型目录: {} - {}", standardType, standardDir);

        // 创建基本子目录结构
        String[] uploadDirs = {"p21-spec", "xpt", "project-spec", "acrf"};
        for (String dir : uploadDirs) {
            Path uploadDir = standardDir.resolve("uploads").resolve(dir);
            Files.createDirectories(uploadDir);
            logger.debug("创建上传目录: {}", uploadDir);
        }

        // 创建define子目录
        String[] defineDirs = {"p21空spec", "sdtm define package", "项目Spec", "SDTM注释CRF"};
        for (String dir : defineDirs) {
            Path defineDir = standardDir.resolve("define").resolve(dir);
            Files.createDirectories(defineDir);
            logger.debug("创建define目录: {}", defineDir);
        }

        // 创建output子目录
        String[] outputDirs = {"define", "reports"};
        for (String dir : outputDirs) {
            Path outputDir = standardDir.resolve("output").resolve(dir);
            Files.createDirectories(outputDir);
            logger.debug("创建输出目录: {}", outputDir);
        }

        logger.info("标准类型文件夹结构手动创建成功: {}", standardType);
        return true;
    }

    /**
     * 转换为DTO
     */
    private ProjectDTO convertToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        BeanUtils.copyProperties(project, dto);
        dto.setStatus(project.getStatus()); // 这会自动设置statusText
        return dto;
    }
}