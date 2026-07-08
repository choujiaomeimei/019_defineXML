package com.stat.service;

import com.stat.common.dto.VlmDataDTO;
import com.stat.common.result.PageCommonResult;

import java.util.List;
import java.util.Map;

/**
 * VLM数据服务接口
 */
public interface IVlmDataService {
    
    /**
     * 新增VLM数据
     */
    boolean addVlmData(VlmDataDTO vlmDataDTO);
    
    /**
     * 更新VLM数据
     */
    boolean updateVlmData(VlmDataDTO vlmDataDTO);
    
    /**
     * 删除VLM数据
     */
    boolean deleteVlmData(Long id);
    
    /**
     * 根据ID查询VLM数据
     */
    VlmDataDTO getVlmDataById(Long id);
    
    /**
     * 根据项目ID查询VLM数据列表
     */
    List<VlmDataDTO> getVlmDataByProjectId(String projectId);
    
    /**
     * 根据项目ID和数据集查询VLM数据列表
     */
    List<VlmDataDTO> getVlmDataByProjectIdAndDataset(String projectId, String dataset);
    
    /**
     * 分页查询VLM数据
     */
    PageCommonResult<VlmDataDTO> getVlmDataByPage(Map<String, Object> params);
    
    /**
     * 批量更新排序顺序
     */
    boolean batchUpdateSortOrder(List<Map<String, Object>> sortOrderList);
    
    /**
     * 获取项目中所有不同的数据集列表
     */
    List<String> getDistinctDatasetsByProjectId(String projectId);
    
    /**
     * 根据项目ID删除所有VLM数据
     */
    boolean deleteByProjectId(String projectId);
}