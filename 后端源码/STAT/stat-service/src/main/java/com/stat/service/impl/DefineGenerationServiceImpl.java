package com.stat.service.impl;

import com.alibaba.fastjson.JSON;
import com.stat.dal.mapper.CodelistDataMapper;
import com.stat.dal.mapper.PagesDataMapper;
import com.stat.dal.mapper.ProjectSpecMapper;
import com.stat.dal.mapper.VlmDataMapper;
import com.stat.dal.po.CodelistDataPO;
import com.stat.dal.po.PagesDataPO;
import com.stat.dal.po.ProjectSpecPO;
import com.stat.dal.po.VlmDataPO;
import com.stat.common.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.stat.service.util.ExcelStyleHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DB-driven Define.xlsx generation.
 * Exports current DB spec/VLM/codelist data to temp Excel files,
 * then invokes the Python processor with explicit file paths.
 */
@Slf4j
@Service
public class DefineGenerationServiceImpl {

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    @Value("${app.projects.base-path:C:/Project_Web/019_defineXML/projects}")
    private String projectsBasePath;

    @Autowired
    private ProjectSpecMapper projectSpecMapper;

    @Autowired
    private VlmDataMapper vlmDataMapper;

    @Autowired
    private CodelistDataMapper codelistDataMapper;

    @Autowired
    private PagesDataMapper pagesDataMapper;

    @Autowired
    private ProjectSnapshotServiceImpl snapshotService;

    public Map<String, Object> generateDefine(String projectId, Map<String, String> config) throws Exception {
        log.info("开始DB驱动的Define生成: projectId={}", projectId);

        // Auto-snapshot before generation
        try {
            snapshotService.createAutoSnapshot(projectId, "define-generation");
        } catch (Exception e) {
            log.warn("自动快照创建失败，继续生成: {}", e.getMessage());
        }

        // 1. Export DB data to temp spec file
        Path tempDir = Files.createTempDirectory("define-gen-" + projectId);
        Path specFile = tempDir.resolve("spec.xlsx");

        exportSpecToExcel(projectId, specFile);

        // 2. Export VLM + Codelist + Pages + Spec as JSON for Python to consume
        Path vlmJson = tempDir.resolve("vlm_data.json");
        Path codelistJson = tempDir.resolve("codelist_data.json");
        Path pagesJson = tempDir.resolve("pages_data.json");
        Path specJson = tempDir.resolve("spec_data.json");

        String username = UserContext.getUsername();
        List<VlmDataPO> vlmList = vlmDataMapper.selectByProjectId(projectId, username);
        Files.writeString(vlmJson, JSON.toJSONString(vlmList));

        List<CodelistDataPO> codelistList = codelistDataMapper.selectByProjectId(projectId, username);
        Files.writeString(codelistJson, JSON.toJSONString(codelistList));

        List<PagesDataPO> pagesList = pagesDataMapper.selectByProjectId(projectId, username);
        Files.writeString(pagesJson, JSON.toJSONString(pagesList));

        List<ProjectSpecPO> specList2 = projectSpecMapper.selectByProjectId(projectId, username);
        Files.writeString(specJson, JSON.toJSONString(specList2));

        // 3. Determine output directory
        Path outputDir = Paths.get(projectsBasePath, projectId, "define", "output");
        Files.createDirectories(outputDir);

        // 4. Build Python command with explicit paths
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(pythonPath, "run_p21report_api.py").toString());
        command.add("--spec-file");
        command.add(specFile.toString());
        command.add("--vlm-json");
        command.add(vlmJson.toString());
        command.add("--codelist-json");
        command.add(codelistJson.toString());
        command.add("--pages-json");
        command.add(pagesJson.toString());
        command.add("--spec-json");
        command.add(specJson.toString());
        command.add("--output-dir");
        command.add(outputDir.toString());

        if (config != null) {
            if (config.containsKey("ig")) {
                command.add("--ig");
                command.add(config.get("ig"));
            }
            if (config.containsKey("lang")) {
                command.add("--lang");
                command.add(config.get("lang"));
            }
            if (config.containsKey("protocol")) {
                command.add("--protocol");
                command.add(config.get("protocol"));
            }
            if (config.containsKey("studyTitle")) {
                command.add("--study-title");
                command.add(config.get("studyTitle"));
            }
        }

