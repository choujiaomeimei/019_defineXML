package com.stat.service.impl;

import com.stat.common.entity.FileUploadRecord;
import com.stat.dal.mapper.FileUploadRecordMapper;
import com.stat.service.IFileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 统一文件上传服务实现类
 *
 * @author system
 * @date 2025-01-10
 */
@Service
@Transactional
public class FileUploadServiceImpl implements IFileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    @Resource
    private FileUploadRecordMapper fileUploadRecordMapper;

    @Value("${app.upload.base-path:E:/JAVAPROJ/008_defineXML/projects}")
    private String uploadBasePath;

    @Value("${app.python.script-path:E:/JAVAPROJ/008_defineXML/Python}")
    private String pythonScriptPath;

    @Override
    public FileUploadRecord handleFileUpload(MultipartFile file, String projectId,
                                           FileUploadRecord.FileCategory fileCategory,
                                           String username) {
        logger.info("开始处理文件上传: 项目={}, 类别={}, 文件={}, 用户={}",
                    projectId, fileCategory, file.getOriginalFilename(), username);

        try {
            // 验证文件
            validateFile(file, fileCategory);

            // 检查文件是否已存在
            if (isFileExists(projectId, fileCategory, file.getOriginalFilename())) {
                throw new RuntimeException("文件已存在: " + file.getOriginalFilename());
            }

            // 生成文件ID和路径
            String fileId = generateFileId();
            String filePath = getUploadPath(projectId, fileCategory, file.getOriginalFilename());
            String serverFileName = generateServerFileName(file.getOriginalFilename(), fileCategory);

            // 创建文件记录
            FileUploadRecord record = new FileUploadRecord(
                fileId, projectId, username, fileCategory,
                file.getOriginalFilename(), filePath, file.getSize()
            );
            record.setServerFileName(serverFileName);
            record.setFileMd5(calculateMD5(file));

            // 保存文件到磁盘
            Path targetPath = Paths.get(filePath);
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());

            // 标记上传成功
            record.markUploadSuccess();

            // 保存记录到数据库
            int result = fileUploadRecordMapper.insert(record);
            if (result <= 0) {
                throw new RuntimeException("保存文件记录失败");
            }

            // 复制文件到项目定义目录
            copyToDefineDirectory(projectId, fileCategory, filePath, file.getOriginalFilename());

            logger.info("文件上传成功: {}, 记录ID: {}", file.getOriginalFilename(), record.getId());
            return record;

        } catch (Exception e) {
            logger.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public FileUploadRecord getFileRecordById(Long id) {
        return fileUploadRecordMapper.selectById(id);
    }

    @Override
    public FileUploadRecord getFileRecordByFileId(String fileId) {
        return fileUploadRecordMapper.selectByFileId(fileId);
    }

    @Override
    public List<FileUploadRecord> getProjectFiles(String projectId) {
        return fileUploadRecordMapper.selectByProjectId(projectId);
    }

    @Override
    public List<FileUploadRecord> getProjectFilesByCategory(String projectId,
                                                          FileUploadRecord.FileCategory fileCategory) {
        return fileUploadRecordMapper.selectByProjectIdAndCategory(projectId, fileCategory.getCode());
    }

    @Override
    public List<FileUploadRecord> getUserFiles(String username) {
        return fileUploadRecordMapper.selectByUsername(username);
    }

    @Override
    public Map<String, Object> getProjectFileStats(String projectId) {
        List<FileUploadRecord> files = getProjectFiles(projectId);
        Map<String, Object> stats = new HashMap<>();

        // 按类别统计
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, Long> categorySize = new HashMap<>();
        long totalSize = 0;

        for (FileUploadRecord file : files) {
            String category = file.getFileCategory().getCode();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
            categorySize.put(category, categorySize.getOrDefault(category, 0L) + file.getFileSize());
            totalSize += file.getFileSize();
        }

        stats.put("totalFiles", files.size());
        stats.put("totalSize", totalSize);
        stats.put("categoryCount", categoryCount);
        stats.put("categorySize", categorySize);

        // 具体类别统计
        stats.put("p21SpecCount", categoryCount.getOrDefault("P21_SPEC", 0));
        stats.put("xptCount", categoryCount.getOrDefault("XPT", 0));
        stats.put("projectSpecCount", categoryCount.getOrDefault("PROJECT_SPEC", 0));
        stats.put("acrfCount", categoryCount.getOrDefault("ACRF", 0));
        stats.put("vlmCount", categoryCount.getOrDefault("VLM", 0));
        stats.put("codelistCount", categoryCount.getOrDefault("CODELIST", 0));

        return stats;
    }

    @Override
    public boolean updateUploadStatus(Long id, FileUploadRecord.UploadStatus status, String errorMessage) {
        int result = fileUploadRecordMapper.updateUploadStatus(id, status.getCode(), errorMessage);
        return result > 0;
    }

    @Override
    public boolean startProcessing(Long id) {
        FileUploadRecord record = getFileRecordById(id);
        if (record == null) {
            return false;
        }

        record.startProcessing();
        int result = fileUploadRecordMapper.updateById(record);
        return result > 0;
    }

    @Override
    public boolean markProcessCompleted(Long id, String outputFilePath) {
        FileUploadRecord record = getFileRecordById(id);
        if (record == null) {
            return false;
        }

        record.markProcessCompleted(outputFilePath);
        int result = fileUploadRecordMapper.updateById(record);
        return result > 0;
    }

    @Override
    public boolean markProcessFailed(Long id, String errorMessage) {
        FileUploadRecord record = getFileRecordById(id);
        if (record == null) {
            return false;
        }

        record.markProcessFailed(errorMessage);
        int result = fileUploadRecordMapper.updateById(record);
        return result > 0;
    }

    @Override
    public boolean deleteFileRecord(Long id) {
        int result = fileUploadRecordMapper.deleteById(id);
        return result > 0;
    }

    @Override
    public boolean deleteFileRecordByFileId(String fileId) {
        int result = fileUploadRecordMapper.deleteByFileId(fileId);
        return result > 0;
    }

    @Override
    public boolean isFileExists(String projectId, FileUploadRecord.FileCategory fileCategory, String originalName) {
        int count = fileUploadRecordMapper.existsFile(projectId, fileCategory.getCode(), originalName);
        return count > 0;
    }

    @Override
    public FileUploadRecord getLatestFileByCategory(String projectId, FileUploadRecord.FileCategory fileCategory) {
        return fileUploadRecordMapper.getLatestFileByCategory(projectId, fileCategory.getCode());
    }

    @Override
    public boolean processP21SpecFile(Long fileId) {
        logger.info("开始处理P21 Spec文件: {}", fileId);

        try {
            startProcessing(fileId);

            FileUploadRecord record = getFileRecordById(fileId);
            if (record == null) {
                throw new RuntimeException("文件记录不存在");
            }

            // 调用Python脚本处理P21 Spec文件
            String pythonScript = Paths.get(pythonScriptPath, "process_p21_spec.py").toString();
            String[] command = {
                "python", pythonScript,
                "--file", record.getFilePath(),
                "--project", record.getProjectId(),
                "--output", getOutputPath(record.getProjectId(), "p21_spec_processed.xlsx")
            };

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                markProcessCompleted(fileId, getOutputPath(record.getProjectId(), "p21_spec_processed.xlsx"));
                logger.info("P21 Spec文件处理完成: {}", fileId);
                return true;
            } else {
                markProcessFailed(fileId, "Python脚本执行失败，退出码: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("处理P21 Spec文件失败: {}", e.getMessage(), e);
            markProcessFailed(fileId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean processProjectSpecFile(Long fileId) {
        logger.info("开始处理项目Spec文件: {}", fileId);
        return executeProcessing(fileId, "process_project_spec.py", "project_spec_processed.xlsx");
    }

    @Override
    public boolean processAcrfFile(Long fileId) {
        logger.info("开始处理aCRF文件: {}", fileId);
        return executeProcessing(fileId, "process_acrf.py", "acrf_annotations.xlsx");
    }

    @Override
    public boolean generateVlmData(String projectId) {
        logger.info("开始生成VLM数据: {}", projectId);
        return executePythonScript(projectId, "generate_vlm.py", "vlm_generated.xlsx");
    }

    @Override
    public boolean generateCodeListData(String projectId) {
        logger.info("开始生成CodeList数据: {}", projectId);
        return executePythonScript(projectId, "generate_codelist.py", "codelist_generated.xlsx");
    }

    @Override
    public boolean updatePageInfo(String projectId) {
        logger.info("开始更新页面信息: {}", projectId);
        return executePythonScript(projectId, "update_page_info.py", "project_spec_updated.xlsx");
    }

    @Override
    public int cleanupFailedRecords(int days) {
        return fileUploadRecordMapper.cleanupFailedRecords(days);
    }

    // 私有辅助方法

    private void validateFile(MultipartFile file, FileUploadRecord.FileCategory fileCategory) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 根据文件类别验证文件扩展名
        String extension = getFileExtension(originalName).toLowerCase();
        switch (fileCategory) {
            case ACRF:
                if (!extension.equals("pdf")) {
                    throw new IllegalArgumentException("aCRF文件必须是PDF格式");
                }
                break;
            case P21_SPEC:
            case PROJECT_SPEC:
            case VLM:
            case CODELIST:
                if (!extension.equals("xlsx") && !extension.equals("xls")) {
                    throw new IllegalArgumentException("Spec文件必须是Excel格式");
                }
                break;
            case XPT:
                if (!extension.equals("xpt")) {
                    throw new IllegalArgumentException("数据文件必须是XPT格式");
                }
                break;
            default:
                throw new IllegalArgumentException("不支持的文件类型: " + fileCategory);
        }

        // 文件大小验证（100MB）
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过100MB");
        }
    }

    private String generateFileId() {
        return "FILE_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String getUploadPath(String projectId, FileUploadRecord.FileCategory fileCategory, String originalName) {
        String categoryDir = getCategoryDirectory(fileCategory);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = timestamp + "_" + originalName;
        return Paths.get(uploadBasePath, projectId, "uploads", categoryDir, fileName).toString();
    }

    private String generateServerFileName(String originalName, FileUploadRecord.FileCategory fileCategory) {
        String extension = getFileExtension(originalName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + fileCategory.getCode() + "_" + randomSuffix + "." + extension;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }

    private String getCategoryDirectory(FileUploadRecord.FileCategory fileCategory) {
        switch (fileCategory) {
            case P21_SPEC: return "p21-spec";
            case XPT: return "xpt";
            case PROJECT_SPEC: return "project-spec";
            case ACRF: return "acrf";
            case VLM: return "vlm";
            case CODELIST: return "codelist";
            default: return "others";
        }
    }

    private String calculateMD5(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.warn("计算文件MD5失败", e);
            return null;
        }
    }

    private void copyToDefineDirectory(String projectId, FileUploadRecord.FileCategory fileCategory,
                                     String filePath, String originalName) {
        try {
            String defineDir = getDefineDirectory(fileCategory);
            String targetFileName = getStandardFileName(fileCategory, originalName);
            Path targetPath = Paths.get(uploadBasePath, projectId, "define", defineDir, targetFileName);

            Files.createDirectories(targetPath.getParent());
            Files.copy(Paths.get(filePath), targetPath);

            logger.debug("文件已复制到定义目录: {}", targetPath);
        } catch (IOException e) {
            logger.warn("复制文件到定义目录失败: {}", e.getMessage());
        }
    }

    private String getDefineDirectory(FileUploadRecord.FileCategory fileCategory) {
        switch (fileCategory) {
            case P21_SPEC: return "p21空spec";
            case PROJECT_SPEC: return "项目Spec";
            case ACRF: return "SDTM注释CRF";
            default: return "others";
        }
    }

    private String getStandardFileName(FileUploadRecord.FileCategory fileCategory, String originalName) {
        String extension = getFileExtension(originalName);
        switch (fileCategory) {
            case P21_SPEC: return "P21空Spec." + extension;
            case PROJECT_SPEC: return "项目Spec." + extension;
            case ACRF: return "aCRF." + extension;
            default: return originalName;
        }
    }

    private String getOutputPath(String projectId, String fileName) {
        return Paths.get(uploadBasePath, projectId, "output", fileName).toString();
    }

    private boolean executeProcessing(Long fileId, String scriptName, String outputFileName) {
        try {
            startProcessing(fileId);

            FileUploadRecord record = getFileRecordById(fileId);
            if (record == null) {
                throw new RuntimeException("文件记录不存在");
            }

            String pythonScript = Paths.get(pythonScriptPath, scriptName).toString();
            String outputPath = getOutputPath(record.getProjectId(), outputFileName);

            String[] command = {
                "python", pythonScript,
                "--file", record.getFilePath(),
                "--project", record.getProjectId(),
                "--output", outputPath
            };

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                markProcessCompleted(fileId, outputPath);
                return true;
            } else {
                markProcessFailed(fileId, "Python脚本执行失败，退出码: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("执行文件处理失败: {}", e.getMessage(), e);
            markProcessFailed(fileId, e.getMessage());
            return false;
        }
    }

    private boolean executePythonScript(String projectId, String scriptName, String outputFileName) {
        try {
            String pythonScript = Paths.get(pythonScriptPath, scriptName).toString();
            String outputPath = getOutputPath(projectId, outputFileName);

            String[] command = {
                "python", pythonScript,
                "--project", projectId,
                "--output", outputPath
            };

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Python脚本执行成功: {}", scriptName);
                return true;
            } else {
                logger.error("Python脚本执行失败: {}，退出码: {}", scriptName, exitCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("执行Python脚本失败: {}", e.getMessage(), e);
            return false;
        }
    }
}