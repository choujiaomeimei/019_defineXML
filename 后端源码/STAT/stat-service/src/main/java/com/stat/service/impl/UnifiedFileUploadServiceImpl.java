package com.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stat.common.entity.FileUploadRecord;
import com.stat.common.entity.FileVersionHistory;
import com.stat.dal.mapper.FileUploadRecordMapper;
import com.stat.dal.mapper.FileVersionHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UnifiedFileUploadServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(UnifiedFileUploadServiceImpl.class);

    private static final Set<String> SINGLE_FILE_CATEGORIES = new HashSet<>(
            Arrays.asList(FileUploadRecord.FileCategory.P21_SPEC,
                    FileUploadRecord.FileCategory.PROJECT_SPEC,
                    FileUploadRecord.FileCategory.ACRF));

    @Autowired
    private FileUploadRecordMapper fileUploadRecordMapper;

    @Autowired
    private FileVersionHistoryMapper fileVersionHistoryMapper;

    @Autowired(required = false)
    private com.stat.service.ProjectSpecService projectSpecService;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadBasePath;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    @Value("${app.projects.base-path:C:/Project_Web/019_defineXML/projects}")
    private String projectsPath;

    @Transactional(rollbackFor = Exception.class)
    public FileUploadRecord uploadFile(String projectId, String fileCategory,
                                        String username, MultipartFile file) throws IOException {
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);
        String serverFileName = fileId + "_" + originalName;

        String categoryDir = fileCategory.toLowerCase().replace("_", "-");
        String dirPath = uploadBasePath + "/" + projectId + "/" + categoryDir;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        int newVersion = 1;

        if (SINGLE_FILE_CATEGORIES.contains(fileCategory)) {
            List<FileUploadRecord> existing = fileUploadRecordMapper.selectByProjectIdAndCategory(projectId, fileCategory);
            if (existing != null && !existing.isEmpty()) {
                FileUploadRecord old = existing.get(0);
                newVersion = (old.getVersionNumber() != null ? old.getVersionNumber() : 1) + 1;

                archiveToHistory(old, username);

                LambdaQueryWrapper<FileUploadRecord> delWrapper = new LambdaQueryWrapper<>();
                delWrapper.eq(FileUploadRecord::getProjectId, projectId)
                          .eq(FileUploadRecord::getFileCategory, fileCategory)
                          .eq(FileUploadRecord::getDeleted, 0);
                fileUploadRecordMapper.delete(delWrapper);

                log.info("已归档旧文件 [{}] v{}, 准备写入 v{}", old.getOriginalName(), old.getVersionNumber(), newVersion);
            }
        }

        File dest = new File(dir, serverFileName);
        file.transferTo(dest);

        FileUploadRecord record = new FileUploadRecord();
        record.setFileId(fileId);
        record.setProjectId(projectId);
        record.setUsername(username);
        record.setFileCategory(fileCategory);
        record.setOriginalName(originalName);
        record.setServerFileName(serverFileName);
        record.setFilePath(dest.getAbsolutePath());
        record.setFileSize(file.getSize());
        record.setFileExtension(extension);
        record.setUploadTime(LocalDateTime.now());
        record.setUploadStatus(FileUploadRecord.UploadStatus.SUCCESS);
        record.setProcessStatus(FileUploadRecord.ProcessStatus.PENDING);
        record.setVersionNumber(newVersion);
        record.setDeleted(0);

        fileUploadRecordMapper.insert(record);
        log.info("文件上传成功: {} v{} -> {}", originalName, newVersion, projectId);
        return record;
    }

    private void archiveToHistory(FileUploadRecord old, String replacedBy) {
        FileVersionHistory history = new FileVersionHistory();
        history.setFileId(old.getFileId());
        history.setProjectId(old.getProjectId());
        history.setUsername(old.getUsername());
        history.setFileCategory(old.getFileCategory());
        history.setVersionNumber(old.getVersionNumber() != null ? old.getVersionNumber() : 1);
        history.setOriginalName(old.getOriginalName());
        history.setServerFileName(old.getServerFileName());
        history.setFilePath(old.getFilePath());
        history.setFileSize(old.getFileSize());
        history.setFileExtension(old.getFileExtension());
        history.setFileMd5(old.getFileMd5());
        history.setUploadTime(old.getUploadTime());
        history.setProcessStatus(old.getProcessStatus());
        history.setReplacedTime(LocalDateTime.now());
        history.setReplacedBy(replacedBy);
        fileVersionHistoryMapper.insert(history);
    }

    public List<FileUploadRecord> listFiles(String projectId, String fileCategory) {
        if (fileCategory != null && !fileCategory.isEmpty()) {
            return fileUploadRecordMapper.selectByProjectIdAndCategory(projectId, fileCategory);
        }
        return fileUploadRecordMapper.selectByProjectId(projectId);
    }

    public FileUploadRecord getFileByFileId(String fileId) {
        return fileUploadRecordMapper.selectByFileId(fileId);
    }

    public FileUploadRecord getCurrentFile(String projectId, String fileCategory) {
        List<FileUploadRecord> records = fileUploadRecordMapper.selectByProjectIdAndCategory(projectId, fileCategory);
        return (records != null && !records.isEmpty()) ? records.get(0) : null;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(String fileId) {
        LambdaQueryWrapper<FileUploadRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileUploadRecord::getFileId, fileId);
        return fileUploadRecordMapper.delete(wrapper) > 0;
    }

    public List<FileVersionHistory> getFileHistory(String projectId, String fileCategory) {
        return fileVersionHistoryMapper.selectByProjectIdAndCategory(projectId, fileCategory);
    }

    public List<FileVersionHistory> getAllHistory(String projectId) {
        LambdaQueryWrapper<FileVersionHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileVersionHistory::getProjectId, projectId)
               .orderByDesc(FileVersionHistory::getReplacedTime);
        return fileVersionHistoryMapper.selectList(wrapper);
    }

    public Map<String, Object> processFile(String fileId) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        FileUploadRecord record = fileUploadRecordMapper.selectByFileId(fileId);
        if (record == null) {
            result.put("success", false);
            result.put("message", "未找到上传记录");
            return result;
        }

        Path filePath = Paths.get(record.getFilePath());
        if (!Files.exists(filePath)) {
            result.put("success", false);
            result.put("message", "上传文件不存在: " + record.getFilePath());
            return result;
        }

        fileUploadRecordMapper.updateProcessStatus(fileId,
                FileUploadRecord.ProcessStatus.PROCESSING,
                LocalDateTime.now(), null, null, null);

        try {
            String category = record.getFileCategory();
            switch (category) {
                case FileUploadRecord.FileCategory.ACRF:
                    result = processAcrf(record);
                    break;
                case FileUploadRecord.FileCategory.PROJECT_SPEC:
                    result = processProjectSpec(record);
                    break;
                case FileUploadRecord.FileCategory.P21_SPEC:
                case FileUploadRecord.FileCategory.XPT:
                    result.put("success", true);
                    result.put("message", category + " 文件标记为已处理");
                    break;
                default:
                    result.put("success", true);
                    result.put("message", "文件已标记为处理完成");
            }
        } catch (Exception e) {
            log.error("文件处理异常: fileId={}, category={}", fileId, record.getFileCategory(), e);
            result.put("success", false);
            result.put("message", "处理异常: " + e.getMessage());
        }

        boolean success = Boolean.TRUE.equals(result.get("success"));
        int durationMs = (int) (System.currentTimeMillis() - startTime);
        String outputPath = (String) result.get("outputFilePath");
        String errorMsg = success ? null : (String) result.get("message");

        fileUploadRecordMapper.updateProcessStatus(fileId,
                success ? FileUploadRecord.ProcessStatus.COMPLETED : FileUploadRecord.ProcessStatus.FAILED,
                LocalDateTime.now(), durationMs, outputPath, errorMsg);

        result.put("processStatus", success ? "completed" : "failed");
        result.put("processDurationMs", durationMs);
        return result;
    }

    private Map<String, Object> processAcrf(FileUploadRecord record) throws Exception {
        Map<String, Object> result = new HashMap<>();

        Path projectOutputDir = Paths.get(uploadBasePath, record.getProjectId(), "output");
        if (!Files.exists(projectOutputDir)) {
            Files.createDirectories(projectOutputDir);
        }
        Path canonicalOutputPath = projectOutputDir.resolve("Annots2.xlsx");

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(pythonPath, "pgm", "extract_pdf_annotations.py").toString());
        command.add(record.getFilePath());
        command.add(canonicalOutputPath.toString());

        log.info("执行Python命令: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(pythonPath));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("Python输出: {}", line);
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        int exitCode = finished ? process.exitValue() : -1;
        if (!finished) {
            process.destroyForcibly();
            result.put("success", false);
            result.put("message", "Python脚本执行超时");
            return result;
        }

        if (exitCode == 0 && Files.exists(canonicalOutputPath)) {
            result.put("success", true);
            result.put("message", "aCRF解析成功");
            result.put("outputFilePath", canonicalOutputPath.toString());
            result.put("outputFileName", "Annots2.xlsx");
        } else {
            result.put("success", false);
            result.put("message", "Python脚本执行失败，退出码: " + exitCode);
        }

        return result;
    }

    private Map<String, Object> processProjectSpec(FileUploadRecord record) {
        Map<String, Object> result = new HashMap<>();
        try {
            String projectId = record.getProjectId();
            log.info("开始解析项目SPEC文件, projectId={}, filePath={}", projectId, record.getFilePath());

            if (projectSpecService != null) {
                int parsedCount = projectSpecService.parseAndSaveProjectSpec(record.getFilePath(), projectId);
                if (parsedCount > 0) {
                    String outputDir = uploadBasePath + "/" + projectId + "/output";
                    try {
                        String exportedPath = projectSpecService.exportProjectSpecToExcel(projectId, outputDir);
                        if (exportedPath != null) {
                            result.put("outputFilePath", exportedPath);
                            log.info("SPEC导出Excel成功: {}", exportedPath);
                        }
                    } catch (Exception exportEx) {
                        log.warn("SPEC导出Excel失败，但解析已成功: {}", exportEx.getMessage());
                    }
                    try {
                        String syncedPath = projectSpecService.syncSpecToFile(projectId, outputDir);
                        if (syncedPath != null) {
                            log.info("SPEC同步文件已生成: {}", syncedPath);
                        }
                    } catch (Exception syncEx) {
                        log.warn("SPEC同步文件生成失败，但解析已成功: {}", syncEx.getMessage());
                    }
                    result.put("success", true);
                    result.put("message", String.format("项目SPEC解析成功，共 %d 条记录", parsedCount));
                } else {
                    result.put("success", false);
                    result.put("message", "未解析到有效的SPEC数据，请检查文件格式");
                }
            } else {
                result.put("success", true);
                result.put("message", "项目SPEC文件已标记为处理完成");
            }
        } catch (Exception e) {
            log.error("项目SPEC处理失败", e);
            result.put("success", false);
            String msg = e.getMessage();
            result.put("message", "SPEC处理失败: " + (msg != null && msg.length() > 80 ? msg.substring(0, 80) + "..." : msg));
        }
        return result;
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
}
