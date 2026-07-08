package com.stat.web.controller;

import com.stat.service.*;
import com.stat.common.result.CommonResult;
import com.stat.common.dto.*;
import com.stat.dal.po.DefineSheetDataPO;
import com.stat.dal.mapper.DefineSheetDataMapper;
import com.stat.dal.mapper.CodelistDataMapper;
import com.stat.dal.mapper.ProjectSpecMapper;
import com.stat.dal.mapper.DatasetsDataMapper;
import com.stat.dal.mapper.ProjectConfigMapper;
import com.stat.dal.po.CodelistDataPO;
import com.stat.dal.po.ProjectSpecPO;
import com.stat.dal.po.DatasetsDataPO;
import com.stat.dal.po.ProjectConfigPO;
import com.stat.common.entity.Project;
import com.stat.common.security.UserContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Optional;

@RestController
@RequestMapping("/project")
public class ProjectController {

    private final IProjectService projectService;
    private final ISasAcrfUploadService sasAcrfUploadService;
    private final DefineSheetDataMapper defineSheetDataMapper;
    private final IVlmDataService vlmDataService;
    private final IPagesDataService pagesDataService;
    private final CodelistDataMapper codelistDataMapper;
    private final ProjectSpecMapper projectSpecMapper;
    private final DatasetsDataMapper datasetsDataMapper;
    private final ProjectConfigMapper projectConfigMapper;
    private final IMethodsDataService methodsDataService;
    private final ICommentsDataService commentsDataService;
    private final IDictionariesDataService dictionariesDataService;
    private final IDocumentsDataService documentsDataService;
    private final ICodelistDataService codelistDataService;
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    
    @Value("${app.upload.path:E:/JAVAPROJ/008_defineXML/uploads}")
    private String uploadPath;
    
    @Value("${app.python.path:E:/JAVAPROJ/008_defineXML/Python}")
    private String pythonPath;

    public ProjectController(IProjectService projectService, ISasAcrfUploadService sasAcrfUploadService,
                             DefineSheetDataMapper defineSheetDataMapper,
                             IVlmDataService vlmDataService,
                             IPagesDataService pagesDataService,
                             CodelistDataMapper codelistDataMapper,
                             ProjectSpecMapper projectSpecMapper,
                             DatasetsDataMapper datasetsDataMapper,
                             ProjectConfigMapper projectConfigMapper,
                             IMethodsDataService methodsDataService,
                             ICommentsDataService commentsDataService,
                             IDictionariesDataService dictionariesDataService,
                             IDocumentsDataService documentsDataService,
                             ICodelistDataService codelistDataService) {
        this.projectService = projectService;
        this.sasAcrfUploadService = sasAcrfUploadService;
        this.defineSheetDataMapper = defineSheetDataMapper;
        this.vlmDataService = vlmDataService;
        this.pagesDataService = pagesDataService;
        this.codelistDataMapper = codelistDataMapper;
        this.projectSpecMapper = projectSpecMapper;
        this.datasetsDataMapper = datasetsDataMapper;
        this.projectConfigMapper = projectConfigMapper;
        this.methodsDataService = methodsDataService;
        this.commentsDataService = commentsDataService;
        this.dictionariesDataService = dictionariesDataService;
        this.documentsDataService = documentsDataService;
        this.codelistDataService = codelistDataService;
    }

    @DeleteMapping("/deleteProject")
    public CommonResult deleteProject(@RequestBody Map<String, Object> params) {
        logger.info("Received parameters: {}", params);
        String projectId = (String) params.get("projectId");
        Boolean deleteFiles = params.get("deleteFiles") != null ? (Boolean) params.get("deleteFiles") : false;
        
        logger.info("删除项目: {}, 是否删除文件夹: {}", projectId, deleteFiles);
        
        boolean success = projectService.deleteProject(projectId, deleteFiles);
        if (success) {
            String message = deleteFiles ? "项目及文件夹删除成功" : "项目删除成功";
            return CommonResult.success(message);
        } else {
            return CommonResult.fail("500", "删除项目失败");
        }
    }

