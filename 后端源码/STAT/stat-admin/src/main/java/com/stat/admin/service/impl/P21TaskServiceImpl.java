package com.stat.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stat.admin.mapper.P21GenerationTaskMapper;
import com.stat.admin.service.P21TaskService;
import com.stat.common.dto.P21GenerationRequestDTO;
import com.stat.common.dto.TaskStatusDTO;
import com.stat.common.entity.P21GenerationTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class P21TaskServiceImpl implements P21TaskService {

    private final P21GenerationTaskMapper taskMapper;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String UPLOAD_DIR;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    private String getOutputDir() {
        return pythonPath + "/output";
    }

    @Override
    @Transactional
    public Long createTask(P21GenerationRequestDTO request) {
        log.info("创建P21生成任务: {}", request.getTaskName());

        try {
            // 1. 保存上传的文件
            String specFilePath = saveUploadedFile(request.getSpecFile(), "spec");
            String templateFilePath = saveUploadedFile(request.getTemplateFile(), "template");

            // 2. 创建任务记录
            P21GenerationTask task = new P21GenerationTask();
            task.setTaskName(request.getTaskName())
                .setTaskStatus(P21GenerationTask.TaskStatus.PENDING)
                .setSpecFilePath(specFilePath)
                .setSpecFileName(request.getSpecFile().getOriginalFilename())
                .setTemplateFilePath(templateFilePath)
                .setTemplateFileName(request.getTemplateFile().getOriginalFilename())
                .setProtocolId(request.getProtocolId())
                .setStudyTitle(request.getStudyTitle())
                .setProgressPercentage(0)
                .setCreatedBy(request.getCreatedBy())
                .setRemark(request.getRemark())
                .setCreatedAt(LocalDateTime.now());

            // 3. 设置输出文件路径
            String outputFileName = generateOutputFileName(request.getTaskName());
            task.setOutputFileName(outputFileName)
                .setOutputFilePath(Paths.get(getOutputDir(), outputFileName).toString());

            taskMapper.insert(task);
            log.info("任务创建成功，任务ID: {}", task.getId());

            return task.getId();

        } catch (Exception e) {
            log.error("创建任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建任务失败: " + e.getMessage());
        }
    }

    @Override
    public TaskStatusDTO getTaskStatus(Long taskId) {
        P21GenerationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        TaskStatusDTO dto = new TaskStatusDTO();
        BeanUtils.copyProperties(task, dto);
        return dto;
    }

    @Override
    public void updateTaskStatus(Long taskId, P21GenerationTask.TaskStatus status) {
        log.info("更新任务状态: {} -> {}", taskId, status);
        taskMapper.updateTaskStatus(taskId, status.name());

        // 如果是开始处理状态，设置开始时间
        if (status == P21GenerationTask.TaskStatus.PROCESSING) {
            P21GenerationTask task = taskMapper.selectById(taskId);
            task.setStartedAt(LocalDateTime.now());
            task.setEstimatedCompletion(LocalDateTime.now().plusMinutes(5)); // 估计5分钟完成
            taskMapper.updateById(task);
        }
    }

    @Override
    public void updateTaskProgress(Long taskId, Integer progress) {
        log.debug("更新任务进度: {} -> {}%", taskId, progress);
        taskMapper.updateTaskProgress(taskId, progress);
    }

    @Override
    public void failTask(Long taskId, String errorMessage) {
        log.error("任务执行失败: {} - {}", taskId, errorMessage);
        taskMapper.updateTaskError(taskId, errorMessage);
    }

    @Override
    public void completeTask(Long taskId, String outputFilePath) {
        log.info("任务执行完成: {} - {}", taskId, outputFilePath);

        // 获取文件名
        String outputFileName = Paths.get(outputFilePath).getFileName().toString();
        taskMapper.completeTask(taskId, outputFilePath, outputFileName);
    }

    @Override
    public List<TaskStatusDTO> getUserTasks(String userId, Integer limit) {
        List<P21GenerationTask> tasks = taskMapper.selectUserTasks(userId, limit != null ? limit : 10);
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskStatusDTO> getProcessingTasks() {
        List<P21GenerationTask> tasks = taskMapper.selectProcessingTasks();
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public String getTaskOutputPath(Long taskId) {
        P21GenerationTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        return task.getOutputFilePath();
    }

    /**
     * 保存上传的文件
     */
    private String saveUploadedFile(MultipartFile file, String category) throws IOException {
        // 创建上传目录
        Path uploadPath = Paths.get(UPLOAD_DIR, category);
        Files.createDirectories(uploadPath);

        // 生成唯一文件名
        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String uniqueName = UUID.randomUUID().toString() + "_" +
                           LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                           extension;

        Path filePath = uploadPath.resolve(uniqueName);
        file.transferTo(filePath.toFile());

        log.info("文件保存成功: {}", filePath);
        return filePath.toString();
    }

    /**
     * 生成输出文件名
     */
    private String generateOutputFileName(String taskName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String cleanTaskName = taskName.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "_");
        return String.format("P21_Report_%s_%s.xlsx", cleanTaskName, timestamp);
    }

    /**
     * 转换为DTO
     */
    private TaskStatusDTO convertToDTO(P21GenerationTask task) {
        TaskStatusDTO dto = new TaskStatusDTO();
        BeanUtils.copyProperties(task, dto);
        return dto;
    }
}