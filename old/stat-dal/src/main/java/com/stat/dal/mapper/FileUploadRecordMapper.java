package com.stat.dal.mapper;

import com.stat.common.entity.FileUploadRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 统一文件上传记录 Mapper接口
 *
 * @author system
 * @date 2025-01-10
 */
@Mapper
public interface FileUploadRecordMapper {

    /**
     * 插入文件上传记录
     */
    @Insert("INSERT INTO file_upload_records (" +
            "file_id, project_id, username, file_category, original_name, " +
            "server_file_name, file_path, file_size, file_extension, file_md5, " +
            "upload_time, upload_status, process_status, create_time, update_time, deleted" +
            ") VALUES (" +
            "#{fileId}, #{projectId}, #{username}, #{fileCategory}, #{originalName}, " +
            "#{serverFileName}, #{filePath}, #{fileSize}, #{fileExtension}, #{fileMd5}, " +
            "#{uploadTime}, #{uploadStatus}, #{processStatus}, #{createTime}, #{updateTime}, #{deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileUploadRecord record);

    /**
     * 根据ID查询文件记录
     */
    @Select("SELECT * FROM file_upload_records WHERE id = #{id} AND deleted = 0")
    @Results(id = "FileUploadRecordResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "fileId", column = "file_id"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "fileCategory", column = "file_category"),
        @Result(property = "originalName", column = "original_name"),
        @Result(property = "serverFileName", column = "server_file_name"),
        @Result(property = "filePath", column = "file_path"),
        @Result(property = "fileSize", column = "file_size"),
        @Result(property = "fileExtension", column = "file_extension"),
        @Result(property = "fileMd5", column = "file_md5"),
        @Result(property = "uploadTime", column = "upload_time"),
        @Result(property = "uploadStatus", column = "upload_status"),
        @Result(property = "processStatus", column = "process_status"),
        @Result(property = "processTime", column = "process_time"),
        @Result(property = "processDurationMs", column = "process_duration_ms"),
        @Result(property = "outputFilePath", column = "output_file_path"),
        @Result(property = "errorMessage", column = "error_message"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "deleted", column = "deleted"),
        @Result(property = "remarks", column = "remarks")
    })
    FileUploadRecord selectById(Long id);

    /**
     * 根据文件ID查询文件记录
     */
    @Select("SELECT * FROM file_upload_records WHERE file_id = #{fileId} AND deleted = 0")
    @ResultMap("FileUploadRecordResultMap")
    FileUploadRecord selectByFileId(String fileId);

    /**
     * 根据项目ID查询所有文件记录
     */
    @Select("SELECT * FROM file_upload_records WHERE project_id = #{projectId} AND deleted = 0 ORDER BY upload_time DESC")
    @ResultMap("FileUploadRecordResultMap")
    List<FileUploadRecord> selectByProjectId(String projectId);

    /**
     * 根据项目ID和文件类别查询文件记录
     */
    @Select("SELECT * FROM file_upload_records WHERE project_id = #{projectId} AND file_category = #{fileCategory} AND deleted = 0 ORDER BY upload_time DESC")
    @ResultMap("FileUploadRecordResultMap")
    List<FileUploadRecord> selectByProjectIdAndCategory(String projectId, String fileCategory);

    /**
     * 根据用户名查询文件记录
     */
    @Select("SELECT * FROM file_upload_records WHERE username = #{username} AND deleted = 0 ORDER BY upload_time DESC")
    @ResultMap("FileUploadRecordResultMap")
    List<FileUploadRecord> selectByUsername(String username);

    /**
     * 查询指定状态的文件记录
     */
    @Select("SELECT * FROM file_upload_records WHERE upload_status = #{uploadStatus} AND deleted = 0 ORDER BY upload_time DESC")
    @ResultMap("FileUploadRecordResultMap")
    List<FileUploadRecord> selectByUploadStatus(String uploadStatus);

    /**
     * 查询指定处理状态的文件记录
     */
    @Select("SELECT * FROM file_upload_records WHERE process_status = #{processStatus} AND deleted = 0 ORDER BY upload_time DESC")
    @ResultMap("FileUploadRecordResultMap")
    List<FileUploadRecord> selectByProcessStatus(String processStatus);

