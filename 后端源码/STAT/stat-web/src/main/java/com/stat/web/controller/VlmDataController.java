package com.stat.web.controller;

import com.stat.common.dto.VlmDataDTO;
import com.stat.common.result.CommonResult;
import com.stat.common.result.PageCommonResult;
import com.stat.common.security.RequireProjectAccess;
import com.stat.common.security.UserContext;
import com.stat.dal.mapper.PagesDataMapper;
import com.stat.dal.mapper.ProjectSpecMapper;
import com.stat.dal.mapper.VlmDataMapper;
import com.stat.dal.mapper.CodelistDataMapper;
import com.stat.dal.mapper.ProjectConfigMapper;
import com.stat.dal.po.PagesDataPO;
import com.stat.dal.po.ProjectConfigPO;
import com.stat.dal.po.ProjectSpecPO;
import com.stat.dal.po.VlmDataPO;
import com.stat.dal.po.CodelistDataPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import com.stat.service.IVlmDataService;
import com.stat.service.ProjectFilePathResolver;
import com.stat.service.CodelistExtractionResult;
import com.stat.service.CodelistExtractionService;
import jakarta.annotation.Resource;
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
import java.util.*;

/**
 * VLM数据管理Controller
 */
@RestController
@RequestMapping("/api/vlm")

public class VlmDataController {

    private static final Logger logger = LoggerFactory.getLogger(VlmDataController.class);

    @Autowired
    private IVlmDataService vlmDataService;

    @Resource
    private VlmDataMapper vlmDataMapper;

    @Resource
    private PagesDataMapper pagesDataMapper;

    @Resource
    private ProjectSpecMapper projectSpecMapper;

    @Resource
    private CodelistDataMapper codelistDataMapper;

    @Resource
    private ProjectConfigMapper projectConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectFilePathResolver pathResolver;

    @Autowired
    private CodelistExtractionService codelistExtractionService;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadBasePath;

    /**
     * 分页查询VLM数据
     */
    @RequireProjectAccess("projectId")
    @GetMapping("/list")
    public CommonResult<PageCommonResult<VlmDataDTO>> getVlmDataList(
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "dataset", required = false) String dataset,
            @RequestParam(value = "variable", required = false) String variable,
            @RequestParam(value = "label", required = false) String label,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("projectId", projectId != null ? projectId : "");
            params.put("username", UserContext.getUsername());
            params.put("dataset", dataset != null ? dataset : "");
            params.put("variable", variable != null ? variable : "");
            params.put("label", label != null ? label : "");
            params.put("offset", (page - 1) * size);
            params.put("limit", size);
            
            PageCommonResult<VlmDataDTO> result = vlmDataService.getVlmDataByPage(params);
            
            // 如果分页服务返回空数据但项目ID存在，则尝试手动分页
            if ((result == null || result.getData() == null || result.getData().isEmpty()) && projectId != null && !projectId.trim().isEmpty()) {
                System.out.println("分页查询结果为空，尝试手动分页...");
                List<VlmDataDTO> allData = vlmDataService.getVlmDataByProjectId(projectId);
                if (allData != null && !allData.isEmpty()) {
                    int startIndex = (page - 1) * size;
                    int endIndex = Math.min(startIndex + size, allData.size());
                    List<VlmDataDTO> pageData = allData.subList(startIndex, endIndex);
                    
                    PageCommonResult<VlmDataDTO> manualResult = new PageCommonResult<>();
                    manualResult.setData(pageData);
                    manualResult.setPageSize(size);
                    manualResult.setPageNum(page);
                    manualResult.setTotalCount((long)allData.size());
                    manualResult.setSuccess(true);
                    manualResult.setCode("0");
                    manualResult.setMessage("");
                    
                    return CommonResult.success(manualResult);
                }
            }
            
