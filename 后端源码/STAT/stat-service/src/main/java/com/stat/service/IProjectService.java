package com.stat.service;

import com.stat.common.dto.ProjectDTO;
import com.stat.common.entity.Project;

import java.util.List;

/**
 * 项目服务接口
 */
public interface IProjectService {

    /**
     * 删除项目
     * @param projectId 项目ID
     * @return 是否成功
     */
    boolean deleteProject(String projectId);

    /**
     * 删除项目（支持删除文件夹）
     * @param projectId 项目ID
     * @param deleteFiles 是否删除项目文件夹
     * @return 是否成功
     */
    boolean deleteProject(String projectId, boolean deleteFiles);

    /**
     * 创建项目
     * @param projectId 项目ID
     * @param projectName 项目名称
     * @param protocolNumber 方案编号
     * @param protocolName 方案名称
     * @param protocolVersion 方案版本
     * @param protocolDateStr 方案版本日期
     * @param sponsor 申办方/赞助方
     * @param standardTypes 标准类型列表（SDTM、ADAM、SEND）
     * @param username 用户名
     * @return 创建的项目
     */
    Project createProject(String projectId, String projectName, String protocolNumber,
                          String protocolName, String protocolVersion, String protocolDateStr,
                          String sponsor, List<String> standardTypes, String username);

    /**
     * 确保项目存在，如果不存在则创建
     * @param projectId 项目ID
     * @param projectName 项目名称（可选）
     * @return 项目信息
     */
    Project ensureProjectExists(String projectId, String projectName);

    /**
     * 获取项目信息
     * @param projectId 项目ID
     * @return 项目信息
     */
    Project getProject(String projectId);


    /**
     * 获取项目DTO信息（包含文件统计）
     * @param projectId 项目ID
     * @return 项目DTO
     */
    ProjectDTO getProjectDTO(String projectId);


    /**
     * 列出所有活跃项目
     * @return 项目列表
     */
    List<ProjectDTO> listActiveProjects();

    /**
     * 列出所有项目
     * @return 项目列表
     */
    List<ProjectDTO> listAllProjects();

    /**
     * 根据用户名列出活跃项目
     * @param username 用户名
     * @return 项目列表
     */
    List<ProjectDTO> listActiveProjectsByUsername(String username);

    /**
     * 根据用户名列出所有项目
     * @param username 用户名
     * @return 项目列表
     */
    List<ProjectDTO> listProjectsByUsername(String username);

    /**
     * 更新项目信息
     * @param projectId 项目ID
     * @param projectName 项目名称
     * @param description 项目描述
     * @return 更新后的项目
     */
    Project updateProject(String projectId, String projectName, String description);

    /**
     * 完整更新项目信息
     * @param projectId 项目ID
     * @param projectName 项目名称
     * @param protocolNumber 方案编号
     * @param protocolName 方案名称
     * @param protocolVersion 方案版本
     * @param protocolDateStr 方案日期
     * @param standardType 标准类型
     * @param sponsor 申办方
     * @return 更新后的项目
     */
    Project updateProjectFull(String projectId, String projectName, String protocolNumber, 
                             String protocolName, String protocolVersion, String protocolDateStr, 
                             String standardType, String sponsor,
                             String encoding, String language, String standardVersion);

    /**
     * 归档项目
     * @param projectId 项目ID
     * @return 是否成功
     */
    boolean archiveProject(String projectId);

    /**
     * 激活项目
     * @param projectId 项目ID
     * @return 是否成功
     */
    boolean activateProject(String projectId);

    /**
     * 创建项目文件夹结构
     * @param projectId 项目ID
     * @return 是否成功
     */
    boolean createProjectDirectories(String projectId);

    /**
     * 获取项目文件路径
     * @param projectId 项目ID
     * @param fileType 文件类型（uploads, define, output）
     * @param subType 子类型（p21-spec, xpt, project-spec, acrf等）
     * @return 文件路径
     */
    String getProjectPath(String projectId, String fileType, String subType);

    /**
     * 检查项目是否存在
     * @param projectId 项目ID
     * @return 是否存在
     */
    boolean projectExists(String projectId);

    /**
     * 检查用户是否有项目权限
     * @param projectId 项目ID
     * @param username 用户名
     * @return 是否有权限
     */
    boolean hasProjectPermission(String projectId, String username);
} 