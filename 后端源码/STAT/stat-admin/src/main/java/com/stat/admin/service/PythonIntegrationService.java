package com.stat.admin.service;

import com.stat.common.dto.P21GenerationRequestDTO;

/**
 * Python集成服务接口
 */
public interface PythonIntegrationService {

    /**
     * 异步执行P21报告生成
     * @param taskId 任务ID
     * @param request 生成请求
     */
    void executeP21GenerationAsync(Long taskId, P21GenerationRequestDTO request);

    /**
     * 检查Python环境是否可用
     * @return 是否可用
     */
    boolean isPythonEnvironmentAvailable();

    /**
     * 获取Python脚本执行状态
     * @param processId 进程ID
     * @return 是否正在运行
     */
    boolean isProcessRunning(String processId);

    /**
     * 终止Python进程
     * @param processId 进程ID
     * @return 是否成功终止
     */
    boolean terminateProcess(String processId);
}