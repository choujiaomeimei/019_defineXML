package com.stat.dal.mapper;

import com.stat.common.entity.Project;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 项目数据访问层
 */
@Mapper
public interface ProjectMapper {

    /**
     * 插入项目
     */
    @Insert("INSERT INTO project (project_id, project_name, protocol_number, protocol_name, " +
            "protocol_version, protocol_date, standard_type, encoding, language, standard_version, sponsor, username, status, created_time, updated_time, deleted) " +
            "VALUES (#{projectId}, #{projectName}, #{protocolNumber}, #{protocolName}, " +
            "#{protocolVersion}, #{protocolDate}, #{standardType}, #{encoding}, #{language}, #{standardVersion}, #{sponsor}, #{username}, #{status}, #{createdTime}, #{updatedTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Project project);

    /**
     * 根据项目ID查询项目
     */
    @Select("SELECT * FROM project WHERE project_id = #{projectId} AND deleted = 0")
    @Results({
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "projectName", column = "project_name"),
        @Result(property = "protocolNumber", column = "protocol_number"),
        @Result(property = "protocolName", column = "protocol_name"),
        @Result(property = "protocolVersion", column = "protocol_version"),
        @Result(property = "protocolDate", column = "protocol_date"),
        @Result(property = "standardType", column = "standard_type"),
        @Result(property = "encoding", column = "encoding"),
        @Result(property = "language", column = "language"),
        @Result(property = "standardVersion", column = "standard_version"),
        @Result(property = "sponsor", column = "sponsor"),
        @Result(property = "username", column = "username"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "deleted", column = "deleted"),
        @Result(property = "id", column = "id")
    })
    Project selectByProjectId(String projectId);

