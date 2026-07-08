package com.stat.dal.mapper;

import com.stat.dal.po.ProjectSpecPO;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface ProjectSpecMapper {
    
    int insert(ProjectSpecPO projectSpec);
    
    int batchInsert(@Param("list") List<ProjectSpecPO> projectSpecs);
    
    int updateById(ProjectSpecPO projectSpec);
    
    int deleteById(Long id);
    
    int deleteByProjectId(@Param("projectId") String projectId, @Param("username") String username);
    
    ProjectSpecPO selectById(Long id);
    
    List<ProjectSpecPO> selectByProjectId(@Param("projectId") String projectId, @Param("username") String username);
    
    List<ProjectSpecPO> selectByProjectIdAndDomain(@Param("projectId") String projectId, 
                                                    @Param("domain") String domain,
                                                    @Param("username") String username);
    
    List<ProjectSpecPO> selectByPage(Map<String, Object> params);
    
    Integer countByParams(Map<String, Object> params);
    
    List<String> selectDistinctDomainsByProjectId(@Param("projectId") String projectId, @Param("username") String username);
    
    List<Map<String, Object>> selectDomainStatsByProjectId(@Param("projectId") String projectId, @Param("username") String username);
    
    int checkVariableExists(@Param("projectId") String projectId, 
                           @Param("domain") String domain, 
                           @Param("variable") String variable,
                           @Param("username") String username);
    
    List<Map<String, Object>> getVariableDomainMapping(@Param("projectId") String projectId, @Param("username") String username);
    
    List<Map<String, Object>> getVcdDomainMapping(@Param("projectId") String projectId, @Param("username") String username);
}
