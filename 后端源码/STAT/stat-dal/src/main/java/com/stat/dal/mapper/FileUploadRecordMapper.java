package com.stat.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stat.common.entity.FileUploadRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 统一文件上传记录Mapper接口
 * 
 * @author System
 * @since 2025-01-10
 */
@Mapper
public interface FileUploadRecordMapper extends BaseMapper<FileUploadRecord> {

    /**
     * 根据项目ID和文件类别查询文件列表
     * 
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @return 文件列表
     */
    List<FileUploadRecord> selectByProjectIdAndCategory(@Param("projectId") String projectId, 
                                                        @Param("fileCategory") String fileCategory);

    /**
     * 根据项目ID查询所有文件
     * 
     * @param projectId 项目ID
     * @return 文件列表
     */
    List<FileUploadRecord> selectByProjectId(@Param("projectId") String projectId);

    /**
     * 根据文件ID查询文件信息
     * 
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileUploadRecord selectByFileId(@Param("fileId") String fileId);

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
    IPage<FileUploadRecord> selectPageByCondition(Page<FileUploadRecord> page,
                                                  @Param("projectId") String projectId,
                                                  @Param("fileCategory") String fileCategory,
                                                  @Param("uploadStatus") String uploadStatus,
                                                  @Param("processStatus") String processStatus);

    /**
     * 统计项目文件数量
     * 
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @return 文件数量
     */
    Integer countByProjectIdAndCategory(@Param("projectId") String projectId, 
                                       @Param("fileCategory") String fileCategory);

    /**
     * 统计项目总文件大小
     * 
     * @param projectId 项目ID
     * @return 总文件大小(字节)
     */
    Long sumFileSizeByProjectId(@Param("projectId") String projectId);

    /**
     * 根据用户名查询文件列表
     * 
     * @param username 用户名
     * @return 文件列表
     */
    List<FileUploadRecord> selectByUsername(@Param("username") String username);

    /**
     * 更新文件处理状态
     * 
     * @param fileId 文件ID
     * @param processStatus 处理状态
     * @param processTime 处理时间
     * @param processDurationMs 处理耗时
     * @param outputFilePath 输出文件路径
     * @param errorMessage 错误信息
     * @return 更新行数
     */
    Integer updateProcessStatus(@Param("fileId") String fileId,
                               @Param("processStatus") String processStatus,
                               @Param("processTime") java.time.LocalDateTime processTime,
                               @Param("processDurationMs") Integer processDurationMs,
                               @Param("outputFilePath") String outputFilePath,
                               @Param("errorMessage") String errorMessage);

    /**
     * 更新文件上传状态
     * 
     * @param fileId 文件ID
     * @param uploadStatus 上传状态
     * @param errorMessage 错误信息
     * @return 更新行数
     */
    Integer updateUploadStatus(@Param("fileId") String fileId,
                              @Param("uploadStatus") String uploadStatus,
                              @Param("errorMessage") String errorMessage);
}
