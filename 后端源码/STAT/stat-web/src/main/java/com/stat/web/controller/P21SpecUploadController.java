package com.stat.web.controller;

import com.stat.common.result.CommonResult;
import com.stat.common.dto.SasP21SpecUploadDTO;
import com.stat.common.entity.SasP21SpecUpload;
import com.stat.dal.mapper.SasP21SpecUploadMapper;
import com.stat.service.IProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping("/p21-spec")
public class P21SpecUploadController {

    private static final Logger logger = LoggerFactory.getLogger(P21SpecUploadController.class);

    @Resource
    private SasP21SpecUploadMapper sasP21SpecUploadMapper;

    @Resource
    private IProjectService projectService;

    @Value("${app.upload.path:E:/JAVAPROJ/008_defineXML/uploads}")
    private String uploadPath;

    /**
     * P21空SPEC文件重新上传（覆盖原文件）
     */
    @PostMapping("/reupload")
    public CommonResult<SasP21SpecUploadDTO> reUploadP21SpecFile(@RequestParam("file") MultipartFile file,
                                                                @RequestParam("fileId") String fileId,
                                                                @RequestParam(value = "projectId", required = false, defaultValue = "DEFAULT") String projectId) {
        logger.info("接收P21空SPEC文件重新上传请求，文件ID: {}, 新文件名: {}, 文件大小: {}", 
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
            
            if (!isValidP21SpecFile(originalFilename)) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持Excel格式文件(.xlsx, .xls)");
            }
            
            // 检查原记录是否存在
            SasP21SpecUpload existingRecord = sasP21SpecUploadMapper.selectByFileId(fileId);
            if (existingRecord == null) {
                logger.warn("未找到要重新上传的文件记录，文件ID: {}", fileId);
                return CommonResult.fail("404", "未找到原文件记录");
            }
            
            // 创建P21空SPEC上传目录
            Path p21SpecUploadDir = Paths.get(uploadPath, "p21-spec");
            if (!Files.exists(p21SpecUploadDir)) {
                Files.createDirectories(p21SpecUploadDir);
                logger.info("创建P21空SPEC上传目录: {}", p21SpecUploadDir);
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
            logger.info("P21空SPEC文件重新上传成功: {}", filePath);

            // 同时更新到项目的define目录
            try {
                String defineP21SpecPath = projectService.getProjectPath(projectId, "define", "p21空spec");
                Path p21EmptySpecDir = Paths.get(defineP21SpecPath);
                if (!Files.exists(p21EmptySpecDir)) {
                    Files.createDirectories(p21EmptySpecDir);
                    logger.info("创建项目define p21空spec文件夹: {}", p21EmptySpecDir);
                }

                Path destSpecFile = p21EmptySpecDir.resolve("spec.xlsx");
                Files.copy(filePath, destSpecFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("同步更新spec.xlsx到项目define目录: {}", destSpecFile);
            } catch (Exception e) {
                logger.error("同步更新spec.xlsx到项目define目录失败", e);
            }

            // 更新数据库记录 - 仅更新文件大小、上传时间和重置处理状态
            existingRecord.setFileSize(file.getSize());
            existingRecord.setUploadTime(LocalDateTime.now());
            existingRecord.setUpdateTime(LocalDateTime.now());
            existingRecord.setProcessStatus(0); // 重置处理状态为未处理，需要重新处理
            
            logger.info("准备更新实体: fileId={}, originalName={}, fileSize={}", 
                       existingRecord.getFileId(), existingRecord.getOriginalName(), existingRecord.getFileSize());
            
            int result = sasP21SpecUploadMapper.updateById(existingRecord);
            if (result > 0) {
                logger.info("P21空SPEC重新上传记录更新成功，数据库ID: {}", existingRecord.getId());
                SasP21SpecUploadDTO dto = convertToDTO(existingRecord);
                return CommonResult.success(dto);
            } else {
                return CommonResult.fail("500", "更新上传记录失败");
            }
            
        } catch (Exception e) {
            logger.error("P21空SPEC文件重新上传失败", e);
            return CommonResult.fail("500", "文件重新上传失败: " + e.getMessage());
        }
    }

    /**
     * P21空SPEC文件上传
     */
    @PostMapping("/upload")
    public CommonResult<SasP21SpecUploadDTO> uploadP21SpecFile(@RequestParam("file") MultipartFile file,
                                                               @RequestParam(value = "projectId", required = false, defaultValue = "DEFAULT") String projectId) {
        logger.info("接收P21空SPEC文件上传请求，项目ID: {}, 文件名: {}, 文件大小: {}",
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
            
            if (!isValidP21SpecFile(originalFilename)) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持Excel格式文件(.xlsx, .xls)");
            }

            // 确保项目存在
            projectService.ensureProjectExists(projectId, null);

            // 创建项目的P21空SPEC上传目录
            String projectUploadPath = projectService.getProjectPath(projectId, "uploads", "p21-spec");
            Path p21SpecUploadDir = Paths.get(projectUploadPath);
            if (!Files.exists(p21SpecUploadDir)) {
                Files.createDirectories(p21SpecUploadDir);
                logger.info("创建P21空SPEC上传目录: {}", p21SpecUploadDir);
            }
            
            // 生成唯一文件名和文件ID
            String fileId = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String serverFileName = fileId + "_" + originalFilename;
            Path filePath = p21SpecUploadDir.resolve(serverFileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("P21空SPEC文件保存成功: {}", filePath);

            // 同时更新到项目的define目录
            try {
                String defineP21SpecPath = projectService.getProjectPath(projectId, "define", "p21空spec");
                Path p21EmptySpecDir = Paths.get(defineP21SpecPath);
                if (!Files.exists(p21EmptySpecDir)) {
                    Files.createDirectories(p21EmptySpecDir);
                    logger.info("创建项目define p21空spec文件夹: {}", p21EmptySpecDir);
                }

                Path destSpecFile = p21EmptySpecDir.resolve("spec.xlsx");
                Files.copy(filePath, destSpecFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("同步更新spec.xlsx到项目define目录: {}", destSpecFile);
            } catch (Exception e) {
                logger.error("同步更新spec.xlsx到项目define目录失败", e);
            }

            // 保存上传记录到数据库
            SasP21SpecUpload entity = new SasP21SpecUpload();
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
            
            int result = sasP21SpecUploadMapper.insert(entity);
            if (result > 0) {
                logger.info("P21空SPEC上传记录保存成功，数据库ID: {}", entity.getId());
                SasP21SpecUploadDTO dto = convertToDTO(entity);
                return CommonResult.success(dto);
            } else {
                // 如果数据库保存失败，删除已上传的文件
                Files.deleteIfExists(filePath);
                return CommonResult.fail("500", "保存上传记录失败");
            }
            
        } catch (Exception e) {
            logger.error("P21空SPEC文件上传失败", e);
            return CommonResult.fail("500", "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有P21空SPEC上传记录
     */
    @GetMapping("/records")
    public CommonResult<List<SasP21SpecUploadDTO>> getP21SpecUploadRecords() {
        logger.info("获取P21空SPEC上传记录列表");
        
        try {
            List<SasP21SpecUpload> entities = sasP21SpecUploadMapper.selectAllValid();
            List<SasP21SpecUploadDTO> records = entities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            logger.info("获取到 {} 条P21空SPEC上传记录", records.size());
            return CommonResult.success(records);
        } catch (Exception e) {
            logger.error("获取P21空SPEC上传记录失败", e);
            return CommonResult.fail("500", "获取上传记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除P21空SPEC上传记录
     */
    @DeleteMapping("/delete")
    public CommonResult<Void> deleteP21SpecUpload(@RequestParam("fileId") String fileId) {
        logger.info("删除P21空SPEC上传记录，文件ID: {}", fileId);
        
        try {
            // 获取上传记录
            SasP21SpecUpload uploadRecord = sasP21SpecUploadMapper.selectByFileId(fileId);
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
            boolean deleted = sasP21SpecUploadMapper.updateById(uploadRecord) > 0;
            
            if (deleted) {
                logger.info("P21空SPEC上传记录删除成功，文件ID: {}", fileId);
                return CommonResult.success();
            } else {
                return CommonResult.fail("500", "删除记录失败");
            }
            
        } catch (Exception e) {
            logger.error("删除P21空SPEC上传记录失败", e);
            return CommonResult.fail("500", "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理P21空SPEC文件
     */
    @PostMapping("/process")
    public CommonResult<Map<String, Object>> processP21Spec(@RequestBody Map<String, Object> request) {
        logger.info("接收P21空SPEC处理请求: {}", request);
        
        try {
            String fileId = (String) request.get("fileId");
            
            if (fileId == null || fileId.trim().isEmpty()) {
                return CommonResult.fail("400", "文件ID不能为空");
            }
            
            // 根据fileId查找上传记录
            SasP21SpecUpload uploadRecord = sasP21SpecUploadMapper.selectByFileId(fileId);
            if (uploadRecord == null) {
                return CommonResult.fail("404", "未找到上传记录");
            }
            
            // 检查文件是否存在
            Path filePath = Paths.get(uploadRecord.getFilePath());
            if (!Files.exists(filePath)) {
                return CommonResult.fail("404", "文件不存在");
            }
            
            // 这里可以添加P21空SPEC文件处理逻辑
            // 目前只是简单地标记为处理成功
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "P21空SPEC文件处理成功");
            result.put("processTime", System.currentTimeMillis());
            
            // 更新处理状态
            sasP21SpecUploadMapper.updateProcessStatus(
                uploadRecord.getFileId(), 
                1, // 处理成功
                null, 
                null
            );
            
            return CommonResult.success(result);
            
        } catch (Exception e) {
            logger.error("P21空SPEC处理失败", e);
            return CommonResult.fail("500", "处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载P21空SPEC处理结果
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadP21SpecResult(@RequestParam("file") String fileName) {
        logger.info("接收P21空SPEC结果文件下载请求: {}", fileName);
        
        try {
            // 安全检查：确保文件名不包含路径分隔符
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new IllegalArgumentException("非法的文件名");
            }
            
            // 检查P21空SPEC输出目录
            Path outputDir = Paths.get(uploadPath, "p21-spec", "output");
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
            logger.error("P21空SPEC结果文件下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 验证是否是有效的P21空SPEC文件
     */
    private boolean isValidP21SpecFile(String filename) {
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls");
    }
    
    private SasP21SpecUploadDTO convertToDTO(SasP21SpecUpload entity) {
        if (entity == null) {
            return null;
        }
        
        SasP21SpecUploadDTO dto = new SasP21SpecUploadDTO();
        BeanUtils.copyProperties(entity, dto);
        
        // 构建处理结果对象
        if (entity.getProcessStatus() != null && entity.getProcessStatus() > 0) {
            SasP21SpecUploadDTO.ProcessResult processResult = new SasP21SpecUploadDTO.ProcessResult();
            processResult.setSuccess(entity.getProcessStatus() == 1);
            processResult.setOutputFile(entity.getOutputFilePath());
            processResult.setProcessTime(entity.getProcessTime());
            processResult.setError(entity.getErrorMessage());
            dto.setProcessResult(processResult);
        }
        
        return dto;
    }
}