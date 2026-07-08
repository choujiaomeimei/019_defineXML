package com.stat.dal.mapper;

import com.stat.common.entity.SasAcrfUpload;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * aCRF上传记录Mapper接口
 */
@Mapper
public interface SasAcrfUploadMapper {

    /**
     * 插入上传记录
     */
    int insert(SasAcrfUpload record);

    /**
     * 根据ID查询上传记录
     */
    SasAcrfUpload selectById(@Param("id") Long id);

    /**
     * 根据文件ID查询上传记录
     */
    SasAcrfUpload selectByFileId(@Param("fileId") String fileId);

    /**
     * 查询所有有效的上传记录（按上传时间倒序）
     */
    List<SasAcrfUpload> selectAllValid();

    /**
     * 更新记录
     */
    int updateById(SasAcrfUpload record);

    /**
     * 更新处理状态
     */
    int updateProcessStatus(@Param("fileId") String fileId, 
                           @Param("processStatus") Integer processStatus,
                           @Param("outputFilePath") String outputFilePath,
                           @Param("errorMessage") String errorMessage);
}