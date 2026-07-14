package com.stat.web.controller;

import com.stat.common.result.CommonResult;
import com.stat.common.dto.ProjectSpecDTO;
import com.stat.common.entity.FileUploadRecord;
import com.stat.dal.mapper.FileUploadRecordMapper;
import com.stat.dal.mapper.CommentsDataMapper;
import com.stat.dal.mapper.MethodsDataMapper;
import com.stat.dal.mapper.PagesDataMapper;
import com.stat.dal.mapper.ProjectSpecMapper;
import com.stat.dal.mapper.CodelistDataMapper;
import com.stat.dal.mapper.DictionariesDataMapper;
import com.stat.dal.mapper.ProjectConfigMapper;
import com.stat.dal.po.CommentsDataPO;
import com.stat.dal.po.DictionariesDataPO;
import com.stat.dal.po.MethodsDataPO;
import com.stat.dal.po.PagesDataPO;
import com.stat.dal.po.ProjectSpecPO;
import com.stat.dal.po.CodelistDataPO;
import com.stat.dal.po.ProjectConfigPO;
import org.springframework.jdbc.core.JdbcTemplate;
import com.stat.service.ProjectSpecService;
import com.stat.service.ProjectFilePathResolver;
import com.stat.service.CodelistExtractionResult;
import com.stat.service.CodelistExtractionService;
import com.stat.common.security.RequireProjectAccess;
import com.stat.common.security.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 项目Spec数据控制器
 */
