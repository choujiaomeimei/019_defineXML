package com.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stat.common.entity.FileUploadRecord;
import com.stat.common.dto.FileUploadRecordDTO;
import com.stat.dal.mapper.FileUploadRecordMapper;
import com.stat.service.IFileUploadRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一文件上传记录服务实现类
 * 
 * @author System
 * @since 2025-01-10
 */
@Service
public class FileUploadRecordServiceImpl extends ServiceImpl<FileUploadRecordMapper, FileUploadRecord> 
        implements IFileUploadRecordService {

    @Override
    public List<FileUploadRecordDTO> getFilesByProjectIdAndCategory(String projectId, String fileCategory) {
        List<FileUploadRecord> records = baseMapper.selectByProjectIdAndCategory(projectId, fileCategory);
        return records.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<FileUploadRecordDTO> getFilesByProjectId(String projectId) {
        List<FileUploadRecord> records = baseMapper.selectByProjectId(projectId);
        return records.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public FileUploadRecordDTO getFileByFileId(String fileId) {
        FileUploadRecord record = baseMapper.selectByFileId(fileId);
        return record != null ? convertToDTO(record) : null;
    }

    @Override
    public IPage<FileUploadRecordDTO> getFilesPage(Page<FileUploadRecord> page, String projectId, 
                                                  String fileCategory, String uploadStatus, String processStatus) {
        IPage<FileUploadRecord> recordPage = baseMapper.selectPageByCondition(page, projectId, fileCategory, uploadStatus, processStatus);
        
        // 转换为DTO分页结果
        Page<FileUploadRecordDTO> dtoPage = new Page<>(page.getCurrent(), page.getSize(), recordPage.getTotal());
        List<FileUploadRecordDTO> dtoList = recordPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    @Override
    public Integer countFilesByProjectIdAndCategory(String projectId, String fileCategory) {
        return baseMapper.countByProjectIdAndCategory(projectId, fileCategory);
    }

    @Override
    public Long getTotalFileSizeByProjectId(String projectId) {
        return baseMapper.sumFileSizeByProjectId(projectId);
    }

    @Override
    public List<FileUploadRecordDTO> getFilesByUsername(String username) {
        List<FileUploadRecord> records = baseMapper.selectByUsername(username);
        return records.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public boolean saveFileUploadRecord(FileUploadRecordDTO fileUploadRecordDTO) {
        FileUploadRecord record = convertToEntity(fileUploadRecordDTO);
        record.setUploadTime(LocalDateTime.now());
        record.setUploadStatus(FileUploadRecord.UploadStatus.UPLOADING);
        record.setProcessStatus(FileUploadRecord.ProcessStatus.PENDING);
        record.setDeleted(0);
        
        return save(record);
    }

    @Override
    public boolean updateProcessStatus(String fileId, String processStatus, Integer processDurationMs, 
                                      String outputFilePath, String errorMessage) {
        LocalDateTime processTime = LocalDateTime.now();
        int result = baseMapper.updateProcessStatus(fileId, processStatus, processTime, processDurationMs, outputFilePath, errorMessage);
        return result > 0;
    }

    @Override
    public boolean updateUploadStatus(String fileId, String uploadStatus, String errorMessage) {
        int result = baseMapper.updateUploadStatus(fileId, uploadStatus, errorMessage);
        return result > 0;
    }

    @Override
    public boolean deleteFileRecord(String fileId) {
        LambdaQueryWrapper<FileUploadRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileUploadRecord::getFileId, fileId);
        return remove(wrapper);
    }

    @Override
    public Integer deleteFilesByProjectIdAndCategory(String projectId, String fileCategory) {
        LambdaQueryWrapper<FileUploadRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileUploadRecord::getProjectId, projectId);
        wrapper.eq(FileUploadRecord::getFileCategory, fileCategory);
        List<FileUploadRecord> records = list(wrapper);
        
        if (!records.isEmpty()) {
            remove(wrapper);
            return records.size();
        }
        return 0;
    }

    /**
     * 实体转DTO
     */
    private FileUploadRecordDTO convertToDTO(FileUploadRecord record) {
        FileUploadRecordDTO dto = new FileUploadRecordDTO();
        BeanUtils.copyProperties(record, dto);
        return dto;
    }

    /**
     * DTO转实体
     */
    private FileUploadRecord convertToEntity(FileUploadRecordDTO dto) {
        FileUploadRecord record = new FileUploadRecord();
        BeanUtils.copyProperties(dto, record);
        return record;
    }
}
