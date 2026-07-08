package com.stat.dal.mapper;

import com.stat.dal.po.MethodsDataPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MethodsDataMapper {

    int insert(MethodsDataPO po);

    int updateById(MethodsDataPO po);

    int deleteById(@Param("id") Long id);

    int deleteByProjectId(@Param("projectId") String projectId, @Param("username") String username);

    MethodsDataPO selectById(@Param("id") Long id);

    List<MethodsDataPO> selectByProjectId(@Param("projectId") String projectId, @Param("username") String username);
}
