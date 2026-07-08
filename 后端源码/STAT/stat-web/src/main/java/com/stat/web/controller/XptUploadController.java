package com.stat.web.controller;

import com.stat.common.result.CommonResult;
import com.stat.common.dto.SasXptUploadDTO;
import com.stat.common.entity.SasXptUpload;
import com.stat.dal.mapper.SasXptUploadMapper;
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
@RequestMapping("/xpt")
public class XptUploadController {

    private static final Logger logger = LoggerFactory.getLogger(XptUploadController.class);

    @Resource
    private SasXptUploadMapper sasXptUploadMapper;

    @Resource
    private IProjectService projectService;

    @Value("${app.upload.path:E:/JAVAPROJ/008_defineXML/uploads}")
    private String uploadPath;

    /**
     * XPT文件重新上传（覆盖原文件）
     */
    @PostMapping("/reupload")
    public CommonResult<SasXptUploadDTO> reUploadXptFile(@RequestParam("file") MultipartFile file,
                                                        @RequestParam("fileId") String fileId,
                                                        @RequestParam(value = "projectId", required = false, defaultValue = "DEFAULT") String projectId) {
        logger.info("接收XPT文件重新上传请求，文件ID: {}, 新文件名: {}, 文件大小: {}", 
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
            
            if (!originalFilename.toLowerCase().endsWith(".xpt")) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持XPT格式文件");
            }
            
            // 检查原记录是否存在
            SasXptUpload existingRecord = sasXptUploadMapper.selectByFileId(fileId);
            if (existingRecord == null) {
                logger.warn("未找到要重新上传的文件记录，文件ID: {}", fileId);
                return CommonResult.fail("404", "未找到原文件记录");
            }
            
            // 创建XPT上传目录
            Path xptUploadDir = Paths.get(uploadPath, "xpt");
            if (!Files.exists(xptUploadDir)) {
                Files.createDirectories(xptUploadDir);
                logger.info("创建XPT上传目录: {}", xptUploadDir);
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
            logger.info("XPT文件重新上传成功: {}", filePath);

            // 清空并复制sdtm define package到Python处理目录
            try {
                String defineSdtmPackagePath = projectService.getProjectPath(projectId, "define", "sdtm define package");
                Path sdtmDefinePackageDir = Paths.get(defineSdtmPackagePath);

                // 清空目录
                if (Files.exists(sdtmDefinePackageDir)) {
                    Files.walk(sdtmDefinePackageDir)
                        .filter(Files::isRegularFile)
                        .forEach(fileToDelete -> {
                            try {
                                Files.delete(fileToDelete);
                                logger.debug("删除文件: {}", fileToDelete);
                            } catch (Exception e) {
                                logger.warn("删除文件失败: {}", fileToDelete, e);
                            }
                        });
                    logger.info("清空项目sdtm define package目录: {}", sdtmDefinePackageDir);
                } else {
                    Files.createDirectories(sdtmDefinePackageDir);
                    logger.info("创建项目sdtm define package目录: {}", sdtmDefinePackageDir);
                }

                // 复制当前XPT文件到该目录
                Path destXptFile = sdtmDefinePackageDir.resolve(originalFilename);
                Files.copy(filePath, destXptFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("复制XPT文件到项目sdtm define package目录: {}", destXptFile);
            } catch (Exception e) {
                logger.error("清空并复制项目sdtm define package失败", e);
            }

            // 更新数据库记录 - 仅更新文件大小、上传时间和重置处理状态
            existingRecord.setFileSize(file.getSize());
            existingRecord.setUploadTime(LocalDateTime.now());
            existingRecord.setUpdateTime(LocalDateTime.now());
            existingRecord.setProcessStatus(0); // 重置处理状态为未处理，需要重新处理
            
            logger.info("准备更新实体: fileId={}, originalName={}, fileSize={}", 
                       existingRecord.getFileId(), existingRecord.getOriginalName(), existingRecord.getFileSize());
            
            int result = sasXptUploadMapper.updateById(existingRecord);
            if (result > 0) {
                logger.info("XPT重新上传记录更新成功，数据库ID: {}", existingRecord.getId());
                SasXptUploadDTO dto = convertToDTO(existingRecord);
                return CommonResult.success(dto);
            } else {
                return CommonResult.fail("500", "更新上传记录失败");
            }
            
        } catch (Exception e) {
            logger.error("XPT文件重新上传失败", e);
            return CommonResult.fail("500", "文件重新上传失败: " + e.getMessage());
        }
    }

    /**
     * XPT文件上传
     */
    @PostMapping("/upload")
    public CommonResult<SasXptUploadDTO> uploadXptFile(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "projectId", required = false, defaultValue = "DEFAULT") String projectId) {
        logger.info("接收XPT文件上传请求，项目ID: {}, 文件名: {}, 文件大小: {}",
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
            
            if (!originalFilename.toLowerCase().endsWith(".xpt")) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持XPT格式文件");
            }

            // 确保项目存在
            projectService.ensureProjectExists(projectId, null);

            // 创建项目的XPT上传目录
            String projectUploadPath = projectService.getProjectPath(projectId, "uploads", "xpt");
            Path xptUploadDir = Paths.get(projectUploadPath);
            if (!Files.exists(xptUploadDir)) {
                Files.createDirectories(xptUploadDir);
                logger.info("创建XPT上传目录: {}", xptUploadDir);
            }
            
            // 生成唯一文件名和文件ID
            String fileId = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String serverFileName = fileId + "_" + originalFilename;
            Path filePath = xptUploadDir.resolve(serverFileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("XPT文件保存成功: {}", filePath);

            // 清空并复制sdtm define package到Python处理目录
            try {
                String defineSdtmPackagePath = projectService.getProjectPath(projectId, "define", "sdtm define package");
                Path sdtmDefinePackageDir = Paths.get(defineSdtmPackagePath);

                // 清空目录
                if (Files.exists(sdtmDefinePackageDir)) {
                    Files.walk(sdtmDefinePackageDir)
                        .filter(Files::isRegularFile)
                        .forEach(fileToDelete -> {
                            try {
                                Files.delete(fileToDelete);
                                logger.debug("删除文件: {}", fileToDelete);
                            } catch (Exception e) {
                                logger.warn("删除文件失败: {}", fileToDelete, e);
                            }
                        });
                    logger.info("清空项目sdtm define package目录: {}", sdtmDefinePackageDir);
                } else {
                    Files.createDirectories(sdtmDefinePackageDir);
                    logger.info("创建项目sdtm define package目录: {}", sdtmDefinePackageDir);
                }

                // 复制当前XPT文件到该目录
                Path destXptFile = sdtmDefinePackageDir.resolve(originalFilename);
                Files.copy(filePath, destXptFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("复制XPT文件到项目sdtm define package目录: {}", destXptFile);
            } catch (Exception e) {
                logger.error("清空并复制项目sdtm define package失败", e);
            }

            // 保存上传记录到数据库
            SasXptUpload entity = new SasXptUpload();
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
            
            int result = sasXptUploadMapper.insert(entity);
            if (result > 0) {
                logger.info("XPT上传记录保存成功，数据库ID: {}", entity.getId());
                SasXptUploadDTO dto = convertToDTO(entity);
                return CommonResult.success(dto);
            } else {
                // 如果数据库保存失败，删除已上传的文件
                Files.deleteIfExists(filePath);
                return CommonResult.fail("500", "保存上传记录失败");
            }
            
        } catch (Exception e) {
            logger.error("XPT文件上传失败", e);
            return CommonResult.fail("500", "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有XPT上传记录
     */
    @GetMapping("/records")
    public CommonResult<List<SasXptUploadDTO>> getXptUploadRecords() {
        logger.info("获取XPT上传记录列表");
        
        try {
            List<SasXptUpload> entities = sasXptUploadMapper.selectAllValid();
            List<SasXptUploadDTO> records = entities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            logger.info("获取到 {} 条XPT上传记录", records.size());
            return CommonResult.success(records);
        } catch (Exception e) {
            logger.error("获取XPT上传记录失败", e);
            return CommonResult.fail("500", "获取上传记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除XPT上传记录
     */
    @DeleteMapping("/delete")
    public CommonResult<Void> deleteXptUpload(@RequestParam("fileId") String fileId) {
        logger.info("删除XPT上传记录，文件ID: {}", fileId);
        
        try {
            // 获取上传记录
            SasXptUpload uploadRecord = sasXptUploadMapper.selectByFileId(fileId);
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
            boolean deleted = sasXptUploadMapper.updateById(uploadRecord) > 0;
            
            if (deleted) {
                logger.info("XPT上传记录删除成功，文件ID: {}", fileId);
                return CommonResult.success();
            } else {
                return CommonResult.fail("500", "删除记录失败");
            }
            
        } catch (Exception e) {
            logger.error("删除XPT上传记录失败", e);
            return CommonResult.fail("500", "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理XPT文件
     */
    @PostMapping("/process")
    public CommonResult<Map<String, Object>> processXpt(@RequestBody Map<String, Object> request) {
        logger.info("接收XPT处理请求: {}", request);
        
        try {
            String fileId = (String) request.get("fileId");
            
            if (fileId == null || fileId.trim().isEmpty()) {
                return CommonResult.fail("400", "文件ID不能为空");
            }
            
            // 根据fileId查找上传记录
            SasXptUpload uploadRecord = sasXptUploadMapper.selectByFileId(fileId);
            if (uploadRecord == null) {
                return CommonResult.fail("404", "未找到上传记录");
            }
            
            // 检查文件是否存在
            Path filePath = Paths.get(uploadRecord.getFilePath());
            if (!Files.exists(filePath)) {
                return CommonResult.fail("404", "文件不存在");
            }
            
            // 这里可以添加XPT文件处理逻辑
            // 目前只是简单地标记为处理成功
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "XPT文件处理成功");
            result.put("processTime", System.currentTimeMillis());
            
            // 更新处理状态
            sasXptUploadMapper.updateProcessStatus(
                uploadRecord.getFileId(), 
                1, // 处理成功
                null, 
                null
            );
            
            return CommonResult.success(result);
            
        } catch (Exception e) {
            logger.error("XPT处理失败", e);
            return CommonResult.fail("500", "处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载XPT处理结果
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadXptResult(@RequestParam("file") String fileName) {
        logger.info("接收XPT结果文件下载请求: {}", fileName);
        
        try {
            // 安全检查：确保文件名不包含路径分隔符
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new IllegalArgumentException("非法的文件名");
            }
            
            // 检查XPT输出目录
            Path outputDir = Paths.get(uploadPath, "xpt", "output");
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
            logger.error("XPT结果文件下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    private SasXptUploadDTO convertToDTO(SasXptUpload entity) {
        if (entity == null) {
            return null;
        }
        
        SasXptUploadDTO dto = new SasXptUploadDTO();
        BeanUtils.copyProperties(entity, dto);
        
        // 构建处理结果对象
        if (entity.getProcessStatus() != null && entity.getProcessStatus() > 0) {
            SasXptUploadDTO.ProcessResult processResult = new SasXptUploadDTO.ProcessResult();
            processResult.setSuccess(entity.getProcessStatus() == 1);
            processResult.setOutputFile(entity.getOutputFilePath());
            processResult.setProcessTime(entity.getProcessTime());
            processResult.setError(entity.getErrorMessage());
            dto.setProcessResult(processResult);
        }
        
        return dto;
    }
}