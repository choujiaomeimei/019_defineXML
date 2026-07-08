package com.stat.dal.mapper;

import com.stat.dal.po.CommentsDataPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentsDataMapper {

    int insert(CommentsDataPO po);

    int updateById(CommentsDataPO po);

    int deleteById(@Param("id") Long id);

    int deleteByProjectId(@Param("projectId") String projectId, @Param("username") String username);

    CommentsDataPO selectById(@Param("id") Long id);

    List<CommentsDataPO> selectByProjectId(@Param("projectId") String projectId, @Param("username") String username);
}