    /**
     * 根据主键ID查询项目
     */
    @Select("SELECT * FROM project WHERE id = #{id} AND deleted = 0")
    @Results({
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "projectName", column = "project_name"),
        @Result(property = "protocolNumber", column = "protocol_number"),
        @Result(property = "protocolName", column = "protocol_name"),
        @Result(property = "protocolVersion", column = "protocol_version"),
        @Result(property = "protocolDate", column = "protocol_date"),
        @Result(property = "standardType", column = "standard_type"),
        @Result(property = "encoding", column = "encoding"),
        @Result(property = "language", column = "language"),
        @Result(property = "standardVersion", column = "standard_version"),
        @Result(property = "sponsor", column = "sponsor"),
        @Result(property = "username", column = "username"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "deleted", column = "deleted"),
        @Result(property = "id", column = "id")
    })
    Project selectById(Long id);

    /**
     * 查询所有有效项目
     */
    @Select("SELECT * FROM project WHERE deleted = 0 ORDER BY created_time DESC")
    @Results({
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "projectName", column = "project_name"),
        @Result(property = "protocolNumber", column = "protocol_number"),
        @Result(property = "protocolName", column = "protocol_name"),
        @Result(property = "protocolVersion", column = "protocol_version"),
        @Result(property = "protocolDate", column = "protocol_date"),
        @Result(property = "standardType", column = "standard_type"),
        @Result(property = "encoding", column = "encoding"),
        @Result(property = "language", column = "language"),
        @Result(property = "standardVersion", column = "standard_version"),
        @Result(property = "sponsor", column = "sponsor"),
        @Result(property = "username", column = "username"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "deleted", column = "deleted"),
        @Result(property = "id", column = "id")
    })
    List<Project> selectAllValid();

    /**
     * 查询活跃项目
     */
    @Select("SELECT * FROM project WHERE status = 1 AND deleted = 0 ORDER BY created_time DESC")
    @Results({
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "projectName", column = "project_name"),
        @Result(property = "protocolNumber", column = "protocol_number"),
        @Result(property = "protocolName", column = "protocol_name"),
        @Result(property = "protocolVersion", column = "protocol_version"),
        @Result(property = "protocolDate", column = "protocol_date"),
        @Result(property = "standardType", column = "standard_type"),
        @Result(property = "encoding", column = "encoding"),
        @Result(property = "language", column = "language"),
        @Result(property = "standardVersion", column = "standard_version"),
        @Result(property = "sponsor", column = "sponsor"),
        @Result(property = "username", column = "username"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "deleted", column = "deleted"),
        @Result(property = "id", column = "id")
    })
    List<Project> selectActiveProjects();

    /**
     * 根据用户名查询活跃项目
     */
    @Select("SELECT * FROM project WHERE username = #{username} AND status = 1 AND deleted = 0 ORDER BY created_time DESC")
    @Results({
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "projectName", column = "project_name"),
        @Result(property = "protocolNumber", column = "protocol_number"),
        @Result(property = "protocolName", column = "protocol_name"),
        @Result(property = "protocolVersion", column = "protocol_version"),
        @Result(property = "protocolDate", column = "protocol_date"),
        @Result(property = "standardType", column = "standard_type"),
        @Result(property = "encoding", column = "encoding"),
        @Result(property = "language", column = "language"),
        @Result(property = "standardVersion", column = "standard_version"),
        @Result(property = "sponsor", column = "sponsor"),
        @Result(property = "username", column = "username"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "deleted", column = "deleted"),
        @Result(property = "id", column = "id")
    })
    List<Project> selectActiveProjectsByUsername(String username);

    /**
     * 根据用户名查询所有项目
     */
    @Select("SELECT * FROM project WHERE username = #{username} AND deleted = 0 ORDER BY created_time DESC")
    @Results({
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "projectName", column = "project_name"),
        @Result(property = "protocolNumber", column = "protocol_number"),
        @Result(property = "protocolName", column = "protocol_name"),
        @Result(property = "protocolVersion", column = "protocol_version"),
        @Result(property = "protocolDate", column = "protocol_date"),
        @Result(property = "standardType", column = "standard_type"),
        @Result(property = "encoding", column = "encoding"),
        @Result(property = "language", column = "language"),
        @Result(property = "standardVersion", column = "standard_version"),
        @Result(property = "sponsor", column = "sponsor"),
        @Result(property = "username", column = "username"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdTime", column = "created_time"),
        @Result(property = "updatedTime", column = "updated_time"),
        @Result(property = "deleted", column = "deleted"),
        @Result(property = "id", column = "id")
    })
    List<Project> selectProjectsByUsername(String username);

    /**
     * 更新项目信息
     */
    @Update("UPDATE project SET project_name = #{projectName}, protocol_number = #{protocolNumber}, " +
            "protocol_name = #{protocolName}, protocol_version = #{protocolVersion}, protocol_date = #{protocolDate}, " +
            "standard_type = #{standardType}, encoding = #{encoding}, language = #{language}, standard_version = #{standardVersion}, " +
            "sponsor = #{sponsor}, status = #{status}, updated_time = #{updatedTime} WHERE id = #{id}")
    int updateById(Project project);

    /**
     * 逻辑删除项目
     */
    @Update("UPDATE project SET deleted = 1, updated_time = NOW() WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据项目ID逻辑删除项目
     */
    @Update("UPDATE project SET deleted = 1, updated_time = NOW() WHERE project_id = #{projectId}")
    int deleteByProjectId(String projectId);

    /**
     * 检查项目ID是否存在
     */
    @Select("SELECT COUNT(1) FROM project WHERE project_id = #{projectId} AND deleted = 0")
    int existsByProjectId(String projectId);

    /**
     * 归档项目
     */
    @Update("UPDATE project SET status = 0, updated_time = NOW() WHERE project_id = #{projectId}")
    int archiveProject(String projectId);

    /**
     * 激活项目
     */
    @Update("UPDATE project SET status = 1, updated_time = NOW() WHERE project_id = #{projectId}")
    int activateProject(String projectId);
}