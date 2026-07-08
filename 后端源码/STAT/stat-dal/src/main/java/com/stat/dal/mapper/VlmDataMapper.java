package com.stat.dal.mapper;

import com.stat.dal.po.VlmDataPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface VlmDataMapper {
    
    int insert(VlmDataPO vlmData);
    
    int updateById(VlmDataPO vlmData);
    
    int deleteById(@Param("id") Long id);
    
    int deleteByProjectId(@Param("projectId") String projectId, @Param("username") String username);
    
    VlmDataPO selectById(@Param("id") Long id);
    
    List<VlmDataPO> selectByProjectId(@Param("projectId") String projectId, @Param("username") String username);
    
    List<VlmDataPO> selectByProjectIdAndDataset(
        @Param("projectId") String projectId, 
        @Param("dataset") String dataset,
        @Param("username") String username
    );
    
    List<VlmDataPO> selectByPage(@Param("params") Map<String, Object> params);
    
    int countByParams(@Param("params") Map<String, Object> params);
    
    int batchUpdateSortOrder(@Param("list") List<Map<String, Object>> sortOrderList);
    
    List<String> selectDistinctDatasetsByProjectId(@Param("projectId") String projectId, @Param("username") String username);
}