        log.info("执行Python命令: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(pythonPath));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("Python: {}", line);
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Python脚本执行超时");
        }

        int exitCode = process.exitValue();
        log.info("Python执行完成, exitCode={}", exitCode);

        // Cleanup temp files
        try {
            Files.deleteIfExists(specFile);
            Files.deleteIfExists(vlmJson);
            Files.deleteIfExists(codelistJson);
            Files.deleteIfExists(pagesJson);
            Files.deleteIfExists(specJson);
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            log.warn("清理临时文件失败: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", exitCode == 0);
        result.put("output", output.toString());
        result.put("outputDir", outputDir.toString());
        if (exitCode != 0) {
            result.put("error", "Python脚本退出码: " + exitCode);
        }
        return result;
    }

    private void exportSpecToExcel(String projectId, Path outputPath) throws IOException {
        List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, UserContext.getUsername());

        Map<String, List<ProjectSpecPO>> byDomain = specList.stream()
                .collect(Collectors.groupingBy(ProjectSpecPO::getDomain, LinkedHashMap::new, Collectors.toList()));

        try (Workbook wb = new XSSFWorkbook()) {
            // TOC sheet
            Sheet toc = wb.createSheet("TOC");
            Row tocHeader = toc.createRow(0);
            tocHeader.createCell(0).setCellValue("Domain");
            tocHeader.createCell(1).setCellValue("Description");
            tocHeader.createCell(2).setCellValue("Variables");
            int tocRow = 1;
            for (Map.Entry<String, List<ProjectSpecPO>> entry : byDomain.entrySet()) {
                Row r = toc.createRow(tocRow++);
                r.createCell(0).setCellValue(entry.getKey());
                r.createCell(1).setCellValue(entry.getKey() + " Domain");
                r.createCell(2).setCellValue(entry.getValue().size());
            }

            // Per-domain sheets
            for (Map.Entry<String, List<ProjectSpecPO>> entry : byDomain.entrySet()) {
                Sheet sheet = wb.createSheet(entry.getKey());
                Row header = sheet.createRow(0);
                String[] cols = {"Variable", "Label", "Type", "Length", "Controlled Terms or Format",
                        "CDISC Submission Value", "Origin", "Role", "CDISC Notes", "Core",
                        "Codelist", "Format", "Comment", "Pages", "Method", "Derivation"};
                for (int i = 0; i < cols.length; i++) {
                    header.createCell(i).setCellValue(cols[i]);
                }

                int rowNum = 1;
                for (ProjectSpecPO spec : entry.getValue()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(nullSafe(spec.getVariable()));
                    row.createCell(1).setCellValue(nullSafe(spec.getLabel()));
                    row.createCell(2).setCellValue(nullSafe(spec.getType()));
                    if (spec.getLength() != null) row.createCell(3).setCellValue(spec.getLength());
                    row.createCell(4).setCellValue(nullSafe(spec.getControlledTermsOrFormat()));
                    row.createCell(5).setCellValue(nullSafe(spec.getCdiscSubmissionValue()));
                    row.createCell(6).setCellValue(nullSafe(spec.getOrigin()));
                    row.createCell(7).setCellValue(nullSafe(spec.getRole()));
                    row.createCell(8).setCellValue(nullSafe(spec.getCdiscNotes()));
                    row.createCell(9).setCellValue(nullSafe(spec.getCore()));
                    row.createCell(10).setCellValue(nullSafe(spec.getCodelist()));
                    row.createCell(11).setCellValue(nullSafe(spec.getFormat()));
                    row.createCell(12).setCellValue(nullSafe(spec.getComment()));
                    row.createCell(13).setCellValue(nullSafe(spec.getPages()));
                    row.createCell(14).setCellValue(nullSafe(spec.getMethod()));
                    row.createCell(15).setCellValue(nullSafe(spec.getDerivation()));
                }
            }

            ExcelStyleHelper.styleWorkbook(wb);

            try (OutputStream os = Files.newOutputStream(outputPath)) {
                wb.write(os);
            }
        }
        log.info("Spec exported to Excel: {}, domains={}", outputPath, byDomain.size());
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
