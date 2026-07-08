package com.stat.service.impl;

import com.stat.common.dto.SasAcrfUploadDTO;
import com.stat.common.entity.SasAcrfUpload;
import com.stat.dal.mapper.SasAcrfUploadMapper;
import com.stat.service.ISasAcrfUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * aCRF上传记录Service实现类
 */
@Service
public class SasAcrfUploadServiceImpl implements ISasAcrfUploadService {

    private static final Logger logger = LoggerFactory.getLogger(SasAcrfUploadServiceImpl.class);

    @Resource
    private SasAcrfUploadMapper sasAcrfUploadMapper;

    @Override
    public SasAcrfUploadDTO saveUploadRecord(SasAcrfUploadDTO uploadDTO) {
        logger.info("开始保存aCRF上传记录: {}", uploadDTO.getOriginalName());
        
        SasAcrfUpload entity = convertToEntity(uploadDTO);
        entity.setUploadTime(LocalDateTime.now());
        entity.setProcessStatus(0); // 初始状态为未处理
        entity.setDeleted(0); // 确保设置未删除状态
        
        logger.info("准备保存实体: fileId={}, originalName={}, fileSize={}", 
                   entity.getFileId(), entity.getOriginalName(), entity.getFileSize());
        
        try {
            int result = sasAcrfUploadMapper.insert(entity);
            if (result > 0) {
                logger.info("aCRF上传记录保存成功，数据库ID: {}", entity.getId());
                return convertToDTO(entity);
            } else {
                logger.error("aCRF上传记录保存失败");
                return null;
            }
        } catch (Exception e) {
            logger.error("保存aCRF上传记录时发生异常", e);
            return null;
        }
    }

    @Override
    public List<SasAcrfUploadDTO> getAllUploadRecords() {
        List<SasAcrfUpload> entities = sasAcrfUploadMapper.selectAllValid();
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SasAcrfUploadDTO getUploadRecordByFileId(String fileId) {
        SasAcrfUpload entity = sasAcrfUploadMapper.selectByFileId(fileId);
        return entity != null ? convertToDTO(entity) : null;
    }

    @Override
    public boolean updateProcessStatus(String fileId, Integer processStatus, String outputFilePath, String errorMessage) {
        return sasAcrfUploadMapper.updateProcessStatus(fileId, processStatus, outputFilePath, errorMessage) > 0;
    }

    @Override
    public boolean deleteUploadRecord(String fileId) {
        SasAcrfUpload entity = sasAcrfUploadMapper.selectByFileId(fileId);
        if (entity != null) {
            // 使用逻辑删除
            entity.setDeleted(1);
            entity.setUpdateTime(LocalDateTime.now());
            return sasAcrfUploadMapper.updateById(entity) > 0;
        }
        return false;
    }

    @Override
    public SasAcrfUploadDTO convertToDTO(SasAcrfUpload entity) {
        if (entity == null) {
            return null;
        }
        
        SasAcrfUploadDTO dto = new SasAcrfUploadDTO();
        BeanUtils.copyProperties(entity, dto);
        
        // 构建处理结果对象
        if (entity.getProcessStatus() != null && entity.getProcessStatus() > 0) {
            SasAcrfUploadDTO.ProcessResult processResult = new SasAcrfUploadDTO.ProcessResult();
            processResult.setSuccess(entity.getProcessStatus() == 1);
            processResult.setOutputFile(entity.getOutputFilePath());
            processResult.setProcessTime(entity.getProcessTime());
            processResult.setError(entity.getErrorMessage());
            dto.setProcessResult(processResult);
        }
        
        return dto;
    }

    @Override
    public SasAcrfUpload convertToEntity(SasAcrfUploadDTO dto) {
        if (dto == null) {
            return null;
        }
        
        SasAcrfUpload entity = new SasAcrfUpload();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }
}