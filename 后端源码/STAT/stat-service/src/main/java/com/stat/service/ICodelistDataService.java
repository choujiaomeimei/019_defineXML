package com.stat.service;

import com.stat.common.dto.CodelistDataDTO;
import com.stat.common.result.PageCommonResult;

import java.util.List;
import java.util.Map;

/**
 * CodeList数据服务接口
 */
public interface ICodelistDataService {
    
    /**
     * 新增CodeList数据
     */
    boolean addCodelistData(CodelistDataDTO codelistDataDTO);
    
    /**
     * 更新CodeList数据
     */
    boolean updateCodelistData(CodelistDataDTO codelistDataDTO);
    
    /**
     * 删除CodeList数据
     */
    boolean deleteCodelistData(Long id);
    
    /**
     * 根据ID查询CodeList数据
     */
    CodelistDataDTO getCodelistDataById(Long id);
    
    /**
     * 根据项目ID查询CodeList数据列表
     */
    List<CodelistDataDTO> getCodelistDataByProjectId(String projectId);
    
    /**
     * 根据项目ID和VCD查询CodeList数据列表
     */
    List<CodelistDataDTO> getCodelistDataByProjectIdAndVcd(String projectId, String vcd);
    
    /**
     * 分页查询CodeList数据
     */
    PageCommonResult<CodelistDataDTO> getCodelistDataByPage(Map<String, Object> params);
    
    /**
     * 批量更新排序顺序
     */
    boolean batchUpdateSortOrder(List<Map<String, Object>> sortOrderList);
    
    /**
     * 获取项目中所有不同的VCD列表
     */
    List<String> getDistinctVcdsByProjectId(String projectId);
    
    /**
     * 根据项目ID删除所有CodeList数据
     */
    boolean deleteByProjectId(String projectId);
    
    /**
     * 获取VCD与Domain的映射关系
     */
    Map<String, Object> getVcdDomainsMapping(String projectId);
}