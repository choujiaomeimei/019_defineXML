package com.stat.admin.service.impl;

import com.stat.admin.service.P21TaskService;
import com.stat.admin.service.PythonIntegrationService;
import com.stat.common.dto.P21GenerationRequestDTO;
import com.stat.common.dto.TaskStatusDTO;
import com.stat.common.entity.P21GenerationTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PythonIntegrationServiceImpl implements PythonIntegrationService {

    private final P21TaskService taskService;
    private final ObjectMapper objectMapper;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonBasePath;

    private static final String PYTHON_ENV_PATH = "python";
    private static final long TIMEOUT_MINUTES = 30;

    @Override
    @Async("p21TaskExecutor")
    public void executeP21GenerationAsync(Long taskId, P21GenerationRequestDTO request) {
        log.info("开始异步执行P21报告生成任务: {}", taskId);

        try {
            // 1. 更新任务状态为处理中
            taskService.updateTaskStatus(taskId, P21GenerationTask.TaskStatus.PROCESSING);
            taskService.updateTaskProgress(taskId, 10);

            // 2. 准备Python执行环境和配置文件
            String configFilePath = prepareConfigFile(taskId, request);
            taskService.updateTaskProgress(taskId, 20);

            // 3. 构建Python命令
            ProcessBuilder processBuilder = buildPythonCommand(configFilePath);
            taskService.updateTaskProgress(taskId, 30);

            // 4. 执行Python脚本
            log.info("执行Python命令: {}", processBuilder.command());
            Process process = processBuilder.start();

            // 5. 监控执行过程
            boolean success = monitorPythonExecution(process, taskId);

            // 6. 处理执行结果
            if (success) {
                handleSuccessResult(taskId, request);
            } else {
                handleFailureResult(process, taskId);
            }

        } catch (Exception e) {
            log.error("执行P21生成任务失败: {}", e.getMessage(), e);
            taskService.failTask(taskId, "系统错误: " + e.getMessage());
        }
    }

    @Override
    public boolean isPythonEnvironmentAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON_ENV_PATH, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            log.info("Python环境检查结果: exitCode={}, output={}", exitCode, output.toString());
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("Python环境检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isProcessRunning(String processId) {
        // 这里可以实现进程状态检查逻辑
        // 由于Java Process对象不能持久化，这里简化处理
        return false;
    }

    @Override
    public boolean terminateProcess(String processId) {
        // 这里可以实现进程终止逻辑
        log.info("尝试终止进程: {}", processId);
        return true;
    }

    /**
     * 准备Python脚本的配置文件
     */
    private String prepareConfigFile(Long taskId, P21GenerationRequestDTO request) throws IOException {
        log.info("准备配置文件，任务ID: {}", taskId);

        // 获取任务详情
        TaskStatusDTO taskStatus = taskService.getTaskStatus(taskId);
        log.info("任务详情: specFilePath={}, templateFilePath={}", taskStatus.getSpecFilePath(), taskStatus.getTemplateFilePath());

        // 创建配置对象
        Map<String, Object> config = new HashMap<>();
        config.put("PROTOCAL", request.getProtocolId());
        config.put("STUDY_TITLE", request.getStudyTitle());
        config.put("BASE_DIR", Paths.get(pythonBasePath).getParent().toString());

        // 使用上传文件的完整路径
        config.put("SPEC_FILE_PATH", taskStatus.getSpecFilePath());
        config.put("TEMPLATE_FILE_PATH", taskStatus.getTemplateFilePath());
        config.put("SPEC", taskStatus.getSpecFileName());
        config.put("P21_SPEC", taskStatus.getTemplateFileName());
        config.put("CT_SHEETNAME", request.getCtSheetName() != null ? request.getCtSheetName() : "");
        config.put("OUTPUT_DIR", pythonBasePath + "/output");

        // 创建配置文件
        String configFileName = String.format("config_task_%d_%s.json",
                                             taskId,
                                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        Path configDir = Paths.get(pythonBasePath, "config");
        Files.createDirectories(configDir);

        Path configFilePath = configDir.resolve(configFileName);
        objectMapper.writeValue(configFilePath.toFile(), config);

        log.info("配置文件创建成功: {}", configFilePath);
        log.info("配置内容: {}", objectMapper.writeValueAsString(config));
        return configFilePath.toString();
    }

    /**
     * 构建Python执行命令
     */
    private ProcessBuilder buildPythonCommand(String configFilePath) {
        String scriptPath = Paths.get(pythonBasePath, "pgm", "simple_test.py").toString();
        ProcessBuilder pb = new ProcessBuilder(
                PYTHON_ENV_PATH,
                scriptPath,
                "--config", configFilePath
        );

        pb.directory(new File(pythonBasePath));

        // 重定向错误输出到标准输出
        pb.redirectErrorStream(true);

        return pb;
    }

    /**
     * 监控Python脚本执行
     */
    private boolean monitorPythonExecution(Process process, Long taskId) {
        try {
            // 读取输出流
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));

            // 使用独立线程读取输出，避免缓冲区满导致阻塞
            Thread outputThread = new Thread(() -> {
                try {
                    String line;
                    int progress = 30;
                    while ((line = reader.readLine()) != null) {
                        log.info("Python输出: {}", line);

                        // 根据输出内容更新进度
                        if (line.contains("正在处理") && progress < 90) {
                            progress += 10;
                            taskService.updateTaskProgress(taskId, progress);
                        }
                    }
                } catch (IOException e) {
                    log.error("读取Python输出失败: {}", e.getMessage());
                }
            });

            outputThread.start();

            // 等待进程完成，设置超时
            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (!finished) {
                log.error("Python脚本执行超时，强制终止进程");
                process.destroyForcibly();
                taskService.failTask(taskId, "执行超时（超过" + TIMEOUT_MINUTES + "分钟）");
                return false;
            }

            int exitCode = process.exitValue();
            log.info("Python脚本执行完成，退出码: {}", exitCode);

            return exitCode == 0;

        } catch (Exception e) {
            log.error("监控Python执行失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理成功结果
     */
    private void handleSuccessResult(Long taskId, P21GenerationRequestDTO request) {
        log.info("P21报告生成成功，任务ID: {}", taskId);

        // 查找生成的输出文件
        String outputPath = findOutputFile(taskId);

        if (outputPath != null) {
            taskService.completeTask(taskId, outputPath);
            taskService.updateTaskProgress(taskId, 100);
        } else {
            taskService.failTask(taskId, "生成成功但未找到输出文件");
        }
    }

    /**
     * 处理失败结果
     */
    private void handleFailureResult(Process process, Long taskId) {
        log.error("P21报告生成失败，任务ID: {}", taskId);

        try {
            // 读取错误输出
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
            StringBuilder errorMessage = new StringBuilder();
            String line;

            while ((line = errorReader.readLine()) != null) {
                errorMessage.append(line).append("\n");
            }

            String error = errorMessage.toString();
            if (error.isEmpty()) {
                error = "Python脚本执行失败，退出码: " + process.exitValue();
            }

            taskService.failTask(taskId, error);

        } catch (IOException e) {
            log.error("读取错误信息失败: {}", e.getMessage());
            taskService.failTask(taskId, "执行失败且无法获取错误信息");
        }
    }

    /**
     * 查找输出文件
     */
    private String findOutputFile(Long taskId) {
        try {
            Path outputDir = Paths.get(pythonBasePath, "output");

            if (!Files.exists(outputDir)) {
                log.warn("输出目录不存在: {}", outputDir);
                return null;
            }

            // 优先查找带任务ID的测试文件
            Path testFile = outputDir.resolve(String.format("test_output_task_%d.xlsx", taskId));
            if (Files.exists(testFile)) {
                log.info("找到测试输出文件: {}", testFile);
                return testFile.toString();
            }

            // 查找最新的xlsx文件
            return Files.list(outputDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".xlsx"))
                    .filter(path -> path.toString().contains("sdtm_define_fixed") || path.toString().contains("test_output"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .findFirst()
                    .map(Path::toString)
                    .orElse(null);

        } catch (IOException e) {
            log.error("查找输出文件失败: {}", e.getMessage(), e);
            return null;
        }
    }
}