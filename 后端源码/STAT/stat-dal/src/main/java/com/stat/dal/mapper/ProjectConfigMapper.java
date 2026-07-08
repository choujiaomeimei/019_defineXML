package com.stat.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stat.dal.po.ProjectConfigPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProjectConfigMapper extends BaseMapper<ProjectConfigPO> {

    @Select("SELECT * FROM project_config WHERE project_id = #{projectId} LIMIT 1")
    ProjectConfigPO selectByProjectId(@Param("projectId") String projectId);
}
