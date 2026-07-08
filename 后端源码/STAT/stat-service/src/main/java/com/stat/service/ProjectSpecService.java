package com.stat.service;

import com.stat.common.dto.ProjectSpecDTO;
import com.stat.dal.po.ProjectSpecPO;

import java.util.List;
import java.util.Map;

/**
 * 项目Spec数据服务接口
 */
public interface ProjectSpecService {
    
    /**
     * 解析并保存项目Spec文件
     * @param filePath Spec文件路径
     * @param projectId 项目ID
     * @return 解析成功的记录数量
     */
    int parseAndSaveProjectSpec(String filePath, String projectId);
    
    /**
     * 批量保存项目Spec数据
     * @param projectSpecs Spec数据列表
     * @return 保存成功的记录数量
     */
    int batchSaveProjectSpec(List<ProjectSpecPO> projectSpecs);
    
    /**
     * 根据项目ID删除所有Spec数据
     * @param projectId 项目ID
     * @return 删除的记录数量
     */
    int deleteByProjectId(String projectId);
    
    /**
     * 根据项目ID查询Spec数据列表
     * @param projectId 项目ID
     * @return Spec数据列表
     */
    List<ProjectSpecDTO> getByProjectId(String projectId);
    
    /**
     * 根据项目ID和域查询Spec数据列表
     * @param projectId 项目ID
     * @param domain 域名
     * @return Spec数据列表
     */
    List<ProjectSpecDTO> getByProjectIdAndDomain(String projectId, String domain);
    
    /**
     * 分页查询项目Spec数据
     * @param params 查询参数
     * @return Spec数据列表
     */
    List<ProjectSpecDTO> getByPage(Map<String, Object> params);
    
    /**
     * 查询项目Spec数据总数
     * @param params 查询参数
     * @return 总数
     */
    int countByParams(Map<String, Object> params);
    
    /**
     * 获取项目中所有不同的域列表
     * @param projectId 项目ID
     * @return 域名列表
     */
    List<String> getDistinctDomainsByProjectId(String projectId);
    
    /**
     * 获取项目域统计信息
     * @param projectId 项目ID
     * @return 域统计信息
     */
    List<Map<String, Object>> getDomainStatsByProjectId(String projectId);
    
    /**
     * 检查项目变量是否已存在
     * @param projectId 项目ID
     * @param domain 域名
     * @param variable 变量名
     * @return 是否存在
     */
    boolean checkVariableExists(String projectId, String domain, String variable);
    
    /**
     * 获取CDISC Submission Value对应的Domain映射
     * @param projectId 项目ID，为null时查询所有项目
     * @return Map<String, List<String>> 变量名对应的域列表映射
     */
    Map<String, List<String>> getVariableDomainMapping(String projectId);
    
    /**
     * 获取VCD到域的映射 - 基于codelist表的VCD获取对应域
     * @param projectId 项目ID，为null时查询所有项目
     * @return Map<String, List<String>> VCD对应的域列表映射
     */
    Map<String, List<String>> getVcdDomainMapping(String projectId);

    /**
     * 将项目Spec数据导出为Excel文件，每个domain一个sheet
     * @param projectId 项目ID
     * @param outputDir 输出目录路径
     * @return 生成的Excel文件绝对路径
     */
    String exportProjectSpecToExcel(String projectId, String outputDir);

    ProjectSpecDTO getById(Long id);

    ProjectSpecDTO addVariable(ProjectSpecPO po);

    ProjectSpecDTO updateVariable(ProjectSpecPO po);

    int deleteVariable(Long id);

    /**
     * 将数据库中的 Spec 数据同步导出为 spec_synced_{projectId}.xlsx，
     * 供下游 Python 脚本（VLM提取、Pages提取、Define生成）统一读取。
     * @param projectId 项目ID
     * @param outputDir 输出目录路径
     * @return 生成的同步文件绝对路径，无数据时返回 null
     */
    String syncSpecToFile(String projectId, String outputDir);
}