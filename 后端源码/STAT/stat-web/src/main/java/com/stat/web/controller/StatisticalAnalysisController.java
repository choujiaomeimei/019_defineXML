package com.stat.web.controller;

import com.stat.common.result.CommonResult;
import com.stat.common.security.RequireProjectAccess;
import com.stat.service.impl.DefineGenerationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 统计分析控制器
 * 处理XPT文件上传和Define.xlsx生成
 */
@RestController
@RequestMapping("/api/analysis")
public class StatisticalAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(StatisticalAnalysisController.class);

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadPath;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    @Autowired
    private DefineGenerationServiceImpl defineGenerationService;
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public CommonResult<Map<String, Object>> health() {
        logger.info("健康检查请求");
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("timestamp", System.currentTimeMillis());
        result.put("uploadPath", uploadPath);
        result.put("pythonPath", pythonPath);
        return CommonResult.success(result);
    }
    
    /**
     * 文件上传接口
     */
    @PostMapping("/upload")
    public CommonResult<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        logger.info("接收文件上传请求，文件名: {}, 文件大小: {}", 
                   file.getOriginalFilename(), file.getSize());
        
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
            
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                logger.info("创建上传目录: {}", uploadDir);
            }
            
            // 生成唯一文件名
            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = uploadDir.resolve(fileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("文件保存成功: {}", filePath);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("originalName", originalFilename);
            result.put("filePath", filePath.toString());
            result.put("fileSize", file.getSize());
            
            return CommonResult.success(result);
            
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            return CommonResult.fail("500", "文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理Define.xlsx生成
     */
    @PostMapping("/process-define")
    public CommonResult<Map<String, Object>> processDefine(@RequestBody Map<String, Object> request) {
        logger.info("接收Define处理请求: {}", request);
        
        try {
            @SuppressWarnings("unchecked")
            List<String> fileNames = (List<String>) request.get("files");
            
            if (fileNames == null || fileNames.isEmpty()) {
                return CommonResult.fail("400", "没有指定要处理的文件");
            }
            
            // 调用Python脚本处理
            Map<String, Object> result = callPythonScript(fileNames);
            
            return CommonResult.success(result);
            
        } catch (Exception e) {
            logger.error("Define处理失败", e);
            return CommonResult.fail("500", "处理失败: " + e.getMessage());
        }
    }
    
    /**
     * DB-driven Define.xlsx generation.
     * Reads spec/VLM/codelist from the database instead of relying on fixed file paths.
     */
    @RequireProjectAccess("projectId")
    @PostMapping("/generate-define")
    public CommonResult<Map<String, Object>> generateDefineFromDB(@RequestBody Map<String, String> request) {
        String projectId = request.get("projectId");
        if (projectId == null || projectId.trim().isEmpty()) {
            return CommonResult.fail("400", "项目ID不能为空");
        }
        try {
            Map<String, Object> result = defineGenerationService.generateDefine(projectId, request);
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("DB驱动Define生成失败", e);
            return CommonResult.fail("500", "生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("file") String fileName) {
        logger.info("接收文件下载请求: {}", fileName);
        
        try {
            // 安全检查：确保文件名不包含路径分隔符
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new IllegalArgumentException("非法的文件名");
            }
            
            // 检查Python输出目录
            Path outputDir = Paths.get(pythonPath, "output");
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
            logger.error("文件下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 调用Python脚本进行Define处理
     */
    private Map<String, Object> callPythonScript(List<String> fileNames) throws Exception {
        logger.info("开始调用Python脚本处理Define");
        
        // 构建Python命令
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(pythonPath, "run_p21report_api.py").toString());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonPath));
        processBuilder.redirectErrorStream(true);
        
        logger.info("执行命令: {}", String.join(" ", command));
        logger.info("工作目录: {}", pythonPath);
        
        Process process = processBuilder.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean inJsonResult = false;
            StringBuilder jsonResult = new StringBuilder();
            
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
            
            // 等待进程结束
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python脚本执行超时");
            }
            
            int exitCode = process.exitValue();
            logger.info("Python脚本执行完成，退出码: {}", exitCode);
            
            // 解析JSON结果
            if (jsonResult.length() > 0) {
                String jsonStr = jsonResult.toString().trim();
                logger.info("解析JSON结果: {}", jsonStr);
                
                // 简单的JSON解析（生产环境建议使用Jackson或Gson）
                Map<String, Object> result = new HashMap<>();
                if (jsonStr.contains("\"success\": true")) {
                    result.put("success", true);
                    result.put("message", "Define.xlsx生成成功");
                    
                    // 提取输出文件路径
                    if (jsonStr.contains("\"output_file\"")) {
                        String outputFile = extractJsonValue(jsonStr, "output_file");
                        result.put("output_file", outputFile);
                    }
                    
                    // 提取输出目录
                    if (jsonStr.contains("\"output_dir\"")) {
                        String outputDir = extractJsonValue(jsonStr, "output_dir");
                        result.put("output_dir", outputDir);
                    }
                } else {
                    result.put("success", false);
                    String error = extractJsonValue(jsonStr, "error");
                    result.put("error", error != null ? error : "Python脚本执行失败");
                }
                
                return result;
            } else {
                // 如果没有找到JSON结果，根据退出码判断
                Map<String, Object> result = new HashMap<>();
                if (exitCode == 0) {
                    result.put("success", true);
                    result.put("message", "Define.xlsx生成成功");
                } else {
                    result.put("success", false);
                    result.put("error", "Python脚本执行失败，退出码: " + exitCode);
                }
                return result;
            }
            
        } catch (Exception e) {
            logger.error("执行Python脚本时发生错误", e);
            throw e;
        }
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
}