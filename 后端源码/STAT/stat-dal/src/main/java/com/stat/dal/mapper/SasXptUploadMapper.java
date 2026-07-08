package com.stat.dal.mapper;

import com.stat.common.entity.SasXptUpload;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * XPT上传记录Mapper接口
 */
public interface SasXptUploadMapper {
    
    /**
     * 插入XPT上传记录
     */
    int insert(SasXptUpload record);
    
    /**
     * 根据ID更新记录
     */
    int updateById(SasXptUpload record);
    
    /**
     * 根据文件ID查询记录
     */
    SasXptUpload selectByFileId(@Param("fileId") String fileId);
    
    /**
     * 查询所有有效记录
     */
    List<SasXptUpload> selectAllValid();
    
    /**
     * 更新处理状态
     */
    int updateProcessStatus(@Param("fileId") String fileId, 
                           @Param("processStatus") Integer processStatus,
                           @Param("outputFilePath") String outputFilePath, 
                           @Param("errorMessage") String errorMessage);
}