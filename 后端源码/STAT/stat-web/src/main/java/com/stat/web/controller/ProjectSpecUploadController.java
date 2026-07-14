package com.stat.web.controller;

import com.stat.common.result.CommonResult;
import com.stat.common.dto.SasProjectSpecUploadDTO;
import com.stat.common.entity.SasProjectSpecUpload;
import com.stat.dal.mapper.SasProjectSpecUploadMapper;
import com.stat.service.ProjectSpecService;
import com.stat.service.IProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@ConditionalOnProperty(name = "app.legacy-file-upload.enabled", havingValue = "true")
@RequestMapping("/project-spec")
public class ProjectSpecUploadController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectSpecUploadController.class);

    @Resource
    private SasProjectSpecUploadMapper sasProjectSpecUploadMapper;

    @Resource
    private ProjectSpecService projectSpecService;

    @Resource
    private IProjectService projectService;

    @Value("${app.upload.path:E:/JAVAPROJ/008_defineXML/uploads}")
    private String uploadPath;

    /**
     * 项目SPEC文件重新上传（覆盖原文件）
     */
    @PostMapping("/reupload")
    public CommonResult<SasProjectSpecUploadDTO> reUploadProjectSpecFile(@RequestParam("file") MultipartFile file,
                                                                         @RequestParam("fileId") String fileId,
                                                                         @RequestParam(value = "projectId", required = false, defaultValue = "DEFAULT") String projectId) {
        logger.info("接收项目SPEC文件重新上传请求，文件ID: {}, 新文件名: {}, 文件大小: {}", 
                   fileId, file.getOriginalFilename(), file.getSize());
        
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                logger.warn("重新上传的文件为空");
                return CommonResult.fail("400", "上传的文件为空");
            }
            
            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                logger.warn("文件名为空");
                return CommonResult.fail("400", "文件名不能为空");
            }
            
            if (!isValidProjectSpecFile(originalFilename)) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持Excel格式文件(.xlsx, .xls)");
            }
            
            // 检查原记录是否存在
            SasProjectSpecUpload existingRecord = sasProjectSpecUploadMapper.selectByFileId(fileId);
            if (existingRecord == null) {
                logger.warn("未找到要重新上传的文件记录，文件ID: {}", fileId);
                return CommonResult.fail("404", "未找到原文件记录");
            }
            
            // 创建项目SPEC上传目录
            Path projectSpecUploadDir = Paths.get(uploadPath, "project-spec");
            if (!Files.exists(projectSpecUploadDir)) {
                Files.createDirectories(projectSpecUploadDir);
                logger.info("创建项目SPEC上传目录: {}", projectSpecUploadDir);
            }
            
            // 使用原有的服务器文件名覆盖
            Path filePath = Paths.get(existingRecord.getFilePath());
            
            // 删除旧文件（如果存在）
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("删除旧文件: {}", filePath);
            }
            
            // 保存新文件，使用原有文件路径
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("项目SPEC文件重新上传成功: {}", filePath);

            // 同时保留一份spec.xlsx到Python处理目录的项目Spec文件夹
            try {
                String defineProjectSpecPath = projectService.getProjectPath(projectId, "define", "项目Spec");
                Path projectSpecDir = Paths.get(defineProjectSpecPath);
                if (!Files.exists(projectSpecDir)) {
                    Files.createDirectories(projectSpecDir);
                    logger.info("创建项目define/项目Spec文件夹: {}", projectSpecDir);
                }

                Path destSpecFile = projectSpecDir.resolve("spec.xlsx");
                Files.copy(filePath, destSpecFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("保留spec.xlsx到项目Spec目录: {}", destSpecFile);
            } catch (Exception e) {
                logger.error("保留spec.xlsx到项目Spec目录失败", e);
            }

            // 更新数据库记录 - 仅更新文件大小、上传时间和重置处理状态
            existingRecord.setFileSize(file.getSize());
            existingRecord.setUploadTime(LocalDateTime.now());
            existingRecord.setUpdateTime(LocalDateTime.now());
            existingRecord.setProcessStatus(0); // 重置处理状态为未处理，需要重新处理
            
            logger.info("准备更新实体: fileId={}, originalName={}, fileSize={}", 
                       existingRecord.getFileId(), existingRecord.getOriginalName(), existingRecord.getFileSize());
            
            int result = sasProjectSpecUploadMapper.updateById(existingRecord);
            if (result > 0) {
                logger.info("项目SPEC重新上传记录更新成功，数据库ID: {}", existingRecord.getId());
                SasProjectSpecUploadDTO dto = convertToDTO(existingRecord);
                return CommonResult.success(dto);
            } else {
                return CommonResult.fail("500", "更新上传记录失败");
            }
            
        } catch (Exception e) {
            logger.error("项目SPEC文件重新上传失败", e);
            return CommonResult.fail("500", "文件重新上传失败: " + e.getMessage());
        }
    }

    /**
     * 项目SPEC文件上传
     */
    @PostMapping("/upload")
    public CommonResult<SasProjectSpecUploadDTO> uploadProjectSpecFile(@RequestParam("file") MultipartFile file,
                                                                        @RequestParam(value = "projectId", required = false, defaultValue = "DEFAULT") String projectId) {
        logger.info("接收项目SPEC文件上传请求，项目ID: {}, 文件名: {}, 文件大小: {}",
                   projectId, file.getOriginalFilename(), file.getSize());
        
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                logger.warn("上传的文件为空");
                return CommonResult.fail("400", "上传的文件为空");
            }
            
            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                logger.warn("文件名为空");
                return CommonResult.fail("400", "文件名不能为空");
            }
            
            if (!isValidProjectSpecFile(originalFilename)) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持Excel格式文件(.xlsx, .xls)");
            }

            // 确保项目存在
            projectService.ensureProjectExists(projectId, null);

            // 创建项目的项目SPEC上传目录
            String projectUploadPath = projectService.getProjectPath(projectId, "uploads", "project-spec");
            Path projectSpecUploadDir = Paths.get(projectUploadPath);
            if (!Files.exists(projectSpecUploadDir)) {
                Files.createDirectories(projectSpecUploadDir);
                logger.info("创建项目SPEC上传目录: {}", projectSpecUploadDir);
            }
            
            // 生成唯一文件名和文件ID
            String fileId = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String serverFileName = fileId + "_" + originalFilename;
            Path filePath = projectSpecUploadDir.resolve(serverFileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("项目SPEC文件保存成功: {}", filePath);

            // 同时保留一份spec.xlsx到Python处理目录的项目Spec文件夹
            try {
                String defineProjectSpecPath = projectService.getProjectPath(projectId, "define", "项目Spec");
                Path projectSpecDir = Paths.get(defineProjectSpecPath);
                if (!Files.exists(projectSpecDir)) {
                    Files.createDirectories(projectSpecDir);
                    logger.info("创建项目define/项目Spec文件夹: {}", projectSpecDir);
                }

                Path destSpecFile = projectSpecDir.resolve("spec.xlsx");
                Files.copy(filePath, destSpecFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("保留spec.xlsx到项目Spec目录: {}", destSpecFile);
            } catch (Exception e) {
                logger.error("保留spec.xlsx到项目Spec目录失败", e);
            }

            // 保存上传记录到数据库
            SasProjectSpecUpload entity = new SasProjectSpecUpload();
            entity.setFileId(fileId);
            entity.setOriginalName(originalFilename);
            entity.setServerFileName(serverFileName);
            entity.setFilePath(filePath.toString());
            entity.setFileSize(file.getSize());
            entity.setUploadTime(LocalDateTime.now());
            entity.setProcessStatus(0); // 初始状态为未处理
            entity.setDeleted(0); // 确保设置未删除状态
            
            logger.info("准备保存实体: fileId={}, originalName={}, fileSize={}", 
                       entity.getFileId(), entity.getOriginalName(), entity.getFileSize());
            
            int result = sasProjectSpecUploadMapper.insert(entity);
            if (result > 0) {
                logger.info("项目SPEC上传记录保存成功，数据库ID: {}", entity.getId());
                SasProjectSpecUploadDTO dto = convertToDTO(entity);
                return CommonResult.success(dto);
            } else {
                // 如果数据库保存失败，删除已上传的文件
                Files.deleteIfExists(filePath);
                return CommonResult.fail("500", "保存上传记录失败");
            }
            
        } catch (Exception e) {
            logger.error("项目SPEC文件上传失败", e);
            return CommonResult.fail("500", "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有项目SPEC上传记录
     */
    @GetMapping("/records")
    public CommonResult<List<SasProjectSpecUploadDTO>> getProjectSpecUploadRecords() {
        logger.info("获取项目SPEC上传记录列表");
        
        try {
            List<SasProjectSpecUpload> entities = sasProjectSpecUploadMapper.selectAllValid();
            List<SasProjectSpecUploadDTO> records = entities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            logger.info("获取到 {} 条项目SPEC上传记录", records.size());
            return CommonResult.success(records);
        } catch (Exception e) {
            logger.error("获取项目SPEC上传记录失败", e);
            return CommonResult.fail("500", "获取上传记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除项目SPEC上传记录
     */
    @DeleteMapping("/delete")
    public CommonResult<Void> deleteProjectSpecUpload(@RequestParam("fileId") String fileId) {
        logger.info("删除项目SPEC上传记录，文件ID: {}", fileId);
        
        try {
            // 获取上传记录
            SasProjectSpecUpload uploadRecord = sasProjectSpecUploadMapper.selectByFileId(fileId);
            if (uploadRecord == null) {
                return CommonResult.fail("404", "未找到上传记录");
            }
            
            // 删除文件
            Path filePath = Paths.get(uploadRecord.getFilePath());
            Files.deleteIfExists(filePath);
            
            // 删除输出文件（如果存在）
            if (uploadRecord.getOutputFilePath() != null) {
                Path outputPath = Paths.get(uploadRecord.getOutputFilePath());
                Files.deleteIfExists(outputPath);
            }
            
            // 逻辑删除数据库记录
            uploadRecord.setDeleted(1);
            uploadRecord.setUpdateTime(LocalDateTime.now());
            boolean deleted = sasProjectSpecUploadMapper.updateById(uploadRecord) > 0;
            
            if (deleted) {
                logger.info("项目SPEC上传记录删除成功，文件ID: {}", fileId);
                return CommonResult.success();
            } else {
                return CommonResult.fail("500", "删除记录失败");
            }
            
        } catch (Exception e) {
            logger.error("删除项目SPEC上传记录失败", e);
            return CommonResult.fail("500", "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理项目SPEC文件
     */
    @PostMapping("/process")
    public CommonResult<Map<String, Object>> processProjectSpec(@RequestBody Map<String, Object> request) {
        logger.info("接收项目SPEC处理请求: {}", request);
        
        try {
            String fileId = (String) request.get("fileId");
            
            if (fileId == null || fileId.trim().isEmpty()) {
                return CommonResult.fail("400", "文件ID不能为空");
            }
            
            // 根据fileId查找上传记录
            SasProjectSpecUpload uploadRecord = sasProjectSpecUploadMapper.selectByFileId(fileId);
            if (uploadRecord == null) {
                return CommonResult.fail("404", "未找到上传记录");
            }
            
            // 检查文件是否存在
            Path filePath = Paths.get(uploadRecord.getFilePath());
            if (!Files.exists(filePath)) {
                return CommonResult.fail("404", "文件不存在");
            }
            
            // 实际解析和处理项目SPEC文件
            Map<String, Object> result = new HashMap<>();
            
            try {
                // 生成项目ID（基于文件ID）
                String projectId = "PROJECT_" + fileId;
                
                logger.info("开始解析项目SPEC文件，项目ID: {}, 文件路径: {}", projectId, filePath);
                
                // 调用服务解析Excel文件并存储到数据库
                int parsedCount = projectSpecService.parseAndSaveProjectSpec(filePath.toString(), projectId);
                
                logger.info("项目SPEC文件解析完成，项目ID: {}, 解析记录数: {}", projectId, parsedCount);
                
                if (parsedCount > 0) {
                    result.put("success", true);
                    result.put("message", String.format("项目SPEC文件处理成功，共解析 %d 条记录", parsedCount));
                    result.put("projectId", projectId);
                    result.put("parsedCount", parsedCount);
                    result.put("processTime", System.currentTimeMillis());
                } else {
                    result.put("success", false);
                    result.put("message", "没有解析到有效的SPEC数据，请检查文件格式");
                    result.put("projectId", projectId);
                    result.put("parsedCount", 0);
                    result.put("processTime", System.currentTimeMillis());
                    
                    // 记录为处理失败
                    sasProjectSpecUploadMapper.updateProcessStatus(
                        uploadRecord.getFileId(), 
                        2, // 处理失败
                        null, 
                        "没有解析到有效的SPEC数据"
                    );
                    return CommonResult.success(result);
                }
                
                // 更新处理状态为成功
                sasProjectSpecUploadMapper.updateProcessStatus(
                    uploadRecord.getFileId(), 
                    1, // 处理成功
                    null, 
                    null
                );
                
            } catch (Exception e) {
                logger.error("项目SPEC文件处理失败: {}", e.getMessage(), e);
                
                result.put("success", false);
                result.put("message", "项目SPEC文件处理失败: " + e.getMessage());
                result.put("processTime", System.currentTimeMillis());
                
                // 使用简短的错误消息更新处理状态
                String simpleErrorMsg = "Excel解析失败";
                if (e.getMessage() != null) {
                    // 只取错误消息的前50个字符作为摘要
                    String errorSummary = e.getMessage().length() > 50 ? 
                        e.getMessage().substring(0, 50) + "..." : 
                        e.getMessage();
                    simpleErrorMsg = "处理失败: " + errorSummary;
                }
                
                // 更新处理状态为失败
                sasProjectSpecUploadMapper.updateProcessStatus(
                    uploadRecord.getFileId(), 
                    2, // 处理失败
                    null, 
                    simpleErrorMsg
                );
            }
            
            return CommonResult.success(result);
            
        } catch (Exception e) {
            logger.error("项目SPEC处理失败", e);
            return CommonResult.fail("500", "处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载项目SPEC处理结果
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadProjectSpecResult(@RequestParam("file") String fileName) {
        logger.info("接收项目SPEC结果文件下载请求: {}", fileName);
        
        try {
            // 安全检查：确保文件名不包含路径分隔符
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new IllegalArgumentException("非法的文件名");
            }
            
            // 检查项目SPEC输出目录
            Path outputDir = Paths.get(uploadPath, "project-spec", "output");
            Path filePath = outputDir.resolve(fileName);
            
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("文件不存在: " + fileName);
            }
            
            InputStream inputStream = Files.newInputStream(filePath);
            InputStreamResource resource = new InputStreamResource(inputStream);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("项目SPEC结果文件下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 验证是否是有效的项目SPEC文件
     */
    private boolean isValidProjectSpecFile(String filename) {
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls");
    }
    
    /**
     * 截断错误消息以避免数据库字段长度限制
     */
    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        
        final int MAX_LENGTH = 200; // 保守的数据库字段最大长度
        if (errorMessage.length() > MAX_LENGTH) {
            return errorMessage.substring(0, MAX_LENGTH) + "...";
        }
        return errorMessage;
    }
    
    private SasProjectSpecUploadDTO convertToDTO(SasProjectSpecUpload entity) {
        if (entity == null) {
            return null;
        }
        
        SasProjectSpecUploadDTO dto = new SasProjectSpecUploadDTO();
        BeanUtils.copyProperties(entity, dto);
        
        // 构建处理结果对象
        if (entity.getProcessStatus() != null && entity.getProcessStatus() > 0) {
            SasProjectSpecUploadDTO.ProcessResult processResult = new SasProjectSpecUploadDTO.ProcessResult();
            processResult.setSuccess(entity.getProcessStatus() == 1);
            processResult.setOutputFile(entity.getOutputFilePath());
            processResult.setProcessTime(entity.getProcessTime());
            processResult.setError(entity.getErrorMessage());
            dto.setProcessResult(processResult);
        }
        
        return dto;
    }
}