package com.stat.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stat.common.entity.ProjectSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectSnapshotMapper extends BaseMapper<ProjectSnapshot> {

    List<ProjectSnapshot> selectByProjectId(@Param("projectId") String projectId);
}
