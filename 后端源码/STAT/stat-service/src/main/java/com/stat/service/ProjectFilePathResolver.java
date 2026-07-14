package com.stat.service;

import com.stat.common.entity.FileUploadRecord;
import com.stat.common.entity.Project;
import com.stat.dal.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * 项目文件路径的唯一解析入口。
 *
 * uploads 保存不可变的原始版本；projects 保存当前标准下供解析器使用的工作副本。
 */
@Component
public class ProjectFilePathResolver {

    private final ProjectMapper projectMapper;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadBasePath;

    @Value("${app.projects.base-path:C:/Project_Web/019_defineXML/projects}")
    private String projectsBasePath;

    public ProjectFilePathResolver(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    public String resolveStandardType(String projectId, String requestedStandardType) {
        if (requestedStandardType != null && !requestedStandardType.isBlank()) {
            return normalizeStandardType(requestedStandardType);
        }
        Project project = projectMapper.selectByProjectId(projectId);
        if (project != null && project.getStandardType() != null && !project.getStandardType().isBlank()) {
            return normalizeStandardType(project.getStandardType().split(",")[0]);
        }
        return "SDTM";
    }

    public Path archiveDirectory(String projectId, String fileCategory) {
        return Paths.get(uploadBasePath, safeSegment(projectId), categoryDirectory(fileCategory));
    }

    public Path projectStandardRoot(String projectId, String standardType) {
        return Paths.get(projectsBasePath, safeSegment(projectId), normalizeStandardType(standardType));
    }

    public Path workspaceFile(String projectId, String standardType, String fileCategory, String originalName) {
        Path root = projectStandardRoot(projectId, standardType);
        return switch (fileCategory) {
            case FileUploadRecord.FileCategory.ACRF ->
                    root.resolve(Paths.get("define", "SDTM注释CRF", "aCRF.pdf"));
            case FileUploadRecord.FileCategory.P21_SPEC ->
                    root.resolve(Paths.get("define", "p21空spec", "define.xlsx"));
            case FileUploadRecord.FileCategory.PROJECT_SPEC ->
                    root.resolve(Paths.get("define", "项目Spec", "spec.xlsx"));
            case FileUploadRecord.FileCategory.XPT ->
                    root.resolve(Paths.get("uploads", "xpt", safeFileName(originalName).toLowerCase(Locale.ROOT)));
            case FileUploadRecord.FileCategory.EDC_CODELIST ->
                    root.resolve(Paths.get("define", "EDC建库说明", "edc_codelist.xlsx"));
            default ->
                    root.resolve(Paths.get("uploads", categoryDirectory(fileCategory), safeFileName(originalName)));
        };
    }

    public Path acrfAnnotations(String projectId, String standardType) {
        return projectStandardRoot(projectId, standardType)
                .resolve(Paths.get("define", "SDTM注释CRF", "Annots2.xlsx"));
    }

    public Path edcCodelist(String projectId, String standardType) {
        return projectStandardRoot(projectId, standardType)
                .resolve(Paths.get("define", "EDC建库说明", "edc_codelist.xlsx"));
    }

    public Path projectSpecDirectory(String projectId, String standardType) {
        return projectStandardRoot(projectId, standardType).resolve(Paths.get("define", "项目Spec"));
    }

    public Path definePackageDirectory(String projectId, String standardType) {
        return projectStandardRoot(projectId, standardType).resolve(Paths.get("define", "sdtm define package"));
    }

    public Path xptDirectory(String projectId, String standardType) {
        return projectStandardRoot(projectId, standardType).resolve(Paths.get("uploads", "xpt"));
    }

    public Path extractionOutputDirectory(String projectId, String standardType) {
        return projectStandardRoot(projectId, standardType).resolve(Paths.get("output", "reports"));
    }

    private String categoryDirectory(String fileCategory) {
        return fileCategory.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String normalizeStandardType(String value) {
        String normalized = safeSegment(value).toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? "SDTM" : normalized;
    }

    private String safeSegment(String value) {
        if (value == null) return "";
        String safe = value.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (".".equals(safe) || "..".equals(safe)) return "_";
        return safe;
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) return "file";
        return safeSegment(Paths.get(value).getFileName().toString());
    }
}
