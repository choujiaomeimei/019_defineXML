package com.stat.web.controller;

import com.stat.common.entity.FileUploadRecord;
import com.stat.common.entity.FileVersionHistory;
import com.stat.common.result.CommonResult;
import com.stat.service.impl.UnifiedFileUploadServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/files")

public class UnifiedFileController {

    private static final Logger log = LoggerFactory.getLogger(UnifiedFileController.class);

    @Autowired
    private UnifiedFileUploadServiceImpl unifiedFileUploadService;

    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS = new HashMap<>();
    static {
        ALLOWED_EXTENSIONS.put("ACRF", new HashSet<>(Arrays.asList("pdf")));
        ALLOWED_EXTENSIONS.put("P21_SPEC", new HashSet<>(Arrays.asList("xlsx", "xls")));
        ALLOWED_EXTENSIONS.put("PROJECT_SPEC", new HashSet<>(Arrays.asList("xlsx", "xls")));
        ALLOWED_EXTENSIONS.put("XPT", new HashSet<>(Arrays.asList("xpt")));
    }

    @PostMapping("/upload")
    public CommonResult<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") String projectId,
            @RequestParam("fileCategory") String fileCategory,
            @RequestParam(value = "username", required = false) String username) {
        try {
            if (file.isEmpty()) {
                return CommonResult.failed("文件不能为空");
            }

            String normalizedCategory = fileCategory.toUpperCase().replace("-", "_");
            Set<String> allowed = ALLOWED_EXTENSIONS.get(normalizedCategory);
            if (allowed != null) {
                String ext = getFileExtension(file.getOriginalFilename());
                if (!allowed.contains(ext)) {
                    return CommonResult.failed("不支持的文件格式: ." + ext + ", 允许: " + allowed);
                }
            }

            if (username == null || username.isEmpty()) {
                username = "system";
            }

            FileUploadRecord record = unifiedFileUploadService.uploadFile(
                    projectId, normalizedCategory, username, file);

            Map<String, Object> data = new HashMap<>();
            data.put("fileId", record.getFileId());
            data.put("originalName", record.getOriginalName());
            data.put("fileSize", record.getFileSize());
            data.put("versionNumber", record.getVersionNumber());
            data.put("uploadTime", record.getUploadTime());
            data.put("fileCategory", record.getFileCategory());
            data.put("processStatus", record.getProcessStatus());

            return CommonResult.success(data);
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return CommonResult.failed("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public CommonResult<?> listFiles(
            @RequestParam("projectId") String projectId,
            @RequestParam(value = "fileCategory", required = false) String fileCategory) {
        try {
            String normalizedCategory = null;
            if (fileCategory != null && !fileCategory.isEmpty()) {
                normalizedCategory = fileCategory.toUpperCase().replace("-", "_");
            }
            List<FileUploadRecord> files = unifiedFileUploadService.listFiles(projectId, normalizedCategory);
            return CommonResult.success(files);
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage(), e);
            return CommonResult.failed("获取文件列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/current")
    public CommonResult<?> getCurrentFile(
            @RequestParam("projectId") String projectId,
            @RequestParam("fileCategory") String fileCategory) {
        try {
            String normalizedCategory = fileCategory.toUpperCase().replace("-", "_");
            FileUploadRecord record = unifiedFileUploadService.getCurrentFile(projectId, normalizedCategory);
            if (record == null) {
                return CommonResult.success("0", "该类文件尚未上传", null);
            }
            return CommonResult.success(record);
        } catch (Exception e) {
            log.error("获取当前文件失败: {}", e.getMessage(), e);
            return CommonResult.failed("获取当前文件失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public CommonResult<?> deleteFile(@RequestParam("fileId") String fileId) {
        try {
            boolean deleted = unifiedFileUploadService.deleteFile(fileId);
            if (deleted) {
                return CommonResult.success("文件删除成功");
            }
            return CommonResult.failed("文件不存在或已删除");
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return CommonResult.failed("文件删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public CommonResult<?> getFileHistory(
            @RequestParam("projectId") String projectId,
            @RequestParam(value = "fileCategory", required = false) String fileCategory) {
        try {
            if (fileCategory != null && !fileCategory.isEmpty()) {
                String normalizedCategory = fileCategory.toUpperCase().replace("-", "_");
                List<FileVersionHistory> history = unifiedFileUploadService.getFileHistory(projectId, normalizedCategory);
                return CommonResult.success(history);
            }
            List<FileVersionHistory> allHistory = unifiedFileUploadService.getAllHistory(projectId);
            return CommonResult.success(allHistory);
        } catch (Exception e) {
            log.error("获取文件历史失败: {}", e.getMessage(), e);
            return CommonResult.failed("获取文件历史失败: " + e.getMessage());
        }
    }

    @PostMapping("/process")
    public CommonResult<?> processFile(@RequestBody Map<String, Object> request) {
        try {
            String fileId = (String) request.get("fileId");
            if (fileId == null || fileId.trim().isEmpty()) {
                return CommonResult.failed("文件ID不能为空");
            }

            log.info("接收文件处理请求, fileId={}", fileId);
            Map<String, Object> result = unifiedFileUploadService.processFile(fileId);

            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                return CommonResult.success(result);
            } else {
                return CommonResult.fail("500", (String) result.getOrDefault("message", "处理失败"));
            }
        } catch (Exception e) {
            log.error("文件处理失败: {}", e.getMessage(), e);
            return CommonResult.failed("文件处理失败: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadProcessedFile(@RequestParam("fileId") String fileId) {
        log.info("接收文件下载请求, fileId={}", fileId);
        try {
            FileUploadRecord record = unifiedFileUploadService.getFileByFileId(fileId);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }

            String outputPath = record.getOutputFilePath();
            if (outputPath == null || outputPath.isEmpty()) {
                log.warn("该文件没有处理结果可下载, fileId={}", fileId);
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(outputPath);
            if (!Files.exists(filePath)) {
                log.warn("处理结果文件不存在: {}", outputPath);
                return ResponseEntity.notFound().build();
            }

            String fileName = filePath.getFileName().toString();
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            InputStream inputStream = Files.newInputStream(filePath);
            InputStreamResource resource = new InputStreamResource(inputStream);

            MediaType mediaType = resolveMediaType(fileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName)
                    .contentType(mediaType)
                    .contentLength(Files.size(filePath))
                    .body(resource);

        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private MediaType resolveMediaType(String fileName) {
        if (fileName == null) return MediaType.APPLICATION_OCTET_STREAM;
        String ext = fileName.toLowerCase();
        if (ext.endsWith(".xlsx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if (ext.endsWith(".xls")) {
            return MediaType.parseMediaType("application/vnd.ms-excel");
        } else if (ext.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else if (ext.endsWith(".csv")) {
            return MediaType.parseMediaType("text/csv");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
}
