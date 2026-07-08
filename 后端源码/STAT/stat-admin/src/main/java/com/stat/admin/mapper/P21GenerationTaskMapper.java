package com.stat.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stat.common.entity.P21GenerationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface P21GenerationTaskMapper extends BaseMapper<P21GenerationTask> {

    /**
     * 更新任务状态
     */
    @Update("UPDATE p21_generation_task SET task_status = #{status}, " +
            "started_at = CASE WHEN #{status} = 'PROCESSING' THEN NOW() ELSE started_at END, " +
            "completed_at = CASE WHEN #{status} IN ('COMPLETED', 'FAILED') THEN NOW() ELSE completed_at END " +
            "WHERE id = #{taskId}")
    int updateTaskStatus(@Param("taskId") Long taskId, @Param("status") String status);

    /**
     * 更新任务进度
     */
    @Update("UPDATE p21_generation_task SET progress_percentage = #{progress} WHERE id = #{taskId}")
    int updateTaskProgress(@Param("taskId") Long taskId, @Param("progress") Integer progress);

    /**
     * 更新错误信息
     */
    @Update("UPDATE p21_generation_task SET error_message = #{errorMessage}, task_status = 'FAILED', " +
            "completed_at = NOW() WHERE id = #{taskId}")
    int updateTaskError(@Param("taskId") Long taskId, @Param("errorMessage") String errorMessage);

    /**
     * 完成任务
     */
    @Update("UPDATE p21_generation_task SET task_status = 'COMPLETED', output_file_path = #{outputPath}, " +
            "output_file_name = #{outputName}, progress_percentage = 100, completed_at = NOW() " +
            "WHERE id = #{taskId}")
    int completeTask(@Param("taskId") Long taskId, @Param("outputPath") String outputPath,
                    @Param("outputName") String outputName);

    /**
     * 获取正在处理的任务列表
     */
    @Select("SELECT * FROM p21_generation_task WHERE task_status = 'PROCESSING' ORDER BY started_at")
    List<P21GenerationTask> selectProcessingTasks();

    /**
     * 获取用户的任务列表
     */
    @Select("SELECT * FROM p21_generation_task WHERE created_by = #{userId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<P21GenerationTask> selectUserTasks(@Param("userId") String userId, @Param("limit") Integer limit);
}