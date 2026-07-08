package com.stat.admin.service;

import com.stat.common.dto.P21GenerationRequestDTO;
import com.stat.common.dto.TaskStatusDTO;
import com.stat.common.entity.P21GenerationTask;

import java.util.List;

/**
 * P21任务管理服务接口
 */
public interface P21TaskService {

    /**
     * 创建P21生成任务
     * @param request 任务请求信息
     * @return 任务ID
     */
    Long createTask(P21GenerationRequestDTO request);

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    TaskStatusDTO getTaskStatus(Long taskId);

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 新状态
     */
    void updateTaskStatus(Long taskId, P21GenerationTask.TaskStatus status);

    /**
     * 更新任务进度
     * @param taskId 任务ID
     * @param progress 进度百分比
     */
    void updateTaskProgress(Long taskId, Integer progress);

    /**
     * 标记任务失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void failTask(Long taskId, String errorMessage);

    /**
     * 完成任务
     * @param taskId 任务ID
     * @param outputFilePath 输出文件路径
     */
    void completeTask(Long taskId, String outputFilePath);

    /**
     * 获取用户任务列表
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 任务列表
     */
    List<TaskStatusDTO> getUserTasks(String userId, Integer limit);

    /**
     * 获取正在处理的任务列表
     * @return 处理中任务列表
     */
    List<TaskStatusDTO> getProcessingTasks();

    /**
     * 获取任务的输出文件路径
     * @param taskId 任务ID
     * @return 文件路径
     */
    String getTaskOutputPath(Long taskId);
}