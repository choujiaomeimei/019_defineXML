package com.stat.web.controller;

import com.stat.common.dto.PagesDataDTO;
import com.stat.common.result.CommonResult;
import com.stat.common.security.UserContext;
import com.stat.service.IPagesDataService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pages")
public class PagesDataController {

    @Autowired
    private IPagesDataService pagesDataService;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadBasePath;

    @GetMapping("/project/{projectId}")
    public CommonResult<List<PagesDataDTO>> getByProject(@PathVariable String projectId) {
        try {
            List<PagesDataDTO> result = pagesDataService.getPagesDataByProjectId(projectId);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("查询Pages数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/project/{projectId}/dataset/{dataset}")
    public CommonResult<List<PagesDataDTO>> getByProjectAndDataset(
            @PathVariable String projectId, @PathVariable String dataset) {
        try {
            List<PagesDataDTO> result = pagesDataService.getPagesDataByProjectIdAndDataset(projectId, dataset);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("查询数据集Pages数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public CommonResult<PagesDataDTO> getById(@PathVariable Long id) {
        try {
            PagesDataDTO result = pagesDataService.getPagesDataById(id);
            return result != null ? CommonResult.success(result) : CommonResult.failed("数据不存在");
        } catch (Exception e) {
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    public CommonResult<String> add(@RequestBody PagesDataDTO dto) {
        try {
            return pagesDataService.addPagesData(dto)
                    ? CommonResult.success("新增成功")
                    : CommonResult.failed("新增失败");
        } catch (Exception e) {
            return CommonResult.failed("新增失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public CommonResult<String> update(@PathVariable Long id, @RequestBody PagesDataDTO dto) {
        try {
            dto.setId(id);
            return pagesDataService.updatePagesData(dto)
                    ? CommonResult.success("更新成功")
                    : CommonResult.failed("更新失败");
        } catch (Exception e) {
            return CommonResult.failed("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> delete(@PathVariable Long id) {
        try {
            return pagesDataService.deletePagesData(id)
                    ? CommonResult.success("删除成功")
                    : CommonResult.failed("删除失败");
        } catch (Exception e) {
            return CommonResult.failed("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/datasets/{projectId}")
    public CommonResult<List<String>> getDatasets(@PathVariable String projectId) {
        try {
            List<String> datasets = pagesDataService.getDistinctDatasetsByProjectId(projectId);
            return CommonResult.success(datasets);
        } catch (Exception e) {
            return CommonResult.failed("获取数据集列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/extract-pages")
    public CommonResult<String> extractPages(@RequestBody Map<String, String> request) {
        try {
            String projectId = request.get("projectId");
            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.failed("项目ID不能为空");
            }

            File annotsFile = new File(uploadBasePath + "/" + projectId + "/output/Annots2.xlsx");
            if (!annotsFile.exists()) {
                return CommonResult.failed("未找到aCRF注释文件 (Annots2.xlsx)。请先上传aCRF文件并确保处理成功，然后再提取Pages数据。");
            }

            String scriptPath = pythonPath + "/define/extraction_pipeline.py";
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                return CommonResult.failed("提取脚本不存在: " + scriptPath);
            }

            String[] pythonExecutables = {"python", "python3", "py"};
            String pythonExecutable = null;
            for (String exec : pythonExecutables) {
                try {
                    Process tp = new ProcessBuilder(exec, "--version").start();
                    if (tp.waitFor() == 0) { pythonExecutable = exec; break; }
                } catch (Exception ignored) {}
            }
            if (pythonExecutable == null) {
                return CommonResult.failed("未找到可用的Python可执行文件");
            }

            String currentUser = UserContext.getUsername();
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable, scriptPath,
                    "--project-id", projectId,
                    "--upload-base", uploadBasePath,
                    "--python-base", pythonPath,
                    "--steps", "pages",
                    "--username", currentUser != null ? currentUser : ""
            );
            pb.directory(new File(pythonPath + "/define"));
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            if (currentUser != null && !currentUser.isEmpty()) {
                env.put("USERNAME_CONTEXT", currentUser);
            }

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) { process.destroy(); return CommonResult.failed("Pages数据提取超时"); }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return CommonResult.success("Pages数据提取成功！\n" + output);
            } else {
                return CommonResult.failed("Pages数据提取失败，退出代码: " + exitCode + "\n" + output);
            }
        } catch (Exception e) {
            return CommonResult.failed("Pages数据提取失败: " + e.getMessage());
        }
    }

    @GetMapping("/export-xlsx/{projectId}")
    public ResponseEntity<byte[]> exportXlsx(@PathVariable String projectId) {
        try {
            List<PagesDataDTO> dataList = pagesDataService.getPagesDataByProjectId(projectId);
            String[] headers = {"Dataset", "Variable", "Where Clause", "Pages", "Origin"};

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Pages");
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                for (int i = 0; i < dataList.size(); i++) {
                    PagesDataDTO d = dataList.get(i);
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(d.getDataset() != null ? d.getDataset() : "");
                    row.createCell(1).setCellValue(d.getVariable() != null ? d.getVariable() : "");
                    row.createCell(2).setCellValue(d.getWhereClause() != null ? d.getWhereClause() : "");
                    row.createCell(3).setCellValue(d.getPages() != null ? d.getPages() : "");
                    row.createCell(4).setCellValue(d.getOrigin() != null ? d.getOrigin() : "");
                }

                for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                workbook.write(out);
                byte[] bytes = out.toByteArray();

                String fileName = URLEncoder.encode("Pages_" + projectId + ".xlsx", StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/import-xlsx/{projectId}")
    public CommonResult<String> importXlsx(@PathVariable String projectId,
                                            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sheets = (List<Map<String, Object>>) request.get("sheets");
            if (sheets == null || sheets.isEmpty()) {
                return CommonResult.failed("无有效工作表数据");
            }

            pagesDataService.deleteByProjectId(projectId);

            int importCount = 0;
            for (Map<String, Object> sheetObj : sheets) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> celldata = (List<Map<String, Object>>) sheetObj.get("celldata");
                if (celldata == null) continue;

                Map<Integer, Map<Integer, String>> grid = new HashMap<>();
                for (Map<String, Object> cell : celldata) {
                    int r = toInt(cell.get("r"));
                    int c = toInt(cell.get("c"));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> v = (Map<String, Object>) cell.get("v");
                    String val = "";
                    if (v != null) {
                        Object m = v.get("m");
                        Object vv = v.get("v");
                        val = m != null ? m.toString() : (vv != null ? vv.toString() : "");
                    }
                    grid.computeIfAbsent(r, k -> new HashMap<>()).put(c, val);
                }

                int maxRow = grid.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
                for (int r = 1; r <= maxRow; r++) {
                    Map<Integer, String> row = grid.get(r);
                    if (row == null) continue;
                    String dataset = row.getOrDefault(0, "").trim();
                    String variable = row.getOrDefault(1, "").trim();
                    if (dataset.isEmpty() && variable.isEmpty()) continue;

                    PagesDataDTO dto = new PagesDataDTO();
                    dto.setProjectId(projectId);
                    dto.setDataset(dataset);
                    dto.setVariable(variable);
                    dto.setWhereClause(row.getOrDefault(2, "").trim());
                    dto.setPages(row.getOrDefault(3, "").trim());
                    dto.setOrigin(row.getOrDefault(4, "").trim());
                    dto.setSortOrder(importCount);
                    pagesDataService.addPagesData(dto);
                    importCount++;
                }
                break;
            }
            return CommonResult.success("导入成功，共 " + importCount + " 条Pages数据");
        } catch (Exception e) {
            return CommonResult.failed("导入Pages数据失败: " + e.getMessage());
        }
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
}
