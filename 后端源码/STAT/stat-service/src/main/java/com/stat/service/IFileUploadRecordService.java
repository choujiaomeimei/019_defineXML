package com.stat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stat.common.entity.FileUploadRecord;
import com.stat.common.dto.FileUploadRecordDTO;

import java.util.List;

/**
 * 统一文件上传记录服务接口
 * 
 * @author System
 * @since 2025-01-10
 */
public interface IFileUploadRecordService extends IService<FileUploadRecord> {

    /**
     * 根据项目ID和文件类别查询文件列表
     * 
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @return 文件列表
     */
    List<FileUploadRecordDTO> getFilesByProjectIdAndCategory(String projectId, String fileCategory);

    /**
     * 根据项目ID查询所有文件
     * 
     * @param projectId 项目ID
     * @return 文件列表
     */
    List<FileUploadRecordDTO> getFilesByProjectId(String projectId);

    /**
     * 根据文件ID查询文件信息
     * 
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileUploadRecordDTO getFileByFileId(String fileId);

    /**
     * 分页查询文件列表
     * 
     * @param page 分页参数
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @param uploadStatus 上传状态
     * @param processStatus 处理状态
     * @return 分页结果
     */
    IPage<FileUploadRecordDTO> getFilesPage(Page<FileUploadRecord> page, String projectId, 
                                           String fileCategory, String uploadStatus, String processStatus);

    /**
     * 统计项目文件数量
     * 
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @return 文件数量
     */
    Integer countFilesByProjectIdAndCategory(String projectId, String fileCategory);

    /**
     * 统计项目总文件大小
     * 
     * @param projectId 项目ID
     * @return 总文件大小(字节)
     */
    Long getTotalFileSizeByProjectId(String projectId);

    /**
     * 根据用户名查询文件列表
     * 
     * @param username 用户名
     * @return 文件列表
     */
    List<FileUploadRecordDTO> getFilesByUsername(String username);

    /**
     * 保存文件上传记录
     * 
     * @param fileUploadRecordDTO 文件上传记录DTO
     * @return 是否成功
     */
    boolean saveFileUploadRecord(FileUploadRecordDTO fileUploadRecordDTO);

    /**
     * 更新文件处理状态
     * 
     * @param fileId 文件ID
     * @param processStatus 处理状态
     * @param processDurationMs 处理耗时
     * @param outputFilePath 输出文件路径
     * @param errorMessage 错误信息
     * @return 是否成功
     */
    boolean updateProcessStatus(String fileId, String processStatus, Integer processDurationMs, 
                               String outputFilePath, String errorMessage);

    /**
     * 更新文件上传状态
     * 
     * @param fileId 文件ID
     * @param uploadStatus 上传状态
     * @param errorMessage 错误信息
     * @return 是否成功
     */
    boolean updateUploadStatus(String fileId, String uploadStatus, String errorMessage);

    /**
     * 删除文件记录
     * 
     * @param fileId 文件ID
     * @return 是否成功
     */
    boolean deleteFileRecord(String fileId);

    /**
     * 根据项目ID和文件类别删除文件记录
     * 
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @return 删除的文件数量
     */
    Integer deleteFilesByProjectIdAndCategory(String projectId, String fileCategory);
}
