package com.stat.web.controller;

import com.stat.common.entity.SdrgContent;
import com.stat.common.entity.SdrgTemplate;
import com.stat.common.result.CommonResult;
import com.stat.common.security.RequireProjectAccess;
import com.stat.common.security.UserContext;
import com.stat.service.impl.SdrgServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sdrg")
public class SdrgController {

    @Autowired
    private SdrgServiceImpl sdrgService;

    @RequireProjectAccess("projectId")
    @GetMapping("/sections")
    public CommonResult<List<SdrgContent>> getSections(@RequestParam("projectId") String projectId) {
        try {
            List<SdrgContent> sections = sdrgService.getSections(projectId);
            return CommonResult.success(sections);
        } catch (Exception e) {
            log.error("获取SDRG章节失败", e);
            return CommonResult.failed("获取章节失败: " + e.getMessage());
        }
    }

    @RequireProjectAccess("projectId")
    @PostMapping("/section")
    public CommonResult<SdrgContent> saveSection(@RequestBody SdrgContent content) {
        try {
            String username = UserContext.getUsername();
            content.setUpdatedBy(username);
            if (content.getId() == null) {
                content.setCreatedBy(username);
            }
            SdrgContent saved = sdrgService.saveSection(content);
            return CommonResult.success(saved);
        } catch (Exception e) {
            log.error("保存SDRG章节失败", e);
            return CommonResult.failed("保存失败: " + e.getMessage());
        }
    }

    @RequireProjectAccess("projectId")
    @GetMapping("/export")
    public ResponseEntity<?> exportSdrg(@RequestParam("projectId") String projectId) {
        try {
            String filePath = sdrgService.exportSdrg(projectId);
            Path path = Paths.get(filePath);
            InputStream is = Files.newInputStream(path);
            InputStreamResource resource = new InputStreamResource(is);

            String filename = path.getFileName().toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            log.error("导出SDRG失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/templates")
    public CommonResult<List<SdrgTemplate>> listTemplates() {
        try {
            return CommonResult.success(sdrgService.listTemplates());
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return CommonResult.failed("获取模板列表失败: " + e.getMessage());
        }
    }
}
