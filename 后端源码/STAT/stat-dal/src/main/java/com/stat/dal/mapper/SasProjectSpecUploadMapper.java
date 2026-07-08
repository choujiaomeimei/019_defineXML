package com.stat.dal.mapper;

import com.stat.common.entity.SasProjectSpecUpload;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目SPEC上传记录Mapper接口
 */
public interface SasProjectSpecUploadMapper {
    
    /**
     * 插入项目SPEC上传记录
     */
    int insert(SasProjectSpecUpload record);
    
    /**
     * 根据ID更新记录
     */
    int updateById(SasProjectSpecUpload record);
    
    /**
     * 根据文件ID查询记录
     */
    SasProjectSpecUpload selectByFileId(@Param("fileId") String fileId);
    
    /**
     * 查询所有有效记录
     */
    List<SasProjectSpecUpload> selectAllValid();
    
    /**
     * 更新处理状态
     */
    int updateProcessStatus(@Param("fileId") String fileId, 
                           @Param("processStatus") Integer processStatus,
                           @Param("outputFilePath") String outputFilePath, 
                           @Param("errorMessage") String errorMessage);
}