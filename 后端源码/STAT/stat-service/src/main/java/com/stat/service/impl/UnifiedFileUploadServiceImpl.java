package com.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stat.common.entity.FileUploadRecord;
import com.stat.common.entity.FileVersionHistory;
import com.stat.dal.mapper.FileUploadRecordMapper;
import com.stat.dal.mapper.FileVersionHistoryMapper;
import com.stat.service.ProjectFilePathResolver;
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
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UnifiedFileUploadServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(UnifiedFileUploadServiceImpl.class);

    private static final Set<String> SINGLE_FILE_CATEGORIES = new HashSet<>(
            Arrays.asList(FileUploadRecord.FileCategory.P21_SPEC,
                    FileUploadRecord.FileCategory.PROJECT_SPEC,
                    FileUploadRecord.FileCategory.ACRF,
                    FileUploadRecord.FileCategory.EDC_CODELIST));

    @Autowired
    private FileUploadRecordMapper fileUploadRecordMapper;

    @Autowired
    private FileVersionHistoryMapper fileVersionHistoryMapper;

    @Autowired(required = false)
    private com.stat.service.ProjectSpecService projectSpecService;

    @Autowired
    private ProjectFilePathResolver pathResolver;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    @Transactional(rollbackFor = Exception.class)
    public FileUploadRecord uploadFile(String projectId, String fileCategory,
                                        String standardType, String username,
                                        MultipartFile file) throws IOException {
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);
        String serverFileName = fileId + "_" + originalName;
        String resolvedStandardType = pathResolver.resolveStandardType(projectId, standardType);
        Path archiveDir = pathResolver.archiveDirectory(projectId, fileCategory);
        Path archivePath = archiveDir.resolve(serverFileName);
        Path workspacePath = pathResolver.workspaceFile(
                projectId, resolvedStandardType, fileCategory, originalName);
        Files.createDirectories(archiveDir);
        Files.createDirectories(workspacePath.getParent());

        int newVersion = 1;
        FileUploadRecord replacedRecord = null;

        if (SINGLE_FILE_CATEGORIES.contains(fileCategory)) {
            List<FileUploadRecord> existing = fileUploadRecordMapper.selectByProjectIdAndCategory(projectId, fileCategory);
            if (existing != null && !existing.isEmpty()) {
                FileUploadRecord old = existing.get(0);
                replacedRecord = old;
                newVersion = (old.getVersionNumber() != null ? old.getVersionNumber() : 1) + 1;

                archiveToHistory(old, username);

                LambdaQueryWrapper<FileUploadRecord> delWrapper = new LambdaQueryWrapper<>();
                delWrapper.eq(FileUploadRecord::getProjectId, projectId)
                          .eq(FileUploadRecord::getFileCategory, fileCategory)
                          .eq(FileUploadRecord::getDeleted, 0);
                fileUploadRecordMapper.delete(delWrapper);

                log.info("已归档旧文件 [{}] v{}, 准备写入 v{}", old.getOriginalName(), old.getVersionNumber(), newVersion);
            }
        } else if (FileUploadRecord.FileCategory.XPT.equals(fileCategory)) {
            List<FileUploadRecord> existing = fileUploadRecordMapper.selectByProjectIdAndCategory(projectId, fileCategory);
            if (existing != null) {
                for (FileUploadRecord old : existing) {
                    if (old.getOriginalName() != null && old.getOriginalName().equalsIgnoreCase(originalName)) {
                        replacedRecord = old;
                        newVersion = (old.getVersionNumber() != null ? old.getVersionNumber() : 1) + 1;
                        archiveToHistory(old, username);
                        fileUploadRecordMapper.deleteById(old.getId());
                        break;
                    }
                }
            }
        }

        try {
            file.transferTo(archivePath);
            copyAtomically(archivePath, workspacePath);

            FileUploadRecord record = new FileUploadRecord();
            record.setFileId(fileId);
            record.setProjectId(projectId);
            record.setUsername(username);
            record.setFileCategory(fileCategory);
            record.setOriginalName(originalName);
            record.setServerFileName(serverFileName);
            record.setFilePath(archivePath.toAbsolutePath().toString());
            record.setWorkspaceFilePath(workspacePath.toAbsolutePath().toString());
            record.setStandardType(resolvedStandardType);
            record.setFileSize(file.getSize());
            record.setFileExtension(extension);
            record.setUploadTime(LocalDateTime.now());
            record.setUploadStatus(FileUploadRecord.UploadStatus.SUCCESS);
            record.setProcessStatus(FileUploadRecord.ProcessStatus.PENDING);
            record.setVersionNumber(newVersion);
            record.setDeleted(0);

            fileUploadRecordMapper.insert(record);
            log.info("文件上传并同步工作区成功: {} v{} -> {}", originalName, newVersion, workspacePath);
            return record;
        } catch (Exception e) {
            Files.deleteIfExists(archivePath);
            if (replacedRecord != null && replacedRecord.getFilePath() != null
                    && Files.exists(Paths.get(replacedRecord.getFilePath()))) {
                copyAtomically(Paths.get(replacedRecord.getFilePath()), workspacePath);
            } else {
                Files.deleteIfExists(workspacePath);
            }
            if (e instanceof IOException ioException) throw ioException;
            throw new IOException("保存文件及工作副本失败", e);
        }
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
        List<FileUploadRecord> records;
        if (fileCategory != null && !fileCategory.isEmpty()) {
            records = fileUploadRecordMapper.selectByProjectIdAndCategory(projectId, fileCategory);
        } else {
            records = fileUploadRecordMapper.selectByProjectId(projectId);
        }
        records.forEach(this::ensureWorkspaceCopy);
        return records;
    }

    public FileUploadRecord getFileByFileId(String fileId) {
        return fileUploadRecordMapper.selectByFileId(fileId);
    }

    public FileUploadRecord getCurrentFile(String projectId, String fileCategory) {
        List<FileUploadRecord> records = fileUploadRecordMapper.selectByProjectIdAndCategory(projectId, fileCategory);
        if (records == null || records.isEmpty()) return null;
        FileUploadRecord record = records.get(0);
        ensureWorkspaceCopy(record);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(String fileId) {
        FileUploadRecord record = fileUploadRecordMapper.selectByFileId(fileId);
        if (record == null) return false;
        int deleted = fileUploadRecordMapper.deleteById(record.getId());
        if (deleted <= 0) return false;
        deleteQuietly(record.getWorkspaceFilePath());
        deleteQuietly(record.getOutputFilePath());
        deleteQuietly(record.getFilePath());
        return true;
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

        ensureWorkspaceCopy(record);
        Path filePath = resolveWorkspacePath(record);
        if (!Files.exists(filePath)) {
            result.put("success", false);
            result.put("message", "项目工作副本不存在，请重新上传文件: " + filePath);
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

        Path canonicalOutputPath = pathResolver.acrfAnnotations(
                record.getProjectId(), record.getStandardType());
        Files.createDirectories(canonicalOutputPath.getParent());

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(pythonPath, "pgm", "extract_pdf_annotations.py").toString());
        command.add(resolveWorkspacePath(record).toString());
        command.add(canonicalOutputPath.toString());

        log.info("执行Python命令: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(pythonPath));
        pb.redirectErrorStream(true);
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (line.startsWith("{") || line.contains("success") || line.contains("ERROR")) {
                    log.info("Python输出: {}", line);
                } else {
                    log.debug("Python输出: {}", line);
                }
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
            String workspaceFile = resolveWorkspacePath(record).toString();
            log.info("开始解析项目SPEC文件, projectId={}, filePath={}", projectId, workspaceFile);

            if (projectSpecService != null) {
                int parsedCount = projectSpecService.parseAndSaveProjectSpec(workspaceFile, projectId);
                if (parsedCount > 0) {
                    String outputDir = pathResolver.projectSpecDirectory(
                            projectId, record.getStandardType()).toString();
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

    private Path resolveWorkspacePath(FileUploadRecord record) {
        if (record.getWorkspaceFilePath() != null && !record.getWorkspaceFilePath().isBlank()) {
            return Paths.get(record.getWorkspaceFilePath());
        }
        return pathResolver.workspaceFile(
                record.getProjectId(),
                pathResolver.resolveStandardType(record.getProjectId(), record.getStandardType()),
                record.getFileCategory(),
                record.getOriginalName());
    }

    private void ensureWorkspaceCopy(FileUploadRecord record) {
        try {
            String standardType = pathResolver.resolveStandardType(
                    record.getProjectId(), record.getStandardType());
            Path workspace = pathResolver.workspaceFile(
                    record.getProjectId(), standardType,
                    record.getFileCategory(), record.getOriginalName());
            Path archive = Paths.get(record.getFilePath());
            if (!Files.exists(archive)) {
                record.setUploadStatus(FileUploadRecord.UploadStatus.FAILED);
                record.setProcessStatus(FileUploadRecord.ProcessStatus.FAILED);
                record.setErrorMessage("原始归档文件不存在: " + archive);
                fileUploadRecordMapper.updateById(record);
                return;
            }
            if (!Files.exists(workspace)) {
                copyAtomically(archive, workspace);
            }
            String previousOutputPath = record.getOutputFilePath();
            String previousProcessStatus = record.getProcessStatus();
            String previousErrorMessage = record.getErrorMessage();
            migrateOutputFile(record, standardType);
            String workspaceValue = workspace.toAbsolutePath().toString();
            if (!workspaceValue.equals(record.getWorkspaceFilePath())
                    || !standardType.equals(record.getStandardType())
                    || !Objects.equals(previousOutputPath, record.getOutputFilePath())
                    || !Objects.equals(previousProcessStatus, record.getProcessStatus())
                    || !Objects.equals(previousErrorMessage, record.getErrorMessage())) {
                record.setWorkspaceFilePath(workspaceValue);
                record.setStandardType(standardType);
                fileUploadRecordMapper.updateById(record);
            }
        } catch (Exception e) {
            log.warn("同步历史文件到项目工作区失败: fileId={}", record.getFileId(), e);
            record.setProcessStatus(FileUploadRecord.ProcessStatus.FAILED);
            record.setErrorMessage("同步项目工作区失败: " + e.getMessage());
            fileUploadRecordMapper.updateById(record);
        }
    }

    private void migrateOutputFile(FileUploadRecord record, String standardType) throws IOException {
        if (record.getOutputFilePath() == null || record.getOutputFilePath().isBlank()) return;
        Path oldOutput = Paths.get(record.getOutputFilePath());
        Path target = null;
        if (FileUploadRecord.FileCategory.ACRF.equals(record.getFileCategory())) {
            target = pathResolver.acrfAnnotations(record.getProjectId(), standardType);
        } else if (FileUploadRecord.FileCategory.PROJECT_SPEC.equals(record.getFileCategory())) {
            target = pathResolver.projectSpecDirectory(record.getProjectId(), standardType)
                    .resolve(oldOutput.getFileName());
        }
        if (target == null) return;
        if (!Files.exists(target) && Files.exists(oldOutput)) {
            copyAtomically(oldOutput, target);
        }
        if (Files.exists(target)) {
            record.setOutputFilePath(target.toAbsolutePath().toString());
        } else if (FileUploadRecord.FileCategory.ACRF.equals(record.getFileCategory())
                && FileUploadRecord.ProcessStatus.COMPLETED.equals(record.getProcessStatus())) {
            record.setProcessStatus(FileUploadRecord.ProcessStatus.FAILED);
            record.setErrorMessage("解析产物不存在，请重新处理 aCRF: " + target);
        }
    }

    private void copyAtomically(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp-" + UUID.randomUUID());
        try {
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void deleteQuietly(String path) {
        if (path == null || path.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("清理文件失败: {}", path, e);
        }
    }
}
