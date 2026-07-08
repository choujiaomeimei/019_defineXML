package com.stat.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 调试版P21控制器 - 逐步排查问题
 */
@Slf4j
@RestController
@RequestMapping("/api/p21")

public class DebugP21Controller {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("P21健康检查请求");

        Map<String, Object> response = new HashMap<>();
        response.put("service", "P21 Generation Service");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());

        log.info("返回健康检查响应: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> testGenerate(
            @RequestParam("taskName") String taskName,
            @RequestParam("protocolId") String protocolId,
            @RequestParam("studyTitle") String studyTitle,
            @RequestParam(value = "specFile", required = false) MultipartFile specFile,
            @RequestParam(value = "templateFile", required = false) MultipartFile templateFile) {

        log.info("收到P21生成请求");
        log.info("任务名称: {}", taskName);
        log.info("协议号: {}", protocolId);
        log.info("研究标题: {}", studyTitle);

        if (specFile != null) {
            log.info("Spec文件: {} ({}字节)", specFile.getOriginalFilename(), specFile.getSize());
        } else {
            log.warn("未接收到Spec文件");
        }

        if (templateFile != null) {
            log.info("模板文件: {} ({}字节)", templateFile.getOriginalFilename(), templateFile.getSize());
        } else {
            log.warn("未接收到模板文件");
        }

        // 模拟创建任务
        Long taskId = System.currentTimeMillis() % 10000;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("taskId", taskId);
        response.put("message", "调试模式：任务已提交");

        log.info("返回响应: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/task/{taskId}/status")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable Long taskId) {
        log.info("查询任务状态: {}", taskId);

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("taskId", taskId);
        taskData.put("taskName", "调试任务");
        taskData.put("taskStatus", "COMPLETED");
        taskData.put("progressPercentage", 100);
        taskData.put("statusDescription", "调试完成");
        taskData.put("isCompleted", true);
        taskData.put("isFailed", false);
        taskData.put("isProcessing", false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", taskData);

        log.info("返回任务状态: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getTasks() {
        log.info("获取任务列表");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", new Object[0]);
        response.put("total", 0);

        log.info("返回任务列表: {}", response);
        return ResponseEntity.ok(response);
    }
}