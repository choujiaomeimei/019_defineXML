package com.stat.service;

import com.stat.common.entity.FileUploadRecord;
import com.stat.dal.mapper.FileUploadRecordMapper;
import com.stat.dal.mapper.FileVersionHistoryMapper;
import com.stat.dal.mapper.ProjectMapper;
import com.stat.service.impl.UnifiedFileUploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectFilePathResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesStableWorkspacePathsForEveryUnifiedCategory() {
        ProjectFilePathResolver resolver = resolver();

        assertTrue(resolver.workspaceFile("P001", "SDTM",
                FileUploadRecord.FileCategory.ACRF, "source.pdf")
                .endsWith(Path.of("P001", "SDTM", "define", "SDTM注释CRF", "aCRF.pdf")));
        assertTrue(resolver.workspaceFile("P001", "SDTM",
                FileUploadRecord.FileCategory.P21_SPEC, "source.xlsx")
                .endsWith(Path.of("P001", "SDTM", "define", "p21空spec", "define.xlsx")));
        assertTrue(resolver.workspaceFile("P001", "SDTM",
                FileUploadRecord.FileCategory.PROJECT_SPEC, "source.xlsx")
                .endsWith(Path.of("P001", "SDTM", "define", "项目Spec", "spec.xlsx")));
        assertTrue(resolver.workspaceFile("P001", "SDTM",
                FileUploadRecord.FileCategory.XPT, "AE.XPT")
                .endsWith(Path.of("P001", "SDTM", "uploads", "xpt", "ae.xpt")));
        assertTrue(resolver.workspaceFile("P001", "SDTM",
                FileUploadRecord.FileCategory.EDC_CODELIST, "edc.xlsx")
                .endsWith(Path.of("P001", "SDTM", "define", "EDC建库说明", "edc_codelist.xlsx")));
    }

    @Test
    void removesArchiveAndWorkspaceWhenDatabaseInsertFails() {
        ProjectFilePathResolver resolver = resolver();
        FileUploadRecordMapper recordMapper = mock(FileUploadRecordMapper.class);
        when(recordMapper.selectByProjectIdAndCategory(anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(recordMapper.insert(any(FileUploadRecord.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        UnifiedFileUploadServiceImpl service = new UnifiedFileUploadServiceImpl();
        ReflectionTestUtils.setField(service, "fileUploadRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "fileVersionHistoryMapper", mock(FileVersionHistoryMapper.class));
        ReflectionTestUtils.setField(service, "pathResolver", resolver);

        MockMultipartFile file = new MockMultipartFile(
                "file", "spec.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "test-content".getBytes());

        assertThrows(IOException.class, () -> service.uploadFile(
                "P001", FileUploadRecord.FileCategory.PROJECT_SPEC,
                "SDTM", "tester", file));
        assertFalse(Files.exists(resolver.workspaceFile(
                "P001", "SDTM", FileUploadRecord.FileCategory.PROJECT_SPEC, "spec.xlsx")));
        assertDoesNotThrow(() -> {
            Path archiveDir = resolver.archiveDirectory("P001", FileUploadRecord.FileCategory.PROJECT_SPEC);
            if (Files.exists(archiveDir)) {
                try (var stream = Files.list(archiveDir)) {
                    assertEquals(0, stream.count());
                }
            }
        });
    }

    @Test
    void uploadsFourPrimaryTypesToArchiveAndStableWorkspace() throws Exception {
        ProjectFilePathResolver resolver = resolver();
        FileUploadRecordMapper recordMapper = mock(FileUploadRecordMapper.class);
        when(recordMapper.selectByProjectIdAndCategory(anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(recordMapper.insert(any(FileUploadRecord.class))).thenReturn(1);

        UnifiedFileUploadServiceImpl service = new UnifiedFileUploadServiceImpl();
        ReflectionTestUtils.setField(service, "fileUploadRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "fileVersionHistoryMapper", mock(FileVersionHistoryMapper.class));
        ReflectionTestUtils.setField(service, "pathResolver", resolver);

        assertWorkspaceCopy(service, resolver, FileUploadRecord.FileCategory.ACRF, "annotated.pdf");
        assertWorkspaceCopy(service, resolver, FileUploadRecord.FileCategory.P21_SPEC, "define-template.xlsx");
        assertWorkspaceCopy(service, resolver, FileUploadRecord.FileCategory.PROJECT_SPEC, "study-spec.xlsx");
        assertWorkspaceCopy(service, resolver, FileUploadRecord.FileCategory.XPT, "AE.XPT");

        verify(recordMapper, times(4)).insert(argThat((FileUploadRecord record) ->
                "SDTM".equals(record.getStandardType())
                        && record.getFilePath() != null
                        && record.getWorkspaceFilePath() != null));
    }

    @Test
    void reportsMissingArchiveAndWorkspaceInsteadOfCompletingProcessing() {
        ProjectFilePathResolver resolver = resolver();
        FileUploadRecordMapper recordMapper = mock(FileUploadRecordMapper.class);
        FileUploadRecord record = new FileUploadRecord()
                .setId(1L)
                .setFileId("missing-file")
                .setProjectId("P001")
                .setFileCategory(FileUploadRecord.FileCategory.XPT)
                .setOriginalName("ae.xpt")
                .setFilePath(tempDir.resolve("missing-ae.xpt").toString())
                .setStandardType("SDTM")
                .setUploadTime(LocalDateTime.now())
                .setDeleted(0);
        when(recordMapper.selectByFileId("missing-file")).thenReturn(record);

        UnifiedFileUploadServiceImpl service = new UnifiedFileUploadServiceImpl();
        ReflectionTestUtils.setField(service, "fileUploadRecordMapper", recordMapper);
        ReflectionTestUtils.setField(service, "fileVersionHistoryMapper", mock(FileVersionHistoryMapper.class));
        ReflectionTestUtils.setField(service, "pathResolver", resolver);

        Map<String, Object> result = service.processFile("missing-file");

        assertEquals(Boolean.FALSE, result.get("success"));
        assertTrue(result.get("message").toString().contains("工作副本不存在"));
        verify(recordMapper).updateById(argThat((FileUploadRecord item) ->
                FileUploadRecord.ProcessStatus.FAILED.equals(item.getProcessStatus())));
    }

    private ProjectFilePathResolver resolver() {
        ProjectFilePathResolver resolver = new ProjectFilePathResolver(mock(ProjectMapper.class));
        ReflectionTestUtils.setField(resolver, "uploadBasePath", tempDir.resolve("uploads").toString());
        ReflectionTestUtils.setField(resolver, "projectsBasePath", tempDir.resolve("projects").toString());
        return resolver;
    }

    private void assertWorkspaceCopy(UnifiedFileUploadServiceImpl service,
                                     ProjectFilePathResolver resolver,
                                     String category,
                                     String originalName) throws Exception {
        byte[] content = (category + "-content").getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", originalName, "application/octet-stream", content);
        FileUploadRecord record = service.uploadFile(
                "P002", category, "SDTM", "tester", file);

        assertArrayEquals(content, Files.readAllBytes(Path.of(record.getFilePath())));
        assertArrayEquals(content, Files.readAllBytes(Path.of(record.getWorkspaceFilePath())));
        assertEquals(resolver.workspaceFile("P002", "SDTM", category, originalName),
                Path.of(record.getWorkspaceFilePath()));
    }
}