            return CommonResult.success(result);
            
        } catch (Exception e) {
            System.out.println("VLM分页查询异常: " + e.getMessage());
            e.printStackTrace();
            return CommonResult.failed("查询VLM数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据项目ID查询所有VLM数据
     */
    @RequireProjectAccess("projectId")
    @GetMapping("/project/{projectId}")
    public CommonResult<List<VlmDataDTO>> getVlmDataByProject(@PathVariable String projectId) {
        try {
            List<VlmDataDTO> result = vlmDataService.getVlmDataByProjectId(projectId);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("查询项目VLM数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据项目ID和数据集查询VLM数据
     */
    @GetMapping("/project/{projectId}/dataset/{dataset}")
    public CommonResult<List<VlmDataDTO>> getVlmDataByProjectAndDataset(
            @PathVariable String projectId, 
            @PathVariable String dataset) {
        try {
            List<VlmDataDTO> result = vlmDataService.getVlmDataByProjectIdAndDataset(projectId, dataset);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("查询数据集VLM数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询VLM数据详情
     */
    @GetMapping("/{id}")
    public CommonResult<VlmDataDTO> getVlmDataById(@PathVariable Long id) {
        try {
            VlmDataDTO result = vlmDataService.getVlmDataById(id);
            if (result != null) {
                return CommonResult.success(result);
            } else {
                return CommonResult.failed("VLM数据不存在");
            }
        } catch (Exception e) {
            return CommonResult.failed("查询VLM数据详情失败: " + e.getMessage());
        }
    }

    /**
     * 新增VLM数据
     */
    @PostMapping
    public CommonResult<String> addVlmData(@RequestBody VlmDataDTO vlmDataDTO) {
        try {
            boolean success = vlmDataService.addVlmData(vlmDataDTO);
            if (success) {
                return CommonResult.success("新增VLM数据成功");
            } else {
                return CommonResult.failed("新增VLM数据失败");
            }
        } catch (Exception e) {
            return CommonResult.failed("新增VLM数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新VLM数据
     */
    @PutMapping("/{id}")
    public CommonResult<String> updateVlmData(@PathVariable Long id, @RequestBody VlmDataDTO vlmDataDTO) {
        try {
            vlmDataDTO.setId(id);
            boolean success = vlmDataService.updateVlmData(vlmDataDTO);
            if (success) {
                return CommonResult.success("更新VLM数据成功");
            } else {
                return CommonResult.failed("更新VLM数据失败");
            }
        } catch (Exception e) {
            return CommonResult.failed("更新VLM数据失败: " + e.getMessage());
        }
    }

    /**
     * 删除VLM数据
     */
    @DeleteMapping("/{id}")
    public CommonResult<String> deleteVlmData(@PathVariable Long id) {
        try {
            boolean success = vlmDataService.deleteVlmData(id);
            if (success) {
                return CommonResult.success("删除VLM数据成功");
            } else {
                return CommonResult.failed("删除VLM数据失败");
            }
        } catch (Exception e) {
            return CommonResult.failed("删除VLM数据失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新排序顺序
     */
    @PutMapping("/sort-order")
    public CommonResult<String> updateSortOrder(@RequestBody List<Map<String, Object>> sortOrderList) {
        try {
            boolean success = vlmDataService.batchUpdateSortOrder(sortOrderList);
            if (success) {
                return CommonResult.success("更新排序顺序成功");
            } else {
                return CommonResult.failed("更新排序顺序失败");
            }
        } catch (Exception e) {
            return CommonResult.failed("更新排序顺序失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目中所有不同的数据集列表
     */
    @GetMapping("/datasets/{projectId}")
    public CommonResult<List<String>> getDatasetsByProject(@PathVariable String projectId) {
        try {
            List<String> datasets = vlmDataService.getDistinctDatasetsByProjectId(projectId);
            return CommonResult.success(datasets);
        } catch (Exception e) {
            return CommonResult.failed("获取数据集列表失败: " + e.getMessage());
        }
    }

    /**
     * VLM控制器健康检查
     */
    @GetMapping("/health")
    public CommonResult<String> healthCheck() {
        try {
            System.out.println("VLM控制器健康检查被调用");
            return CommonResult.success("VLM控制器运行正常");
        } catch (Exception e) {
            System.out.println("VLM控制器健康检查异常: " + e.getMessage());
            e.printStackTrace();
            return CommonResult.failed("VLM控制器健康检查失败: " + e.getMessage());
        }
    }

    /**
     * 测试数据提取环境
     * 用于调试Python环境和依赖问题
     */
    @PostMapping("/test-extract")
    public CommonResult<String> testExtractEnvironment(@RequestBody Map<String, String> request) {
        System.out.println("收到测试提取环境请求: " + request);
        
        try {
            String projectId = request.get("projectId");
            System.out.println("测试环境，项目ID: " + projectId);

            String pythonTestScriptPath = pythonPath + "/define/test_extraction.py";
            
            // 尝试多个Python可执行文件
            String[] pythonExecutables = {"python", "python3", "py"};
            String pythonExecutable = null;
            
            for (String exec : pythonExecutables) {
                try {
                    Process testProcess = new ProcessBuilder(exec, "--version").start();
                    int testExitCode = testProcess.waitFor();
                    if (testExitCode == 0) {
                        pythonExecutable = exec;
                        System.out.println("找到Python可执行文件: " + exec);
                        break;
                    }
                } catch (Exception ignored) {
                    // 继续尝试下一个
                }
            }
            
            if (pythonExecutable == null) {
                return CommonResult.failed("未找到可用的Python可执行文件");
            }

            // 检查测试脚本是否存在
            File testScriptFile = new File(pythonTestScriptPath);
            if (!testScriptFile.exists()) {
                return CommonResult.failed("测试脚本不存在: " + pythonTestScriptPath);
            }

            // 构建命令
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, pythonTestScriptPath);
            processBuilder.directory(new File(pythonPath + "/define"));
            processBuilder.redirectErrorStream(true);

            // 设置环境变量
            Map<String, String> env = processBuilder.environment();
            env.put("PROJECT_ID", projectId != null ? projectId : "TEST");
            env.put("PYTHONIOENCODING", "utf-8");

            // 启动进程
            Process process = processBuilder.start();
            System.out.println("测试进程已启动");

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("测试输出: " + line);
                }
            }

            // 等待进程完成
            boolean finished = false;
            try {
                finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroy();
                return CommonResult.failed("测试被中断");
            }
            
            if (!finished) {
                process.destroy();
                return CommonResult.failed("测试超时");
            }

            int exitCode = process.exitValue();
            System.out.println("测试进程结束，退出代码: " + exitCode);

            String resultMessage = "环境测试完成，退出代码: " + exitCode;
            if (output.length() > 0) {
                resultMessage += "\n测试结果:\n" + output.toString();
            }

            if (exitCode == 0) {
                return CommonResult.success(resultMessage);
            } else {
                return CommonResult.failed(resultMessage);
            }

        } catch (Exception e) {
            String errorMessage = "环境测试失败: " + e.getMessage();
            System.out.println("测试异常: " + errorMessage);
            e.printStackTrace();
            return CommonResult.failed(errorMessage);
        }
    }

    /**
     * Step 1: 提取 Where Clause（VLM数据提取 + Origin/Source回填 + Order重排）
     */
    @PostMapping("/extract-vlm")
    public CommonResult<String> extractVlmData(@RequestBody Map<String, String> request) {
        try {
            String projectId = request.get("projectId");
            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.failed("项目ID不能为空");
            }

            String pythonExecutable = findPythonExecutable();
            if (pythonExecutable == null) {
                return CommonResult.failed("未找到可用的Python");
            }
            String currentUser = UserContext.getUsername();

            String vlmScript = pythonPath + "/define/vlm_codelist_extractor.py";
            if (!new File(vlmScript).exists()) {
                return CommonResult.failed("VLM提取脚本不存在: " + vlmScript);
            }

            int vlmExit = runPythonScript(pythonExecutable, vlmScript, projectId, currentUser, 300);
            if (vlmExit != 0) {
                return CommonResult.failed("VLM数据提取失败，退出代码: " + vlmExit);
            }

            List<VlmDataPO> vlmList = vlmDataMapper.selectByProjectId(projectId, currentUser);
            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, currentUser);
            Map<String, ProjectSpecPO> specMap = new HashMap<>();
            for (ProjectSpecPO spec : specList) {
                String ds = spec.getDomain() != null ? spec.getDomain().toUpperCase().trim() : "";
                String var = spec.getVariable() != null ? spec.getVariable().toUpperCase().trim() : "";
                if (!ds.isEmpty() && !var.isEmpty()) specMap.put(ds + "|" + var, spec);
            }
            int osMatched = 0;
            Map<String, Integer> dsCounter = new HashMap<>();
            for (VlmDataPO vlm : vlmList) {
                String ds = vlm.getDataset() != null ? vlm.getDataset().toUpperCase().trim() : "";
                String var = vlm.getVariable() != null ? vlm.getVariable().toUpperCase().trim() : "";
                int order = dsCounter.merge(ds, 1, Integer::sum);
                vlm.setSortOrder(order);
                vlm.setMandatory("No");
                ProjectSpecPO spec = specMap.get(ds + "|" + var);
                if (spec != null) {
                    vlm.setOrigin(spec.getOrigin());
                    vlm.setSource(spec.getSource());
                    osMatched++;
                }
                vlmDataMapper.updateById(vlm);
            }

            return CommonResult.success(String.format(
                    "Where Clause 提取完成：%d 条，Origin/Source匹配 %d 条", vlmList.size(), osMatched));
        } catch (Exception e) {
            return CommonResult.failed("提取Where Clause失败: " + e.getMessage());
        }
    }

    /**
     * Step 2: 提取 XPT 字段（Data Type / Length / Significant Digits / Format）
     */
    @PostMapping("/extract-xpt-fields/{projectId}")
    public CommonResult<String> extractXptFields(@PathVariable String projectId) {
        try {
            String pythonExecutable = findPythonExecutable();
            if (pythonExecutable == null) return CommonResult.failed("未找到可用的Python");

            String script = pythonPath + "/define/vlm_xpt_metadata.py";
            if (!new File(script).exists()) return CommonResult.failed("脚本不存在: " + script);

            String currentUser = UserContext.getUsername();
            int exitCode = runPythonScript(pythonExecutable, script, projectId, currentUser, 120);
            if (exitCode == 0) {
                return CommonResult.success("XPT字段提取完成（Data Type / Length / Significant Digits / Format）");
            } else {
                return CommonResult.failed("XPT字段提取失败，退出代码: " + exitCode);
            }
        } catch (Exception e) {
            return CommonResult.failed("提取XPT字段失败: " + e.getMessage());
        }
    }

    /**
     * Step 3: 提取 Codelist
     */
    @RequireProjectAccess("projectId")
    @PostMapping("/extract-vlm-codelist/{projectId}")
    public CommonResult<CodelistExtractionResult> extractVlmCodelist(@PathVariable String projectId) {
        try {
            CodelistExtractionResult result = codelistExtractionService.extract(
                    projectId, UserContext.getUsername(), CodelistExtractionService.Scope.VLM);
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("统一提取 VLM Codelist 失败", e);
            return CommonResult.failed("提取Codelist失败: " + e.getMessage());
        }
    }

    private CommonResult<String> extractVlmCodelistLegacy(String projectId) {
        try {
            String pythonExecutable = findPythonExecutable();
            if (pythonExecutable == null) return CommonResult.failed("未找到可用的Python");

            String script = pythonPath + "/define/vlm_codelist_fill.py";
            if (!new File(script).exists()) return CommonResult.failed("脚本不存在: " + script);

            String currentUser = UserContext.getUsername();
            int exitCode = runPythonScript(pythonExecutable, script, projectId, currentUser, 120);
            if (exitCode != 0) {
                return CommonResult.failed("Codelist 提取失败，退出代码: " + exitCode);
            }

            // After vlm_codelist_fill.py sets sas_vlm_data.codelist, now extract actual terms into sas_codelist_data
            int termCount = extractVlmTermsToCodelistData(projectId, currentUser, pythonExecutable);

            return CommonResult.success(String.format("VLM Codelist 提取完成，写入 %d 条Term", termCount));
        } catch (Exception e) {
            return CommonResult.failed("提取Codelist失败: " + e.getMessage());
        }
    }

    private int extractVlmTermsToCodelistData(String projectId, String username, String pythonExec) {
        // Delete existing VLM-level codelist data
        jdbcTemplate.update(
                "DELETE FROM sas_codelist_data WHERE project_id = ? AND username = ? AND created_by = 'extract_vlm_codelist'",
                projectId, username);

        List<VlmDataPO> vlmRows = vlmDataMapper.selectByProjectId(projectId, username);
        if (vlmRows == null) return 0;

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
        // CT term: "nciCodelistCode|submVal_upper" -> termCode
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
                    if (!submVal.isEmpty()) {
                        ctTermLookup.put(clCode.toUpperCase() + "|" + submVal.toUpperCase(), termCode);
                    }
                }
            }
            logger.info("[vlm-codelist] CT loaded: {} headers, {} terms", ctHeaderMap.size(), ctTermLookup.size());
        }

        String xptDir = pathResolver.xptDirectory(projectId, standardType).toString();
        int totalTerms = 0;
        // VLM always starts from sort_order = 500000 (Variables uses 1..N)
        int globalOrder = 500000;

        // Each VLM row is its OWN origin block (dataset.variable.VLM)
        // Group VLM rows by codelist ID for shared NCI Codelist Code
        // Only accept dot-containing IDs (DS.VAR.FILTERVAL) to avoid colliding with Variables-level vcd
        Map<String, List<VlmDataPO>> clGroups = new java.util.LinkedHashMap<>();
        for (VlmDataPO vlm : vlmRows) {
            String clId = vlm.getCodelist() != null ? vlm.getCodelist().trim() : "";
            if (clId.isEmpty()) continue;
            if (!clId.contains(".")) {
                logger.warn("[vlm-codelist] Skipping codelist '{}' - not in DS.VAR.FILTERVAL format", clId);
                continue;
            }
            // Skip TS dataset entries (TS.TSVAL.xx)
            String dsPrefix = clId.split("\\.")[0].toUpperCase();
            if ("TS".equals(dsPrefix)) {
                logger.info("[vlm-codelist] Skipping TS codelist '{}'", clId);
                continue;
            }
            clGroups.computeIfAbsent(clId, k -> new ArrayList<>()).add(vlm);
        }

        for (Map.Entry<String, List<VlmDataPO>> entry : clGroups.entrySet()) {
            String clId = entry.getKey();
            List<VlmDataPO> group = entry.getValue();
            String vlmLabel = group.get(0).getLabel() != null ? group.get(0).getLabel().trim() : "";

            // CT header lookup by clId
            String vcdKey = clId.contains(".") ? clId.substring(clId.lastIndexOf('.') + 1) : clId;
            Map<String, String> ctHdr = ctHeaderMap.get(vcdKey.toUpperCase());
            String nciClCode = ctHdr != null ? ctHdr.get("nciCodelistCode") : null;

            // Collect all terms for this codelist first, then sort before inserting
            // Each entry: [term, nciLookupKey, originStr]
            List<String[]> collectedTerms = new ArrayList<>();
            Set<String> seenTerms = new LinkedHashSet<>();

            for (VlmDataPO vlm : group) {
                String dataset = vlm.getDataset() != null ? vlm.getDataset().trim() : "";
                String variable = vlm.getVariable() != null ? vlm.getVariable().trim().toUpperCase() : "";
                String wc = vlm.getWhereClause() != null ? vlm.getWhereClause().trim() : "";

                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\w+)\\s+EQ\\s+\"([^\"]*)\"").matcher(wc);
                if (!m.find()) continue;
                String filterCol = m.group(1).toUpperCase();
                String filterVal = m.group(2);

                String originStr = dataset + "." + variable + ".VLM";
                boolean isTs = dataset.toUpperCase().equals("TS");
                String strescCol = variable.endsWith("ORRES") ? variable.substring(0, variable.length() - 5) + "STRESC" : variable;

                try {
                    java.io.File xptFile = findXptFile(new java.io.File(xptDir), dataset);
                    if (xptFile == null) continue;

                    String script;
                    if (isTs) {
                        script = String.format(
                                "import pyreadstat,sys\n" +
                                "try:\n" +
                                "    d,_=pyreadstat.read_xport(sys.argv[1]);d.columns=d.columns.str.upper()\n" +
                                "    sc='%s';vc='%s';fv='%s';fc='%s'\n" +
                                "    c=sc if sc in d.columns else vc\n" +
                                "    mask=d[fc].astype(str).str.strip()==fv if fc in d.columns else [False]*len(d)\n" +
                                "    sub=d.loc[mask]\n" +
                                "    seen=dict()\n" +
                                "    for i in range(len(sub)):\n" +
                                "        v=str(sub[c].iloc[i]).strip() if str(sub[c].iloc[i]).strip()!='' and str(sub[c].iloc[i])!='nan' else ''\n" +
                                "        if not v or v in seen: continue\n" +
                                "        seen[v]=1\n" +
                                "        tv=str(sub['TSVALCD'].iloc[i]).strip() if 'TSVALCD' in sub.columns and str(sub['TSVALCD'].iloc[i])!='nan' else ''\n" +
                                "        print(v+'\\t'+tv)\n" +
                                "except Exception as e:\n" +
                                "    sys.stderr.write(str(e)+'\\n')\n",
                                strescCol, variable, filterVal, filterCol);
                    } else {
                        script = String.format(
                                "import pyreadstat,sys\n" +
                                "def _is_num(s):\n" +
                                "    try: float(s); return True\n" +
                                "    except: return False\n" +
                                "try:\n" +
                                "    d,_=pyreadstat.read_xport(sys.argv[1]);d.columns=d.columns.str.upper()\n" +
                                "    sc='%s';vc='%s';fv='%s';fc='%s'\n" +
                                "    c=sc if sc in d.columns else vc\n" +
                                "    mask=d[fc].astype(str).str.strip()==fv if fc in d.columns else [False]*len(d)\n" +
                                "    vals=d.loc[mask,c].dropna().astype(str).str.strip()\n" +
                                "    seen=dict()\n" +
                                "    for v in vals:\n" +
                                "        if v and v not in seen and not _is_num(v):\n" +
                                "            seen[v]=1;print(v)\n" +
                                "except Exception as e:\n" +
                                "    sys.stderr.write(str(e)+'\\n')\n",
                                strescCol, variable, filterVal, filterCol);
                    }

                    ProcessBuilder pb = new ProcessBuilder(pythonExec, "-c", script, xptFile.getAbsolutePath());
                    pb.environment().put("PYTHONIOENCODING", "utf-8");
                    Process proc = pb.start();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty()) continue;

                            String term;
                            String nciLookupKey;
                            if (isTs && line.contains("\t")) {
                                String[] parts = line.split("\t", 2);
                                term = parts[0].trim();
                                nciLookupKey = parts.length > 1 && !parts[1].trim().isEmpty() ? parts[1].trim() : term;
                            } else {
                                term = line;
                                nciLookupKey = term;
                            }
                            if (term.isEmpty() || seenTerms.contains(term)) continue;
                            seenTerms.add(term);
                            collectedTerms.add(new String[]{term, nciLookupKey, originStr});
                        }
                    }
                    proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    logger.warn("[vlm-codelist] Error extracting {}: {}", clId, e.getMessage());
                }
            }

            // Sort collected terms before inserting
            collectedTerms.sort((a, b) -> {
                int wa = vlmTermWeight(a[0]);
                int wb = vlmTermWeight(b[0]);
                if (wa != wb) return Integer.compare(wa, wb);
                return a[0].compareToIgnoreCase(b[0]);
            });

            // Insert in sorted order
            int cdnum = 1;
            for (String[] rec : collectedTerms) {
                String term = rec[0];
                String nciLookupKey = rec[1];
                String originStr = rec[2];

                globalOrder++;
                CodelistDataPO po = new CodelistDataPO();
                po.setProjectId(projectId);
                po.setUsername(username);
                po.setVcd(clId);
                po.setVlabel(vlmLabel);
                po.setNciCodelistCode(nciClCode);
                po.setType("Char");
                po.setTerminology(nciClCode != null ? terminologyLabel : null);
                po.setCdnum(cdnum++);
                po.setCode(term);

                String scopedKey = (nciClCode != null && !nciClCode.isEmpty())
                        ? nciClCode.toUpperCase() + "|" + nciLookupKey.toUpperCase() : null;
                String nciTermCode = scopedKey != null ? ctTermLookup.get(scopedKey) : null;
                po.setNciTermCode(nciTermCode);

                po.setCodeDes("");
                po.setOrigin(originStr);
                po.setSortOrder(globalOrder);
                po.setCreatedBy("extract_vlm_codelist");
                codelistDataMapper.insert(po);
                totalTerms++;
            }
        }
        return totalTerms;
    }

    /** Sort weight for VLM term (lower = earlier). */
    private int vlmTermWeight(String term) {
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

    private java.io.File findXptFile(java.io.File dir, String domain) {
        if (domain == null || domain.isEmpty() || !dir.exists()) return null;
        java.io.File f = new java.io.File(dir, domain.toLowerCase() + ".xpt");
        if (f.exists()) return f;
        f = new java.io.File(dir, domain.toUpperCase() + ".xpt");
        if (f.exists()) return f;
        java.io.File[] files = dir.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.getName().toLowerCase().endsWith("_" + domain.toLowerCase() + ".xpt")) return file;
            }
        }
        return null;
    }

    private String findPythonExecutable() {
        for (String exec : new String[]{"python", "python3", "py"}) {
            try {
                Process p = new ProcessBuilder(exec, "--version").start();
                if (p.waitFor() == 0) return exec;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private int runPythonScript(String pythonExec, String scriptPath, String projectId,
                                String username, int timeoutSec) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(pythonExec, scriptPath);
        pb.directory(new File(pythonPath + "/define"));
        pb.redirectErrorStream(true);
        Map<String, String> env = pb.environment();
            env.put("PROJECT_ID", projectId);
            env.put("PYTHONIOENCODING", "utf-8");
        env.put("PYTHON_BASE_PATH", pythonPath);
        env.put("UPLOAD_BASE_PATH", uploadBasePath);
        String standardType = pathResolver.resolveStandardType(projectId, null);
        env.put("DATA_PATH", pathResolver.xptDirectory(projectId, standardType).toString());
        env.put("OUTPUT_PATH", pathResolver.extractionOutputDirectory(projectId, standardType).toString());
        if (username != null && !username.isEmpty()) {
            env.put("USERNAME_CONTEXT", username);
        }
        Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                System.out.println("[py] " + line);
            }
        }
        boolean finished = process.waitFor(timeoutSec, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) { process.destroy(); return -1; }
        return process.exitValue();
    }

    @GetMapping("/export-xlsx/{projectId}")
    public ResponseEntity<byte[]> exportVlmXlsx(@PathVariable String projectId) {
        try {
            List<VlmDataDTO> dataList = vlmDataService.getVlmDataByProjectId(projectId);
            String[] headers = {
                "Order", "Dataset", "Variable", "Where Clause", "Label",
                "Data Type", "Length", "Significant Digits", "Format", "Mandatory",
                "Assigned Value", "Codelist", "Origin", "Source", "Pages",
                "Method", "Predecessor", "Comment", "Developer Notes"
            };

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("ValueLevel");
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
                    VlmDataDTO d = dataList.get(i);
                    Row row = sheet.createRow(i + 1);
                    int c = 0;
                    row.createCell(c++).setCellValue(d.getSortOrder() != null ? d.getSortOrder() : i + 1);
                    row.createCell(c++).setCellValue(d.getDataset() != null ? d.getDataset() : "");
                    row.createCell(c++).setCellValue(d.getVariable() != null ? d.getVariable() : "");
                    row.createCell(c++).setCellValue(d.getWhereClause() != null ? d.getWhereClause() : "");
                    row.createCell(c++).setCellValue(d.getLabel() != null ? d.getLabel() : "");
                    row.createCell(c++).setCellValue(d.getDataType() != null ? d.getDataType() : "");
                    row.createCell(c++).setCellValue(d.getLength() != null ? d.getLength() : "");
                    row.createCell(c++).setCellValue(d.getSignificantDigits() != null ? d.getSignificantDigits() : "");
                    row.createCell(c++).setCellValue(d.getFormat() != null ? d.getFormat() : "");
                    row.createCell(c++).setCellValue(d.getMandatory() != null ? d.getMandatory() : "No");
                    row.createCell(c++).setCellValue(d.getAssignedValue() != null ? d.getAssignedValue() : "");
                    row.createCell(c++).setCellValue(d.getCodelist() != null ? d.getCodelist() : "");
                    row.createCell(c++).setCellValue(d.getOrigin() != null ? d.getOrigin() : "");
                    row.createCell(c++).setCellValue(d.getSource() != null ? d.getSource() : "");
                    row.createCell(c++).setCellValue(d.getPages() != null ? d.getPages() : "");
                    row.createCell(c++).setCellValue(d.getMethod() != null ? d.getMethod() : "");
                    row.createCell(c++).setCellValue(d.getPredecessor() != null ? d.getPredecessor() : "");
                    row.createCell(c++).setCellValue(d.getComment() != null ? d.getComment() : "");
                    row.createCell(c++).setCellValue(d.getDeveloperNotes() != null ? d.getDeveloperNotes() : "");
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                workbook.write(out);
                byte[] bytes = out.toByteArray();

                String fileName = URLEncoder.encode("VLM_" + projectId + ".xlsx", StandardCharsets.UTF_8);
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
    public CommonResult<String> importVlmXlsx(@PathVariable String projectId,
                                               @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sheets = (List<Map<String, Object>>) request.get("sheets");
            if (sheets == null || sheets.isEmpty()) {
                return CommonResult.failed("无有效工作表数据");
            }

            vlmDataService.deleteByProjectId(projectId);

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
                    String dataset = row.getOrDefault(1, "").trim();
                    String variable = row.getOrDefault(2, "").trim();
                    if (dataset.isEmpty() && variable.isEmpty()) continue;

                    VlmDataDTO dto = new VlmDataDTO();
                    dto.setProjectId(projectId);
                    dto.setSortOrder(importCount + 1);
                    dto.setDataset(dataset);
                    dto.setVariable(variable);
                    dto.setWhereClause(row.getOrDefault(3, "").trim());
                    dto.setLabel(row.getOrDefault(4, "").trim());
                    dto.setDataType(row.getOrDefault(5, "").trim());
                    dto.setLength(row.getOrDefault(6, "").trim());
                    dto.setSignificantDigits(row.getOrDefault(7, "").trim());
                    dto.setFormat(row.getOrDefault(8, "").trim());
                    dto.setMandatory(row.getOrDefault(9, "").trim());
                    dto.setAssignedValue(row.getOrDefault(10, "").trim());
                    dto.setCodelist(row.getOrDefault(11, "").trim());
                    dto.setOrigin(row.getOrDefault(12, "").trim());
                    dto.setSource(row.getOrDefault(13, "").trim());
                    dto.setPages(row.getOrDefault(14, "").trim());
                    dto.setMethod(row.getOrDefault(15, "").trim());
                    dto.setPredecessor(row.getOrDefault(16, "").trim());
                    dto.setComment(row.getOrDefault(17, "").trim());
                    dto.setDeveloperNotes(row.getOrDefault(18, "").trim());
                    vlmDataService.addVlmData(dto);
                    importCount++;
                }
                break;
            }
            return CommonResult.success("导入成功，共 " + importCount + " 条VLM数据");
        } catch (Exception e) {
            return CommonResult.failed("导入VLM数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/extract-vlm-pages/{projectId}")
    public CommonResult<String> extractVlmPages(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            List<VlmDataPO> vlmList = vlmDataMapper.selectByProjectId(projectId, username);
            if (vlmList == null || vlmList.isEmpty()) {
                return CommonResult.failed("未找到 VLM 数据");
            }

            List<PagesDataPO> pagesList = pagesDataMapper.selectByProjectId(projectId, username);

            // Only use VLM_WhereClause pages: key = DS|VAR|WHERE_CLAUSE
            Map<String, String> vlmPagesMap = new HashMap<>();
            for (PagesDataPO p : pagesList) {
                String origin = p.getOrigin() != null ? p.getOrigin().trim() : "";
                if (!"VLM_WhereClause".equals(origin)) continue;

                String ds = p.getDataset() != null ? p.getDataset().toUpperCase().trim() : "";
                String var = p.getVariable() != null ? p.getVariable().toUpperCase().trim() : "";
                String pg = p.getPages() != null ? p.getPages().trim() : "";
                String wc = p.getWhereClause() != null ? p.getWhereClause().trim() : "";
                if (ds.isEmpty() || var.isEmpty() || pg.isEmpty() || wc.isEmpty()) continue;

                vlmPagesMap.put(ds + "|" + var + "|" + wc, pg);
            }

            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);
            Map<String, String> specSourceMap = new HashMap<>();
            for (ProjectSpecPO spec : specList) {
                String ds = spec.getDomain() != null ? spec.getDomain().toUpperCase().trim() : "";
                String var = spec.getVariable() != null ? spec.getVariable().toUpperCase().trim() : "";
                String src = spec.getSource() != null ? spec.getSource().trim() : "";
                if (!ds.isEmpty() && !var.isEmpty()) {
                    specSourceMap.put(ds + "|" + var, src);
                }
            }

            int matchCount = 0;
            int skippedSupp = 0;
            for (VlmDataPO vlm : vlmList) {
                String ds = vlm.getDataset() != null ? vlm.getDataset().toUpperCase().trim() : "";
                if (ds.startsWith("SUPP")) { skippedSupp++; continue; }

                String var = vlm.getVariable() != null ? vlm.getVariable().toUpperCase().trim() : "";
                String wc = vlm.getWhereClause() != null ? vlm.getWhereClause().trim() : "";
                String varSource = specSourceMap.getOrDefault(ds + "|" + var, "");

                if (varSource.equalsIgnoreCase("Investigator") && !wc.isEmpty()) {
                    String pages = vlmPagesMap.get(ds + "|" + var + "|" + wc);
                    if (pages != null) {
                        vlm.setPages(pages);
                        vlmDataMapper.updateById(vlm);
                        matchCount++;
                    }
                }
            }

            String suppMsg = skippedSupp > 0 ? String.format("，跳过 SUPPxx %d 条", skippedSupp) : "";
            return CommonResult.success(String.format(
                    "VLM Pages 提取完成：VLM级Pages数据 %d 条，匹配填充 %d 条%s",
                    vlmPagesMap.size(), matchCount, suppMsg));
        } catch (Exception e) {
            return CommonResult.failed("提取VLM Pages失败: " + e.getMessage());
        }
    }

    @PostMapping("/fill-origin-source/{projectId}")
    public CommonResult<String> fillOriginSource(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            List<VlmDataPO> vlmList = vlmDataMapper.selectByProjectId(projectId, username);
            if (vlmList == null || vlmList.isEmpty()) {
                return CommonResult.failed("未找到 VLM 数据");
            }

            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);
            Map<String, ProjectSpecPO> specMap = new HashMap<>();
            for (ProjectSpecPO spec : specList) {
                String ds = spec.getDomain() != null ? spec.getDomain().toUpperCase().trim() : "";
                String var = spec.getVariable() != null ? spec.getVariable().toUpperCase().trim() : "";
                if (!ds.isEmpty() && !var.isEmpty()) {
                    specMap.put(ds + "|" + var, spec);
                }
            }

            int matchCount = 0;
            for (VlmDataPO vlm : vlmList) {
                String ds = vlm.getDataset() != null ? vlm.getDataset().toUpperCase().trim() : "";
                String var = vlm.getVariable() != null ? vlm.getVariable().toUpperCase().trim() : "";
                ProjectSpecPO spec = specMap.get(ds + "|" + var);
                if (spec != null) {
                    vlm.setOrigin(spec.getOrigin());
                    vlm.setSource(spec.getSource());
                    vlmDataMapper.updateById(vlm);
                    matchCount++;
                }
            }

            return CommonResult.success(String.format("Origin/Source 填充完成，匹配更新 %d 条", matchCount));
        } catch (Exception e) {
            return CommonResult.failed("填充Origin/Source失败: " + e.getMessage());
        }
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
}
