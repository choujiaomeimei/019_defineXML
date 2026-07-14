package com.stat.web.controller;

import com.stat.common.result.CommonResult;
import com.stat.common.dto.SasAcrfUploadDTO;
import com.stat.common.entity.SasAcrfUpload;
import com.stat.dal.mapper.SasAcrfUploadMapper;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@ConditionalOnProperty(name = "app.legacy-file-upload.enabled", havingValue = "true")
@RequestMapping("/acrf")
public class AcrfUploadController {

    private static final Logger logger = LoggerFactory.getLogger(AcrfUploadController.class);

    @Resource
    private SasAcrfUploadMapper sasAcrfUploadMapper;

    @Resource
    private IProjectService projectService;

    @Value("${app.upload.path:E:/JAVAPROJ/008_defineXML/uploads}")
    private String uploadPath;

    @Value("${app.python.path:E:/JAVAPROJ/008_defineXML/Python}")
    private String pythonPath;

    @Value("${app.projects.base-path:E:/JAVAPROJ/008_defineXML/projects}")
    private String projectsPath;

    /**
     * aCRF文件重新上传（覆盖原文件）
     */
    @PostMapping("/reupload")
    public CommonResult<SasAcrfUploadDTO> reUploadAcrfFile(@RequestParam("file") MultipartFile file,
                                                          @RequestParam("fileId") String fileId,
                                                          @RequestParam(value = "projectId", required = false, defaultValue = "DEFAULT") String projectId) {
        logger.info("接收aCRF文件重新上传请求，文件ID: {}, 新文件名: {}, 文件大小: {}", 
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
            
            if (!originalFilename.toLowerCase().endsWith(".pdf")) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持PDF格式文件");
            }
            
            // 检查原记录是否存在
            SasAcrfUpload existingRecord = sasAcrfUploadMapper.selectByFileId(fileId);
            if (existingRecord == null) {
                logger.warn("未找到要重新上传的文件记录，文件ID: {}", fileId);
                return CommonResult.fail("404", "未找到原文件记录");
            }
            
            // 创建aCRF上传目录
            Path acrfUploadDir = Paths.get(uploadPath, "acrf");
            if (!Files.exists(acrfUploadDir)) {
                Files.createDirectories(acrfUploadDir);
                logger.info("创建aCRF上传目录: {}", acrfUploadDir);
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
            logger.info("aCRF文件重新上传成功: {}", filePath);
            
            // 更新数据库记录 - 仅更新文件大小、上传时间和重置解析状态
            existingRecord.setFileSize(file.getSize());
            existingRecord.setUploadTime(LocalDateTime.now());
            existingRecord.setUpdateTime(LocalDateTime.now());
            existingRecord.setProcessStatus(0); // 重置处理状态为未处理，需要重新解析
            
            logger.info("准备更新实体: fileId={}, originalName={}, fileSize={}", 
                       existingRecord.getFileId(), existingRecord.getOriginalName(), existingRecord.getFileSize());
            
            int result = sasAcrfUploadMapper.updateById(existingRecord);
            if (result > 0) {
                logger.info("aCRF重新上传记录更新成功，数据库ID: {}", existingRecord.getId());

                // 如果存在项目ID，同时更新项目文件夹中的文件
                if (existingRecord.getProjectId() != null && !existingRecord.getProjectId().trim().isEmpty()) {
                    copyToProjectFolder(filePath, existingRecord.getProjectId(), originalFilename);
                }

                SasAcrfUploadDTO dto = convertToDTO(existingRecord);
                return CommonResult.success(dto);
            } else {
                return CommonResult.fail("500", "更新上传记录失败");
            }
            
        } catch (Exception e) {
            logger.error("aCRF文件重新上传失败", e);
            return CommonResult.fail("500", "文件重新上传失败: " + e.getMessage());
        }
    }

    /**
     * aCRF文件上传
     */
    @PostMapping("/upload")
    public CommonResult<SasAcrfUploadDTO> uploadAcrfFile(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(value = "projectId", required = false, defaultValue = "") String projectId) {
        logger.info("接收aCRF文件上传请求，文件名: {}, 文件大小: {}, 项目ID: {}",
                   file.getOriginalFilename(), file.getSize(), projectId);
        
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
            
            if (!originalFilename.toLowerCase().endsWith(".pdf")) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持PDF格式文件");
            }
            