    /**
     * Spec文件上传接口 - 支持P21空Spec和项目Spec上传
     */
    @PostMapping("/uploadSpec")
    public CommonResult<Map<String, Object>> uploadSpec(@RequestParam("file") MultipartFile file) {
        logger.info("接收Spec文件上传请求，文件名: {}, 文件大小: {}",
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

            if (!(originalFilename.toLowerCase().endsWith(".xlsx") || originalFilename.toLowerCase().endsWith(".xls"))) {
                logger.warn("不支持的文件类型: {}", originalFilename);
                return CommonResult.fail("400", "只支持Excel格式文件(.xlsx/.xls)");
            }

            // 创建规格文件上传目录
            Path specUploadDir = Paths.get(uploadPath, "specs");
            if (!Files.exists(specUploadDir)) {
                Files.createDirectories(specUploadDir);
                logger.info("创建Spec上传目录: {}", specUploadDir);
            }

            // 生成唯一文件名和文件ID
            String fileId = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String serverFileName = fileId + "_" + originalFilename;
            Path filePath = specUploadDir.resolve(serverFileName);

            // 保存文件
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Spec文件保存成功: {}", filePath);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文件上传成功");

            // 返回数据结构，与StatisticalAnalysis.vue期望的格式匹配
            Map<String, Object> data = new HashMap<>();
            data.put("fileId", fileId);
            data.put("originalName", originalFilename);
            data.put("serverFileName", serverFileName);
            data.put("filePath", filePath.toString());
            data.put("fileSize", file.getSize());
            data.put("uploadTime", System.currentTimeMillis());

            result.put("data", data);

            return CommonResult.success(result);

        } catch (Exception e) {
            logger.error("Spec文件上传失败", e);
            return CommonResult.fail("500", "文件上传失败: " + e.getMessage());
        }
    }
    
    // 临时添加文件上传功能
    @PostMapping("/uploadXpt")
    public CommonResult<Map<String, Object>> uploadXptFile(@RequestParam("file") MultipartFile file) {
        logger.info("接收XPT文件上传请求，文件名: {}, 文件大小: {}", 
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
     * Generate Define.xlsx by merging all sheets from DB.
     * Sheets: Study, Datasets, Variables, ValueLevel, Codelists, Dictionaries, Methods, Comments, Documents
     */
    @GetMapping("/generateDefineXlsx/{projectId}")
    public ResponseEntity<byte[]> generateDefineXlsx(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            XSSFWorkbook workbook = new XSSFWorkbook();

            // ── Styling: orange header (matching the standard Define.xlsx look) ──
            CellStyle headerStyle = workbook.createCellStyle();
            Font hf = workbook.createFont();
            hf.setBold(true);
            hf.setColor(IndexedColors.WHITE.getIndex());
            hf.setFontName("Times New Roman");
            hf.setFontHeightInPoints((short) 11);
            headerStyle.setFont(hf);
            headerStyle.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new byte[]{(byte) 0xE2, (byte) 0x8C, (byte) 0x00}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle bodyStyle = workbook.createCellStyle();
            Font bf = workbook.createFont();
            bf.setFontName("Times New Roman");
            bf.setFontHeightInPoints((short) 10);
            bodyStyle.setFont(bf);
            bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            bodyStyle.setBorderTop(BorderStyle.THIN);
            bodyStyle.setBorderBottom(BorderStyle.THIN);
            bodyStyle.setBorderLeft(BorderStyle.THIN);
            bodyStyle.setBorderRight(BorderStyle.THIN);

            // ── 1. define ──
            Project project = projectService.getProject(projectId);
            ProjectConfigPO config = projectConfigMapper.selectByProjectId(projectId);
            Sheet studySheet = workbook.createSheet("define");
            addRow(studySheet, 0, headerStyle, "Attribute", "Value");
            String stdVer = config != null && config.getStandardVersion() != null ? config.getStandardVersion() : "";
            String stdType = config != null && config.getStandardType() != null ? config.getStandardType() : "SDTM";
            addRow(studySheet, 1, bodyStyle, "StudyName", project != null ? project.getProjectName() : "");
            addRow(studySheet, 2, bodyStyle, "StudyDescription", project != null ? project.getProtocolName() : "");
            addRow(studySheet, 3, bodyStyle, "ProtocolName", project != null ? project.getProtocolNumber() : "");
            addRow(studySheet, 4, bodyStyle, "StandardName", stdType);
            addRow(studySheet, 5, bodyStyle, "StandardVersion", stdVer);

            // ── 2. Datasets ──
            Sheet dsSheet = workbook.createSheet("Datasets");
            addRow(dsSheet, 0, headerStyle, "Dataset", "Description", "Class", "Structure", "Purpose", "Key Variables", "Standard", "Has No Data", "Repeating", "Reference Data", "Comment");
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DatasetsDataPO> dsQuery =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DatasetsDataPO>()
                            .eq(DatasetsDataPO::getProjectId, projectId)
                            .eq(DatasetsDataPO::getUsername, username)
                            .orderByAsc(DatasetsDataPO::getSortOrder);
            List<DatasetsDataPO> dsList = datasetsDataMapper.selectList(dsQuery);
            if (dsList != null) {
                int r = 1;
                for (DatasetsDataPO ds : dsList) {
                    addRow(dsSheet, r++, bodyStyle,
                            ds.getDataset(), ds.getLabel(), ds.getDataClass(),
                            ds.getStructure(), "Tabulation", ds.getKeyVariables(),
                            ds.getStandard(), ds.getHasNoData(), ds.getRepeating(),
                            ds.getReferenceData(), ds.getComment());
                }
            }

            // ── 3. Variables ──
            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);
            Sheet varSheet = workbook.createSheet("Variables");
            addRow(varSheet, 0, headerStyle, "Order", "Dataset", "Variable", "Label", "Data Type", "Length",
                    "Significant Digits", "Format", "Mandatory", "Assigned Value", "Codelist", "Common",
                    "Origin", "Source", "Pages", "Method", "Predecessor", "Role",
                    "Has No Data", "Comment", "Developer Notes");
            if (specList != null) {
                int r = 1;
                for (ProjectSpecPO s : specList) {
                    addRow(varSheet, r++, bodyStyle,
                            s.getSortOrder(), s.getDomain(), s.getVariable(), s.getLabel(), s.getType(),
                            s.getLength(), s.getSignificantDigits(), s.getFormat(),
                            s.getMandatory(), s.getAssignedValue(), s.getCodelist(), s.getCommon(),
                            s.getOrigin(), s.getSource(), s.getPages(), s.getMethod(),
                            s.getPredecessor(), s.getRole(), s.getHasNoData(),
                            s.getComment(), s.getDeveloperNotes());
                }
            }

            // ── 4. ValueLevel ──
            List<VlmDataDTO> vlmList = vlmDataService.getVlmDataByProjectId(projectId);
            Sheet vlmSheet = workbook.createSheet("ValueLevel");
            addRow(vlmSheet, 0, headerStyle, "Order", "Dataset", "Variable", "Where Clause", "Label",
                    "Data Type", "Length", "Significant Digits", "Format", "Mandatory",
                    "Assigned Value", "Codelist", "Origin", "Source", "Pages", "Method",
                    "Predecessor", "Comment", "Developer Notes");
            if (vlmList != null) {
                int r = 1;
                for (VlmDataDTO v : vlmList) {
                    addRow(vlmSheet, r++, bodyStyle,
                            v.getSortOrder(), v.getDataset(), v.getVariable(), v.getWhereClause(),
                            v.getLabel(), v.getDataType(), v.getLength(), v.getSignificantDigits(),
                            v.getFormat(), v.getMandatory(), v.getAssignedValue(), v.getCodelist(),
                            v.getOrigin(), v.getSource(), v.getPages(), v.getMethod(),
                            v.getPredecessor(), v.getComment(), v.getDeveloperNotes());
                }
            }

            // ── 5. Codelists ──
            List<CodelistDataDTO> clList = codelistDataService.getCodelistDataByProjectId(projectId);
            Sheet clSheet = workbook.createSheet("Codelists");
            addRow(clSheet, 0, headerStyle, "ID", "Name", "NCI Codelist Code", "Data Type",
                    "Terminology", "Comment", "Order", "Term", "NCI Term Code", "Decoded Value");
            if (clList != null) {
                int r = 1;
                for (CodelistDataDTO c : clList) {
                    addRow(clSheet, r++, bodyStyle,
                            c.getVcd(), c.getVlabel(), c.getNciCodelistCode(), c.getType(),
                            c.getTerminology(), c.getComment(), c.getCdnum(), c.getCode(),
                            c.getNciTermCode(), c.getCodeDes());
                }
            }

            // ── 6. Dictionaries ──
            List<DictionariesDataDTO> dictList = dictionariesDataService.getDictionariesDataByProjectId(projectId);
            Sheet dictSheet = workbook.createSheet("Dictionaries");
            addRow(dictSheet, 0, headerStyle, "ID", "Name", "Data Type", "Dictionary", "Version");
            if (dictList != null) {
                int r = 1;
                for (DictionariesDataDTO d : dictList) {
                    addRow(dictSheet, r++, bodyStyle,
                            d.getDictionaryId(), d.getName(), d.getDataType(),
                            d.getDictionary(), d.getVersion());
                }
            }

            // ── 7. Methods ──
            List<MethodsDataDTO> methList = methodsDataService.getMethodsDataByProjectId(projectId);
            Sheet methSheet = workbook.createSheet("Methods");
            addRow(methSheet, 0, headerStyle, "ID", "Name", "Type", "Description",
                    "Expression Context", "Expression Code", "Document", "Pages");
            if (methList != null) {
                int r = 1;
                for (MethodsDataDTO m : methList) {
                    addRow(methSheet, r++, bodyStyle,
                            m.getMethodId(), m.getName(), m.getType(), m.getDescription(),
                            m.getExpressionContext(), m.getExpressionCode(), m.getDocument(), m.getPages());
                }
            }

            // ── 8. Comments ──
            List<CommentsDataDTO> cmtList = commentsDataService.getCommentsDataByProjectId(projectId);
            Sheet cmtSheet = workbook.createSheet("Comments");
            addRow(cmtSheet, 0, headerStyle, "ID", "Description", "Document", "Pages");
            if (cmtList != null) {
                int r = 1;
                for (CommentsDataDTO c : cmtList) {
                    addRow(cmtSheet, r++, bodyStyle,
                            c.getCommentId(), c.getDescription(), c.getDocument(), c.getPages());
                }
            }

            // ── 9. Documents ──
            List<DocumentsDataDTO> docList = documentsDataService.getDocumentsDataByProjectId(projectId);
            Sheet docSheet = workbook.createSheet("Documents");
            addRow(docSheet, 0, headerStyle, "ID", "Title", "Href");
            if (docList != null) {
                int r = 1;
                for (DocumentsDataDTO d : docList) {
                    addRow(docSheet, r++, bodyStyle, d.getDocumentId(), d.getTitle(), d.getHref());
                }
            }

            // Auto-size columns and freeze header row for all sheets
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sh = workbook.getSheetAt(i);
                Row hr = sh.getRow(0);
                if (hr != null) {
                    for (int c = 0; c < hr.getLastCellNum(); c++) {
                        sh.autoSizeColumn(c);
                        int w = sh.getColumnWidth(c);
                        sh.setColumnWidth(c, Math.min(w + 512, 15000));
                    }
                }
                sh.createFreezePane(0, 1);
                sh.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                        0, 0, 0, hr != null ? hr.getLastCellNum() - 1 : 0));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();
            byte[] bytes = out.toByteArray();

            // Save to output directory for online editor to access
            Path outDir = Paths.get(uploadPath, projectId, "output");
            Files.createDirectories(outDir);
            Path outFile = outDir.resolve("Define_" + projectId + ".xlsx");
            Files.write(outFile, bytes);

            String fileName = java.net.URLEncoder.encode("Define_" + projectId + ".xlsx", "UTF-8");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            logger.error("生成Define.xlsx失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void addRow(Sheet sheet, int rowIdx, CellStyle style, Object... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            if (values[i] == null) {
                cell.setCellValue("");
            } else if (values[i] instanceof Number) {
                cell.setCellValue(((Number) values[i]).doubleValue());
            } else {
                cell.setCellValue(values[i].toString());
            }
            if (style != null) cell.setCellStyle(style);
        }
    }

    /**
     * 处理Define.xlsx生成
     */
    @PostMapping("/processDefine")
    public CommonResult<Map<String, Object>> processDefine(@RequestBody Map<String, Object> request) {
        logger.info("接收Define处理请求: {}", request);

        try {
            String p21SpecId = (String) request.get("p21SpecId");
            String projectSpecId = (String) request.get("projectSpecId");
            @SuppressWarnings("unchecked")
            Map<String, Object> projectConfig = (Map<String, Object>) request.get("projectConfig");
            if (projectConfig == null) projectConfig = new HashMap<>();
            if (request.containsKey("projectId") && !projectConfig.containsKey("projectId")) {
                projectConfig.put("projectId", request.get("projectId"));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> dictionaryConfig = (Map<String, Object>) request.get("dictionaryConfig");

            if (p21SpecId == null || p21SpecId.trim().isEmpty()) {
                return CommonResult.fail("400", "请选择P21空Spec");
            }

            if (projectSpecId == null || projectSpecId.trim().isEmpty()) {
                return CommonResult.fail("400", "请选择项目Spec");
            }

            logger.info("处理Define.xlsx生成请求 - P21 Spec ID: {}, 项目Spec ID: {}", p21SpecId, projectSpecId);

            // 调用Python脚本处理Define.xlsx生成
            Map<String, Object> result = callDefineGenerationPythonScript(p21SpecId, projectSpecId, projectConfig, dictionaryConfig);

            return CommonResult.success(result);

        } catch (Exception e) {
            logger.error("Define处理失败", e);
            return CommonResult.fail("500", "处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 调用Python脚本生成Define.xlsx
     */
    private Map<String, Object> callDefineGenerationPythonScript(String p21SpecId, String projectSpecId,
                                                               Map<String, Object> projectConfig,
                                                               Map<String, Object> dictionaryConfig) throws Exception {
        logger.info("开始调用Python脚本生成Define.xlsx，P21 Spec ID: {}, 项目Spec ID: {}", p21SpecId, projectSpecId);

        String projectId = projectConfig != null ? (String) projectConfig.get("projectId") : null;

        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(pythonPath, "run_p21report_api.py").toString());

        // Export VLM/Codelist/Pages from DB as JSON temp files if projectId is available
        if (projectId != null && !projectId.trim().isEmpty()) {
            try {
                Path tmpDir = Paths.get(uploadPath, projectId, "output");
                Files.createDirectories(tmpDir);

                // VLM
                List<VlmDataDTO> vlmList = vlmDataService.getVlmDataByProjectId(projectId);
                if (vlmList != null && !vlmList.isEmpty()) {
                    List<Map<String, String>> vlmJson = new ArrayList<>();
                    for (VlmDataDTO v : vlmList) {
                        Map<String, String> m = new HashMap<>();
                        m.put("Dataset", v.getDataset() != null ? v.getDataset() : "");
                        m.put("Variable", v.getVariable() != null ? v.getVariable() : "");
                        m.put("Where Clause", v.getWhereClause() != null ? v.getWhereClause() : "");
                        m.put("Label", v.getLabel() != null ? v.getLabel() : "");
                        m.put("Controlled Terms or Format", v.getControlledTermsOrFormat() != null ? v.getControlledTermsOrFormat() : "");
                        m.put("Origin", v.getOrigin() != null ? v.getOrigin() : "");
                        m.put("Pages", v.getPages() != null ? v.getPages() : "");
                        m.put("Derivation/Comment", v.getDerivationComment() != null ? v.getDerivationComment() : "");
                        m.put("Method", v.getMethod() != null ? v.getMethod() : "");
                        m.put("Comment", v.getComment() != null ? v.getComment() : "");
                        vlmJson.add(m);
                    }
                    Path vlmFile = tmpDir.resolve("vlm_data.json");
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    om.writeValue(vlmFile.toFile(), vlmJson);
                    command.add("--vlm-json");
                    command.add(vlmFile.toString());
                    logger.info("导出VLM JSON: {} 条记录", vlmList.size());
                }

                // Codelist
                List<CodelistDataPO> clList = codelistDataMapper.selectByProjectId(projectId, UserContext.getUsername());
                if (clList != null && !clList.isEmpty()) {
                    List<Map<String, String>> clJson = new ArrayList<>();
                    for (CodelistDataPO c : clList) {
                        Map<String, String> m = new HashMap<>();
                        m.put("vcd", c.getVcd() != null ? c.getVcd() : "");
                        m.put("vlabel", c.getVlabel() != null ? c.getVlabel() : "");
                        m.put("type", c.getType() != null ? c.getType() : "");
                        m.put("cdnum", c.getCdnum() != null ? String.valueOf(c.getCdnum()) : "");
                        m.put("code", c.getCode() != null ? c.getCode() : "");
                        m.put("codeDes", c.getCodeDes() != null ? c.getCodeDes() : "");
                        m.put("codever", c.getCodeVer() != null ? c.getCodeVer() : "");
                        clJson.add(m);
                    }
                    Path clFile = tmpDir.resolve("codelist_data.json");
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    om.writeValue(clFile.toFile(), clJson);
                    command.add("--codelist-json");
                    command.add(clFile.toString());
                    logger.info("导出Codelist JSON: {} 条记录", clList.size());
                }

                // Pages
                List<PagesDataDTO> pagesList = pagesDataService.getPagesDataByProjectId(projectId);
                if (pagesList != null && !pagesList.isEmpty()) {
                    List<Map<String, String>> pagesJson = new ArrayList<>();
                    for (PagesDataDTO p : pagesList) {
                        Map<String, String> m = new HashMap<>();
                        m.put("dataset", p.getDataset() != null ? p.getDataset() : "");
                        m.put("variable", p.getVariable() != null ? p.getVariable() : "");
                        m.put("pages", p.getPages() != null ? p.getPages() : "");
                        m.put("origin", p.getOrigin() != null ? p.getOrigin() : "");
                        pagesJson.add(m);
                    }
                    Path pagesFile = tmpDir.resolve("pages_data.json");
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    om.writeValue(pagesFile.toFile(), pagesJson);
                    command.add("--pages-json");
                    command.add(pagesFile.toString());
                    logger.info("导出Pages JSON: {} 条记录", pagesList.size());
                }
            } catch (Exception e) {
                logger.warn("导出DB数据为JSON失败，将使用Spec文件中的数据: {}", e.getMessage());
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonPath));
        processBuilder.redirectErrorStream(true);

        logger.info("执行Python命令: {}", String.join(" ", command));
        logger.info("工作目录: {}", pythonPath);

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

            // 等待进程结束
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python脚本执行超时");
            }

            int exitCode = process.exitValue();
            logger.info("Python脚本执行完成，退出码: {}", exitCode);

            Map<String, Object> result = new HashMap<>();

            // 检查脚本是否成功执行
            if (exitCode == 0) {
                // 查找生成的Define文件
                String outputFileName = findGeneratedDefineFile();
                if (outputFileName != null) {
                    result.put("success", true);
                    result.put("message", "Define.xlsx生成成功");
                    result.put("output_file", outputFileName);
                    result.put("output_dir", Paths.get(pythonPath, "output").toString());
                    result.put("processTime", System.currentTimeMillis());
                } else {
                    result.put("success", false);
                    result.put("error", "Python脚本执行成功但未找到生成的Define文件");
                }
            } else {
                result.put("success", false);
                result.put("error", "Python脚本执行失败，退出码: " + exitCode + "\n输出: " + output.toString());
            }

            // 尝试解析JSON结果（如果有的话）
            if (jsonResult.length() > 0) {
                try {
                    String jsonStr = jsonResult.toString().trim();
                    logger.info("解析Python返回的JSON: {}", jsonStr);

                    // 简单的JSON解析
                    if (jsonStr.contains("\"success\": true")) {
                        result.put("success", true);
                        result.put("message", extractJsonValue(jsonStr, "message"));
                        result.put("output_file", extractJsonValue(jsonStr, "output_file"));
                        result.put("output_dir", extractJsonValue(jsonStr, "output_dir"));
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
                // 没有JSON结果，使用传统方式
                String outputFileName = findGeneratedDefineFile();
                if (exitCode == 0 && outputFileName != null) {
                    result.put("success", true);
                    result.put("message", "Define.xlsx生成成功");
                    result.put("output_file", outputFileName);
                    result.put("output_dir", Paths.get(pythonPath, "output").toString());
                    result.put("processTime", System.currentTimeMillis());
                } else {
                    result.put("success", false);
                    result.put("error", "Python脚本执行失败，退出码: " + exitCode);
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("执行Python脚本时发生错误", e);
            throw e;
        }
    }

    /**
     * 根据文件ID查找P21空Spec文件路径
     */
    private String findP21SpecFilePath(String fileId) {
        try {
            // 首先检查是否为模拟数据ID，如果是则返回对应的默认文件路径
            String mockFilePath = getMockP21SpecFilePath(fileId);
            if (mockFilePath != null) {
                logger.info("使用模拟P21空Spec文件: {}", mockFilePath);
                return mockFilePath;
            }

            Path specUploadDir = Paths.get(uploadPath, "specs");
            if (!Files.exists(specUploadDir)) {
                logger.warn("Spec上传目录不存在: {}", specUploadDir);
                return null;
            }

            // 查找以fileId开头的文件
            Optional<Path> foundFile = Files.list(specUploadDir)
                .filter(path -> path.getFileName().toString().startsWith(fileId + "_"))
                .findFirst();

            if (foundFile.isPresent()) {
                String filePath = foundFile.get().toString();
                logger.info("找到P21空Spec文件: {}", filePath);
                return filePath;
            } else {
                logger.warn("未找到P21空Spec文件，文件ID: {}", fileId);
                return null;
            }
        } catch (IOException e) {
            logger.error("查找P21空Spec文件时发生错误", e);
            return null;
        }
    }

    /**
     * 根据文件ID查找项目Spec文件路径
     */
    private String findProjectSpecFilePath(String fileId) {
        try {
            // 首先检查是否为模拟数据ID，如果是则返回对应的默认文件路径
            String mockFilePath = getMockProjectSpecFilePath(fileId);
            if (mockFilePath != null) {
                logger.info("使用模拟项目Spec文件: {}", mockFilePath);
                return mockFilePath;
            }

            Path specUploadDir = Paths.get(uploadPath, "specs");
            if (!Files.exists(specUploadDir)) {
                logger.warn("Spec上传目录不存在: {}", specUploadDir);
                return null;
            }

            // 查找以fileId开头的文件
            Optional<Path> foundFile = Files.list(specUploadDir)
                .filter(path -> path.getFileName().toString().startsWith(fileId + "_"))
                .findFirst();

            if (foundFile.isPresent()) {
                String filePath = foundFile.get().toString();
                logger.info("找到项目Spec文件: {}", filePath);
                return filePath;
            } else {
                logger.warn("未找到项目Spec文件，文件ID: {}", fileId);
                return null;
            }
        } catch (IOException e) {
            logger.error("查找项目Spec文件时发生错误", e);
            return null;
        }
    }

    /**
     * 获取模拟P21空Spec文件路径
     */
    private String getMockP21SpecFilePath(String fileId) {
        // P21空Spec文件路径映射 - 使用相对路径
        String mockPath = "./define/p21空spec/define.xlsx";
        Path fullPath = Paths.get(pythonPath, mockPath);
        if (Files.exists(fullPath)) {
            return mockPath;
        }
        return null;
    }

    /**
     * 获取模拟项目Spec文件路径
     */
    private String getMockProjectSpecFilePath(String fileId) {
        // 项目Spec文件路径映射 - 使用相对路径
        String mockPath = "./define/项目Spec/spec.xlsx";
        Path fullPath = Paths.get(pythonPath, mockPath);
        if (Files.exists(fullPath)) {
            return mockPath;
        }
        return null;
    }

    /**
     * 查找生成的Define.xlsx文件
     */
    private String findGeneratedDefineFile() {
        try {
            Path outputDir = Paths.get(pythonPath, "output");
            if (!Files.exists(outputDir)) {
                logger.warn("输出目录不存在: {}", outputDir);
                return null;
            }

            // 查找最新的Define.xlsx文件
            Optional<Path> latestFile = Files.list(outputDir)
                .filter(path -> path.toString().toLowerCase().endsWith(".xlsx"))
                .filter(path -> {
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.contains("define") || fileName.startsWith("sdtm_define_");
                })
                .max(Comparator.comparing(path -> {
                    try {
                        return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                        return null;
                    }
                }));

            if (latestFile.isPresent()) {
                String fileName = latestFile.get().getFileName().toString();
                logger.info("找到生成的Define文件: {}", fileName);
                return fileName;
            } else {
                logger.warn("未找到生成的Define文件");
                return null;
            }
        } catch (IOException e) {
            logger.error("查找生成Define文件时发生错误", e);
            return null;
        }
    }

    /**
     * 调用Python脚本进行Define处理（原有方法保留）
     */
    private Map<String, Object> callPythonScript(List<String> fileNames) throws Exception {
        logger.info("开始调用Python脚本处理Define，文件列表: {}", fileNames);
        
        // 构建Python命令 - 先使用测试脚本验证
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(pythonPath, "test_p21_simple.py").toString());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonPath));
        processBuilder.redirectErrorStream(true);
        
        logger.info("执行Python命令: {}", String.join(" ", command));
        logger.info("工作目录: {}", pythonPath);
        
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
            
            // 等待进程结束
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python脚本执行超时");
            }
            
            int exitCode = process.exitValue();
            logger.info("Python脚本执行完成，退出码: {}", exitCode);
            
            Map<String, Object> result = new HashMap<>();
            
            // 尝试解析JSON结果
            if (jsonResult.length() > 0) {
                try {
                    String jsonStr = jsonResult.toString().trim();
                    logger.info("解析Python返回的JSON: {}", jsonStr);
                    
                    // 简单的JSON解析 (生产环境建议使用Jackson)
                    if (jsonStr.contains("\"success\": true")) {
                        result.put("success", true);
                        result.put("message", extractJsonValue(jsonStr, "message"));
                        result.put("output_file", extractJsonValue(jsonStr, "output_file"));
                        result.put("output_dir", extractJsonValue(jsonStr, "output_dir"));
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
                // 没有JSON结果，使用传统方式
                String outputFileName = findGeneratedFile();
                if (exitCode == 0 && outputFileName != null) {
                    result.put("success", true);
                    result.put("message", "Define.xlsx生成成功");
                    result.put("output_file", outputFileName);
                    result.put("output_dir", Paths.get(pythonPath, "output").toString());
                    result.put("processTime", System.currentTimeMillis());
                } else {
                    result.put("success", false);
                    result.put("error", "Python脚本执行失败，退出码: " + exitCode);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("执行Python脚本时发生错误", e);
            throw e;
        }
    }
    
    /**
     * 查找生成的Define文件
     */
    private String findGeneratedFile() {
        try {
            Path outputDir = Paths.get(pythonPath, "output");
            if (!Files.exists(outputDir)) {
                logger.warn("输出目录不存在: {}", outputDir);
                return null;
            }
            
            // 查找最新的xlsx文件 (支持测试文件和正式文件)
            Optional<Path> latestFile = Files.list(outputDir)
                .filter(path -> path.toString().toLowerCase().endsWith(".xlsx"))
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.startsWith("sdtm_define_") || fileName.startsWith("sdtm_define_test_");
                })
                .max(Comparator.comparing(path -> {
                    try {
                        return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                        return null;
                    }
                }));
                
            if (latestFile.isPresent()) {
                String fileName = latestFile.get().getFileName().toString();
                logger.info("找到生成的文件: {}", fileName);
                return fileName;
            } else {
                logger.warn("未找到生成的Define文件");
                return null;
            }
        } catch (IOException e) {
            logger.error("查找生成文件时发生错误", e);
            return null;
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
    

    // ==================== aCRF相关API ====================
    
    /**
     * aCRF文件上传
     */
    @PostMapping("/uploadAcrf")
    public CommonResult<SasAcrfUploadDTO> uploadAcrfFile(@RequestParam("file") MultipartFile file) {
        logger.info("接收aCRF文件上传请求，文件名: {}, 文件大小: {}", 
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
            SasAcrfUploadDTO uploadDTO = new SasAcrfUploadDTO();
            uploadDTO.setFileId(fileId);
            uploadDTO.setOriginalName(originalFilename);
            uploadDTO.setServerFileName(serverFileName);
            uploadDTO.setFilePath(filePath.toString());
            uploadDTO.setFileSize(file.getSize());
            
            SasAcrfUploadDTO savedRecord = sasAcrfUploadService.saveUploadRecord(uploadDTO);
            if (savedRecord != null) {
                logger.info("aCRF上传记录保存成功，文件ID: {}", fileId);
                return CommonResult.success(savedRecord);
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
    @GetMapping("/getAcrfUploadRecords")
    public CommonResult<List<SasAcrfUploadDTO>> getAcrfUploadRecords() {
        logger.info("获取aCRF上传记录列表");
        
        try {
            List<SasAcrfUploadDTO> records = sasAcrfUploadService.getAllUploadRecords();
            logger.info("获取到 {} 条aCRF上传记录", records.size());
            return CommonResult.success(records);
        } catch (Exception e) {
            logger.error("获取aCRF上传记录失败", e);
            return CommonResult.fail("500", "获取上传记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理aCRF解析
     */
    @PostMapping("/processAcrf")
    public CommonResult<Map<String, Object>> processAcrf(@RequestBody Map<String, Object> request) {
        logger.info("接收aCRF解析请求: {}", request);
        
        try {
            String fileId = (String) request.get("fileId");
            String dateSuffix = (String) request.get("dateSuffix");
            
            if (fileId == null || fileId.trim().isEmpty()) {
                return CommonResult.fail("400", "文件ID不能为空");
            }
            
            // 根据fileId查找上传记录
            SasAcrfUploadDTO uploadRecord = sasAcrfUploadService.getUploadRecordByFileId(fileId);
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
            
            sasAcrfUploadService.updateProcessStatus(
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
     * 删除aCRF上传记录
     */
    @DeleteMapping("/deleteAcrfUpload")
    public CommonResult<Void> deleteAcrfUpload(@RequestParam("fileId") String fileId) {
        logger.info("删除aCRF上传记录，文件ID: {}", fileId);
        
        try {
            // 获取上传记录
            SasAcrfUploadDTO uploadRecord = sasAcrfUploadService.getUploadRecordByFileId(fileId);
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
            
            // 删除数据库记录（逻辑删除）
            boolean deleted = sasAcrfUploadService.deleteUploadRecord(fileId);
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
     * 下载aCRF解析结果
     */
    @GetMapping("/downloadAcrfResult")
    public ResponseEntity<InputStreamResource> downloadAcrfResult(@RequestParam("file") String fileName) {
        logger.info("接收aCRF结果文件下载请求: {}", fileName);
        
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
            logger.error("aCRF结果文件下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 调用Python脚本处理aCRF
     */
    private Map<String, Object> callAcrfPythonScript(SasAcrfUploadDTO uploadRecord, String dateSuffix) throws Exception {
        logger.info("开始调用Python脚本处理aCRF，文件: {}", uploadRecord.getOriginalName());
        
        // 构建Python命令
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(Paths.get(pythonPath, "pgm", "extract_pdf_annotations.py").toString());
        command.add(uploadRecord.getFilePath()); // 输入文件路径
        if (dateSuffix != null) {
            command.add("--date-suffix");
            command.add(dateSuffix);
        }
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(pythonPath));
        processBuilder.redirectErrorStream(true);
        
        logger.info("执行Python命令: {}", String.join(" ", command));
        logger.info("工作目录: {}", pythonPath);
        
        Process process = processBuilder.start();
        
        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logger.debug("Python输出: {}", line);
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
        
        if (exitCode == 0) {
            // 成功，查找生成的文件
            String outputFileName = findAcrfOutputFile(dateSuffix);
            if (outputFileName != null) {
                result.put("success", true);
                result.put("message", "aCRF解析成功");
                result.put("output_file", outputFileName);
                result.put("processTime", System.currentTimeMillis());
            } else {
                result.put("success", false);
                result.put("error", "未找到生成的解析文件");
            }
        } else {
            result.put("success", false);
            result.put("error", "Python脚本执行失败，退出码: " + exitCode + "\n输出: " + output.toString());
        }
        
        return result;
    }
    
    /**
     * 查找aCRF生成的输出文件
     */
    private String findAcrfOutputFile(String dateSuffix) {
        try {
            Path outputDir = Paths.get(pythonPath, "output");
            if (!Files.exists(outputDir)) {
                logger.warn("输出目录不存在: {}", outputDir);
                return null;
            }
            
            // 查找Annots2_日期.xlsx文件
            String expectedFileName = "Annots2" + (dateSuffix != null ? "_" + dateSuffix : "") + ".xlsx";
            Path annotFile = outputDir.resolve(expectedFileName);
            
            if (Files.exists(annotFile)) {
                logger.info("找到生成的aCRF解析文件: {}", expectedFileName);
                return expectedFileName;
            } else {
                logger.warn("未找到生成的aCRF解析文件: {}", expectedFileName);
                return null;
            }
        } catch (Exception e) {
            logger.error("查找aCRF生成文件时发生错误", e);
            return null;
        }
    }

    /**
     * 文件下载接口
     */
    @GetMapping("/downloadDefine")
    public ResponseEntity<InputStreamResource> downloadDefineFile(@RequestParam("file") String fileName) {
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

    @GetMapping("/getLatestDefineFile")
    public CommonResult<Map<String, Object>> getLatestDefineFile() {
        try {
            String latestFile = findGeneratedDefineFile();
            Map<String, Object> result = new HashMap<>();
            if (latestFile != null) {
                result.put("exists", true);
                result.put("output_file", latestFile);
            } else {
                result.put("exists", false);
            }
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("查找Define文件失败", e);
            return CommonResult.fail("500", "查找失败: " + e.getMessage());
        }
    }

    @PostMapping("/saveDefine")
    public CommonResult<Map<String, Object>> saveDefine(@RequestBody Map<String, Object> request) {
        String projectId = (String) request.get("projectId");
        String fileName = (String) request.get("fileName");
        String sheetData = (String) request.get("sheetData");

        logger.info("保存Define编辑数据, projectId={}, fileName={}, dataLength={}", projectId, fileName, sheetData != null ? sheetData.length() : 0);

        if (projectId == null || fileName == null || sheetData == null) {
            return CommonResult.fail("400", "缺少必要参数");
        }

        try {
            DefineSheetDataPO existing = defineSheetDataMapper.selectByProjectAndFile(projectId, fileName);
            if (existing != null) {
                existing.setSheetData(sheetData);
                existing.setUpdatedTime(java.time.LocalDateTime.now());
                defineSheetDataMapper.updateById(existing);
            } else {
                DefineSheetDataPO po = new DefineSheetDataPO();
                po.setProjectId(projectId);
                po.setFileName(fileName);
                po.setSheetData(sheetData);
                po.setCreatedTime(java.time.LocalDateTime.now());
                po.setUpdatedTime(java.time.LocalDateTime.now());
                defineSheetDataMapper.insert(po);
            }

            rebuildXlsxFromLuckysheet(sheetData, fileName);

            Map<String, Object> result = new HashMap<>();
            result.put("output_file", fileName);
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("保存Define编辑数据失败", e);
            return CommonResult.fail("500", "保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/getDefineSavedData")
    public CommonResult<Map<String, Object>> getDefineSavedData(
            @RequestParam("projectId") String projectId,
            @RequestParam("fileName") String fileName) {
        logger.info("查询Define已保存数据, projectId={}, fileName={}", projectId, fileName);
        try {
            DefineSheetDataPO po = defineSheetDataMapper.selectByProjectAndFile(projectId, fileName);
            if (po == null) {
                return CommonResult.success(null);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("sheetData", po.getSheetData());
            result.put("updatedTime", po.getUpdatedTime());
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("查询Define保存数据失败", e);
            return CommonResult.fail("500", "查询失败: " + e.getMessage());
        }
    }

    private void rebuildXlsxFromLuckysheet(String sheetDataJson, String fileName) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> sheets = mapper.readValue(sheetDataJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();

            for (Map<String, Object> sheetObj : sheets) {
                String sheetName = (String) sheetObj.getOrDefault("name", "Sheet");
                if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(sheetName);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> celldata = (List<Map<String, Object>>) sheetObj.get("celldata");
                if (celldata == null) continue;

                for (Map<String, Object> cell : celldata) {
                    int r = toIntSafe(cell.get("r"));
                    int c = toIntSafe(cell.get("c"));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> v = (Map<String, Object>) cell.get("v");
                    if (v == null) continue;

                    org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                    if (row == null) row = sheet.createRow(r);
                    org.apache.poi.ss.usermodel.Cell poiCell = row.createCell(c);

                    Object monitor = v.get("m");
                    Object vValue = v.get("v");
                    Object ctObj = v.get("ct");

                    if (vValue instanceof Number) {
                        poiCell.setCellValue(((Number) vValue).doubleValue());
                    } else if (monitor != null) {
                        poiCell.setCellValue(monitor.toString());
                    } else if (vValue != null) {
                        poiCell.setCellValue(vValue.toString());
                    }
                }
            }

            Path outputDir = Paths.get(pythonPath, "output");
            if (!Files.exists(outputDir)) Files.createDirectories(outputDir);
            Path outputPath = outputDir.resolve(fileName);
            try (OutputStream os = Files.newOutputStream(outputPath)) {
                workbook.write(os);
            }
            workbook.close();
            logger.info("已将编辑数据写回xlsx文件: {}", outputPath);

        } catch (Exception e) {
            logger.warn("写回xlsx文件失败（数据已保存到数据库）: {}", e.getMessage());
        }
    }

    private int toIntSafe(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }
} 