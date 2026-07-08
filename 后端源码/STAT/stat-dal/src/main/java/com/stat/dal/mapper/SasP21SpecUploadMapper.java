package com.stat.dal.mapper;

import com.stat.common.entity.SasP21SpecUpload;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * P21空SPEC上传记录Mapper接口
 */
public interface SasP21SpecUploadMapper {
    
    /**
     * 插入P21空SPEC上传记录
     */
    int insert(SasP21SpecUpload record);
    
    /**
     * 根据ID更新记录
     */
    int updateById(SasP21SpecUpload record);
    
    /**
     * 根据文件ID查询记录
     */
    SasP21SpecUpload selectByFileId(@Param("fileId") String fileId);
    
    /**
     * 查询所有有效记录
     */
    List<SasP21SpecUpload> selectAllValid();
    
    /**
     * 更新处理状态
     */
    int updateProcessStatus(@Param("fileId") String fileId, 
                           @Param("processStatus") Integer processStatus,
                           @Param("outputFilePath") String outputFilePath, 
                           @Param("errorMessage") String errorMessage);
}