            // 创建aCRF上传目录
            Path acrfUploadDir = Paths.get(uploadPath, "acrf");
            if (!Files.exists(acrfUploadDir)) {
                Files.createDirectories(acrfUploadDir);
                logger.info("创建aCRF上传目录: {}", acrfUploadDir);
            }
            
            // 生成唯一文件名和文件ID
            String fileId = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String serverFileName = fileId + "_" + originalFilename;
            Path filePath = acrfUploadDir.resolve(serverFileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("aCRF文件保存成功: {}", filePath);

            // 保存上传记录到数据库
            SasAcrfUpload entity = new SasAcrfUpload();
            entity.setFileId(fileId);
            entity.setOriginalName(originalFilename);
            entity.setServerFileName(serverFileName);
            entity.setFilePath(filePath.toString());
            entity.setFileSize(file.getSize());
            entity.setUploadTime(LocalDateTime.now());
            entity.setProcessStatus(0); // 初始状态为未处理
            entity.setDeleted(0); // 确保设置未删除状态
            // 处理项目ID，如果为空字符串则设为null
            String cleanProjectId = (projectId != null && !projectId.trim().isEmpty()) ? projectId.trim() : null;
            entity.setProjectId(cleanProjectId);

            logger.info("准备保存实体: fileId={}, originalName={}, fileSize={}, projectId={}",
                       entity.getFileId(), entity.getOriginalName(), entity.getFileSize(), entity.getProjectId());

            int result = sasAcrfUploadMapper.insert(entity);
            if (result > 0) {
                logger.info("aCRF上传记录保存成功，数据库ID: {}", entity.getId());

                // 如果指定了项目ID，同时将文件复制到项目文件夹
                if (entity.getProjectId() != null && !entity.getProjectId().trim().isEmpty()) {
                    copyToProjectFolder(filePath, entity.getProjectId(), originalFilename);
                }

                SasAcrfUploadDTO dto = convertToDTO(entity);
                return CommonResult.success(dto);
            } else {
                // 如果数据库保存失败，删除已上传的文件
                Files.deleteIfExists(filePath);
                return CommonResult.fail("500", "保存上传记录失败");
            }
            
        } catch (Exception e) {
            logger.error("aCRF文件上传失败", e);
            return CommonResult.fail("500", "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有aCRF上传记录
     */
    @GetMapping("/records")
    public CommonResult<List<SasAcrfUploadDTO>> getAcrfUploadRecords() {
        logger.info("获取aCRF上传记录列表");
        
        try {
            List<SasAcrfUpload> entities = sasAcrfUploadMapper.selectAllValid();
            List<SasAcrfUploadDTO> records = entities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            logger.info("获取到 {} 条aCRF上传记录", records.size());
            return CommonResult.success(records);
        } catch (Exception e) {
            logger.error("获取aCRF上传记录失败", e);
            return CommonResult.fail("500", "获取上传记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除aCRF上传记录
     */
    @DeleteMapping("/delete")
    public CommonResult<Void> deleteAcrfUpload(@RequestParam("fileId") String fileId) {
        logger.info("删除aCRF上传记录，文件ID: {}", fileId);
        
        try {
            // 获取上传记录
            SasAcrfUpload uploadRecord = sasAcrfUploadMapper.selectByFileId(fileId);
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

            // 删除项目文件夹中的aCRF.pdf文件（如果存在）
            if (uploadRecord.getProjectId() != null && !uploadRecord.getProjectId().trim().isEmpty()) {
                Path projectAcrfFile = Paths.get(projectsPath, uploadRecord.getProjectId(), "define", "SDTM注释CRF", "aCRF.pdf");
                if (Files.exists(projectAcrfFile)) {
                    Files.deleteIfExists(projectAcrfFile);
                    logger.info("已删除项目文件夹中的aCRF文件: {}", projectAcrfFile);
                }
            }
            
            // 逻辑删除数据库记录
            uploadRecord.setDeleted(1);
            uploadRecord.setUpdateTime(LocalDateTime.now());
            boolean deleted = sasAcrfUploadMapper.updateById(uploadRecord) > 0;
            
            if (deleted) {
                logger.info("aCRF上传记录删除成功，文件ID: {}", fileId);
                return CommonResult.success();
            } else {
                return CommonResult.fail("500", "删除记录失败");
            }
            
        } catch (Exception e) {
            logger.error("删除aCRF上传记录失败", e);
            return CommonResult.fail("500", "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理aCRF解析
     */
    @PostMapping("/process")
    public CommonResult<Map<String, Object>> processAcrf(@RequestBody Map<String, Object> request) {
        logger.info("接收aCRF解析请求: {}", request);
        
        try {
            String fileId = (String) request.get("fileId");
            String dateSuffix = (String) request.get("dateSuffix");
            
            if (fileId == null || fileId.trim().isEmpty()) {
                return CommonResult.fail("400", "文件ID不能为空");
            }
            
            // 根据fileId查找上传记录
            SasAcrfUpload uploadRecord = sasAcrfUploadMapper.selectByFileId(fileId);
            if (uploadRecord == null) {
                return CommonResult.fail("404", "未找到上传记录");
            }
            
            // 检查文件是否存在
            Path filePath = Paths.get(uploadRecord.getFilePath());
            if (!Files.exists(filePath)) {
                return CommonResult.fail("404", "文件不存在");
            }
            
            // 调用Python脚本处理aCRF
            Map<String, Object> result = callAcrfPythonScript(uploadRecord, dateSuffix);
            
            // 更新处理状态
            boolean success = (Boolean) result.get("success");
            String outputFile = (String) result.get("output_file");
            String errorMessage = (String) result.get("error");
            
            sasAcrfUploadMapper.updateProcessStatus(
                uploadRecord.getFileId(), 
                success ? 1 : 2, 
                outputFile, 
                errorMessage
            );
            
            return CommonResult.success(result);
            
        } catch (Exception e) {
            logger.error("aCRF解析失败", e);
            return CommonResult.fail("500", "解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载aCRF解析结果
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadAcrfResult(@RequestParam("file") String fileName) {
        logger.info("接收aCRF结果文件下载请求: {}", fileName);
        
        try {
            // 安全检查：确保文件名不包含路径分隔符
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new IllegalArgumentException("非法的文件名");
            }
            
            // 检查Python输出目录 - aCRF结果在Spec_out目录下
            Path outputDir = Paths.get(pythonPath, "output", "Spec_out");
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
            logger.error("aCRF结果文件下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 调用Python脚本处理aCRF
     */
    private Map<String, Object> callAcrfPythonScript(SasAcrfUpload uploadRecord, String dateSuffix) throws Exception {
        logger.info("开始调用Python脚本处理aCRF，文件: {} -> {}", uploadRecord.getOriginalName(), uploadRecord.getFilePath());
        
        // 构建Python命令，传入具体的PDF文件路径
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(pythonPath, "pgm", "extract_pdf_annotations.py").toString());
        command.add(uploadRecord.getFilePath()); // 传入具体的PDF文件路径
        
        // 生成输出文件名（包含日期后缀）
        String expectedOutputFileName = "Annots2" + (dateSuffix != null ? "_" + dateSuffix : "") + ".xlsx";
        Path expectedOutputPath = Paths.get(pythonPath, "output", "Spec_out", expectedOutputFileName);
        command.add(expectedOutputPath.toString()); // 传入输出文件路径
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonPath));
        processBuilder.redirectErrorStream(true);
        
        logger.info("执行Python命令: {}", String.join(" ", command));
        logger.info("工作目录: {}", pythonPath);
        logger.info("输入文件: {}", uploadRecord.getFilePath());
        logger.info("输出文件: {}", expectedOutputPath);
        
        Process process = processBuilder.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        StringBuilder jsonResult = new StringBuilder();
        boolean inJsonResult = false;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.info("Python输出: {}", line);
                
                // 检测JSON结果开始
                if (line.contains("API_RESULT:")) {
                    inJsonResult = true;
                    continue;
                }
                
                // 检测JSON结果结束
                if (line.contains("====")) {
                    if (inJsonResult) {
                        break;
                    }
                }
                
                // 收集JSON结果
                if (inJsonResult) {
                    jsonResult.append(line).append("\n");
                }
            }
        }
        
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        int exitCode = finished ? process.exitValue() : -1;
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python脚本执行超时");
        }
        
        logger.info("Python脚本执行完成，退出码: {}, 输出长度: {}", exitCode, output.length());
        
        Map<String, Object> result = new HashMap<>();
        
        // 尝试解析JSON结果
        if (jsonResult.length() > 0) {
            try {
                String jsonStr = jsonResult.toString().trim();
                logger.info("解析Python返回的JSON: {}", jsonStr);
                
                // 简单的JSON解析
                if (jsonStr.contains("\"success\": true")) {
                    result.put("success", true);
                    result.put("message", extractJsonValue(jsonStr, "message"));
                    result.put("output_file", extractJsonValue(jsonStr, "output_file"));
                    result.put("processTime", System.currentTimeMillis());
                } else {
                    result.put("success", false);
                    String error = extractJsonValue(jsonStr, "error");
                    result.put("error", error != null ? error : "Python脚本执行失败");
                }
            } catch (Exception e) {
                logger.error("解析Python JSON结果失败", e);
                result.put("success", false);
                result.put("error", "解析Python结果失败: " + e.getMessage());
            }
        } else {
            // 没有JSON结果，使用传统方式验证
            if (exitCode == 0) {
                if (Files.exists(expectedOutputPath)) {
                    result.put("success", true);
                    result.put("message", "aCRF解析成功");
                    result.put("output_file", expectedOutputFileName);
                    result.put("processTime", System.currentTimeMillis());
                    logger.info("aCRF解析成功，输出文件: {}", expectedOutputFileName);
                } else {
                    result.put("success", false);
                    result.put("error", "解析完成但未找到输出文件: " + expectedOutputFileName);
                    logger.warn("解析完成但未找到输出文件: {}", expectedOutputPath);
                }
            } else {
                result.put("success", false);
                result.put("error", "Python脚本执行失败，退出码: " + exitCode + "\n输出: " + output.toString());
                logger.error("Python脚本执行失败，退出码: {}", exitCode);
            }
        }
        
        return result;
    }
    
    /**
     * 从JSON字符串中提取指定字段的值（简单实现）
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * 将文件复制到项目文件夹
     */
    private void copyToProjectFolder(Path sourceFile, String projectId, String originalFilename) {
        try {
            // 构建项目SDTM注释CRF文件夹路径
            Path projectAcrfDir = Paths.get(projectsPath, projectId, "define", "SDTM注释CRF");

            // 确保项目SDTM注释CRF目录存在
            if (!Files.exists(projectAcrfDir)) {
                Files.createDirectories(projectAcrfDir);
                logger.info("创建项目SDTM注释CRF目录: {}", projectAcrfDir);
            }

            // 复制文件到项目文件夹，重命名为aCRF.pdf
            Path projectFilePath = projectAcrfDir.resolve("aCRF.pdf");
            Files.copy(sourceFile, projectFilePath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("aCRF文件已复制到项目文件夹: {} -> {}", originalFilename, projectFilePath);

        } catch (Exception e) {
            logger.error("复制aCRF文件到项目文件夹失败", e);
            // 不抛出异常，避免影响主上传流程
        }
    }
    
    private SasAcrfUploadDTO convertToDTO(SasAcrfUpload entity) {
        if (entity == null) {
            return null;
        }
        
        SasAcrfUploadDTO dto = new SasAcrfUploadDTO();
        BeanUtils.copyProperties(entity, dto);
        
        // 构建处理结果对象
        if (entity.getProcessStatus() != null && entity.getProcessStatus() > 0) {
            SasAcrfUploadDTO.ProcessResult processResult = new SasAcrfUploadDTO.ProcessResult();
            processResult.setSuccess(entity.getProcessStatus() == 1);
            processResult.setOutputFile(entity.getOutputFilePath());
            processResult.setProcessTime(entity.getProcessTime());
            processResult.setError(entity.getErrorMessage());
            dto.setProcessResult(processResult);
        }
        
        return dto;
    }
}