package com.stat.dal.mapper;

import com.stat.dal.po.PagesDataPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PagesDataMapper {

    int insert(PagesDataPO pagesData);

    int updateById(PagesDataPO pagesData);

    int deleteById(@Param("id") Long id);

    int deleteByProjectId(@Param("projectId") String projectId, @Param("username") String username);

    PagesDataPO selectById(@Param("id") Long id);

    List<PagesDataPO> selectByProjectId(@Param("projectId") String projectId, @Param("username") String username);

    List<PagesDataPO> selectByProjectIdAndDataset(
        @Param("projectId") String projectId,
        @Param("dataset") String dataset,
        @Param("username") String username
    );

    List<String> selectDistinctDatasetsByProjectId(@Param("projectId") String projectId, @Param("username") String username);
}
