package com.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stat.common.dto.ProjectSpecDTO;
import com.stat.dal.mapper.DatasetsDataMapper;
import com.stat.dal.mapper.ProjectSpecMapper;
import com.stat.dal.po.DatasetsDataPO;
import com.stat.dal.po.ProjectSpecPO;
import com.stat.service.ProjectSpecService;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.stat.service.util.ExcelStyleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.stat.common.security.UserContext;
import jakarta.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 项目Spec数据服务实现类
 */
@Service
public class ProjectSpecServiceImpl implements ProjectSpecService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectSpecServiceImpl.class);
    
    @Resource
    private ProjectSpecMapper projectSpecMapper;
    
    @Resource
    private DatasetsDataMapper datasetsDataMapper;
    
    /**
     * 解析并保存项目Spec文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int parseAndSaveProjectSpec(String filePath, String projectId) {
        logger.info("开始解析项目Spec文件: {}, 项目ID: {}", filePath, projectId);
        
        int totalInserted = 0;
        int processedSheets = 0;
        
        ZipSecureFile.setMinInflateRatio(0);
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            logger.info("Excel文件包含 {} 个工作表", workbook.getNumberOfSheets());
            
            // 先删除该项目的现有数据
            String username = UserContext.getUsername();
            int deletedCount = projectSpecMapper.deleteByProjectId(projectId, username);
            logger.info("已删除项目 {} 的现有Spec数据: {} 条", projectId, deletedCount);
            
            // 解析 TOC sheet 并存入 sas_datasets_data
            parseTocSheetToDatasets(workbook, projectId, username);
            
            // 遍历所有工作表，每个工作表代表一个domain
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                
                logger.info("检查工作表 [{}]: {}", i + 1, sheetName);
                
                // 跳过非域名工作表
                if (!isDomainSheet(sheetName)) {
                    logger.info("跳过非域名工作表: {}", sheetName);
                    continue;
                }
                
                try {
                    logger.info("开始解析域工作表: {} (第 {} 个)", sheetName, i + 1);
                    List<ProjectSpecPO> domainSpecs = parseSheetData(sheet, sheetName, projectId);
                    
                    if (!domainSpecs.isEmpty()) {
                        // 逐个工作表插入数据，避免大批量操作失败
                        int insertCount = projectSpecMapper.batchInsert(domainSpecs);
                        totalInserted += insertCount;
                        processedSheets++;
                        logger.info("域 {} 处理完成: 解析 {} 条记录，插入 {} 条", 
                                   sheetName, domainSpecs.size(), insertCount);
                    } else {
                        logger.info("域 {} 没有有效数据", sheetName);
                    }
                    
                } catch (Exception e) {
                    logger.error("解析工作表 {} 失败: {}", sheetName, e.getMessage(), e);
                    // 继续处理下一个工作表，不中断整个流程
                    continue;
                }
            }
            
            logger.info("项目Spec文件解析完成 - 处理了 {} 个工作表，共插入 {} 条记录", processedSheets, totalInserted);
            
            if (totalInserted == 0) {
                throw new RuntimeException("未找到任何有效的SPEC数据表，请检查文件格式和表头");
            }
            
            return totalInserted;
            
        } catch (IOException e) {
            logger.error("读取Spec文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取Spec文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("解析Spec文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析Spec文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证是否为有效的SPEC工作表（包含标准表头）
     */
    private boolean isValidSpecSheet(Map<String, Integer> columnMap) {
        // 定义SPEC文件必须包含的核心表头字段
        Set<String> requiredHeaders = new HashSet<>(Arrays.asList(
            "variable",     // 变量名
            "label"         // 变量标签
        ));
        
        // 定义SPEC文件常见的可选表头字段
        Set<String> optionalHeaders = new HashSet<>(Arrays.asList(
            "type",         // 数据类型
            "length",       // 长度
            "controlled_terms_or_format", // 受控术语或格式
            "cdisc_submission_value",     // CDISC提交值
            "origin",       // 数据来源
            "role",         // 变量角色
            "core",         // 核心级别
            "codelist",     // 代码列表
            "format",       // 格式
            "comment",      // 备注
            "cdisc_notes"   // CDISC注释
        ));
        
        // 检查必需的表头字段
        for (String requiredHeader : requiredHeaders) {
            if (!columnMap.containsKey(requiredHeader)) {
                logger.debug("缺少必需的表头字段: {}", requiredHeader);
                return false;
            }
        }
        
        // 计算匹配的字段数量（必需字段 + 可选字段）
        Set<String> allValidHeaders = new HashSet<>(requiredHeaders);
        allValidHeaders.addAll(optionalHeaders);
        
        int matchedHeaders = 0;
        for (String header : columnMap.keySet()) {
            if (allValidHeaders.contains(header)) {
                matchedHeaders++;
            }
        }
        
        // 如果匹配的标准字段数量大于等于3个（包含必需字段），认为是有效的SPEC表头
        boolean isValid = matchedHeaders >= 3;
        
        if (isValid) {
            logger.debug("工作表包含 {} 个标准SPEC字段，验证通过", matchedHeaders);
        } else {
            logger.debug("工作表仅包含 {} 个标准SPEC字段，验证失败", matchedHeaders);
        }
        
        return isValid;
    }
    
    /**
     * 判断是否为域名工作表
     */
    private boolean isDomainSheet(String sheetName) {
        if (!StringUtils.hasText(sheetName)) {
            return false;
        }
        
        String upperName = sheetName.trim().toUpperCase();
        
        // 非域名 sheet，已由专用解析处理
        Set<String> excludedSheets = new HashSet<>(Arrays.asList("TOC", "SUMMARY", "COVER", "README"));
        if (excludedSheets.contains(upperName)) {
            return false;
        }
        
        // 常见的CDISC域名
        Set<String> commonDomains = new HashSet<>(Arrays.asList(
            "DM", "AE", "CM", "VS", "LB", "EG", "IE", "DS", "EX", "MH", "SU", "PE", "QS",
            "FA", "PC", "PP", "DA", "DD", "HO", "ML", "OE", "PR", "RP", "SC", "SE", "SR", "TA", "TE", "TI", "TS", "TV"
        ));
        
        // 检查是否为常见域名，或者是2-4个字符的大写字母
        return commonDomains.contains(upperName) || 
               (upperName.length() >= 2 && upperName.length() <= 4 && upperName.matches("[A-Z]+"));
    }
    
    /**
     * 解析 TOC 工作表并存入 sas_datasets_data
     */
    private void parseTocSheetToDatasets(Workbook workbook, String projectId, String username) {
        Sheet tocSheet = null;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String name = workbook.getSheetAt(i).getSheetName();
            if (name != null && name.trim().equalsIgnoreCase("TOC")) {
                tocSheet = workbook.getSheetAt(i);
                break;
            }
        }
        if (tocSheet == null) {
            logger.info("未找到 TOC 工作表，跳过 Datasets 提取");
            return;
        }
        
        logger.info("找到 TOC 工作表，开始解析 Datasets 数据");
        
        if (tocSheet.getPhysicalNumberOfRows() <= 1) {
            logger.info("TOC 工作表数据行不足，跳过");
            return;
        }
        
        Row headerRow = tocSheet.getRow(0);
        if (headerRow == null) return;
        
        Map<String, Integer> colMap = new HashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String h = cell.getStringCellValue().trim().toLowerCase()
                        .replaceAll("[\\s_]+", " ");
                colMap.put(h, c);
            }
        }
        
        Integer datasetCol = findCol(colMap, "dataset", "domain");
        if (datasetCol == null) {
            logger.info("TOC 工作表缺少 Dataset/Domain 列，跳过");
            return;
        }
        
        Integer labelCol = findCol(colMap, "label", "description");
        Integer classCol = findCol(colMap, "class", "observation class");
        Integer subClassCol = findCol(colMap, "subclass", "sub class");
        Integer structureCol = findCol(colMap, "structure");
        Integer keyVarsCol = findCol(colMap, "key variables", "keys");
        Integer standardCol = findCol(colMap, "standard");
        Integer hasNoDataCol = findCol(colMap, "has no data");
        Integer repeatingCol = findCol(colMap, "repeating");
        Integer refDataCol = findCol(colMap, "reference data");
        Integer commentCol = findCol(colMap, "comment", "comments");
        Integer devNotesCol = findCol(colMap, "developer notes");
        
        // 删除该项目和用户的已有 datasets 数据
        QueryWrapper<DatasetsDataPO> delWrapper = new QueryWrapper<>();
        delWrapper.eq("project_id", projectId).eq("username", username);
        datasetsDataMapper.delete(delWrapper);
        logger.info("已清除项目 {} 用户 {} 的旧 Datasets 数据", projectId, username);
        
        List<DatasetsDataPO> datasetsList = new ArrayList<>();
        int order = 1;
        for (int r = 1; r <= tocSheet.getLastRowNum(); r++) {
            Row row = tocSheet.getRow(r);
            if (row == null) continue;
            
            String dsName = getCellValue(row, datasetCol);
            if (!StringUtils.hasText(dsName)) continue;
            
            DatasetsDataPO po = new DatasetsDataPO();
            po.setProjectId(projectId);
            po.setUsername(username);
            po.setDataset(dsName.trim());
            po.setLabel(getCellValue(row, labelCol));
            po.setDataClass(getCellValue(row, classCol));
            po.setSubClass(getCellValue(row, subClassCol));
            po.setStructure(getCellValue(row, structureCol));
            po.setKeyVariables(getCellValue(row, keyVarsCol));
            po.setStandard(getCellValue(row, standardCol));
            po.setHasNoData(getCellValue(row, hasNoDataCol));
            po.setRepeating(getCellValue(row, repeatingCol));
            po.setReferenceData(getCellValue(row, refDataCol));
            po.setComment(getCellValue(row, commentCol));
            po.setDeveloperNotes(getCellValue(row, devNotesCol));
            po.setSortOrder(order++);
            po.setCreatedBy(username);
            
            datasetsList.add(po);
        }
        
        if (!datasetsList.isEmpty()) {
            for (DatasetsDataPO po : datasetsList) {
                datasetsDataMapper.insert(po);
            }
            logger.info("TOC 解析完成，插入 {} 条 Datasets 记录", datasetsList.size());
        } else {
            logger.info("TOC 工作表无有效数据行");
        }
    }
    
    private Integer findCol(Map<String, Integer> colMap, String... aliases) {
        // 优先精确匹配
        for (String alias : aliases) {
            if (colMap.containsKey(alias)) {
                return colMap.get(alias);
            }
        }
        // 回退到 contains 模糊匹配，但排除子串误匹配
        for (String alias : aliases) {
            for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
                String key = entry.getKey();
                if (key.equals(alias)) {
                    return entry.getValue();
                }
                if (key.contains(alias)) {
                    // 避免 "class" 误匹配 "subclass"：检查 alias 前后是否为单词边界
                    int idx = key.indexOf(alias);
                    boolean startOk = (idx == 0 || !Character.isLetterOrDigit(key.charAt(idx - 1)));
                    boolean endOk = (idx + alias.length() >= key.length() || !Character.isLetterOrDigit(key.charAt(idx + alias.length())));
                    if (startOk && endOk) {
                        return entry.getValue();
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 解析工作表数据
     */
    private List<ProjectSpecPO> parseSheetData(Sheet sheet, String domain, String projectId) {
        List<ProjectSpecPO> specs = new ArrayList<>();
        
        logger.info("开始解析工作表 {} - 总行数: {}", domain, sheet.getPhysicalNumberOfRows());
        
        if (sheet.getPhysicalNumberOfRows() <= 1) {
            logger.info("工作表 {} 数据行不足(<=1)，跳过处理", domain);
            return specs;
        }
        
        // 获取表头行
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            logger.warn("工作表 {} 没有表头行", domain);
            return specs;
        }
        
        // 解析表头，建立列名到列索引的映射
        Map<String, Integer> columnMap = parseHeader(headerRow);
        logger.info("工作表 {} 表头解析完成，找到 {} 个列: {}", domain, columnMap.size(), columnMap.keySet());
        
        // 验证是否包含SPEC标准表头
        if (!isValidSpecSheet(columnMap)) {
            logger.info("工作表 {} 不包含SPEC标准表头，跳过处理。需要包含: variable, label", domain);
            return specs;
        }
        
        // 解析数据行
        int sortOrder = 1;
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            
            ProjectSpecPO spec = parseRowData(row, columnMap, domain, projectId);
            if (spec != null) {
                spec.setSortOrder(sortOrder++);
                spec.setCreatedTime(new Date());
                spec.setUpdatedTime(new Date());
                spec.setCreatedBy("system");
                spec.setUsername(UserContext.getUsername());
                specs.add(spec);
            }
        }
        
        return specs;
    }
    
    /**
     * 解析表头，建立列名映射
     */
    private Map<String, Integer> parseHeader(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        logger.info("解析表头行，共 {} 列", headerRow.getLastCellNum());
        
        for (int colIndex = 0; colIndex < headerRow.getLastCellNum(); colIndex++) {
            Cell cell = headerRow.getCell(colIndex);
            if (cell != null && cell.getCellType() == CellType.STRING) {
                String headerName = cell.getStringCellValue();
                if (StringUtils.hasText(headerName)) {
                    // 标准化表头名称，去除空格并转换为小写
                    String normalizedHeader = normalizeHeaderName(headerName.trim());
                    columnMap.put(normalizedHeader, colIndex);
                    logger.info("表头映射: [{}] {} -> {} (列{})", colIndex, headerName, normalizedHeader, colIndex);
                }
            } else {
                logger.debug("第 {} 列为空或非字符串类型", colIndex);
            }
        }
        
        return columnMap;
    }
    
    /**
     * 标准化表头名称
     */
    private String normalizeHeaderName(String headerName) {
        // 创建常见表头名称的映射
        Map<String, String> headerMapping = new HashMap<>();
        
        // 变量相关 - 支持更多格式
        headerMapping.put("variable", "variable");
        headerMapping.put("变量", "variable");
        headerMapping.put("变量名", "variable");
        headerMapping.put("变量名称", "variable");
        headerMapping.put("variable name", "variable");
        headerMapping.put("var", "variable");
        headerMapping.put("varname", "variable");
        headerMapping.put("name", "variable");
        
        // 标签相关 - 支持更多格式
        headerMapping.put("label", "label");
        headerMapping.put("标签", "label");
        headerMapping.put("变量标签", "label");
        headerMapping.put("变量描述", "label");
        headerMapping.put("描述", "label");
        headerMapping.put("variable label", "label");
        headerMapping.put("description", "label");
        headerMapping.put("desc", "label");
        
        // 类型相关 - 支持更多格式
        headerMapping.put("type", "type");
        headerMapping.put("类型", "type");
        headerMapping.put("数据类型", "type");
        headerMapping.put("data type", "type");
        headerMapping.put("datatype", "type");
        
        // 长度相关 - 支持更多格式
        headerMapping.put("length", "length");
        headerMapping.put("长度", "length");
        headerMapping.put("变量长度", "length");
        headerMapping.put("len", "length");
        headerMapping.put("size", "length");
        
        // Origin 来源相关
        headerMapping.put("origin", "origin");
        headerMapping.put("来源", "origin");
        headerMapping.put("数据来源", "origin");
        headerMapping.put("data origin", "origin");
        
        // Source（独立于 Origin）
        headerMapping.put("source", "source");
        headerMapping.put("源", "source");
        
        // 角色相关 - 支持更多格式
        headerMapping.put("role", "role");
        headerMapping.put("角色", "role");
        headerMapping.put("变量角色", "role");
        headerMapping.put("variable role", "role");
        
        // 核心级别相关 - 支持更多格式
        headerMapping.put("core", "core");
        headerMapping.put("核心", "core");
        headerMapping.put("核心级别", "core");
        headerMapping.put("core level", "core");
        headerMapping.put("req", "core");
        headerMapping.put("required", "core");
        
        // 代码列表相关 - 支持更多格式
        headerMapping.put("codelist", "codelist");
        headerMapping.put("代码列表", "codelist");
        headerMapping.put("代码表", "codelist");
        headerMapping.put("code list", "codelist");
        headerMapping.put("controlled terms", "codelist");
        headerMapping.put("ct", "codelist");
        
        // 格式相关 - 支持更多格式
        headerMapping.put("format", "format");
        headerMapping.put("格式", "format");
        headerMapping.put("显示格式", "format");
        headerMapping.put("display format", "format");
        headerMapping.put("fmt", "format");
        
        // 备注相关 - 支持更多格式
        headerMapping.put("comment", "comment");
        headerMapping.put("comments", "comment");
        headerMapping.put("备注", "comment");
        headerMapping.put("说明", "comment");
        headerMapping.put("notes", "comment");
        headerMapping.put("note", "comment");
        headerMapping.put("remark", "comment");
        
        // CDISC注释 - 支持更多格式
        headerMapping.put("cdisc notes", "cdisc_notes");
        headerMapping.put("cdisc注释", "cdisc_notes");
        headerMapping.put("cdisc comment", "cdisc_notes");
        headerMapping.put("cdisc", "cdisc_notes");
        
        // 小数位数
        headerMapping.put("decimal places", "decimal_places");
        headerMapping.put("decimals", "decimal_places");
        headerMapping.put("小数位", "decimal_places");
        headerMapping.put("小数位数", "decimal_places");
        
        // 受控术语或格式 - 支持多种格式
        headerMapping.put("controlled terms or format", "controlled_terms_or_format");
        headerMapping.put("controlled terms", "controlled_terms_or_format");
        headerMapping.put("受控术语或格式", "controlled_terms_or_format");
        headerMapping.put("受控术语", "controlled_terms_or_format");
        headerMapping.put("术语或格式", "controlled_terms_or_format");
        headerMapping.put("format or terms", "controlled_terms_or_format");
        
        // CDISC提交值 - 支持多种格式  
        headerMapping.put("cdisc submission value", "cdisc_submission_value");
        headerMapping.put("cdisc submission", "cdisc_submission_value");
        headerMapping.put("submission value", "cdisc_submission_value");
        headerMapping.put("cdisc提交值", "cdisc_submission_value");
        headerMapping.put("提交值", "cdisc_submission_value");
        headerMapping.put("cdisc value", "cdisc_submission_value");
        
        // 必填字段
        headerMapping.put("mandatory", "mandatory");
        headerMapping.put("必填", "mandatory");
        headerMapping.put("required", "mandatory");
        
        // Pages / CRF Page
        headerMapping.put("pages", "pages");
        headerMapping.put("page", "pages");
        headerMapping.put("crf page", "pages");
        headerMapping.put("crf pages", "pages");
        headerMapping.put("crfpage", "pages");
        headerMapping.put("页码", "pages");
        
        // Method
        headerMapping.put("method", "method");
        headerMapping.put("方法", "method");
        
        // Text (was "Derived Method")
        headerMapping.put("text", "derivation");
        headerMapping.put("derivation", "derivation");
        headerMapping.put("derived method", "derivation");
        headerMapping.put("derivedmethod", "derivation");
        headerMapping.put("derived", "derivation");
        headerMapping.put("推导方法", "derivation");
        headerMapping.put("推导", "derivation");

        // 主键序号
        headerMapping.put("key sequence", "key_sequence");
        headerMapping.put("主键序号", "key_sequence");
        headerMapping.put("key seq", "key_sequence");
        headerMapping.put("keyseq", "key_sequence");
        
        // SUPP
        headerMapping.put("supp", "supp");
        headerMapping.put("supplemental", "supp");
        
        // QEVAL
        headerMapping.put("qeval", "qeval");
        headerMapping.put("evaluator", "qeval");
        
        String normalizedName = headerName.toLowerCase().trim();
        return headerMapping.getOrDefault(normalizedName, normalizedName);
    }
    
    /**
     * 解析数据行
     */
    private ProjectSpecPO parseRowData(Row row, Map<String, Integer> columnMap, String domain, String projectId) {
        // 检查是否有变量名，如果没有变量名则跳过该行
        String variable = getCellValue(row, columnMap.get("variable"));
        if (!StringUtils.hasText(variable)) {
            return null;
        }
        
        ProjectSpecPO spec = new ProjectSpecPO();
        spec.setProjectId(projectId);
        spec.setDomain(domain);
        spec.setVariable(variable);
        
        // 解析其他字段
        spec.setLabel(getCellValue(row, columnMap.get("label")));
        spec.setType(getCellValue(row, columnMap.get("type")));
        spec.setOrigin(getCellValue(row, columnMap.get("origin")));
        spec.setRole(getCellValue(row, columnMap.get("role")));
        spec.setCore(getCellValue(row, columnMap.get("core")));
        spec.setFormat(getCellValue(row, columnMap.get("format")));
        spec.setComment(getCellValue(row, columnMap.get("comment")));
        spec.setCdiscNotes(getCellValue(row, columnMap.get("cdisc_notes")));
        
        // Spec 原始字段
        spec.setControlledTermsOrFormat(getCellValue(row, columnMap.get("controlled_terms_or_format")));
        spec.setCdiscSubmissionValue(getCellValue(row, columnMap.get("cdisc_submission_value")));
        spec.setSource(getCellValue(row, columnMap.get("source")));
        spec.setPages(getCellValue(row, columnMap.get("pages")));
        
        // P21 映射：Codelist ← Spec "Controlled Terms or Format"
        spec.setCodelist(getCellValue(row, columnMap.get("controlled_terms_or_format")));
        // Text column → textContent
        spec.setTextContent(getCellValue(row, columnMap.get("derivation")));
        // Method column
        spec.setMethod(getCellValue(row, columnMap.get("method")));
        spec.setDerivation(null);
        
        // SUPP / QEVAL
        spec.setSupp(getCellValue(row, columnMap.get("supp")));
        spec.setQeval(getCellValue(row, columnMap.get("qeval")));

        // Length: 保留原始字符串，但规范化数值格式（去除文本型数字的 .0 后缀）
        String lengthStr = getCellValue(row, columnMap.get("length"));
        if (StringUtils.hasText(lengthStr)) {
            spec.setLength(normalizeLength(lengthStr.trim()));
        }
        
        // 解析小数位数
        String decimalStr = getCellValue(row, columnMap.get("decimal_places"));
        if (StringUtils.hasText(decimalStr) && decimalStr.matches("\\d+")) {
            try {
                spec.setDecimalPlaces(Integer.parseInt(decimalStr));
            } catch (NumberFormatException e) {
                logger.debug("解析小数位数失败: {}", decimalStr);
            }
        }
        
        // Mandatory: 如果Spec中有明确值则用它，否则根据Core自动派生
        String mandatoryStr = getCellValue(row, columnMap.get("mandatory"));
        if (StringUtils.hasText(mandatoryStr)) {
            String lower = mandatoryStr.toLowerCase().trim();
            if ("yes".equals(lower) || "y".equals(lower) || "true".equals(lower) || "1".equals(lower) || "是".equals(lower)) {
                spec.setMandatory("Yes");
            } else if ("no".equals(lower) || "n".equals(lower) || "false".equals(lower) || "0".equals(lower) || "否".equals(lower)) {
                spec.setMandatory("No");
            } else {
                spec.setMandatory(mandatoryStr.trim());
            }
        } else {
            // 根据Core自动派生: Req → Yes, Exp/Perm → No
            String coreVal = spec.getCore();
            if (StringUtils.hasText(coreVal)) {
                String coreUpper = coreVal.trim().toUpperCase();
                if ("REQ".equals(coreUpper) || "REQUIRED".equals(coreUpper)) {
                    spec.setMandatory("Yes");
                } else {
                    spec.setMandatory("No");
                }
            }
        }
        
        // 解析主键序号
        String keySeqStr = getCellValue(row, columnMap.get("key_sequence"));
        if (StringUtils.hasText(keySeqStr) && keySeqStr.matches("\\d+")) {
            try {
                spec.setKeySequence(Integer.parseInt(keySeqStr));
            } catch (NumberFormatException e) {
                logger.debug("解析主键序号失败: {}", keySeqStr);
            }
        }
        
        return spec;
    }
    
    /**
     * 获取单元格值
     */
    private String getCellValue(Row row, Integer colIndex) {
        if (colIndex == null || row == null) {
            return null;
        }
        
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    // 如果是整数，返回整数格式
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return null;
                    }
                }
            default:
                return null;
        }
    }
    
    /**
     * 批量保存项目Spec数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveProjectSpec(List<ProjectSpecPO> projectSpecs) {
        if (projectSpecs == null || projectSpecs.isEmpty()) {
            return 0;
        }
        
        return projectSpecMapper.batchInsert(projectSpecs);
    }
    
    /**
     * 根据项目ID删除所有Spec数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByProjectId(String projectId) {
        return projectSpecMapper.deleteByProjectId(projectId, UserContext.getUsername());
    }
    
    /**
     * 根据项目ID查询Spec数据列表
     */
    @Override
    public List<ProjectSpecDTO> getByProjectId(String projectId) {
        List<ProjectSpecPO> pos = projectSpecMapper.selectByProjectId(projectId, UserContext.getUsername());
        return pos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * 根据项目ID和域查询Spec数据列表
     */
    @Override
    public List<ProjectSpecDTO> getByProjectIdAndDomain(String projectId, String domain) {
        List<ProjectSpecPO> pos = projectSpecMapper.selectByProjectIdAndDomain(projectId, domain, UserContext.getUsername());
        return pos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * 分页查询项目Spec数据
     */
    @Override
    public List<ProjectSpecDTO> getByPage(Map<String, Object> params) {
        List<ProjectSpecPO> pos = projectSpecMapper.selectByPage(params);
        return pos.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * 查询项目Spec数据总数
     */
    @Override
    public int countByParams(Map<String, Object> params) {
        Integer count = projectSpecMapper.countByParams(params);
        return count != null ? count : 0;
    }
    
    /**
     * 获取项目中所有不同的域列表
     */
    @Override
    public List<String> getDistinctDomainsByProjectId(String projectId) {
        return projectSpecMapper.selectDistinctDomainsByProjectId(projectId, UserContext.getUsername());
    }
    
    /**
     * 获取项目域统计信息
     */
    @Override
    public List<Map<String, Object>> getDomainStatsByProjectId(String projectId) {
        return projectSpecMapper.selectDomainStatsByProjectId(projectId, UserContext.getUsername());
    }
    
    /**
     * 检查项目变量是否已存在
     */
    @Override
    public boolean checkVariableExists(String projectId, String domain, String variable) {
        int count = projectSpecMapper.checkVariableExists(projectId, domain, variable, UserContext.getUsername());
        return count > 0;
    }
    
    /**
     * 获取CDISC Submission Value对应的Domain映射
     */
    @Override
    public Map<String, List<String>> getVariableDomainMapping(String projectId) {
        List<Map<String, Object>> results = projectSpecMapper.getVariableDomainMapping(projectId, UserContext.getUsername());
        Map<String, List<String>> mapping = new HashMap<>();
        
        for (Map<String, Object> result : results) {
            String cdiscValue = (String) result.get("cdiscValue");
            String domainsStr = (String) result.get("domains");
            
            if (cdiscValue != null && domainsStr != null) {
                List<String> domainList = Arrays.asList(domainsStr.split(","))
                    .stream()
                    .map(String::trim)
                    .collect(Collectors.toList());
                mapping.put(cdiscValue, domainList);
            }
        }
        
        return mapping;
    }
    
    /**
     * 获取VCD到域的映射 - 基于codelist表的VCD获取对应域
     */
    @Override
    public Map<String, List<String>> getVcdDomainMapping(String projectId) {
        List<Map<String, Object>> results = projectSpecMapper.getVcdDomainMapping(projectId, UserContext.getUsername());
        Map<String, List<String>> mapping = new HashMap<>();
        
        for (Map<String, Object> result : results) {
            String vcdName = (String) result.get("vcdName");
            String domainsStr = (String) result.get("domains");
            
            if (vcdName != null) {
                List<String> domainList = new ArrayList<>();
                if (domainsStr != null && !domainsStr.trim().isEmpty()) {
                    domainList = Arrays.asList(domainsStr.split(","))
                        .stream()
                        .map(String::trim)
                        .filter(domain -> !domain.isEmpty())
                        .collect(Collectors.toList());
                }
                mapping.put(vcdName, domainList);
            }
        }
        
        return mapping;
    }
    
    @Override
    public String exportProjectSpecToExcel(String projectId, String outputDir) {
        logger.info("开始导出项目Spec为Excel, projectId={}, outputDir={}", projectId, outputDir);

        List<ProjectSpecPO> allSpecs = projectSpecMapper.selectByProjectId(projectId, UserContext.getUsername());
        if (allSpecs == null || allSpecs.isEmpty()) {
            logger.warn("项目 {} 没有Spec数据可导出", projectId);
            return null;
        }

        Map<String, List<ProjectSpecPO>> byDomain = allSpecs.stream()
                .collect(Collectors.groupingBy(ProjectSpecPO::getDomain, LinkedHashMap::new, Collectors.toList()));

        String[] headers = {"Order", "Dataset", "Variable", "Label", "Data Type", "Length",
                "Significant Digits", "Format", "Mandatory", "Assigned Value", "Codelist",
                "Submission Value", "Common", "Origin", "Source", "Pages", "Method",
                "Predecessor", "Role", "Has No Data", "Comment", "Developer Notes",
                "SUPP", "QEVAL", "Method Flag"};

        Path outDir = Paths.get(outputDir);
        try {
            if (!Files.exists(outDir)) {
                Files.createDirectories(outDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建输出目录失败: " + outputDir, e);
        }

        String outputFileName = "spec_export_" + projectId + ".xlsx";
        Path outputPath = outDir.resolve(outputFileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            for (Map.Entry<String, List<ProjectSpecPO>> entry : byDomain.entrySet()) {
                String domain = entry.getKey();
                List<ProjectSpecPO> specs = entry.getValue();

                String sheetName = domain.length() > 31 ? domain.substring(0, 31) : domain;
                Sheet sheet = workbook.createSheet(sheetName);

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                int rowIdx = 1;
                for (ProjectSpecPO spec : specs) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(spec.getSortOrder() != null ? spec.getSortOrder() : rowIdx - 1);
                    row.createCell(1).setCellValue(nvl(spec.getDomain()));
                    row.createCell(2).setCellValue(nvl(spec.getVariable()));
                    row.createCell(3).setCellValue(nvl(spec.getLabel()));
                    row.createCell(4).setCellValue(nvl(spec.getType()));
                    row.createCell(5).setCellValue(nvl(spec.getLength()));
                    row.createCell(6).setCellValue(nvl(spec.getSignificantDigits()));
                    row.createCell(7).setCellValue(nvl(spec.getFormat()));
                    row.createCell(8).setCellValue(nvl(spec.getMandatory()));
                    row.createCell(9).setCellValue(nvl(spec.getAssignedValue()));
                    row.createCell(10).setCellValue(nvl(spec.getCodelist()));
                    row.createCell(11).setCellValue(nvl(spec.getCdiscSubmissionValue()));
                    row.createCell(12).setCellValue(nvl(spec.getCommon()));
                    row.createCell(13).setCellValue(nvl(spec.getOrigin()));
                    row.createCell(14).setCellValue(nvl(spec.getSource()));
                    row.createCell(15).setCellValue(nvl(spec.getPages()));
                    row.createCell(16).setCellValue(nvl(spec.getMethod()));
                    row.createCell(17).setCellValue(nvl(spec.getDerivation()));
                    row.createCell(18).setCellValue(nvl(spec.getRole()));
                    row.createCell(19).setCellValue(nvl(spec.getHasNoData()));
                    row.createCell(20).setCellValue(nvl(spec.getComment()));
                    row.createCell(21).setCellValue(nvl(spec.getDeveloperNotes()));
                    row.createCell(22).setCellValue(nvl(spec.getSupp()));
                    row.createCell(23).setCellValue(nvl(spec.getQeval()));
                    row.createCell(24).setCellValue(nvl(spec.getMethod()));
                }
            }

            ExcelStyleHelper.styleWorkbook(workbook);

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fos);
            }

            logger.info("Spec导出完成: {} ({}个域, {}条记录)", outputPath, byDomain.size(), allSpecs.size());
            return outputPath.toAbsolutePath().toString();

        } catch (IOException e) {
            logger.error("导出Spec Excel失败", e);
            throw new RuntimeException("导出Spec Excel失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ProjectSpecDTO getById(Long id) {
        ProjectSpecPO po = projectSpecMapper.selectById(id);
        return po != null ? convertToDTO(po) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectSpecDTO addVariable(ProjectSpecPO po) {
        po.setCreatedTime(new Date());
        po.setUpdatedTime(new Date());
        po.setUsername(UserContext.getUsername());
        if (po.getSortOrder() == null) {
            List<ProjectSpecPO> existing = projectSpecMapper.selectByProjectIdAndDomain(po.getProjectId(), po.getDomain(), UserContext.getUsername());
            po.setSortOrder(existing.size() + 1);
        }
        projectSpecMapper.insert(po);
        return convertToDTO(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectSpecDTO updateVariable(ProjectSpecPO po) {
        po.setUpdatedTime(new Date());
        projectSpecMapper.updateById(po);
        ProjectSpecPO updated = projectSpecMapper.selectById(po.getId());
        return updated != null ? convertToDTO(updated) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteVariable(Long id) {
        return projectSpecMapper.deleteById(id);
    }

    @Override
    public String syncSpecToFile(String projectId, String outputDir) {
        logger.info("同步Spec数据到文件, projectId={}, outputDir={}", projectId, outputDir);

        List<ProjectSpecPO> allSpecs = projectSpecMapper.selectByProjectId(projectId, UserContext.getUsername());
        if (allSpecs == null || allSpecs.isEmpty()) {
            logger.warn("项目 {} 没有Spec数据可同步", projectId);
            return null;
        }

        Map<String, List<ProjectSpecPO>> byDomain = allSpecs.stream()
                .collect(Collectors.groupingBy(ProjectSpecPO::getDomain, LinkedHashMap::new, Collectors.toList()));

        Path outDir = Paths.get(outputDir);
        try {
            if (!Files.exists(outDir)) {
                Files.createDirectories(outDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建输出目录失败: " + outputDir, e);
        }

        String outputFileName = "spec_synced_" + projectId + ".xlsx";
        Path outputPath = outDir.resolve(outputFileName);

        String[] headers = {"Variable", "Label", "Type", "Length", "Controlled Terms or Format",
                "CDISC Submission Value", "Origin", "Role", "CDISC Notes", "Core",
                "Codelist", "Format", "Comment", "Pages", "Method", "Derivation"};

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet toc = workbook.createSheet("TOC");
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

            for (Map.Entry<String, List<ProjectSpecPO>> entry : byDomain.entrySet()) {
                String domain = entry.getKey();
                List<ProjectSpecPO> specs = entry.getValue();

                String sheetName = domain.length() > 31 ? domain.substring(0, 31) : domain;
                Sheet sheet = workbook.createSheet(sheetName);

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }

                int rowIdx = 1;
                for (ProjectSpecPO spec : specs) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(nvl(spec.getVariable()));
                    row.createCell(1).setCellValue(nvl(spec.getLabel()));
                    row.createCell(2).setCellValue(nvl(spec.getType()));
                    row.createCell(3).setCellValue(nvl(spec.getLength()));
                    row.createCell(4).setCellValue(nvl(spec.getControlledTermsOrFormat()));
                    row.createCell(5).setCellValue(nvl(spec.getCdiscSubmissionValue()));
                    row.createCell(6).setCellValue(nvl(spec.getOrigin()));
                    row.createCell(7).setCellValue(nvl(spec.getRole()));
                    row.createCell(8).setCellValue(nvl(spec.getCdiscNotes()));
                    row.createCell(9).setCellValue(nvl(spec.getCore()));
                    row.createCell(10).setCellValue(nvl(spec.getCodelist()));
                    row.createCell(11).setCellValue(nvl(spec.getFormat()));
                    row.createCell(12).setCellValue(nvl(spec.getComment()));
                    row.createCell(13).setCellValue(nvl(spec.getPages()));
                    row.createCell(14).setCellValue(nvl(spec.getMethod()));
                    row.createCell(15).setCellValue(nvl(spec.getDerivation()));
                }
            }

            ExcelStyleHelper.styleWorkbook(workbook);

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                workbook.write(fos);
            }

            logger.info("Spec同步文件已生成: {} ({}个域, {}条记录, TOC+{}个sheet)", outputPath, byDomain.size(), allSpecs.size(), byDomain.size());
            return outputPath.toAbsolutePath().toString();

        } catch (IOException e) {
            logger.error("生成Spec同步文件失败", e);
            throw new RuntimeException("生成Spec同步文件失败: " + e.getMessage(), e);
        }
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    /**
     * 规范化 length 值：如果是纯数值文本（如 "200.0"），去掉小数部分变成 "200"
     */
    private static String normalizeLength(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        try {
            double d = Double.parseDouble(raw);
            if (d == (long) d) {
                return String.valueOf((long) d);
            }
            return raw;
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    /**
     * PO转DTO
     */
    private ProjectSpecDTO convertToDTO(ProjectSpecPO po) {
        if (po == null) {
            return null;
        }
        
        ProjectSpecDTO dto = new ProjectSpecDTO();
        BeanUtils.copyProperties(po, dto);
        return dto;
    }
}