@RestController
@RequestMapping("/project-spec-data")
public class ProjectSpecController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectSpecController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Resource
    private ProjectSpecService projectSpecService;

    @Resource
    private FileUploadRecordMapper fileUploadRecordMapper;

    @Resource
    private ProjectSpecMapper projectSpecMapper;

    @Resource
    private PagesDataMapper pagesDataMapper;

    @Resource
    private MethodsDataMapper methodsDataMapper;

    @Resource
    private CommentsDataMapper commentsDataMapper;

    @Resource
    private CodelistDataMapper codelistDataMapper;

    @Resource
    private DictionariesDataMapper dictionariesDataMapper;

    @Resource
    private ProjectConfigMapper projectConfigMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ProjectFilePathResolver pathResolver;

    @Resource
    private CodelistExtractionService codelistExtractionService;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadBasePath;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;
    
    /**
     * 根据项目ID获取Spec数据列表
     */
    @GetMapping("/list")
    public CommonResult<List<ProjectSpecDTO>> getProjectSpecList(@RequestParam("projectId") String projectId) {
        logger.info("获取项目Spec数据列表，项目ID: {}", projectId);
        
        try {
            List<ProjectSpecDTO> specList = projectSpecService.getByProjectId(projectId);
            logger.info("获取到 {} 条Spec数据", specList.size());
            return CommonResult.success(specList);
        } catch (Exception e) {
            logger.error("获取项目Spec数据失败", e);
            return CommonResult.fail("500", "获取数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据项目ID和域获取Spec数据列表
     */
    @GetMapping("/list-by-domain")
    public CommonResult<List<ProjectSpecDTO>> getProjectSpecListByDomain(
            @RequestParam("projectId") String projectId,
            @RequestParam("domain") String domain) {
        logger.info("获取项目Spec数据列表，项目ID: {}, 域: {}", projectId, domain);
        
        try {
            List<ProjectSpecDTO> specList = projectSpecService.getByProjectIdAndDomain(projectId, domain);
            logger.info("获取到 {} 条Spec数据", specList.size());
            return CommonResult.success(specList);
        } catch (Exception e) {
            logger.error("获取项目Spec数据失败", e);
            return CommonResult.fail("500", "获取数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取项目中所有域列表
     */
    @GetMapping("/domains")
    public CommonResult<List<String>> getProjectDomains(@RequestParam("projectId") String projectId) {
        logger.info("获取项目域列表，项目ID: {}", projectId);
        
        try {
            List<String> domains = projectSpecService.getDistinctDomainsByProjectId(projectId);
            logger.info("获取到 {} 个域", domains.size());
            return CommonResult.success(domains);
        } catch (Exception e) {
            logger.error("获取项目域列表失败", e);
            return CommonResult.fail("500", "获取数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取项目域统计信息
     */
    @GetMapping("/domain-stats")
    public CommonResult<List<Map<String, Object>>> getProjectDomainStats(@RequestParam("projectId") String projectId) {
        logger.info("获取项目域统计信息，项目ID: {}", projectId);
        
        try {
            List<Map<String, Object>> stats = projectSpecService.getDomainStatsByProjectId(projectId);
            logger.info("获取到 {} 个域的统计信息", stats.size());
            return CommonResult.success(stats);
        } catch (Exception e) {
            logger.error("获取项目域统计信息失败", e);
            return CommonResult.fail("500", "获取数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取变量Domain映射 - 用于显示变量所在的域列表
     */
    @GetMapping("/variable-domain-mapping")
    public CommonResult<Map<String, List<String>>> getVariableDomainMapping(
            @RequestParam(value = "projectId", required = false) String projectId) {
        logger.info("获取变量Domain映射，项目ID: {}", projectId);
        
        try {
            Map<String, List<String>> mapping = projectSpecService.getVariableDomainMapping(projectId);
            logger.info("获取到 {} 个变量的域映射", mapping.size());
            return CommonResult.success(mapping);
        } catch (Exception e) {
            logger.error("获取变量Domain映射失败", e);
            return CommonResult.fail("500", "获取映射失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取VCD到域的映射 - 基于codelist表的VCD获取对应域
     */
    @GetMapping("/vcd-domain-mapping")
    public CommonResult<Map<String, List<String>>> getVcdDomainMapping(
            @RequestParam(value = "projectId", required = false) String projectId) {
        logger.info("获取VCD域映射，项目ID: {}", projectId);
        
        try {
            Map<String, List<String>> mapping = projectSpecService.getVcdDomainMapping(projectId);
            logger.info("获取到 {} 个VCD的域映射", mapping.size());
            return CommonResult.success(mapping);
        } catch (Exception e) {
            logger.error("获取VCD域映射失败", e);
            return CommonResult.fail("500", "获取映射失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除项目的所有Spec数据
     */
    @DeleteMapping("/delete-all")
    public CommonResult<Void> deleteAllProjectSpec(@RequestParam("projectId") String projectId) {
        logger.info("删除项目所有Spec数据，项目ID: {}", projectId);
        
        try {
            int deletedCount = projectSpecService.deleteByProjectId(projectId);
            logger.info("删除了 {} 条Spec数据", deletedCount);
            triggerSpecSync(projectId);
            return CommonResult.success();
        } catch (Exception e) {
            logger.error("删除项目Spec数据失败", e);
            return CommonResult.fail("500", "删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/add")
    public CommonResult<ProjectSpecDTO> addVariable(@RequestBody Map<String, Object> body) {
        logger.info("新增Spec变量: {}", body);
        try {
            ProjectSpecPO po = mapToPO(body);
            ProjectSpecDTO dto = projectSpecService.addVariable(po);
            triggerSpecSync(po.getProjectId());
            return CommonResult.success(dto);
        } catch (Exception e) {
            logger.error("新增Spec变量失败", e);
            return CommonResult.fail("500", "新增失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public CommonResult<ProjectSpecDTO> updateVariable(@RequestBody Map<String, Object> body) {
        logger.info("更新Spec变量: {}", body);
        try {
            Object idObj = body.get("id");
            if (idObj == null) {
                return CommonResult.fail("400", "缺少变量ID");
            }
            Long id = Long.valueOf(idObj.toString());
            ProjectSpecDTO existing = projectSpecService.getById(id);
            if (existing == null) {
                return CommonResult.fail("404", "变量不存在");
            }
            ProjectSpecPO po = new ProjectSpecPO();
            BeanUtils.copyProperties(existing, po);
            applyMapToPO(body, po);
            po.setId(id);
            ProjectSpecDTO dto = projectSpecService.updateVariable(po);
            triggerSpecSync(po.getProjectId());
            return CommonResult.success(dto);
        } catch (Exception e) {
            logger.error("更新Spec变量失败", e);
            return CommonResult.fail("500", "更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public CommonResult<Void> deleteVariable(@RequestParam("id") Long id) {
        logger.info("删除Spec变量, id={}", id);
        try {
            ProjectSpecDTO existing = projectSpecService.getById(id);
            int count = projectSpecService.deleteVariable(id);
            if (count > 0) {
                if (existing != null) {
                    triggerSpecSync(existing.getProjectId());
                }
                return CommonResult.success();
            }
            return CommonResult.fail("404", "变量不存在");
        } catch (Exception e) {
            logger.error("删除Spec变量失败", e);
            return CommonResult.fail("500", "删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/export-xlsx/{projectId}")
    public ResponseEntity<byte[]> exportSpecXlsx(@PathVariable String projectId) {
        try {
            List<ProjectSpecDTO> allData = projectSpecService.getByProjectId(projectId);
            LinkedHashMap<String, List<ProjectSpecDTO>> grouped = new LinkedHashMap<>();
            for (ProjectSpecDTO d : allData) {
                grouped.computeIfAbsent(d.getDomain() != null ? d.getDomain() : "UNKNOWN", k -> new ArrayList<>()).add(d);
            }

            String[] headers = {"Order", "Dataset", "Variable", "Label", "Data Type", "Length",
                    "Significant Digits", "Format", "Mandatory", "Assigned Value", "Codelist",
                    "Submission Value", "Common", "Origin", "Source", "Pages", "Text",
                    "Predecessor", "Role", "Has No Data", "Comment", "Developer Notes",
                    "SUPP", "QEVAL", "Method"};

            try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                for (Map.Entry<String, List<ProjectSpecDTO>> entry : grouped.entrySet()) {
                    Sheet sheet = workbook.createSheet(entry.getKey());
                    Row headerRow = sheet.createRow(0);
                    for (int i = 0; i < headers.length; i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                    }
                    List<ProjectSpecDTO> specList = entry.getValue();
                    for (int i = 0; i < specList.size(); i++) {
                        ProjectSpecDTO d = specList.get(i);
                        Row row = sheet.createRow(i + 1);
                        row.createCell(0).setCellValue(d.getSortOrder() != null ? String.valueOf(d.getSortOrder()) : "");
                        row.createCell(1).setCellValue(nvl(d.getDomain()));
                        row.createCell(2).setCellValue(nvl(d.getVariable()));
                        row.createCell(3).setCellValue(nvl(d.getLabel()));
                        row.createCell(4).setCellValue(nvl(d.getType()));
                        row.createCell(5).setCellValue(nvl(d.getLength()));
                        row.createCell(6).setCellValue(nvl(d.getSignificantDigits()));
                        row.createCell(7).setCellValue(nvl(d.getFormat()));
                        row.createCell(8).setCellValue(nvl(d.getMandatory()));
                        row.createCell(9).setCellValue(nvl(d.getAssignedValue()));
                        row.createCell(10).setCellValue(nvl(d.getCodelist()));
                        row.createCell(11).setCellValue(nvl(d.getCdiscSubmissionValue()));
                        row.createCell(12).setCellValue(nvl(d.getCommon()));
                        row.createCell(13).setCellValue(nvl(d.getOrigin()));
                        row.createCell(14).setCellValue(nvl(d.getSource()));
                        row.createCell(15).setCellValue(nvl(d.getPages()));
                        row.createCell(16).setCellValue(nvl(d.getTextContent()));
                        row.createCell(17).setCellValue(nvl(d.getPredecessor()));
                        row.createCell(18).setCellValue(nvl(d.getRole()));
                        row.createCell(19).setCellValue(nvl(d.getHasNoData()));
                        row.createCell(20).setCellValue(nvl(d.getComment()));
                        row.createCell(21).setCellValue(nvl(d.getDeveloperNotes()));
                        row.createCell(22).setCellValue(nvl(d.getSupp()));
                        row.createCell(23).setCellValue(nvl(d.getQeval()));
                        row.createCell(24).setCellValue(nvl(d.getMethod()));
                    }
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                workbook.write(out);
                byte[] bytes = out.toByteArray();
                workbook.dispose();
                String fileName = URLEncoder.encode("Spec_" + projectId + ".xlsx", StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                        .body(bytes);
            }
        } catch (Exception e) {
            logger.error("导出Spec XLSX失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/import-xlsx/{projectId}")
    public CommonResult<String> importSpecXlsx(@PathVariable String projectId,
                                               @RequestBody Map<String, Object> request) {
        try {
            List<Map<String, Object>> sheets = (List<Map<String, Object>>) request.get("sheets");
            if (sheets == null || sheets.isEmpty()) {
                return CommonResult.failed("无有效工作表数据");
            }

            projectSpecService.deleteByProjectId(projectId);
            int importCount = 0;

            for (Map<String, Object> sheetObj : sheets) {
                String sheetName = (String) sheetObj.getOrDefault("name", "");
                List<Map<String, Object>> celldata = (List<Map<String, Object>>) sheetObj.get("celldata");
                if (celldata == null || sheetName.isEmpty()) continue;

                Map<Integer, Map<Integer, String>> grid = new HashMap<>();
                for (Map<String, Object> cell : celldata) {
                    int r = toInt(cell.get("r"));
                    int c = toInt(cell.get("c"));
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
                    String variable = row.getOrDefault(2, "").trim();
                    if (variable.isEmpty()) continue;

                    ProjectSpecPO po = new ProjectSpecPO();
                    po.setProjectId(projectId);
                    po.setDomain(sheetName);
                    String orderStr = row.getOrDefault(0, "").trim();
                    if (!orderStr.isEmpty()) {
                        try { po.setSortOrder(Integer.parseInt(orderStr.contains(".") ? orderStr.split("\\.")[0] : orderStr)); }
                        catch (NumberFormatException ignored) { po.setSortOrder(importCount); }
                    } else {
                        po.setSortOrder(importCount);
                    }
                    po.setVariable(variable);
                    po.setLabel(row.getOrDefault(3, "").trim());
                    po.setType(row.getOrDefault(4, "").trim());
                    String lenStr = row.getOrDefault(5, "").trim();
                    if (!lenStr.isEmpty()) {
                        po.setLength(lenStr.contains(".") ? lenStr.split("\\.")[0] : lenStr);
                    }
                    po.setSignificantDigits(row.getOrDefault(6, "").trim());
                    po.setFormat(row.getOrDefault(7, "").trim());
                    po.setMandatory(row.getOrDefault(8, "").trim());
                    po.setAssignedValue(row.getOrDefault(9, "").trim());
                    po.setCodelist(row.getOrDefault(10, "").trim());
                    po.setCdiscSubmissionValue(row.getOrDefault(11, "").trim());
                    po.setCommon(row.getOrDefault(12, "").trim());
                    po.setOrigin(row.getOrDefault(13, "").trim());
                    po.setSource(row.getOrDefault(14, "").trim());
                    po.setPages(row.getOrDefault(15, "").trim());
                    po.setTextContent(row.getOrDefault(16, "").trim());
                    po.setDerivation(row.getOrDefault(17, "").trim());
                    po.setRole(row.getOrDefault(18, "").trim());
                    po.setHasNoData(row.getOrDefault(19, "").trim());
                    po.setComment(row.getOrDefault(20, "").trim());
                    po.setDeveloperNotes(row.getOrDefault(21, "").trim());
                    po.setSupp(row.getOrDefault(22, "").trim());
                    po.setQeval(row.getOrDefault(23, "").trim());
                    po.setMethod(row.getOrDefault(24, "").trim());
                    po.setCreatedBy("excel_import");

                    projectSpecService.addVariable(po);
                    importCount++;
                }
            }
            triggerSpecSync(projectId);
            return CommonResult.success("导入成功，共 " + importCount + " 条Spec数据");
        } catch (Exception e) {
            logger.error("导入Spec XLSX失败", e);
            return CommonResult.failed("导入失败: " + e.getMessage());
        }
    }

    private ProjectSpecPO mapToPO(Map<String, Object> m) {
        ProjectSpecPO po = new ProjectSpecPO();
        applyMapToPO(m, po);
        return po;
    }

    private void applyMapToPO(Map<String, Object> m, ProjectSpecPO po) {
        if (m.containsKey("projectId")) po.setProjectId(str(m.get("projectId")));
        if (m.containsKey("domain")) po.setDomain(str(m.get("domain")));
        if (m.containsKey("variable")) po.setVariable(str(m.get("variable")));
        if (m.containsKey("label")) po.setLabel(str(m.get("label")));
        if (m.containsKey("type")) po.setType(str(m.get("type")));
        if (m.containsKey("length")) po.setLength(str(m.get("length")));
        if (m.containsKey("controlledTermsOrFormat")) po.setControlledTermsOrFormat(str(m.get("controlledTermsOrFormat")));
        if (m.containsKey("cdiscSubmissionValue")) po.setCdiscSubmissionValue(str(m.get("cdiscSubmissionValue")));
        if (m.containsKey("origin")) po.setOrigin(str(m.get("origin")));
        if (m.containsKey("source")) po.setSource(str(m.get("source")));
        if (m.containsKey("role")) po.setRole(str(m.get("role")));
        if (m.containsKey("core")) po.setCore(str(m.get("core")));
        if (m.containsKey("codelist")) po.setCodelist(str(m.get("codelist")));
        if (m.containsKey("format")) po.setFormat(str(m.get("format")));
        if (m.containsKey("comment")) po.setComment(str(m.get("comment")));
        if (m.containsKey("cdiscNotes")) po.setCdiscNotes(str(m.get("cdiscNotes")));
        if (m.containsKey("mandatory")) po.setMandatory(str(m.get("mandatory")));
        if (m.containsKey("textContent")) po.setTextContent(str(m.get("textContent")));
        if (m.containsKey("method")) po.setMethod(str(m.get("method")));
        if (m.containsKey("derivation")) po.setDerivation(str(m.get("derivation")));
        if (m.containsKey("predecessor")) po.setPredecessor(str(m.get("predecessor")));
        if (m.containsKey("pages")) po.setPages(str(m.get("pages")));
        if (m.containsKey("significantDigits")) po.setSignificantDigits(str(m.get("significantDigits")));
        if (m.containsKey("assignedValue")) po.setAssignedValue(str(m.get("assignedValue")));
        if (m.containsKey("common")) po.setCommon(str(m.get("common")));
        if (m.containsKey("hasNoData")) po.setHasNoData(str(m.get("hasNoData")));
        if (m.containsKey("developerNotes")) po.setDeveloperNotes(str(m.get("developerNotes")));
        if (m.containsKey("supp")) po.setSupp(str(m.get("supp")));
        if (m.containsKey("qeval")) po.setQeval(str(m.get("qeval")));
        if (m.containsKey("sortOrder")) po.setSortOrder(toInt(m.get("sortOrder")));
    }

    @PostMapping("/generate-supp/{projectId}")
    public CommonResult<Map<String, Object>> generateSupp(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            List<ProjectSpecPO> allSpecs = projectSpecMapper.selectByProjectId(projectId, username);

            Map<String, List<ProjectSpecPO>> suppByDomain = new LinkedHashMap<>();
            for (ProjectSpecPO spec : allSpecs) {
                if ("Y".equalsIgnoreCase(nvl(spec.getSupp()).trim())) {
                    suppByDomain.computeIfAbsent(spec.getDomain(), k -> new ArrayList<>()).add(spec);
                }
            }

            if (suppByDomain.isEmpty()) {
                return CommonResult.failed("未找到 SUPP=Y 的变量，无需生成 SUPP 数据集");
            }

            // Remove existing SUPPxx datasets to avoid duplicates on re-run
            List<String> existingSuppDomains = new ArrayList<>();
            for (ProjectSpecPO spec : allSpecs) {
                String dom = spec.getDomain() != null ? spec.getDomain().toUpperCase() : "";
                if (dom.startsWith("SUPP") && !existingSuppDomains.contains(dom)) {
                    existingSuppDomains.add(dom);
                }
            }
            for (String suppDom : existingSuppDomains) {
                List<ProjectSpecPO> suppDomSpecs = projectSpecMapper.selectByProjectIdAndDomain(projectId, suppDom, username);
                for (ProjectSpecPO s : suppDomSpecs) {
                    projectSpecMapper.deleteById(s.getId());
                }
            }

            String[][] suppTemplate = {
                {"STUDYID",  "研究标识符",       "Identifier"},
                {"RDOMAIN",  "关联域名缩写",     "Identifier"},
                {"USUBJID",  "受试者唯一标识符",  "Identifier"},
                {"IDVAR",    "标识变量",         "Identifier"},
                {"IDVARVAL", "标识变量值",        "Identifier"},
                {"QNAM",     "修饰语变量名称",    "Qualifier"},
                {"QLABEL",   "修饰语变量标签",    "Qualifier"},
                {"QVAL",     "Data Value",       "Qualifier"},
                {"QORIG",    "来源",             "Qualifier"},
                {"QEVAL",    "评估者",           "Qualifier"},
            };

            int deletedCount = 0;
            int createdDatasets = 0;
            List<String> generatedDatasets = new ArrayList<>();

            for (Map.Entry<String, List<ProjectSpecPO>> entry : suppByDomain.entrySet()) {
                String domain = entry.getKey();
                List<ProjectSpecPO> suppVars = entry.getValue();
                String suppDataset = "SUPP" + domain.toUpperCase();

                for (ProjectSpecPO sv : suppVars) {
                    projectSpecMapper.deleteById(sv.getId());
                    deletedCount++;
                }

                int order = 1;
                for (String[] tpl : suppTemplate) {
                    ProjectSpecPO po = new ProjectSpecPO();
                    po.setProjectId(projectId);
                    po.setDomain(suppDataset);
                    po.setVariable(tpl[0]);
                    po.setLabel(tpl[1]);
                    po.setRole(tpl[2]);
                    po.setSortOrder(order++);
                    po.setCreatedBy("supp_gen");
                    po.setUsername(username);
                    projectSpecMapper.insert(po);
                }
                createdDatasets++;
                generatedDatasets.add(suppDataset);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("deletedVariables", deletedCount);
            result.put("createdDatasets", createdDatasets);
            result.put("datasets", generatedDatasets);
            result.put("message", String.format("已移除 %d 个 SUPP=Y 变量，生成 %d 个 SUPP 数据集：%s",
                    deletedCount, createdDatasets, String.join(", ", generatedDatasets)));

            logger.info("SUPP生成完成: projectId={}, deleted={}, datasets={}", projectId, deletedCount, generatedDatasets);
            triggerSpecSync(projectId);
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("生成SUPP数据集失败", e);
            return CommonResult.failed("生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/extract-xpt-metadata/{projectId}")
    public CommonResult<Map<String, Object>> extractXptMetadata(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            String pythonExec = findPythonExec();
            if (pythonExec == null) {
                return CommonResult.failed("未找到可用的 Python，无法提取 XPT 元数据");
            }

            String script = pythonPath + "/define/var_xpt_metadata.py";
            if (!new File(script).exists()) {
                return CommonResult.failed("XPT 元数据提取脚本不存在: " + script);
            }

            String standardType = pathResolver.resolveStandardType(projectId, null);
            ProcessBuilder pb = new ProcessBuilder(pythonExec, script);
            pb.directory(new File(pythonPath + "/define"));
            pb.redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.put("PROJECT_ID", projectId);
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHON_BASE_PATH", pythonPath);
            env.put("DATA_PATH", pathResolver.xptDirectory(projectId, standardType).toString());
            if (username != null && !username.isEmpty()) {
                env.put("USERNAME_CONTEXT", username);
            }

            Process process = pb.start();
            AtomicReference<String> resultJson = new AtomicReference<>();
            CompletableFuture<Void> outputReader = CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("[RESULT] ")) {
                            resultJson.set(line.substring("[RESULT] ".length()));
                        } else {
                            logger.debug("[var-xpt] {}", line);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("读取XPT提取进程输出失败", e);
                }
            });

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return CommonResult.failed("XPT 元数据提取超时");
            }
            outputReader.get(5, TimeUnit.SECONDS);

            Map<String, Object> detail = resultJson.get() != null
                    ? OBJECT_MAPPER.readValue(resultJson.get(), Map.class)
                    : new LinkedHashMap<>();
            int updated = numberValue(detail.get("updated"));
            int skipped = numberValue(detail.get("skipped"));
            boolean partial = Boolean.TRUE.equals(detail.get("partial"));
            detail.put("message", String.format(
                    "%s：更新 %d 条，跳过 %d 条",
                    partial ? "XPT元数据部分提取成功" : "XPT元数据提取成功",
                    updated, skipped));

            if (process.exitValue() != 0 || updated == 0) {
                String errors = detail.get("errors") != null ? detail.get("errors").toString() : "";
                return CommonResult.failed(errors.isEmpty()
                        ? "XPT 元数据未更新，请检查文件和变量匹配关系"
                        : "XPT 元数据提取失败: " + errors);
            }

            triggerSpecSync(projectId);
            return CommonResult.success(detail);
        } catch (Exception e) {
            logger.error("提取XPT元数据失败", e);
            return CommonResult.failed("XPT元数据提取失败: " + e.getMessage());
        }
    }

    @PostMapping("/extract-p21-fields/{projectId}")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Map<String, Object>> extractP21Fields(@PathVariable String projectId) {
        try {
            String[] loadErr = {null};
            Map<String, Map<String, String>> p21Map = loadP21VariablesMap(projectId, loadErr);
            if (p21Map == null) {
                return CommonResult.failed(loadErr[0] != null ? loadErr[0] : "P21文件读取失败");
            }

            String username = UserContext.getUsername();
            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);
            Set<String> specKeys = new LinkedHashSet<>();
            int matchCount = 0;

            for (ProjectSpecPO spec : specList) {
                String key = (spec.getDomain() != null ? spec.getDomain().toUpperCase() : "")
                        + "|" + (spec.getVariable() != null ? spec.getVariable().toUpperCase() : "");
                specKeys.add(key);
                Map<String, String> p21Fields = p21Map.get(key);
                if (p21Fields == null) continue;

                boolean changed = false;
                String val = p21Fields.get("mandatory");
                if (val != null && !val.isEmpty()) { spec.setMandatory(val); changed = true; }
                val = p21Fields.get("role");
                if (val != null && !val.isEmpty()) { spec.setRole(val); changed = true; }
                val = p21Fields.get("hasNoData");
                if (val != null && !val.isEmpty()) { spec.setHasNoData(val); changed = true; }

                if (changed) {
                    spec.setUpdatedTime(new java.util.Date());
                    spec.setUpdatedBy(appendUpdateSource(spec.getUpdatedBy(), "p21_extract"));
                    projectSpecMapper.updateById(spec);
                    matchCount++;
                }
            }

            if (matchCount == 0) {
                return CommonResult.failed("P21与当前项目Spec没有匹配变量，请检查Dataset和Variable列");
            }

            Set<String> specOnly = new LinkedHashSet<>(specKeys);
            specOnly.removeAll(p21Map.keySet());
            Set<String> p21Only = new LinkedHashSet<>(p21Map.keySet());
            p21Only.removeAll(specKeys);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matched", matchCount);
            result.put("specOnlyCount", specOnly.size());
            result.put("p21OnlyCount", p21Only.size());
            result.put("specOnly", new ArrayList<>(specOnly));
            result.put("p21Only", new ArrayList<>(p21Only));
            result.put("message", String.format(
                    "P21字段提取完成：匹配更新 %d 条，Spec独有 %d 条，P21独有 %d 条",
                    matchCount, specOnly.size(), p21Only.size()));

            triggerSpecSync(projectId);
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("提取P21字段失败", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return CommonResult.failed("P21字段提取失败: " + e.getMessage());
        }
    }

    private int numberValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String appendUpdateSource(String existing, String source) {
        if (existing == null || existing.isBlank()) return source;
        if (Arrays.asList(existing.split(",")).contains(source)) return existing;
        return existing + "," + source;
    }

    @GetMapping("/compare-p21/{projectId}")
    public CommonResult<Map<String, Object>> compareP21(@PathVariable String projectId) {
        try {
            String[] loadErr = {null};
            Map<String, Map<String, String>> p21Map = loadP21VariablesMap(projectId, loadErr);
            if (p21Map == null) return CommonResult.failed(loadErr[0]);

            String username = UserContext.getUsername();
            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);

            Set<String> specKeys = new LinkedHashSet<>();
            for (ProjectSpecPO spec : specList) {
                specKeys.add((spec.getDomain() != null ? spec.getDomain().toUpperCase() : "")
                        + "|" + (spec.getVariable() != null ? spec.getVariable().toUpperCase() : ""));
            }
            Set<String> p21Keys = p21Map.keySet();
            Set<String> matched = new LinkedHashSet<>(specKeys);
            matched.retainAll(p21Keys);

            List<Map<String, String>> specOnlyList = new ArrayList<>();
            for (String key : specKeys) {
                if (!matched.contains(key)) {
                    String[] parts = key.split("\\|", 2);
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("dataset", parts[0]);
                    item.put("variable", parts.length > 1 ? parts[1] : "");
                    specOnlyList.add(item);
                }
            }
            List<Map<String, String>> p21OnlyList = new ArrayList<>();
            for (String key : p21Keys) {
                if (!matched.contains(key)) {
                    String[] parts = key.split("\\|", 2);
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("dataset", parts[0]);
                    item.put("variable", parts.length > 1 ? parts[1] : "");
                    p21OnlyList.add(item);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matched", matched.size());
            result.put("specTotal", specList.size());
            result.put("p21Total", p21Map.size());
            result.put("specOnly", specOnlyList);
            result.put("p21Only", p21OnlyList);
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("对比P21变量失败", e);
            return CommonResult.failed("对比失败: " + e.getMessage());
        }
    }

    private Map<String, Map<String, String>> loadP21VariablesMap(String projectId, String[] errOut) throws Exception {
        List<FileUploadRecord> p21Files = fileUploadRecordMapper
                .selectByProjectIdAndCategory(projectId, "P21_SPEC");
        if (p21Files == null || p21Files.isEmpty()) {
            errOut[0] = "未找到已上传的P21空Spec文件，请先在「文件上传」中上传P21空Spec";
            return null;
        }
        FileUploadRecord latest = p21Files.get(0);
        String p21Path = latest.getWorkspaceFilePath() != null && !latest.getWorkspaceFilePath().isBlank()
                ? latest.getWorkspaceFilePath()
                : pathResolver.workspaceFile(projectId,
                    pathResolver.resolveStandardType(projectId, latest.getStandardType()),
                    FileUploadRecord.FileCategory.P21_SPEC, latest.getOriginalName()).toString();
        File p21File = new File(p21Path);
        if (!p21File.exists()) {
            errOut[0] = "P21文件不存在于磁盘: " + latest.getFilePath();
            return null;
        }

        Map<String, Map<String, String>> p21Map = new LinkedHashMap<>();
        try (FileInputStream fis = new FileInputStream(p21File);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet varSheet = null;
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                if ("Variables".equalsIgnoreCase(wb.getSheetName(i).trim())) {
                    varSheet = wb.getSheetAt(i);
                    break;
                }
            }
            if (varSheet == null) {
                errOut[0] = "P21文件中未找到 \"Variables\" 工作表";
                return null;
            }

            Row headerRow = varSheet.getRow(0);
            if (headerRow == null) {
                errOut[0] = "Variables 工作表表头为空";
                return null;
            }
            Map<String, Integer> colIdx = new HashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell != null) {
                    String h = getCellStr(cell).trim();
                    if (!h.isEmpty()) colIdx.put(h.toLowerCase(), c);
                }
            }
            int datasetCol = findColIdx(colIdx, "dataset");
            int variableCol = findColIdx(colIdx, "variable");
            if (datasetCol < 0 || variableCol < 0) {
                errOut[0] = "Variables 工作表缺少 Dataset 或 Variable 列";
                return null;
            }
            int dataTypeCol = findColIdx(colIdx, "data type", "datatype", "type");
            int lengthCol = findColIdx(colIdx, "length");
            int sigDigCol = findColIdx(colIdx, "significant digits", "significantdigits", "sig digits");
            int formatCol = findColIdx(colIdx, "format");
            int mandatoryCol = findColIdx(colIdx, "mandatory");
            int roleCol = findColIdx(colIdx, "role");
            int hasNoDataCol = findColIdx(colIdx, "has no data", "hasnodata");

            for (int r = 1; r <= varSheet.getLastRowNum(); r++) {
                Row row = varSheet.getRow(r);
                if (row == null) continue;
                String ds = getCellStr(row, datasetCol).trim();
                String var = getCellStr(row, variableCol).trim();
                if (ds.isEmpty() || var.isEmpty()) continue;

                Map<String, String> fields = new HashMap<>();
                if (dataTypeCol >= 0) fields.put("type", getCellStr(row, dataTypeCol).trim());
                if (lengthCol >= 0) fields.put("length", getCellStr(row, lengthCol).trim());
                if (sigDigCol >= 0) fields.put("significantDigits", getCellStr(row, sigDigCol).trim());
                if (formatCol >= 0) fields.put("format", getCellStr(row, formatCol).trim());
                if (mandatoryCol >= 0) fields.put("mandatory", getCellStr(row, mandatoryCol).trim());
                if (roleCol >= 0) fields.put("role", getCellStr(row, roleCol).trim());
                if (hasNoDataCol >= 0) fields.put("hasNoData", getCellStr(row, hasNoDataCol).trim());

                p21Map.put(ds.toUpperCase() + "|" + var.toUpperCase(), fields);
            }
        }

        if (p21Map.isEmpty()) {
            errOut[0] = "P21 Variables 工作表无有效数据";
            return null;
        }
        return p21Map;
    }

    private int findColIdx(Map<String, Integer> colIdx, String... aliases) {
        for (String alias : aliases) {
            Integer idx = colIdx.get(alias.toLowerCase());
            if (idx != null) return idx;
        }
        for (String alias : aliases) {
            String lowerAlias = alias.toLowerCase();
            for (Map.Entry<String, Integer> entry : colIdx.entrySet()) {
                if (entry.getKey().contains(lowerAlias)) return entry.getValue();
            }
        }
        return -1;
    }

    private String getCellStr(Row row, int col) {
        if (row == null || col < 0) return "";
        Cell cell = row.getCell(col);
        return cell != null ? getCellStr(cell) : "";
    }

    private String getCellStr(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) {
                    try { yield String.valueOf(cell.getNumericCellValue()); }
                    catch (Exception e2) { yield ""; }
                }
            }
            default -> "";
        };
    }

    @PostMapping("/extract-pages/{projectId}")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<String> extractPages(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            String standardType = pathResolver.resolveStandardType(projectId, null);
            String outputDir = pathResolver.projectSpecDirectory(projectId, standardType).toString();
            String syncedSpec = projectSpecService.syncSpecToFile(projectId, outputDir);
            if (syncedSpec == null || !new File(syncedSpec).isFile()) {
                return CommonResult.failed("无法生成数据库最新Spec，请先检查项目Spec文件");
            }

            String pipelineResult = runPagesPipeline(projectId, username, syncedSpec);
            if (pipelineResult != null) {
                return CommonResult.failed(pipelineResult);
            }

            List<PagesDataPO> pagesList = pagesDataMapper.selectByProjectId(projectId, username);
            if (pagesList == null || pagesList.isEmpty()) {
                return CommonResult.failed("Pages 提取脚本未产生数据，原有Pages字段未修改");
            }

            Map<String, String> pagesMap = new HashMap<>();
            int totalCount = 0;
            int domainVarCount = 0;
            for (PagesDataPO p : pagesList) {
                totalCount++;
                String origin = p.getOrigin() != null ? p.getOrigin().trim() : "";
                if (!"Domain_Variable".equals(origin)) {
                    continue;
                }
                domainVarCount++;
                String ds = p.getDataset() != null ? p.getDataset().toUpperCase().trim() : "";
                String var = p.getVariable() != null ? p.getVariable().toUpperCase().trim() : "";
                String pg = p.getPages() != null ? p.getPages().trim() : "";
                if (!ds.isEmpty() && !var.isEmpty() && !pg.isEmpty()) {
                    pagesMap.put(ds + "|" + var, pg);
                }
            }

            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);

            int clearCount = 0;
            int unchangedCount = 0;
            int skippedSupp = 0;
            int matchCount = 0;
            int skippedNonInv = 0;
            for (ProjectSpecPO spec : specList) {
                String dom = spec.getDomain() != null ? spec.getDomain().toUpperCase().trim() : "";
                if (dom.startsWith("SUPP")) { skippedSupp++; continue; }
                String src = spec.getSource() != null ? spec.getSource().trim() : "";
                String key = dom + "|" + (spec.getVariable() != null ? spec.getVariable().toUpperCase().trim() : "");
                if (!src.equalsIgnoreCase("Investigator")) {
                    if (pagesMap.containsKey(key)) skippedNonInv++;
                    continue;
                }
                String pages = pagesMap.get(key);
                if (Objects.equals(spec.getPages(), pages)) {
                    unchangedCount++;
                    if (pages != null) matchCount++;
                    continue;
                }
                if (pages == null && spec.getPages() != null && !spec.getPages().isBlank()) {
                    clearCount++;
                }
                spec.setPages(pages);
                spec.setUpdatedTime(new java.util.Date());
                spec.setUpdatedBy(appendUpdateSource(spec.getUpdatedBy(), "pages_extract"));
                projectSpecMapper.updateById(spec);
                if (pages != null) {
                    matchCount++;
                }
            }

            logger.info("Pages字段提取完成: projectId={}, total={}, domainVar={}, cleared={}, matched={}, unchanged={}, skippedNonInv={}, skippedSupp={}",
                    projectId, totalCount, domainVarCount, clearCount, matchCount, unchangedCount, skippedNonInv, skippedSupp);
            triggerSpecSync(projectId);

            StringBuilder msg = new StringBuilder();
            msg.append(String.format("已按最新aCRF和Spec重新提取。Pages 数据共 %d 条（Domain_Variable %d 条），Source=Investigator 匹配填充 %d 条",
                    totalCount, domainVarCount, matchCount));
            if (skippedNonInv > 0) {
                msg.append(String.format("，跳过非 Investigator 变量 %d 条", skippedNonInv));
            }
            if (skippedSupp > 0) {
                msg.append(String.format("，跳过 SUPPxx 数据集 %d 条", skippedSupp));
            }
            return CommonResult.success(msg.toString());
        } catch (Exception e) {
            logger.error("提取Pages字段失败", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return CommonResult.failed("提取失败: " + e.getMessage());
        }
    }

    private String runPagesPipeline(String projectId, String username, String syncedSpecPath) {
        String standardType = pathResolver.resolveStandardType(projectId, null);
        File annotsFile = pathResolver.acrfAnnotations(projectId, standardType).toFile();
        if (!annotsFile.exists()) {
            return "未找到 aCRF 注释文件 (Annots2.xlsx)，请先上传 aCRF 并完成处理";
        }

        String scriptPath = pythonPath + "/define/extraction_pipeline.py";
        if (!new File(scriptPath).exists()) {
            return "提取脚本不存在: " + scriptPath;
        }

        String[] pyExecs = {"python", "python3", "py"};
        String pyExec = null;
        for (String exec : pyExecs) {
            try {
                Process tp = new ProcessBuilder(exec, "--version").start();
                if (tp.waitFor() == 0) { pyExec = exec; break; }
            } catch (Exception ignored) {}
        }
        if (pyExec == null) {
            return "未找到可用的 Python 可执行文件";
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pyExec, scriptPath,
                    "--project-id", projectId,
                    "--upload-base", uploadBasePath,
                    "--python-base", pythonPath,
                    "--steps", "pages",
                    "--username", username != null ? username : ""
            );
            pb.directory(new File(pythonPath + "/define"));
            pb.redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("ANNOTS_PATH", annotsFile.getAbsolutePath());
            env.put("DATA_PATH", pathResolver.xptDirectory(projectId, standardType).toString());
            env.put("SPEC_PATH", syncedSpecPath);
            env.put("OUTPUT_PATH", pathResolver.extractionOutputDirectory(projectId, standardType).toString());
            env.put("VLM_PATH", pathResolver.extractionOutputDirectory(projectId, standardType)
                    .resolve("vlm_codelists.xlsx").toString());
            if (username != null && !username.isEmpty()) {
                env.put("USERNAME_CONTEXT", username);
            }

            Process process = pb.start();
            CompletableFuture<String> outputReader = CompletableFuture.supplyAsync(() -> {
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append('\n');
                    }
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
                return output.toString();
            });

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Pages 提取脚本执行超时";
            }
            String output = outputReader.get(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return "Pages 提取脚本执行失败 (exit " + exitCode + "): " + output;
            }
            logger.info("Pages pipeline executed for project {}: {}", projectId, output.trim());
            return null;
        } catch (Exception e) {
            return "运行 Pages 提取脚本异常: " + e.getMessage();
        }
    }

    /**
     * Extract Variables-level codelists.
     * All Terms come from XPT data. NCI codes are matched by looking up each Term in ct_term.
     */
    @RequireProjectAccess("projectId")
    @PostMapping("/extract-var-codelist/{projectId}")
    public CommonResult<CodelistExtractionResult> extractVarCodelist(@PathVariable String projectId) {
        try {
            CodelistExtractionResult result = codelistExtractionService.extract(
                    projectId, UserContext.getUsername(), CodelistExtractionService.Scope.VARIABLES);
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("统一提取 Variables Codelist 失败", e);
            return CommonResult.failed("提取失败: " + e.getMessage());
        }
    }

    private CommonResult<Map<String, Object>> extractVarCodelistLegacy(String projectId) {
        try {
            String username = UserContext.getUsername();
            logger.info("[extract-var-codelist] projectId={}, username={}", projectId, username);

            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);
            if (specList == null || specList.isEmpty()) {
                return CommonResult.failed("未找到 Variables 数据");
            }

            // Resolve CT package for NCI matching
            ProjectConfigPO config = projectConfigMapper.selectByProjectId(projectId);
            if (config == null) config = projectConfigMapper.selectByProjectId("DEFAULT");
            String standardType = config != null && config.getStandardType() != null ? config.getStandardType() : "SDTM";
            String ctVersionStr = config != null ? config.getCtVersion() : "";
            Long packageId = resolveCtPackageId(standardType, ctVersionStr);
            String ctDate = extractDateStr(ctVersionStr);
            String terminologyLabel = packageId != null ? standardType + " Terminology " + ctDate : "";

            // CT header: VCD -> {nciCodelistCode, codelistName}
            Map<String, Map<String, String>> ctHeaderMap = new HashMap<>();
            // CT term lookup: "nciCodelistCode|submVal_upper" -> termCode (scoped by codelist)
            Map<String, String> ctTermLookup = new HashMap<>();
            if (packageId != null) {
                List<Map<String, Object>> ctRows = jdbcTemplate.queryForList(
                        "SELECT codelist_code, codelist_name, term_code, cdisc_submission_value FROM ct_term WHERE package_id = ?",
                        packageId);
                for (Map<String, Object> row : ctRows) {
                    String clCode = row.get("codelist_code") != null ? row.get("codelist_code").toString() : "";
                    String clName = row.get("codelist_name") != null ? row.get("codelist_name").toString() : "";
                    String termCode = row.get("term_code") != null ? row.get("term_code").toString() : "";
                    String submVal = row.get("cdisc_submission_value") != null ? row.get("cdisc_submission_value").toString() : "";

                    if (clCode.isEmpty() || clCode.equals("null")) {
                        Map<String, String> hdr = new HashMap<>();
                        hdr.put("nciCodelistCode", termCode);
                        hdr.put("codelistName", clName);
                        ctHeaderMap.put(submVal.toUpperCase(), hdr);
                    } else {
                        if (!submVal.isEmpty() && !clCode.isEmpty()) {
                            ctTermLookup.put(clCode.toUpperCase() + "|" + submVal.toUpperCase(), termCode);
                        }
                    }
                }
                logger.info("[extract-var-codelist] CT loaded: {} headers, {} term entries", ctHeaderMap.size(), ctTermLookup.size());
            }

            // Delete existing Variables-level codelist data (keep only VLM-level)
            jdbcTemplate.update(
                    "DELETE FROM sas_codelist_data WHERE project_id = ? AND username = ? AND (created_by IS NULL OR created_by != 'extract_vlm_codelist')",
                    projectId, username);

            // Delete existing Dictionaries data for re-extraction
            dictionariesDataMapper.deleteByProjectId(projectId, username);

            int totalCodelists = 0, nciMatched = 0;
            int dictOrder = 0;
            // Variables always starts from sort_order = 1 (VLM uses offset 500000+)
            int globalOrder = 0;
            String pythonExec = findPythonExec();

            // Group spec rows by cdisc_submission_value (sv)
            Map<String, List<ProjectSpecPO>> svGroups = new LinkedHashMap<>();
            for (ProjectSpecPO spec : specList) {
                String sv = spec.getCdiscSubmissionValue() != null ? spec.getCdiscSubmissionValue().trim() : "";
                if (sv.isEmpty()) continue;
                svGroups.computeIfAbsent(sv, k -> new ArrayList<>()).add(spec);
            }

            for (Map.Entry<String, List<ProjectSpecPO>> entry : svGroups.entrySet()) {
                String vcd = entry.getKey();
                List<ProjectSpecPO> rows = entry.getValue();

                // MEDDRA / WHODRUG are dictionaries, not codelists – write to Dictionaries table and skip codelist extraction
                String vcdUpper = vcd.toUpperCase();
                if ("MEDDRA".equals(vcdUpper) || "WHODRUG".equals(vcdUpper)) {
                    DictionariesDataPO dictPo = new DictionariesDataPO();
                    dictPo.setProjectId(projectId);
                    dictPo.setUsername(username);
                    dictPo.setDictionaryId(vcdUpper);
                    if ("MEDDRA".equals(vcdUpper)) {
                        dictPo.setName("Adverse Event Dictionary");
                    } else {
                        dictPo.setName("Drug Dictionary");
                    }
                    dictPo.setDataType("text");
                    dictPo.setDictionary(vcdUpper);
                    dictPo.setVersion("");
                    dictPo.setSortOrder(++dictOrder);
                    dictPo.setCreatedBy("extract_var_codelist");
                    dictionariesDataMapper.insert(dictPo);
                    logger.info("[extract-var-codelist] Dictionary written: {}", vcdUpper);
                    continue;
                }

                Map<String, String> ctHdr = ctHeaderMap.get(vcdUpper);
                String nciClCode = ctHdr != null ? ctHdr.get("nciCodelistCode") : null;

                String decodeCol = deriveDecodeColumn(vcd);
                boolean isDomainSv = "DOMAIN".equalsIgnoreCase(vcd);

                // --- DOMAIN: merge all domain values into one single codelist ---
                if (isDomainSv) {
                    Set<String> seenTerms = new LinkedHashSet<>();
                    String domainLabel = "";
                    for (ProjectSpecPO s : rows) {
                        String dom = s.getDomain() != null ? s.getDomain().trim() : "";
                        String var = s.getVariable() != null ? s.getVariable().trim() : "";
                        if (dom.isEmpty() || var.isEmpty()) continue;
                        if (domainLabel.isEmpty() && s.getLabel() != null) domainLabel = s.getLabel().trim();
                        List<String[]> termPairs = extractXptTermPairs(pythonExec, projectId, dom, var, vcd, decodeCol);
                        for (String[] pair : termPairs) {
                            if (pair[0] != null && !pair[0].isEmpty()) seenTerms.add(pair[0]);
                        }
                        s.setCodelist("DOMAIN");
                        s.setUpdatedTime(new java.util.Date());
                        s.setUpdatedBy("extract_var_codelist");
                        projectSpecMapper.updateById(s);
                    }
                    if (!seenTerms.isEmpty()) {
                        List<String[]> allTerms = new ArrayList<>();
                        for (String t : seenTerms) allTerms.add(new String[]{t, ""});
                        allTerms = sortTermPairs(allTerms);
                        int cdnum = 1;
                        for (String[] pair : allTerms) {
                            globalOrder++;
                            CodelistDataPO po = new CodelistDataPO();
                            po.setProjectId(projectId);
                            po.setUsername(username);
                            po.setVcd("DOMAIN");
                            po.setVlabel(domainLabel);
                            po.setNciCodelistCode(ctHeaderMap.containsKey("DOMAIN") ? ctHeaderMap.get("DOMAIN").get("nciCodelistCode") : null);
                            po.setType("Char");
                            po.setTerminology(po.getNciCodelistCode() != null ? terminologyLabel : null);
                            po.setCdnum(cdnum++);
                            po.setCode(pair[0]);
                            String scopedKey = po.getNciCodelistCode() != null ? po.getNciCodelistCode().toUpperCase() + "|" + pair[0].toUpperCase() : null;
                            po.setNciTermCode(scopedKey != null ? ctTermLookup.get(scopedKey) : null);
                            if (po.getNciTermCode() != null) nciMatched++;
                            po.setCodeDes("");
                            po.setOrigin("DOMAIN");
                            po.setSortOrder(globalOrder);
                            po.setCreatedBy("extract_var_codelist");
                            codelistDataMapper.insert(po);
                        }
                        totalCodelists++;
                    }
                    continue;
                }

                // Collect unique domain.variables and extract terms independently
                // Each entry: {ProjectSpecPO, domain, variable, termPairs, ownLabel}
                List<Object[]> dvInfoList = new ArrayList<>();
                Set<String> seenDV = new HashSet<>();

                for (ProjectSpecPO s : rows) {
                    String dom = s.getDomain() != null ? s.getDomain().trim() : "";
                    String var = s.getVariable() != null ? s.getVariable().trim() : "";
                    String dvKey = dom.toUpperCase() + "." + var.toUpperCase();
                    if (dom.isEmpty() || var.isEmpty() || seenDV.contains(dvKey)) continue;
                    seenDV.add(dvKey);

                    String ownLabel = s.getLabel() != null ? s.getLabel().trim() : "";
                    List<String[]> termPairs = extractXptTermPairs(pythonExec, projectId, dom, var, vcd, decodeCol);
                    dvInfoList.add(new Object[]{s, dom, var, termPairs, ownLabel});
                }

                // Insert codelist entries for each domain.variable
                for (Object[] info : dvInfoList) {
                    ProjectSpecPO s = (ProjectSpecPO) info[0];
                    String dom = (String) info[1];
                    String var = (String) info[2];
                    @SuppressWarnings("unchecked")
                    List<String[]> termPairs = (List<String[]>) info[3];
                    String ownLabel = (String) info[4];

                    String clId = dom + "." + var;

                    if (!termPairs.isEmpty()) {
                        termPairs = sortTermPairs(termPairs);
                        int cdnum = 1;
                        for (String[] pair : termPairs) {
                            String term = pair[0];
                            String xptDecoded = pair.length > 1 ? pair[1] : "";
                            globalOrder++;
                            CodelistDataPO po = new CodelistDataPO();
                            po.setProjectId(projectId);
                            po.setUsername(username);
                            po.setVcd(clId);
                            po.setVlabel(ownLabel);
                            po.setNciCodelistCode(nciClCode);
                            po.setType("Char");
                            po.setTerminology(nciClCode != null ? terminologyLabel : null);
                            po.setCdnum(cdnum++);
                            po.setCode(term);

                            String scopedKey = (nciClCode != null && !nciClCode.isEmpty())
                                    ? nciClCode.toUpperCase() + "|" + term.toUpperCase() : null;
                            String nciTermCode = scopedKey != null ? ctTermLookup.get(scopedKey) : null;
                            po.setNciTermCode(nciTermCode);
                            if (nciTermCode != null) nciMatched++;

                            po.setCodeDes(xptDecoded);
                            po.setOrigin(clId);
                            po.setSortOrder(globalOrder);
                            po.setCreatedBy("extract_var_codelist");
                            codelistDataMapper.insert(po);
                        }
                        totalCodelists++;
                    }

                    s.setCodelist(termPairs.isEmpty() ? null : clId);
                    s.setUpdatedTime(new java.util.Date());
                    s.setUpdatedBy("extract_var_codelist");
                    projectSpecMapper.updateById(s);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("codelists", totalCodelists);
            result.put("nciMatched", nciMatched);
            result.put("message", String.format("Codelist提取完成：%d 个codelist，NCI匹配 %d 个Term", totalCodelists, nciMatched));
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("提取Variables Codelist失败", e);
            return CommonResult.failed("提取失败: " + e.getMessage());
        }
    }

    /**
     * Sort term pairs with the following priority:
     * 1. Clinical normalcy: 正常/NORMAL first, then 异常无临床意义/NCS, then 异常有临床意义/CS
     * 2. Negativity before positivity: 阴性/NEGATIVE before 阳性/POSITIVE
     * 3. Unknown/Other always last: 未知/unknown/其他/other variants
     * 4. Otherwise alphabetical.
     */
    private List<String[]> sortTermPairs(List<String[]> pairs) {
        if (pairs == null || pairs.size() <= 1) return pairs;
        List<String[]> sorted = new ArrayList<>(pairs);
        sorted.sort((a, b) -> {
            int wa = termWeight(a[0]);
            int wb = termWeight(b[0]);
            if (wa != wb) return Integer.compare(wa, wb);
            return a[0].compareToIgnoreCase(b[0]);
        });
        return sorted;
    }

    /** Weight for term sorting (lower = earlier). */
    private int termWeight(String term) {
        if (term == null) return 50;
        String t = term.trim();
        String tu = t.toUpperCase();

        // Unknown / Other → always last
        if (t.equals("未知") || tu.equals("UNKNOWN") || tu.startsWith("UNKNOWN") ||
            t.equals("其他") || tu.equals("OTHER") || tu.startsWith("OTHER")) return 90;

        // Not done / not examined → after all regular terms
        if (t.equals("未查") || t.equals("未做") || tu.equals("NOT DONE") ||
            tu.equals("NOT EXAMINED") || tu.startsWith("NOT DONE") ||
            tu.startsWith("NOT EXAMINED")) return 80;

        // Clinical normalcy ordering
        if (t.equals("正常") || tu.equals("NORMAL") || tu.equals("WNL") ||
            tu.equals("WITHIN NORMAL LIMITS")) return 10;
        if (t.contains("无临床意义") || tu.contains("NCS") ||
            (tu.contains("NOT CLINICALLY") && tu.contains("SIGNIFICANT"))) return 20;
        if (t.equals("异常") || tu.equals("ABNORMAL")) return 25;
        if (t.contains("有临床意义") ||
            (tu.contains("CLINICALLY SIGNIFICANT") && !tu.contains("NOT"))) return 30;

        // Negative before positive
        if (t.equals("阴性") || tu.equals("NEGATIVE") || tu.equals("NEG")) return 11;
        if (t.equals("阳性") || tu.equals("POSITIVE") || tu.equals("POS")) return 12;

        return 50;
    }

    private String findPythonExec() {
        for (String exec : new String[]{"python", "python3", "py"}) {
            try {
                Process p = new ProcessBuilder(exec, "--version").start();
                if (p.waitFor() == 0) return exec;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Derive the decode-value column from a codelist variable name.
     * LBTESTCD → LBTEST, VSTESTCD → VSTEST, TSPARMCD → TSPARM, etc.
     * If the variable doesn't end with "CD", returns null (no paired decode column).
     */
    private String deriveDecodeColumn(String vcd) {
        if (vcd == null) return null;
        String upper = vcd.toUpperCase().trim();
        if (upper.endsWith("CD")) {
            return upper.substring(0, upper.length() - 2);
        }
        return null;
    }

    /**
     * Extract term→decodedValue pairs from XPT, first-appearance order.
     * Returns list of [term, decodedValue]. If decodeCol is null or not found, decodedValue is "".
     */
    private List<String[]> extractXptTermPairs(String pythonExec, String projectId, String domain,
                                                String variable, String vcd, String decodeCol) {
        List<String[]> pairs = new ArrayList<>();
        if (pythonExec == null) return pairs;
        try {
            String xptDir = pathResolver.xptDirectory(
                    projectId, pathResolver.resolveStandardType(projectId, null)).toString();
            java.io.File dir = new java.io.File(xptDir);
            if (!dir.exists()) return pairs;
            java.io.File xptFile = findXptFile(dir, domain);
            if (xptFile == null) return pairs;

            String col = variable != null ? variable.toUpperCase() : vcd.toUpperCase();
            String dcol = decodeCol != null ? decodeCol.toUpperCase() : "";
            // Python script: print "term\tdecodedValue" for each unique term (first-appearance order)
            String script = String.format(
                    "import pyreadstat,sys\n" +
                    "try:\n" +
                    "    d,_=pyreadstat.read_xport(sys.argv[1])\n" +
                    "    d.columns=d.columns.str.upper()\n" +
                    "    c='%s';dc='%s'\n" +
                    "    if c not in d.columns:\n" +
                    "        sys.exit(0)\n" +
                    "    has_dc=dc!='' and dc in d.columns\n" +
                    "    seen=dict()\n" +
                    "    for i in range(len(d)):\n" +
                    "        v=str(d[c].iloc[i]).strip() if d[c].iloc[i] is not None and str(d[c].iloc[i]).strip()!='' and str(d[c].iloc[i])!='nan' else ''\n" +
                    "        if not v or v in seen:\n" +
                    "            continue\n" +
                    "        dv=str(d[dc].iloc[i]).strip() if has_dc and d[dc].iloc[i] is not None and str(d[dc].iloc[i])!='nan' else ''\n" +
                    "        seen[v]=1\n" +
                    "        print(v+'\\t'+dv)\n" +
                    "except Exception as e:\n" +
                    "    sys.stderr.write(str(e)+'\\n')\n", col, dcol);

            ProcessBuilder pb = new ProcessBuilder(pythonExec, "-c", script, xptFile.getAbsolutePath());
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            Process proc = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split("\t", 2);
                    String term = parts[0].trim();
                    String decoded = parts.length > 1 ? parts[1].trim() : "";
                    if (!term.isEmpty()) {
                        pairs.add(new String[]{term, decoded});
                    }
                }
            }
            proc.waitFor(30, TimeUnit.SECONDS);
            if (proc.exitValue() != 0) {
                logger.warn("XPT read error for {}.{}", domain, variable);
            }
        } catch (Exception e) {
            logger.warn("XPT extraction failed for {}.{}: {}", domain, variable, e.getMessage());
        }
        return pairs;
    }

    private List<String> parseCodelistIds(String raw) {
        List<String> ids = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return ids;
        raw = raw.trim();
        // Handle "(ACN)", "(NCOMPLT), (PROTMLST)", "* ", "DM", "ISO 8601"
        if (raw.contains("(")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^)]+)\\)").matcher(raw);
            while (m.find()) {
                ids.add(m.group(1).trim());
            }
        }
        if (ids.isEmpty()) {
            String cleaned = raw.replace("*", "").trim();
            if (!cleaned.isEmpty()) {
                ids.add(cleaned);
            } else {
                ids.add(raw.trim());
            }
        }
        return ids;
    }

    private Long resolveCtPackageId(String standardType, String ctVersionStr) {
        if (ctVersionStr != null && !ctVersionStr.isEmpty()) {
            String dateStr = extractDateStr(ctVersionStr);
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id FROM ct_package WHERE standard_type = ? AND CAST(release_date AS CHAR) LIKE CONCAT(?, '%') LIMIT 1",
                        standardType, dateStr);
                if (!rows.isEmpty()) return ((Number) rows.get(0).get("id")).longValue();
            }
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM ct_package WHERE standard_type = ? AND language_code = 'EN' ORDER BY release_date DESC LIMIT 1",
                standardType);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("id")).longValue();
    }

    private String extractDateStr(String s) {
        if (s == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(s);
        return m.find() ? m.group(1) : "";
    }

    private Map<String, List<Map<String, String>>> loadEdcCodelist(String projectId, String username) {
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        try {
            List<Map<String, Object>> files = jdbcTemplate.queryForList(
                    "SELECT COALESCE(workspace_file_path, file_path) AS file_path FROM file_upload_records WHERE project_id = ? AND file_category = 'EDC_CODELIST' AND deleted = 0 ORDER BY upload_time DESC LIMIT 1",
                    projectId);
            if (files.isEmpty()) return result;
            String filePath = files.get(0).get("file_path").toString();
            java.io.File f = new java.io.File(filePath);
            if (!f.exists()) return result;

            try (Workbook wb = new XSSFWorkbook(new FileInputStream(f))) {
                Sheet sheet = wb.getSheet("CODELIST");
                if (sheet == null) {
                    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                        if (wb.getSheetName(i).toUpperCase().contains("CODELIST")) {
                            sheet = wb.getSheetAt(i);
                            break;
                        }
                    }
                }
                if (sheet == null) return result;

                // Find column indices from header row
                Row hdr = sheet.getRow(0);
                if (hdr == null) return result;
                int colCode = -1, colName = -1, colVal = -1, colLabel = -1, colSeq = -1;
                for (int c = 0; c <= hdr.getLastCellNum(); c++) {
                    Cell cell = hdr.getCell(c);
                    if (cell == null) continue;
                    String v = cell.getStringCellValue().trim();
                    if (v.contains("编码名")) colCode = c;
                    else if (v.contains("中文名称") || v.contains("名称")) colName = c;
                    else if (v.contains("编码值")) colVal = c;
                    else if (v.contains("编码标签") || v.contains("标签")) colLabel = c;
                    else if (v.contains("序列")) colSeq = c;
                }
                if (colCode < 0 || colVal < 0) return result;

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    String code = getEdcCellStr(row, colCode);
                    if (code.isEmpty()) continue;
                    String name = colName >= 0 ? getEdcCellStr(row, colName) : "";
                    String val = getEdcCellStr(row, colVal);
                    String label = colLabel >= 0 ? getEdcCellStr(row, colLabel) : "";
                    result.computeIfAbsent(code, k -> new ArrayList<>()).add(Map.of(
                            "code", val, "label", label, "name", name));
                }
            }
        } catch (Exception e) {
            logger.warn("加载EDC codelist失败: {}", e.getMessage());
        }
        return result;
    }

    private String getEdcCellStr(Row row, int col) {
        if (col < 0) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private List<Map<String, String>> findEdcTerms(Map<String, List<Map<String, String>>> edcMap,
                                                     String vcd, String domain, String variable) {
        if (edcMap.isEmpty()) return null;
        // Try direct match by vcd, domain+variable patterns
        for (Map.Entry<String, List<Map<String, String>>> e : edcMap.entrySet()) {
            String edcCode = e.getKey().toUpperCase();
            List<Map<String, String>> terms = e.getValue();
            if (terms.isEmpty()) continue;
            String edcName = terms.get(0).getOrDefault("name", "").toUpperCase();
            // Match by codelist name containing variable name or vcd
            if (edcCode.equalsIgnoreCase(vcd) || edcName.contains(vcd.toUpperCase()) ||
                edcName.contains(variable != null ? variable.toUpperCase() : "")) {
                return terms;
            }
        }
        return null;
    }

    

    private java.io.File findXptFile(java.io.File dir, String domain) {
        if (dir == null || !dir.isDirectory() || domain == null || domain.isEmpty()) return null;
        java.io.File f = new java.io.File(dir, domain.toLowerCase() + ".xpt");
        if (f.exists()) return f;
        f = new java.io.File(dir, domain.toUpperCase() + ".xpt");
        if (f.exists()) return f;
        java.io.File[] files = dir.listFiles();
        if (files == null) return null;
        for (java.io.File file : files) {
            if (file.getName().toLowerCase().endsWith("_" + domain.toLowerCase() + ".xpt")) return file;
        }
        return null;
    }

    @PostMapping("/extract-methods-comments/{projectId}")
    public CommonResult<Map<String, Object>> extractMethodsComments(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);
            if (specList == null || specList.isEmpty()) {
                return CommonResult.failed("未找到 Variables 数据");
            }

            methodsDataMapper.deleteByProjectId(projectId, username);
            commentsDataMapper.deleteByProjectId(projectId, username);

            // Restore previously assigned IDs (MTxxx / CTxxx) back to "Y"
            // so that re-extraction can pick them up again
            for (ProjectSpecPO spec : specList) {
                boolean needUpdate = false;
                String m = spec.getMethod() != null ? spec.getMethod().trim() : "";
                String c = spec.getComment() != null ? spec.getComment().trim() : "";
                if (m.matches("(?i)MT\\d+")) {
                    spec.setMethod("Y");
                    needUpdate = true;
                }
                if (c.matches("(?i)CT\\d+")) {
                    spec.setComment("Y");
                    needUpdate = true;
                }
                if (needUpdate) {
                    spec.setUpdatedTime(new java.util.Date());
                    spec.setUpdatedBy("extract_methods_comments");
                    projectSpecMapper.updateById(spec);
                }
            }

            // Collect all method/comment entries, grouped by description for dedup
            // description → list of (domain.variable)
            Map<String, List<String>> methodsByDesc = new LinkedHashMap<>();
            Map<String, List<String>> commentsByDesc = new LinkedHashMap<>();
            // Track all variable names per description for common suffix extraction
            Map<String, List<String>> methodVarNames = new LinkedHashMap<>();

            for (ProjectSpecPO spec : specList) {
                String domain = spec.getDomain() != null ? spec.getDomain().trim() : "";
                String variable = spec.getVariable() != null ? spec.getVariable().trim() : "";
                String textContent = spec.getTextContent() != null ? spec.getTextContent().trim() : "";
                String commentFlag = spec.getComment() != null ? spec.getComment().trim() : "";
                String methodFlag = spec.getMethod() != null ? spec.getMethod().trim() : "";

                if (domain.isEmpty() || variable.isEmpty()) continue;
                String idValue = domain + "." + variable;

                if ("Y".equalsIgnoreCase(methodFlag) && !textContent.isEmpty()) {
                    methodsByDesc.computeIfAbsent(textContent, k -> new ArrayList<>()).add(idValue);
                    methodVarNames.computeIfAbsent(textContent, k -> new ArrayList<>()).add(variable);
                }

                if ("Y".equalsIgnoreCase(commentFlag) && !textContent.isEmpty()) {
                    commentsByDesc.computeIfAbsent(textContent, k -> new ArrayList<>()).add(idValue);
                }
            }

            int methodCount = 0;
            int commentCount = 0;
            int sortIdx = 0;

            // Insert deduplicated Methods with sequential ID: MT001, MT002, ...
            Map<String, String> methodDescToId = new LinkedHashMap<>();
            int mtSeq = 1;
            for (Map.Entry<String, List<String>> e : methodsByDesc.entrySet()) {
                String desc = e.getKey();
                String mtId = String.format("MT%03d", mtSeq++);
                List<String> vars = methodVarNames.get(desc);
                String deriveName = findCommonSuffix(vars);

                MethodsDataPO mpo = new MethodsDataPO();
                mpo.setProjectId(projectId);
                mpo.setUsername(username);
                mpo.setMethodId(mtId);
                mpo.setName("Algorithm to derive " + deriveName);
                mpo.setType("Computation");
                mpo.setDescription(desc);
                mpo.setSortOrder(sortIdx++);
                mpo.setCreatedBy("extract_methods_comments");
                methodsDataMapper.insert(mpo);
                methodCount++;
                methodDescToId.put(desc, mtId);
            }

            // Insert deduplicated Comments with sequential ID: CT001, CT002, ...
            Map<String, String> commentDescToId = new LinkedHashMap<>();
            int ctSeq = 1;
            for (Map.Entry<String, List<String>> e : commentsByDesc.entrySet()) {
                String desc = e.getKey();
                String ctId = String.format("CT%03d", ctSeq++);

                CommentsDataPO cpo = new CommentsDataPO();
                cpo.setProjectId(projectId);
                cpo.setUsername(username);
                cpo.setCommentId(ctId);
                cpo.setDescription(desc);
                cpo.setSortOrder(sortIdx++);
                cpo.setCreatedBy("extract_methods_comments");
                commentsDataMapper.insert(cpo);
                commentCount++;
                commentDescToId.put(desc, ctId);
            }

            // Update spec rows: all variables sharing the same description point to same ID
            for (ProjectSpecPO spec : specList) {
                String domain = spec.getDomain() != null ? spec.getDomain().trim() : "";
                String variable = spec.getVariable() != null ? spec.getVariable().trim() : "";
                String textContent = spec.getTextContent() != null ? spec.getTextContent().trim() : "";
                String commentFlag = spec.getComment() != null ? spec.getComment().trim() : "";
                String methodFlag = spec.getMethod() != null ? spec.getMethod().trim() : "";
                if (domain.isEmpty() || variable.isEmpty()) continue;

                boolean updated = false;
                if ("Y".equalsIgnoreCase(methodFlag) && !textContent.isEmpty()) {
                    String mid = methodDescToId.get(textContent);
                    if (mid != null) {
                        spec.setMethod(mid);
                        updated = true;
                    }
                }
                if ("Y".equalsIgnoreCase(commentFlag) && !textContent.isEmpty()) {
                    String cid = commentDescToId.get(textContent);
                    if (cid != null) {
                        spec.setComment(cid);
                        updated = true;
                    }
                }
                if (updated) {
                    spec.setUpdatedTime(new java.util.Date());
                    spec.setUpdatedBy("extract_methods_comments");
                    projectSpecMapper.updateById(spec);
                }
            }

            logger.info("Methods/Comments提取完成: projectId={}, methods={}, comments={}",
                    projectId, methodCount, commentCount);
            triggerSpecSync(projectId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("methods", methodCount);
            result.put("comments", commentCount);
            result.put("message", String.format("提取完成：生成 %d 条 Methods（去重），%d 条 Comments（去重）", methodCount, commentCount));
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("提取Methods/Comments失败", e);
            return CommonResult.failed("提取失败: " + e.getMessage());
        }
    }

    private void triggerSpecSync(String projectId) {
        try {
            String outputDir = pathResolver.projectSpecDirectory(
                    projectId, pathResolver.resolveStandardType(projectId, null)).toString();
            projectSpecService.syncSpecToFile(projectId, outputDir);
        } catch (Exception e) {
            logger.warn("Spec同步文件生成失败(不影响主流程): {}", e.getMessage());
        }
    }

    /**
     * 从一组变量名中提取公共后缀。
     * 例如 [AESEQ, DSSEQ, LBSEQ] → SEQ; [AESTDY] → AESTDY
     */
    private String findCommonSuffix(List<String> names) {
        if (names == null || names.isEmpty()) return "";
        if (names.size() == 1) return names.get(0);

        String first = names.get(0);
        int suffixLen = first.length();
        for (int i = 1; i < names.size(); i++) {
            String s = names.get(i);
            int minLen = Math.min(suffixLen, s.length());
            int matched = 0;
            for (int j = 0; j < minLen; j++) {
                if (first.charAt(first.length() - 1 - j) == s.charAt(s.length() - 1 - j)) {
                    matched++;
                } else {
                    break;
                }
            }
            suffixLen = matched;
            if (suffixLen == 0) break;
        }

        if (suffixLen > 0) {
            return first.substring(first.length() - suffixLen);
        }
        return names.get(0);
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    private String str(Object o) { return o != null ? o.toString() : null; }

    private Integer toInt(Object o) {
        if (o == null) return null;
        try { return Integer.valueOf(o.toString()); } catch (NumberFormatException e) { return null; }
    }
}