package com.stat.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stat.common.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    @Select("SELECT project_id FROM project_member WHERE username = #{username}")
    List<String> selectProjectIdsByUsername(@Param("username") String username);

    @Select("SELECT role FROM project_member WHERE project_id = #{projectId} AND username = #{username}")
    String selectRole(@Param("projectId") String projectId, @Param("username") String username);

    @Select("SELECT COUNT(*) FROM project_member WHERE project_id = #{projectId} AND username = #{username}")
    int checkMembership(@Param("projectId") String projectId, @Param("username") String username);
}
