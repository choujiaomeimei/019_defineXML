package com.stat.dal.mapper;

import com.stat.dal.po.CodelistDataPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CodelistDataMapper {
    
    int insert(CodelistDataPO codelistData);
    
    int updateById(CodelistDataPO codelistData);
    
    int deleteById(@Param("id") Long id);
    
    int deleteByProjectId(@Param("projectId") String projectId, @Param("username") String username);
    
    CodelistDataPO selectById(@Param("id") Long id);
    
    List<CodelistDataPO> selectByProjectId(@Param("projectId") String projectId, @Param("username") String username);
    
    List<CodelistDataPO> selectByProjectIdAndVcd(
        @Param("projectId") String projectId, 
        @Param("vcd") String vcd,
        @Param("username") String username
    );
    
    List<CodelistDataPO> selectByPage(@Param("params") Map<String, Object> params);
    
    int countByParams(@Param("params") Map<String, Object> params);
    
    int batchUpdateSortOrder(@Param("list") List<Map<String, Object>> sortOrderList);
    
    List<String> selectDistinctVcdsByProjectId(@Param("projectId") String projectId, @Param("username") String username);
}
