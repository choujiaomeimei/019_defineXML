package com.stat.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stat.common.dto.FileUploadRecordDTO;
import com.stat.common.entity.FileUploadRecord;
import com.stat.service.IFileUploadRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 统一文件上传控制器
 * 替代原有的多个分类上传控制器
 * 
 * @author System
 * @since 2025-01-10
 */
@RestController
@ConditionalOnProperty(name = "app.legacy-file-upload.enabled", havingValue = "true")
@RequestMapping("/file-upload")

public class FileUploadController {

    @Autowired
    private IFileUploadRecordService fileUploadRecordService;

    /**
     * 上传文件
     * 
     * @param file 文件
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @param username 用户名
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") String projectId,
            @RequestParam("fileCategory") String fileCategory,
            @RequestParam("username") String username) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "文件不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            // 生成文件ID
            String fileId = UUID.randomUUID().toString().replace("-", "");
            
            // 创建文件上传记录
            FileUploadRecordDTO recordDTO = new FileUploadRecordDTO();
            recordDTO.setFileId(fileId);
            recordDTO.setProjectId(projectId);
            recordDTO.setUsername(username);
            recordDTO.setFileCategory(fileCategory);
            recordDTO.setOriginalName(file.getOriginalFilename());
            recordDTO.setServerFileName(fileId + "_" + file.getOriginalFilename());
            recordDTO.setFileSize(file.getSize());
            recordDTO.setFileExtension(getFileExtension(file.getOriginalFilename()));
            
            // 保存文件上传记录
            boolean saved = fileUploadRecordService.saveFileUploadRecord(recordDTO);
            
            if (saved) {
                // TODO: 实际保存文件到服务器
                // 这里应该调用文件存储服务保存文件
                
                // 更新上传状态为成功
                fileUploadRecordService.updateUploadStatus(fileId, FileUploadRecord.UploadStatus.SUCCESS, null);
                
                result.put("success", true);
                result.put("message", "文件上传成功");
                result.put("fileId", fileId);
                result.put("fileName", file.getOriginalFilename());
                result.put("fileSize", file.getSize());
            } else {
                result.put("success", false);
                result.put("message", "保存文件记录失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "文件上传失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 根据项目ID和文件类别获取文件列表
     * 
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @return 文件列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getFileList(
            @RequestParam("projectId") String projectId,
            @RequestParam(value = "fileCategory", required = false) String fileCategory) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<FileUploadRecordDTO> files;
            if (fileCategory != null && !fileCategory.isEmpty()) {
                files = fileUploadRecordService.getFilesByProjectIdAndCategory(projectId, fileCategory);
            } else {
                files = fileUploadRecordService.getFilesByProjectId(projectId);
            }
            
            result.put("success", true);
            result.put("data", files);
            result.put("total", files.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取文件列表失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 分页查询文件列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param projectId 项目ID
     * @param fileCategory 文件类别
     * @param uploadStatus 上传状态
     * @param processStatus 处理状态
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getFilePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "fileCategory", required = false) String fileCategory,
            @RequestParam(value = "uploadStatus", required = false) String uploadStatus,
            @RequestParam(value = "processStatus", required = false) String processStatus) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Page<FileUploadRecord> pageParam = new Page<>(page, size);
            IPage<FileUploadRecordDTO> filePage = fileUploadRecordService.getFilesPage(
                    pageParam, projectId, fileCategory, uploadStatus, processStatus);
            
            result.put("success", true);
            result.put("data", filePage.getRecords());
            result.put("total", filePage.getTotal());
            result.put("current", filePage.getCurrent());
            result.put("size", filePage.getSize());
            result.put("pages", filePage.getPages());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "分页查询失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 根据文件ID获取文件信息
     * 
     * @param fileId 文件ID
     * @return 文件信息
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> getFileInfo(@PathVariable String fileId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            FileUploadRecordDTO file = fileUploadRecordService.getFileByFileId(fileId);
            if (file != null) {
                result.put("success", true);
                result.put("data", file);
            } else {
                result.put("success", false);
                result.put("message", "文件不存在");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取文件信息失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 删除文件
     * 
     * @param fileId 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean deleted = fileUploadRecordService.deleteFileRecord(fileId);
            if (deleted) {
                result.put("success", true);
                result.put("message", "文件删除成功");
            } else {
                result.put("success", false);
                result.put("message", "文件删除失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除文件失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 处理文件
     * 
     * @param fileId 文件ID
     * @param processType 处理类型
     * @return 处理结果
     */
    @PostMapping("/process/{fileId}")
    public ResponseEntity<Map<String, Object>> processFile(
            @PathVariable String fileId,
            @RequestParam(value = "processType", required = false) String processType) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 更新处理状态为处理中
            fileUploadRecordService.updateProcessStatus(fileId, FileUploadRecord.ProcessStatus.PROCESSING, 
                    null, null, null);
            
            // TODO: 调用实际的文件处理服务
            // 这里应该根据文件类别和处理类型调用相应的处理逻辑
            
            // 模拟处理完成
            fileUploadRecordService.updateProcessStatus(fileId, FileUploadRecord.ProcessStatus.COMPLETED, 
                    1000, "/processed/" + fileId, null);
            
            result.put("success", true);
            result.put("message", "文件处理成功");
            
        } catch (Exception e) {
            // 更新处理状态为失败
            fileUploadRecordService.updateProcessStatus(fileId, FileUploadRecord.ProcessStatus.FAILED, 
                    null, null, e.getMessage());
            
            result.put("success", false);
            result.put("message", "文件处理失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取项目文件统计信息
     * 
     * @param projectId 项目ID
     * @return 统计信息
     */
    @GetMapping("/stats/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectFileStats(@PathVariable String projectId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long totalSize = fileUploadRecordService.getTotalFileSizeByProjectId(projectId);
            
            Map<String, Integer> categoryCounts = new HashMap<>();
            String[] categories = {"P21_SPEC", "XPT", "PROJECT_SPEC", "ACRF", "VLM", "CODELIST"};
            for (String category : categories) {
                Integer count = fileUploadRecordService.countFilesByProjectIdAndCategory(projectId, category);
                categoryCounts.put(category, count);
            }
            
            result.put("success", true);
            Map<String, Object> data = new HashMap<>();
            data.put("totalSize", totalSize);
            data.put("categoryCounts", categoryCounts);
            result.put("data", data);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }
}
