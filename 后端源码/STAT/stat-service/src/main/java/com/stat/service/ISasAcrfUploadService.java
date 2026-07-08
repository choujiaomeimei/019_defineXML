package com.stat.service;

import com.stat.common.dto.SasAcrfUploadDTO;
import com.stat.common.entity.SasAcrfUpload;

import java.util.List;

/**
 * aCRF上传记录Service接口
 */
public interface ISasAcrfUploadService {

    /**
     * 保存上传记录
     */
    SasAcrfUploadDTO saveUploadRecord(SasAcrfUploadDTO uploadDTO);

    /**
     * 查询所有上传记录
     */
    List<SasAcrfUploadDTO> getAllUploadRecords();

    /**
     * 根据文件ID查询上传记录
     */
    SasAcrfUploadDTO getUploadRecordByFileId(String fileId);

    /**
     * 更新处理状态
     */
    boolean updateProcessStatus(String fileId, Integer processStatus, String outputFilePath, String errorMessage);

    /**
     * 删除上传记录（逻辑删除）
     */
    boolean deleteUploadRecord(String fileId);

    /**
     * 转换Entity到DTO
     */
    SasAcrfUploadDTO convertToDTO(SasAcrfUpload entity);

    /**
     * 转换DTO到Entity
     */
    SasAcrfUpload convertToEntity(SasAcrfUploadDTO dto);
}