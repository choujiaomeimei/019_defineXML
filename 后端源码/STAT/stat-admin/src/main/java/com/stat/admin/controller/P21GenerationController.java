package com.stat.admin.controller;

import com.stat.admin.service.P21TaskService;
import com.stat.admin.service.PythonIntegrationService;
import com.stat.common.dto.P21GenerationRequestDTO;
import com.stat.common.dto.TaskStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * P21报告生成控制器
 */
@Slf4j
// @RestController
@RequestMapping("/api/p21-full")
@RequiredArgsConstructor

public class P21GenerationController {

    private final P21TaskService taskService;
    private final PythonIntegrationService pythonService;

    /**
     * 提交P21报告生成任务
     * @param request 生成请求
     * @return 任务ID
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateP21Report(@Validated @ModelAttribute P21GenerationRequestDTO request) {
        log.info("收到P21报告生成请求: {}", request.getTaskName());

        try {
            // 1. 检查Python环境
            if (!pythonService.isPythonEnvironmentAvailable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(createErrorResponse("Python环境不可用，请检查系统配置"));
            }

            // 2. 创建任务
            Long taskId = taskService.createTask(request);

            // 3. 异步执行Python处理
            pythonService.executeP21GenerationAsync(taskId, request);

            // 4. 立即返回任务ID
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "任务已提交，正在处理中...");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("提交P21生成任务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("任务提交失败: " + e.getMessage()));
        }
    }

    /**
     * 查询任务状态
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    @GetMapping("/task/{taskId}/status")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable Long taskId) {
        log.debug("查询任务状态: {}", taskId);

        try {
            TaskStatusDTO status = taskService.getTaskStatus(taskId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询任务状态失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("任务不存在或查询失败"));
        }
    }

    /**
     * 下载生成的P21报告文件
     * @param taskId 任务ID
     * @param request HTTP请求
     * @return 文件资源
     */
    @GetMapping("/task/{taskId}/download")
    public ResponseEntity<Resource> downloadReport(@PathVariable Long taskId, HttpServletRequest request) {
        log.info("下载P21报告: {}", taskId);

        try {
            // 1. 获取任务状态
            TaskStatusDTO taskStatus = taskService.getTaskStatus(taskId);

            if (!taskStatus.isCompleted()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // 2. 获取文件路径
            String filePath = taskService.getTaskOutputPath(taskId);
            if (filePath == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // 3. 加载文件资源
            Path file = Paths.get(filePath);
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // 4. 确定文件类型
            String contentType = determineContentType(request, resource);

            // 5. 设置下载响应头
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("下载文件失败，文件路径无效: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取用户任务列表
     * @param userId 用户ID
     * @param limit 数量限制
     * @return 任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getUserTasks(
            @RequestParam(defaultValue = "system") String userId,
            @RequestParam(defaultValue = "10") Integer limit) {

        log.debug("获取用户任务列表: userId={}, limit={}", userId, limit);

        try {
            List<TaskStatusDTO> tasks = taskService.getUserTasks(userId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tasks);
            response.put("total", tasks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取任务列表失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取任务列表失败"));
        }
    }

    /**
     * 获取正在处理的任务列表
     * @return 处理中任务列表
     */
    @GetMapping("/tasks/processing")
    public ResponseEntity<Map<String, Object>> getProcessingTasks() {
        log.debug("获取正在处理的任务列表");

        try {
            List<TaskStatusDTO> tasks = taskService.getProcessingTasks();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tasks);
            response.put("total", tasks.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取处理中任务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("获取处理中任务失败"));
        }
    }

    /**
     * 系统健康检查
     * @return 系统状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("pythonAvailable", pythonService.isPythonEnvironmentAvailable());
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 确定文件内容类型
     */
    private String determineContentType(HttpServletRequest request, Resource resource) {
        String contentType = null;

        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            log.info("无法确定文件类型");
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return contentType;
    }
}