package com.stat.dal.mapper;

import com.stat.dal.po.DocumentsDataPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DocumentsDataMapper {

    int insert(DocumentsDataPO po);

    int updateById(DocumentsDataPO po);

    int deleteById(@Param("id") Long id);

    int deleteByProjectId(@Param("projectId") String projectId, @Param("username") String username);

    DocumentsDataPO selectById(@Param("id") Long id);

    List<DocumentsDataPO> selectByProjectId(@Param("projectId") String projectId, @Param("username") String username);
}
