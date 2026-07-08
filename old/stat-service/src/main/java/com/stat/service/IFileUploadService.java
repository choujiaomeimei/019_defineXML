package com.stat.service;

import com.stat.common.entity.FileUploadRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 统一文件上传服务接口
 *
 * @author system
 * @date 2025-01-10
 */
public interface IFileUploadService {

    /**
     * 处理文件上传
     *
     * @param file 上传的文件
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @param username 上传用户
     * @return 文件上传记录
     */
    FileUploadRecord handleFileUpload(MultipartFile file, String projectId,
                                    FileUploadRecord.FileCategory fileCategory,
                                    String username);

    /**
     * 根据ID获取文件记录
     */
    FileUploadRecord getFileRecordById(Long id);

    /**
     * 根据文件ID获取文件记录
     */
    FileUploadRecord getFileRecordByFileId(String fileId);

    /**
     * 获取项目所有文件记录
     */
    List<FileUploadRecord> getProjectFiles(String projectId);

    /**
     * 获取项目指定类别的文件记录
     */
    List<FileUploadRecord> getProjectFilesByCategory(String projectId,
                                                   FileUploadRecord.FileCategory fileCategory);

    /**
     * 获取用户上传的文件记录
     */
    List<FileUploadRecord> getUserFiles(String username);

    /**
     * 获取项目文件统计信息
     */
    Map<String, Object> getProjectFileStats(String projectId);

    /**
     * 更新文件上传状态
     */
    boolean updateUploadStatus(Long id, FileUploadRecord.UploadStatus status, String errorMessage);

    /**
     * 开始处理文件
     */
    boolean startProcessing(Long id);

    /**
     * 标记文件处理完成
     */
    boolean markProcessCompleted(Long id, String outputFilePath);

    /**
     * 标记文件处理失败
     */
    boolean markProcessFailed(Long id, String errorMessage);

    /**
     * 删除文件记录
     */
    boolean deleteFileRecord(Long id);

    /**
     * 根据文件ID删除文件记录
     */
    boolean deleteFileRecordByFileId(String fileId);

    /**
     * 检查文件是否已存在
     */
    boolean isFileExists(String projectId, FileUploadRecord.FileCategory fileCategory, String originalName);

    /**
     * 获取最新的指定类别文件
     */
    FileUploadRecord getLatestFileByCategory(String projectId, FileUploadRecord.FileCategory fileCategory);

    /**
     * 处理P21 Spec文件并更新数据库
     */
    boolean processP21SpecFile(Long fileId);

    /**
     * 处理项目Spec文件并更新数据库
     */
    boolean processProjectSpecFile(Long fileId);

    /**
     * 处理aCRF文件，调用Python程序提取注释
     */
    boolean processAcrfFile(Long fileId);

    /**
     * 生成VLM，调用Python程序
     */
    boolean generateVlmData(String projectId);

    /**
     * 生成CodeList，调用Python程序
     */
    boolean generateCodeListData(String projectId);

    /**
     * 更新页面信息，调用Python程序
     */
    boolean updatePageInfo(String projectId);

    /**
     * 清理失败的上传记录
     */
    int cleanupFailedRecords(int days);
}