    /**
     * 更新文件记录
     */
    @Update("UPDATE file_upload_records SET " +
            "upload_status = #{uploadStatus}, process_status = #{processStatus}, " +
            "process_time = #{processTime}, process_duration_ms = #{processDurationMs}, " +
            "output_file_path = #{outputFilePath}, error_message = #{errorMessage}, " +
            "update_time = #{updateTime}, remarks = #{remarks} " +
            "WHERE id = #{id}")
    int updateById(FileUploadRecord record);

    /**
     * 更新上传状态
     */
    @Update("UPDATE file_upload_records SET upload_status = #{uploadStatus}, " +
            "error_message = #{errorMessage}, update_time = NOW() WHERE id = #{id}")
    int updateUploadStatus(@Param("id") Long id,
                          @Param("uploadStatus") String uploadStatus,
                          @Param("errorMessage") String errorMessage);

    /**
     * 更新处理状态
     */
    @Update("UPDATE file_upload_records SET process_status = #{processStatus}, " +
            "process_time = #{processTime}, process_duration_ms = #{processDurationMs}, " +
            "output_file_path = #{outputFilePath}, error_message = #{errorMessage}, " +
            "update_time = NOW() WHERE id = #{id}")
    int updateProcessStatus(@Param("id") Long id,
                           @Param("processStatus") String processStatus,
                           @Param("processTime") String processTime,
                           @Param("processDurationMs") Integer processDurationMs,
                           @Param("outputFilePath") String outputFilePath,
                           @Param("errorMessage") String errorMessage);

    /**
     * 逻辑删除文件记录
     */
    @Update("UPDATE file_upload_records SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据文件ID逻辑删除
     */
    @Update("UPDATE file_upload_records SET deleted = 1, update_time = NOW() WHERE file_id = #{fileId}")
    int deleteByFileId(String fileId);

    /**
     * 根据项目ID逻辑删除所有文件记录
     */
    @Update("UPDATE file_upload_records SET deleted = 1, update_time = NOW() WHERE project_id = #{projectId}")
    int deleteByProjectId(String projectId);

    /**
     * 检查文件是否已存在
     */
    @Select("SELECT COUNT(1) FROM file_upload_records WHERE project_id = #{projectId} " +
            "AND file_category = #{fileCategory} AND original_name = #{originalName} AND deleted = 0")
    int existsFile(@Param("projectId") String projectId,
                   @Param("fileCategory") String fileCategory,
                   @Param("originalName") String originalName);

    /**
     * 获取项目文件统计
     */
    @Select("SELECT file_category, COUNT(*) as count, SUM(file_size) as total_size " +
            "FROM file_upload_records WHERE project_id = #{projectId} AND deleted = 0 " +
            "GROUP BY file_category")
    List<FileUploadRecord.FileStats> getProjectFileStats(String projectId);

    /**
     * 获取最新的指定类别文件
     */
    @Select("SELECT * FROM file_upload_records WHERE project_id = #{projectId} " +
            "AND file_category = #{fileCategory} AND upload_status = 'success' AND deleted = 0 " +
            "ORDER BY upload_time DESC LIMIT 1")
    @ResultMap("FileUploadRecordResultMap")
    FileUploadRecord getLatestFileByCategory(@Param("projectId") String projectId,
                                           @Param("fileCategory") String fileCategory);

    /**
     * 清理指定天数前的失败记录
     */
    @Delete("DELETE FROM file_upload_records WHERE upload_status = 'failed' " +
            "AND create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int cleanupFailedRecords(@Param("days") int days);

    /**
     * 文件统计内部类
     */
    class FileStats {
        private String fileCategory;
        private Integer count;
        private Long totalSize;

        // getters and setters
        public String getFileCategory() { return fileCategory; }
        public void setFileCategory(String fileCategory) { this.fileCategory = fileCategory; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Long getTotalSize() { return totalSize; }
        public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
    }
}