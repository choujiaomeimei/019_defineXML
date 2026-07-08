package com.stat.dal.mapper;

import com.stat.dal.po.DictionariesDataPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DictionariesDataMapper {

    int insert(DictionariesDataPO po);

    int updateById(DictionariesDataPO po);

    int deleteById(@Param("id") Long id);

    int deleteByProjectId(@Param("projectId") String projectId, @Param("username") String username);

    DictionariesDataPO selectById(@Param("id") Long id);

    List<DictionariesDataPO> selectByProjectId(@Param("projectId") String projectId, @Param("username") String username);
}
