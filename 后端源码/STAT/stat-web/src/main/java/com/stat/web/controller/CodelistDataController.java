package com.stat.web.controller;

import com.stat.common.dto.CodelistDataDTO;
import com.stat.common.result.CommonResult;
import com.stat.common.result.PageCommonResult;
import com.stat.common.security.UserContext;
import com.stat.dal.mapper.CodelistDataMapper;
import com.stat.dal.mapper.ProjectConfigMapper;
import com.stat.dal.mapper.ProjectSpecMapper;
import com.stat.dal.mapper.VlmDataMapper;
import com.stat.dal.po.CodelistDataPO;
import com.stat.dal.po.ProjectConfigPO;
import com.stat.dal.po.ProjectSpecPO;
import com.stat.dal.po.VlmDataPO;
import com.stat.service.ICodelistDataService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CodeList数据管理Controller
 */
@RestController
@RequestMapping("/api/codelist")

public class CodelistDataController {

    @Autowired
    private ICodelistDataService codelistDataService;

    @Autowired
    private CodelistDataMapper codelistDataMapper;

    @Autowired
    private ProjectConfigMapper projectConfigMapper;

    @Autowired
    private ProjectSpecMapper projectSpecMapper;

    @Autowired
    private VlmDataMapper vlmDataMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.python.path:C:/Project_Web/019_defineXML/Python}")
    private String pythonPath;

    @Value("${app.upload.path:C:/Project_Web/019_defineXML/uploads}")
    private String uploadBasePath;

    /**
     * 分页查询CodeList数据
     */
    @GetMapping("/list")
    public CommonResult<PageCommonResult<CodelistDataDTO>> getCodelistDataList(
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "vcd", required = false) String vcd,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "vlabel", required = false) String vlabel,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        
        System.out.println("=== CodeList数据查询请求 ===");
        System.out.println("projectId: " + projectId);
        System.out.println("vcd: " + vcd);
        System.out.println("code: " + code);
        System.out.println("vlabel: " + vlabel);
        System.out.println("page: " + page + ", size: " + size);
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("projectId", projectId != null ? projectId : "");
            params.put("vcd", vcd != null ? vcd : "");
            params.put("code", code != null ? code : "");
            params.put("vlabel", vlabel != null ? vlabel : "");
            params.put("offset", (page - 1) * size);
            params.put("limit", size);
            
            System.out.println("查询参数: " + params);
            
            PageCommonResult<CodelistDataDTO> result = codelistDataService.getCodelistDataByPage(params);
            
            System.out.println("查询结果: " + (result != null ? "成功" : "空结果"));
            if (result != null) {
                System.out.println("数据条数: " + (result.getData() != null ? result.getData().size() : 0));
                System.out.println("总记录数: " + result.getTotalCount());
            }
            
            return CommonResult.success(result);
            
        } catch (Exception e) {
            System.out.println("查询异常: " + e.getMessage());
            e.printStackTrace();
            return CommonResult.failed("查询CodeList数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据项目ID查询所有CodeList数据
     */
    @GetMapping("/project/{projectId}")
    public CommonResult<List<CodelistDataDTO>> getCodelistDataByProject(@PathVariable String projectId) {
        try {
            List<CodelistDataDTO> result = codelistDataService.getCodelistDataByProjectId(projectId);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("查询项目CodeList数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据项目ID和VCD查询CodeList数据
     */
    @GetMapping("/project/{projectId}/vcd/{vcd}")
    public CommonResult<List<CodelistDataDTO>> getCodelistDataByProjectAndVcd(
            @PathVariable String projectId, 
            @PathVariable String vcd) {
        try {
            List<CodelistDataDTO> result = codelistDataService.getCodelistDataByProjectIdAndVcd(projectId, vcd);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("查询VCD CodeList数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询CodeList数据详情
     */
    @GetMapping("/{id}")
    public CommonResult<CodelistDataDTO> getCodelistDataById(@PathVariable Long id) {
        try {
            CodelistDataDTO result = codelistDataService.getCodelistDataById(id);
            if (result != null) {
                return CommonResult.success(result);
            } else {
                return CommonResult.failed("CodeList数据不存在");
            }
        } catch (Exception e) {
            return CommonResult.failed("查询CodeList数据详情失败: " + e.getMessage());
        }
    }

    /**
     * 新增CodeList数据
     */
    @PostMapping
    public CommonResult<String> addCodelistData(@RequestBody CodelistDataDTO codelistDataDTO) {
        try {
            boolean success = codelistDataService.addCodelistData(codelistDataDTO);
            if (success) {
                return CommonResult.success("新增CodeList数据成功");
            } else {
                return CommonResult.failed("新增CodeList数据失败");
            }
        } catch (Exception e) {
            return CommonResult.failed("新增CodeList数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新CodeList数据
     */
    @PutMapping("/{id}")
    public CommonResult<String> updateCodelistData(@PathVariable Long id, @RequestBody CodelistDataDTO codelistDataDTO) {
        try {
            codelistDataDTO.setId(id);
            boolean success = codelistDataService.updateCodelistData(codelistDataDTO);
            if (success) {
                return CommonResult.success("更新CodeList数据成功");
            } else {
                return CommonResult.failed("更新CodeList数据失败");
            }
        } catch (Exception e) {
            return CommonResult.failed("更新CodeList数据失败: " + e.getMessage());
        }
    }

    /**
     * 删除CodeList数据
     */
    @DeleteMapping("/{id}")
    public CommonResult<String> deleteCodelistData(@PathVariable Long id) {
        try {
            boolean success = codelistDataService.deleteCodelistData(id);
            if (success) {
                return CommonResult.success("删除CodeList数据成功");
            } else {
                return CommonResult.failed("删除CodeList数据失败");
            }
        } catch (Exception e) {
            return CommonResult.failed("删除CodeList数据失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/by-vcd/{projectId}")
    public CommonResult<String> deleteByVcd(@PathVariable String projectId, @RequestParam String vcd) {
        try {
            String username = UserContext.getUsername();
            int deleted = jdbcTemplate.update(
                    "DELETE FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                    projectId, username, vcd);
            clearCodelistRef(projectId, username, vcd);
            // Record deletion for re-extraction tracking
            ensureDeletedTable();
            try {
                jdbcTemplate.update(
                    "DELETE FROM sas_codelist_deleted WHERE project_id = ? AND username = ? AND vcd = ?",
                    projectId, username, vcd);
            } catch (Exception ignored) {}
            jdbcTemplate.update(
                    "INSERT INTO sas_codelist_deleted (project_id, username, vcd) VALUES (?, ?, ?)",
                    projectId, username, vcd);
            return CommonResult.success("已删除 " + deleted + " 条记录");
        } catch (Exception e) {
            return CommonResult.failed("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/deleted-vcds/{projectId}")
    public CommonResult<List<String>> getDeletedVcds(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            ensureDeletedTable();
            List<String> deleted = jdbcTemplate.queryForList(
                    "SELECT vcd FROM sas_codelist_deleted WHERE project_id = ? AND username = ? ORDER BY deleted_time DESC",
                    String.class, projectId, username);
            return CommonResult.success(deleted);
        } catch (Exception e) {
            return CommonResult.success(new ArrayList<>());
        }
    }

    @DeleteMapping("/deleted-vcds/{projectId}")
    public CommonResult<String> clearDeletedVcd(@PathVariable String projectId, @RequestParam String vcd) {
        try {
            String username = UserContext.getUsername();
            jdbcTemplate.update(
                    "DELETE FROM sas_codelist_deleted WHERE project_id = ? AND username = ? AND vcd = ?",
                    projectId, username, vcd);
            return CommonResult.success("已清除记录");
        } catch (Exception e) {
            return CommonResult.success("OK");
        }
    }

    @PostMapping("/batch-delete-marked/{projectId}")
    public CommonResult<String> batchDeleteMarked(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            ensureDeletedTable();
            List<String> markedVcds = jdbcTemplate.queryForList(
                    "SELECT vcd FROM sas_codelist_deleted WHERE project_id = ? AND username = ?",
                    String.class, projectId, username);
            if (markedVcds.isEmpty()) {
                return CommonResult.success("没有需要删除的标记");
            }
            int totalDeleted = 0;
            for (String vcd : markedVcds) {
                totalDeleted += jdbcTemplate.update(
                        "DELETE FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                        projectId, username, vcd);
                clearCodelistRef(projectId, username, vcd);
            }
            return CommonResult.success("已删除 " + markedVcds.size() + " 个Codelist，共 " + totalDeleted + " 条记录");
        } catch (Exception e) {
            return CommonResult.failed("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新排序顺序
     */
    @PutMapping("/sort-order")
    public CommonResult<String> updateSortOrder(@RequestBody List<Map<String, Object>> sortOrderList) {
        try {
            boolean success = codelistDataService.batchUpdateSortOrder(sortOrderList);
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
     * 获取项目中所有不同的VCD列表
     */
    @GetMapping("/vcds/{projectId}")
    public CommonResult<List<String>> getVcdsByProject(@PathVariable String projectId) {
        try {
            List<String> vcds = codelistDataService.getDistinctVcdsByProjectId(projectId);
            return CommonResult.success(vcds);
        } catch (Exception e) {
            return CommonResult.failed("获取VCD列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取VCD与Domain的映射关系
     */
    @GetMapping("/vcd-domains/{projectId}")
    public CommonResult<Map<String, Object>> getVcdDomainsMapping(@PathVariable String projectId) {
        try {
            System.out.println("获取VCD-Domain映射关系，项目ID: " + projectId);
            
            Map<String, Object> result = codelistDataService.getVcdDomainsMapping(projectId);
            
            System.out.println("VCD-Domain映射结果: " + result);
            return CommonResult.success(result);
        } catch (Exception e) {
            System.out.println("获取VCD-Domain映射失败: " + e.getMessage());
            e.printStackTrace();
            return CommonResult.failed("获取VCD域映射失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查和调试信息
     */
    @GetMapping("/health")
    public CommonResult<String> healthCheck() {
        try {
            System.out.println("CodeList健康检查被调用");
            
            // 测试数据库连接
            List<CodelistDataDTO> testData = codelistDataService.getCodelistDataByProjectId("MJR-MR001-01");
            
            String healthInfo = String.format(
                "CodeList控制器运行正常\n数据库连接正常\n项目MJR-MR001-01的数据条数: %d", 
                testData != null ? testData.size() : 0
            );
            
            System.out.println("健康检查结果: " + healthInfo);
            return CommonResult.success(healthInfo);
        } catch (Exception e) {
            System.out.println("健康检查异常: " + e.getMessage());
            e.printStackTrace();
            return CommonResult.failed("健康检查失败: " + e.getMessage());
        }
    }

    /**
     * CodeList数据提取和导入
     * 只处理CodeList数据的提取和导入
     */
    @PostMapping("/extract-codelist")
    public CommonResult<String> extractCodelistData(@RequestBody Map<String, String> request) {
        System.out.println("收到CodeList数据提取请求: " + request);
        
        try {
            String projectId = request.get("projectId");
            if (projectId == null || projectId.trim().isEmpty()) {
                System.out.println("项目ID为空");
                return CommonResult.failed("项目ID不能为空");
            }

            System.out.println("开始执行CodeList数据提取，项目ID: " + projectId);

            String pythonScriptPath = pythonPath + "/define/codelist_extractor.py";
            
            // 尝试多个Python可执行文件
            String[] pythonExecutables = {"python", "python3", "py"};
            String pythonExecutable = null;
            
            // 检查哪个Python命令可用
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
                System.out.println("未找到可用的Python可执行文件");
                return CommonResult.failed("未找到可用的Python可执行文件，请确保Python已安装并添加到系统PATH中");
            }

            // 检查Python脚本是否存在
            java.io.File scriptFile = new java.io.File(pythonScriptPath);
            if (!scriptFile.exists()) {
                System.out.println("CodeList提取脚本不存在: " + pythonScriptPath);
                return CommonResult.failed("CodeList提取脚本不存在: " + pythonScriptPath);
            }

            System.out.println("CodeList提取脚本存在，开始执行，使用Python: " + pythonExecutable);

            // 构建命令
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, pythonScriptPath);
            processBuilder.directory(new java.io.File(pythonPath + "/define"));
            processBuilder.redirectErrorStream(true);

            Map<String, String> env = processBuilder.environment();
            env.put("PROJECT_ID", projectId);
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHON_BASE_PATH", pythonPath);
            env.put("UPLOAD_BASE_PATH", uploadBasePath);
            env.put("DATA_PATH", uploadBasePath + "/" + projectId + "/xpt");
            env.put("OUTPUT_PATH", uploadBasePath + "/" + projectId + "/output");
            String currentUser = UserContext.getUsername();
            if (currentUser != null && !currentUser.isEmpty()) {
                env.put("USERNAME_CONTEXT", currentUser);
            }

            Process process = processBuilder.start();
            System.out.println("Python进程已启动, 用户: " + currentUser);

            // 读取输出
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("Python输出: " + line);
                }
            } catch (Exception e) {
                errorOutput.append("读取输出异常: ").append(e.getMessage());
                System.out.println("读取Python输出异常: " + e.getMessage());
                e.printStackTrace();
            }

            // 等待进程完成，设置超时时间  
            boolean finished = false;
            try {
                finished = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS); // 5分钟超时
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroy();
                return CommonResult.failed("CodeList数据提取被中断");
            }
            
            if (!finished) {
                process.destroy();
                System.out.println("Python进程超时被终止");
                return CommonResult.failed("CodeList数据提取超时（5分钟），进程已被终止。可能是数据文件过大或Python环境问题。");
            }

            int exitCode = process.exitValue();
            System.out.println("Python进程结束，退出代码: " + exitCode);

            if (exitCode == 0) {
                String successMessage = "CodeList数据提取和导入成功！";
                if (output.length() > 0) {
                    // 只返回关键信息，避免输出过长
                    String outputStr = output.toString();
                    if (outputStr.length() > 500) {
                        outputStr = outputStr.substring(outputStr.length() - 500) + "...(显示最后500字符)";
                    }
                    successMessage += " 执行日志: " + outputStr;
                }
                System.out.println("CodeList数据提取成功");
                return CommonResult.success(successMessage);
            } else {
                String failureMessage = "CodeList数据提取和导入失败，退出代码: " + exitCode;
                if (output.length() > 0) {
                    String outputStr = output.toString();
                    if (outputStr.length() > 1000) {
                        outputStr = outputStr.substring(outputStr.length() - 1000) + "...(显示最后1000字符)";
                    }
                    failureMessage += "，执行日志: " + outputStr;
                }
                if (errorOutput.length() > 0) {
                    failureMessage += "，错误信息: " + errorOutput.toString();
                }
                System.out.println("CodeList数据提取失败: " + failureMessage);
                return CommonResult.failed(failureMessage);
            }

        } catch (Exception e) {
            String errorMessage = "执行CodeList数据提取和导入失败: " + e.getMessage();
            System.out.println("执行异常: " + errorMessage);
            e.printStackTrace();
            return CommonResult.failed(errorMessage);
        }
    }

    /**
     * Fill NCI Codelist Code and NCI Term Code from ct_term table.
     * Also fills Terminology from project_config CT version.
     */
    @PostMapping("/fill-nci-codes/{projectId}")
    public CommonResult<String> fillNciCodes(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            System.out.println("[fill-nci] projectId=" + projectId + ", username=" + username);

            ProjectConfigPO config = projectConfigMapper.selectByProjectId(projectId);
            if (config == null) {
                config = projectConfigMapper.selectByProjectId("DEFAULT");
            }

            String standardType = config != null && config.getStandardType() != null ? config.getStandardType() : "SDTM";
            String ctVersionStr = config != null ? config.getCtVersion() : "";

            // Find matching ct_package
            Long packageId = null;
            String terminologyLabel = "";
            String dateStr = "";

            if (ctVersionStr != null && !ctVersionStr.isEmpty()) {
                dateStr = ctVersionStr.replaceAll(".*?(\\d{4}-\\d{2}-\\d{2}).*", "$1");
                if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    List<Map<String, Object>> pkgRows = jdbcTemplate.queryForList(
                            "SELECT id, CAST(release_date AS CHAR) as rd FROM ct_package WHERE standard_type = ? AND CAST(release_date AS CHAR) LIKE CONCAT(?, '%') LIMIT 1",
                            standardType, dateStr);
                    if (!pkgRows.isEmpty()) {
                        packageId = ((Number) pkgRows.get(0).get("id")).longValue();
                        terminologyLabel = standardType + " Terminology " + dateStr;
                    }
                }
            }
            if (packageId == null) {
                List<Map<String, Object>> pkgRows = jdbcTemplate.queryForList(
                        "SELECT id, CAST(release_date AS CHAR) as rd FROM ct_package WHERE standard_type = ? AND language_code = 'EN' ORDER BY release_date DESC LIMIT 1",
                        standardType);
                if (!pkgRows.isEmpty()) {
                    packageId = ((Number) pkgRows.get(0).get("id")).longValue();
                    dateStr = pkgRows.get(0).get("rd").toString().substring(0, 10);
                    terminologyLabel = standardType + " Terminology " + dateStr;
                }
            }

            if (packageId == null) {
                return CommonResult.failed("未找到CT数据包，请确认CT数据已导入。standardType=" + standardType + ", ctVersion=" + ctVersionStr);
            }
            System.out.println("[fill-nci] Using packageId=" + packageId + ", label=" + terminologyLabel);

            // 1. Build CT header map: cdisc_submission_value -> {nciCodelistCode, codelistName}
            List<Map<String, Object>> headers = jdbcTemplate.queryForList(
                    "SELECT cdisc_submission_value, term_code, codelist_name FROM ct_term WHERE package_id = ? AND codelist_code IS NULL",
                    packageId);
            Map<String, String> svToNciClCode = new HashMap<>();
            Map<String, String> svToCodelistName = new HashMap<>();
            for (Map<String, Object> h : headers) {
                String sv = h.get("cdisc_submission_value") != null ? h.get("cdisc_submission_value").toString().toUpperCase() : "";
                String tc = h.get("term_code") != null ? h.get("term_code").toString() : "";
                String cn = h.get("codelist_name") != null ? h.get("codelist_name").toString() : "";
                if (!sv.isEmpty()) {
                    svToNciClCode.put(sv, tc);
                    svToCodelistName.put(sv, cn);
                }
            }

            // Build domain.variable → cdisc_submission_value lookup from spec
            Map<String, String> dvToSv = new HashMap<>();
            List<Map<String, Object>> specRows = jdbcTemplate.queryForList(
                    "SELECT domain, variable, cdisc_submission_value FROM sas_project_spec WHERE project_id = ? AND username = ?",
                    projectId, username);
            for (Map<String, Object> sr : specRows) {
                String dom = sr.get("domain") != null ? sr.get("domain").toString().trim() : "";
                String var = sr.get("variable") != null ? sr.get("variable").toString().trim() : "";
                String csv = sr.get("cdisc_submission_value") != null ? sr.get("cdisc_submission_value").toString().trim() : "";
                if (!dom.isEmpty() && !var.isEmpty() && !csv.isEmpty()) {
                    dvToSv.put((dom + "." + var).toUpperCase(), csv.toUpperCase());
                }
            }

            // Fill NCI Codelist Code + Terminology + Name for codelist rows
            List<CodelistDataPO> allCl = codelistDataMapper.selectByProjectId(projectId, username);
            int nciClUpdated = 0;
            if (allCl != null) {
                for (CodelistDataPO cl : allCl) {
                    if (cl.getNciCodelistCode() != null && !cl.getNciCodelistCode().isEmpty()) continue;
                    String vcd = cl.getVcd() != null ? cl.getVcd().trim().toUpperCase() : "";
                    // Resolve cdisc_submission_value: if vcd is domain.variable format, look up spec
                    String sv = dvToSv.getOrDefault(vcd, vcd);
                    String nciCl = svToNciClCode.get(sv);
                    String clName = svToCodelistName.get(sv);
                    if (nciCl != null) {
                        jdbcTemplate.update(
                                "UPDATE sas_codelist_data SET nci_codelist_code = ?, terminology = ?, vlabel = ?, updated_time = NOW() WHERE id = ?",
                                nciCl, terminologyLabel, clName, cl.getId());
                        nciClUpdated++;
                    }
                }
            }
            System.out.println("[fill-nci] NCI Codelist Code updated: " + nciClUpdated + " rows");

            // Build term map: "nciCodelistCode|submVal_upper" -> termCode (scoped by codelist)
            List<Map<String, Object>> termRows = jdbcTemplate.queryForList(
                    "SELECT codelist_code, cdisc_submission_value, term_code FROM ct_term WHERE package_id = ? AND codelist_code IS NOT NULL",
                    packageId);
            Map<String, String> termLookup = new HashMap<>();
            for (Map<String, Object> t : termRows) {
                String cl = t.get("codelist_code") != null ? t.get("codelist_code").toString().toUpperCase() : "";
                String sv = t.get("cdisc_submission_value") != null ? t.get("cdisc_submission_value").toString().toUpperCase() : "";
                String tc = t.get("term_code") != null ? t.get("term_code").toString() : "";
                if (!cl.isEmpty() && !sv.isEmpty()) {
                    termLookup.put(cl + "|" + sv, tc);
                }
            }

            // Fill NCI Term Code only (Decoded Value comes from XPT, not overwritten)
            List<CodelistDataPO> clList = codelistDataMapper.selectByProjectId(projectId, username);
            int nciTermUpdated = 0;
            if (clList != null) {
                for (CodelistDataPO cl2 : clList) {
                    String nciCl = cl2.getNciCodelistCode() != null ? cl2.getNciCodelistCode().trim().toUpperCase() : "";
                    String code = cl2.getCode() != null ? cl2.getCode().trim().toUpperCase() : "";
                    if (nciCl.isEmpty() || code.isEmpty()) continue;

                    String nciTermCode = termLookup.get(nciCl + "|" + code);
                    if (nciTermCode != null) {
                        jdbcTemplate.update(
                                "UPDATE sas_codelist_data SET nci_term_code = ?, updated_time = NOW() WHERE id = ?",
                                nciTermCode, cl2.getId());
                        nciTermUpdated++;
                    }
                }
            }
            System.out.println("[fill-nci] NCI Term Code updated: " + nciTermUpdated + " rows");

            return CommonResult.success(String.format(
                    "NCI代码填充完成：CT包[%s]，NCI Codelist Code更新 %d 行，NCI Term Code更新 %d 行",
                    terminologyLabel, nciClUpdated, nciTermUpdated));
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("NCI代码填充失败: " + e.getMessage());
        }
    }

    /**
     * Fill codelist ID from Variables (cdisc_submission_value) and VLM (codelist column).
     * Also fills Name from variable labels.
     */
    @PostMapping("/fill-codelist-id/{projectId}")
    public CommonResult<String> fillCodelistId(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();

            // Collect IDs from Variables spec: use codelist (DOMAIN.VARIABLE format) as key
            List<ProjectSpecPO> specList = projectSpecMapper.selectByProjectId(projectId, username);
            Set<String> codelistIds = new LinkedHashSet<>();
            Map<String, String> idToLabel = new HashMap<>();
            if (specList != null) {
                for (ProjectSpecPO spec : specList) {
                    String codelist = spec.getCodelist() != null ? spec.getCodelist().trim() : "";
                    String label = spec.getLabel() != null ? spec.getLabel().trim() : "";
                    if (!codelist.isEmpty()) {
                        codelistIds.add(codelist);
                        if (!label.isEmpty()) {
                            idToLabel.put(codelist, label);
                        }
                    }
                }
            }

            // Collect IDs from VLM: codelist column (format: DS.VAR.FILTERVAL)
            List<VlmDataPO> vlmList = vlmDataMapper.selectByProjectId(projectId, username);
            if (vlmList != null) {
                for (VlmDataPO vlm : vlmList) {
                    String clId = vlm.getCodelist() != null ? vlm.getCodelist().trim() : "";
                    if (!clId.isEmpty()) {
                        codelistIds.add(clId);
                        String vlabel = vlm.getLabel() != null ? vlm.getLabel().trim() : "";
                        if (!vlabel.isEmpty() && !idToLabel.containsKey(clId)) {
                            idToLabel.put(clId, vlabel);
                        }
                    }
                }
            }

            // Now update existing codelist data: set vlabel (Name) from label map
            List<CodelistDataPO> clList = codelistDataMapper.selectByProjectId(projectId, username);
            int updatedCount = 0;
            if (clList != null) {
                for (CodelistDataPO cl : clList) {
                    String vcd = cl.getVcd();
                    String label = idToLabel.get(vcd);
                    if (label != null && !label.isEmpty()) {
                        cl.setVlabel(label);
                        codelistDataMapper.updateById(cl);
                        updatedCount++;
                    }
                }
            }

            return CommonResult.success(String.format(
                    "Codelist ID来源：Variables %d 个，VLM %d 个，共 %d 个唯一ID，更新标签 %d 条",
                    specList != null ? specList.size() : 0,
                    vlmList != null ? vlmList.size() : 0,
                    codelistIds.size(), updatedCount));
        } catch (Exception e) {
            return CommonResult.failed("Codelist ID填充失败: " + e.getMessage());
        }
    }

    /**
     * Compare current codelist data with EDC Excel CODELIST sheet.
     * Returns missing (EDC has, DB lacks) and extra (DB has, EDC lacks) terms.
     */
    @PostMapping("/compare-edc/{projectId}")
    public CommonResult<Map<String, Object>> compareEdc(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();

            // Load EDC CODELIST sheet
            List<Map<String, Object>> files = jdbcTemplate.queryForList(
                    "SELECT file_path FROM file_upload_records WHERE project_id = ? AND file_category = 'EDC_CODELIST' ORDER BY upload_time DESC LIMIT 1",
                    projectId);
            if (files.isEmpty()) {
                return CommonResult.failed("未找到EDC建库说明文件，请先在文件上传页上传");
            }
            String filePath = files.get(0).get("file_path").toString();
            java.io.File f = new java.io.File(filePath);
            if (!f.exists()) {
                return CommonResult.failed("EDC文件不存在: " + filePath);
            }

            // Parse EDC: edcCode -> List<{code, label, name}>
            Map<String, List<Map<String, String>>> edcMap = new LinkedHashMap<>();
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
                if (sheet == null) return CommonResult.failed("EDC文件中未找到CODELIST sheet");

                Row hdr = sheet.getRow(0);
                if (hdr == null) return CommonResult.failed("CODELIST sheet无表头");
                int colCode = -1, colName = -1, colVal = -1, colLabel = -1;
                for (int c = 0; c <= hdr.getLastCellNum(); c++) {
                    Cell cell = hdr.getCell(c);
                    if (cell == null) continue;
                    String v = cell.getStringCellValue().trim();
                    if (v.contains("编码名")) colCode = c;
                    else if (v.contains("中文名称") || v.contains("名称")) colName = c;
                    else if (v.contains("编码值")) colVal = c;
                    else if (v.contains("编码标签") || v.contains("标签")) colLabel = c;
                }
                if (colCode < 0 || colVal < 0) return CommonResult.failed("CODELIST sheet缺少编码名或编码值列");

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    String code = getCellAsString(row, colCode);
                    if (code.isEmpty()) continue;
                    String name = colName >= 0 ? getCellAsString(row, colName) : "";
                    String val = getCellAsString(row, colVal);
                    String label = colLabel >= 0 ? getCellAsString(row, colLabel) : "";
                    edcMap.computeIfAbsent(code, k -> new ArrayList<>()).add(
                            Map.of("code", val, "label", label, "name", name));
                }
            }

            // Load current codelist data
            List<CodelistDataPO> clList = codelistDataMapper.selectByProjectId(projectId, username);
            // Build a set of (vcd_upper + "|" + code_trimmed) for existing terms
            Set<String> existingTerms = new HashSet<>();
            Map<String, List<CodelistDataPO>> clByVcd = new LinkedHashMap<>();
            if (clList != null) {
                for (CodelistDataPO cl : clList) {
                    String vcd = cl.getVcd() != null ? cl.getVcd().trim() : "";
                    String code = cl.getCode() != null ? cl.getCode().trim() : "";
                    existingTerms.add(vcd.toUpperCase() + "|" + code);
                    clByVcd.computeIfAbsent(vcd, k -> new ArrayList<>()).add(cl);
                }
            }

            // Compare: find missing and extra
            List<Map<String, String>> missing = new ArrayList<>();
            Set<String> edcAllTermKeys = new HashSet<>();

            for (Map.Entry<String, List<Map<String, String>>> e : edcMap.entrySet()) {
                String edcCode = e.getKey();
                String edcName = e.getValue().get(0).getOrDefault("name", "");
                for (Map<String, String> term : e.getValue()) {
                    String termVal = term.get("code");
                    String termLabel = term.get("label");
                    // Try to match EDC code name against existing VCDs
                    boolean found = false;
                    for (String vcd : clByVcd.keySet()) {
                        if (existingTerms.contains(vcd.toUpperCase() + "|" + termVal) ||
                            existingTerms.contains(vcd.toUpperCase() + "|" + termLabel)) {
                            found = true;
                            edcAllTermKeys.add(vcd.toUpperCase() + "|" + termVal);
                            edcAllTermKeys.add(vcd.toUpperCase() + "|" + termLabel);
                            break;
                        }
                    }
                    if (!found) {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("edcName", edcCode);
                        m.put("edcLabel", edcName);
                        m.put("code", termVal);
                        m.put("label", termLabel);
                        m.put("vcd", edcCode);
                        missing.add(m);
                    }
                }
            }

            // Extra: terms in DB but not matched by any EDC entry
            List<Map<String, String>> extra = new ArrayList<>();
            for (CodelistDataPO cl : (clList != null ? clList : new ArrayList<CodelistDataPO>())) {
                String vcd = cl.getVcd() != null ? cl.getVcd().trim() : "";
                String code = cl.getCode() != null ? cl.getCode().trim() : "";
                // VLM-level vcd has format DS.VAR.FILTERVAL (2+ dots), skip those
                if (vcd.chars().filter(c -> c == '.').count() >= 2) continue;
                boolean matchedByEdc = false;
                for (String key : edcAllTermKeys) {
                    if (key.startsWith(vcd.toUpperCase() + "|")) {
                        matchedByEdc = true;
                        break;
                    }
                }
                if (!matchedByEdc && !edcMap.isEmpty()) {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("vcd", vcd);
                    m.put("code", code);
                    m.put("codeDes", cl.getCodeDes() != null ? cl.getCodeDes() : "");
                    extra.add(m);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("missing", missing);
            result.put("extra", extra);
            result.put("edcTotal", edcMap.values().stream().mapToInt(List::size).sum());
            result.put("dbTotal", clList != null ? clList.size() : 0);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("比对EDC失败: " + e.getMessage());
        }
    }

    private void ensureDeletedTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sas_codelist_deleted (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  project_id VARCHAR(64)," +
                "  username VARCHAR(100)," +
                "  vcd VARCHAR(200)," +
                "  deleted_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (Exception ignored) {}
    }

    private void clearCodelistRef(String projectId, String username, String vcd) {
        try {
            jdbcTemplate.update(
                    "UPDATE sas_project_spec SET codelist = NULL WHERE project_id = ? AND username = ? AND codelist = ?",
                    projectId, username, vcd);
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.update(
                    "UPDATE sas_vlm_data SET codelist = NULL WHERE project_id = ? AND username = ? AND codelist = ?",
                    projectId, username, vcd);
        } catch (Exception ignored) {}
    }

    private String getCellAsString(Row row, int col) {
        if (col < 0) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    /**
     * Apply selected missing terms from EDC comparison into sas_codelist_data.
     */
    @PostMapping("/apply-edc-diff/{projectId}")
    public CommonResult<String> applyEdcDiff(@PathVariable String projectId, @RequestBody Map<String, Object> request) {
        try {
            String username = UserContext.getUsername();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            if (items == null || items.isEmpty()) {
                return CommonResult.failed("无待补充数据");
            }

            // Get max sort_order
            int maxOrder = 0;
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT MAX(sort_order) as mx FROM sas_codelist_data WHERE project_id = ? AND username = ?",
                        projectId, username);
                if (!rows.isEmpty() && rows.get(0).get("mx") != null) {
                    maxOrder = ((Number) rows.get(0).get("mx")).intValue();
                }
            } catch (Exception ignored) {}

            int inserted = 0;
            for (Map<String, Object> item : items) {
                String vcd = item.get("vcd") != null ? item.get("vcd").toString().trim() : "";
                String code = item.get("code") != null ? item.get("code").toString().trim() : "";
                String label = item.get("label") != null ? item.get("label").toString().trim() : "";
                if (vcd.isEmpty() || code.isEmpty()) continue;

                maxOrder++;
                CodelistDataPO po = new CodelistDataPO();
                po.setProjectId(projectId);
                po.setUsername(username);
                po.setVcd(vcd);
                po.setVlabel(label);
                po.setType("Char");
                po.setCdnum(maxOrder);
                po.setCode(code);
                po.setCodeDes(label);
                po.setSortOrder(maxOrder);
                po.setCreatedBy("edc_diff_apply");
                codelistDataMapper.insert(po);
                inserted++;
            }

            return CommonResult.success("补充完成，新增 " + inserted + " 条Term");
        } catch (Exception e) {
            return CommonResult.failed("补充失败: " + e.getMessage());
        }
    }

    @GetMapping("/export-xlsx/{projectId}")
    public ResponseEntity<byte[]> exportCodelistXlsx(@PathVariable String projectId) {
        try {
            List<CodelistDataDTO> dataList = codelistDataService.getCodelistDataByProjectId(projectId);
            String[] headers = {"ID", "Name", "NCI Codelist Code", "Data Type", "Terminology", "Comment",
                    "Order", "Term", "NCI Term Code", "Decoded Value", "Origin"};

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("CodeList");
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
                    CodelistDataDTO d = dataList.get(i);
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(d.getVcd() != null ? d.getVcd() : "");
                    row.createCell(1).setCellValue(d.getVlabel() != null ? d.getVlabel() : "");
                    row.createCell(2).setCellValue(d.getNciCodelistCode() != null ? d.getNciCodelistCode() : "");
                    row.createCell(3).setCellValue(d.getType() != null ? d.getType() : "");
                    row.createCell(4).setCellValue(d.getTerminology() != null ? d.getTerminology() : "");
                    row.createCell(5).setCellValue(d.getComment() != null ? d.getComment() : "");
                    row.createCell(6).setCellValue(d.getCdnum() != null ? d.getCdnum() : 0);
                    row.createCell(7).setCellValue(d.getCode() != null ? d.getCode() : "");
                    row.createCell(8).setCellValue(d.getNciTermCode() != null ? d.getNciTermCode() : "");
                    row.createCell(9).setCellValue(d.getCodeDes() != null ? d.getCodeDes() : "");
                    row.createCell(10).setCellValue(d.getOrigin() != null ? d.getOrigin() : "");
                }

                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                workbook.write(out);
                byte[] bytes = out.toByteArray();

                String fileName = URLEncoder.encode("CodeList_" + projectId + ".xlsx", StandardCharsets.UTF_8);
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
    public CommonResult<String> importCodelistXlsx(@PathVariable String projectId,
                                                    @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sheets = (List<Map<String, Object>>) request.get("sheets");
            if (sheets == null || sheets.isEmpty()) {
                return CommonResult.failed("无有效工作表数据");
            }

            codelistDataService.deleteByProjectId(projectId);

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
                    String vcd = row.getOrDefault(0, "").trim();
                    if (vcd.isEmpty()) continue;

                    CodelistDataDTO dto = new CodelistDataDTO();
                    dto.setProjectId(projectId);
                    dto.setVcd(vcd);
                    dto.setVlabel(row.getOrDefault(1, "").trim());
                    dto.setNciCodelistCode(row.getOrDefault(2, "").trim());
                    dto.setType(row.getOrDefault(3, "").trim());
                    dto.setTerminology(row.getOrDefault(4, "").trim());
                    dto.setComment(row.getOrDefault(5, "").trim());
                    String cdnumStr = row.getOrDefault(6, "").trim();
                    if (!cdnumStr.isEmpty()) {
                        try { dto.setCdnum((int) Double.parseDouble(cdnumStr)); } catch (NumberFormatException ignored) {}
                    }
                    dto.setCode(row.getOrDefault(7, "").trim());
                    dto.setNciTermCode(row.getOrDefault(8, "").trim());
                    dto.setCodeDes(row.getOrDefault(9, "").trim());
                    dto.setSortOrder(importCount);
                    codelistDataService.addCodelistData(dto);
                    importCount++;
                }
                break;
            }
            return CommonResult.success("导入成功，共 " + importCount + " 条CodeList数据");
        } catch (Exception e) {
            return CommonResult.failed("导入CodeList数据失败: " + e.getMessage());
        }
    }

    // ===== Codelist Merge =====

    /**
     * Analyze codelists to find groups with identical term sets that can be merged.
     */
    @GetMapping("/merge/analyze/{projectId}")
    public CommonResult<List<Map<String, Object>>> analyzeMerge(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            ensureMergeLogTable();

            List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "SELECT term_fingerprint, term_list, term_count, vcd_list, vcd_count, suggested_nci_code, suggested_vlabel " +
                "FROM ( " +
                "  SELECT " +
                "    fp.term_fingerprint, fp.term_list, fp.term_count, " +
                "    GROUP_CONCAT(fp.vcd ORDER BY fp.vcd SEPARATOR '||') AS vcd_list, " +
                "    COUNT(*) AS vcd_count, " +
                "    MAX(fp.nci_codelist_code) AS suggested_nci_code, " +
                "    MAX(fp.vlabel) AS suggested_vlabel " +
                "  FROM ( " +
                "    SELECT " +
                "      vcd, " +
                "      MAX(vlabel) AS vlabel, " +
                "      MAX(nci_codelist_code) AS nci_codelist_code, " +
                "      MD5(GROUP_CONCAT(TRIM(UPPER(code)) ORDER BY TRIM(UPPER(code)) SEPARATOR '||')) AS term_fingerprint, " +
                "      GROUP_CONCAT(TRIM(code) ORDER BY TRIM(UPPER(code)) SEPARATOR ', ') AS term_list, " +
                "      COUNT(*) AS term_count " +
                "    FROM sas_codelist_data " +
                "    WHERE project_id = ? AND username = ? " +
                "    GROUP BY vcd " +
                "  ) fp " +
                "  GROUP BY fp.term_fingerprint, fp.term_list, fp.term_count " +
                "  HAVING COUNT(*) > 1 " +
                ") result " +
                "ORDER BY vcd_count DESC",
                projectId, username
            );

            // Also try to match CT for each group
            ProjectConfigPO config = projectConfigMapper.selectByProjectId(projectId);
            if (config == null) config = projectConfigMapper.selectByProjectId("DEFAULT");
            Long packageId = resolvePackageId(config);

            for (Map<String, Object> group : groups) {
                String termList = group.get("term_list") != null ? group.get("term_list").toString() : "";
                String[] terms = termList.split(",\\s*");
                String nciCode = group.get("suggested_nci_code") != null ? group.get("suggested_nci_code").toString() : "";

                if ((nciCode == null || nciCode.isEmpty()) && packageId != null && terms.length > 0) {
                    Map<String, String> ctMatch = findCtCodelistByTerms(packageId, terms);
                    if (ctMatch != null) {
                        group.put("ct_codelist_code", ctMatch.get("codelist_code"));
                        group.put("ct_codelist_name", ctMatch.get("codelist_name"));
                    }
                }

                // Build per-vcd details with vlabel and reference counts
                String vcdListStr = group.get("vcd_list") != null ? group.get("vcd_list").toString() : "";
                String[] vcdArr = vcdListStr.split("\\|\\|");
                List<Map<String, Object>> vcdDetails = new ArrayList<>();
                for (String vcd : vcdArr) {
                    Map<String, Object> detail = new HashMap<>();
                    String trimmedVcd = vcd.trim();
                    detail.put("vcd", trimmedVcd);
                    // Get vlabel (Name) for this vcd
                    try {
                        String vlabel = jdbcTemplate.queryForObject(
                            "SELECT vlabel FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ? LIMIT 1",
                            String.class, projectId, username, trimmedVcd);
                        detail.put("vlabel", vlabel != null ? vlabel : "");
                    } catch (Exception e) {
                        detail.put("vlabel", "");
                    }
                    // Count references in spec and vlm
                    int specCount = 0, vlmCount = 0;
                    try {
                        specCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM sas_project_spec WHERE project_id = ? AND username = ? AND codelist = ?",
                            Integer.class, projectId, username, trimmedVcd);
                    } catch (Exception ignored) {}
                    try {
                        vlmCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM sas_vlm_data WHERE project_id = ? AND username = ? AND codelist = ?",
                            Integer.class, projectId, username, trimmedVcd);
                    } catch (Exception ignored) {}
                    detail.put("specRefCount", specCount);
                    detail.put("vlmRefCount", vlmCount);
                    vcdDetails.add(detail);
                }
                group.put("vcdDetails", vcdDetails);

                // Get term details (code + nci_term_code + code_des) from the first VCD
                if (vcdArr.length > 0) {
                    String firstVcd = vcdArr[0].trim();
                    try {
                        List<Map<String, Object>> termDetails = jdbcTemplate.queryForList(
                            "SELECT code, nci_term_code, code_des FROM sas_codelist_data " +
                            "WHERE project_id = ? AND username = ? AND vcd = ? ORDER BY cdnum, sort_order",
                            projectId, username, firstVcd);
                        group.put("mergedTermDetails", termDetails);
                    } catch (Exception ignored) {}
                }
            }

            // Append already-merged groups from merge_log so they remain visible after refresh
            Set<String> alreadyInGroups = new HashSet<>();
            for (Map<String, Object> g : groups) {
                String vl = g.get("vcd_list") != null ? g.get("vcd_list").toString() : "";
                for (String v : vl.split("\\|\\|")) alreadyInGroups.add(v.trim());
            }

            List<Map<String, Object>> mergedRules = jdbcTemplate.queryForList(
                "SELECT merged_vcd, merged_vlabel, MAX(merged_nci_code) AS merged_nci_code, " +
                "  GROUP_CONCAT(DISTINCT original_vcd ORDER BY original_vcd SEPARATOR '||') AS original_vcds " +
                "FROM sas_codelist_merge_log " +
                "WHERE project_id = ? AND username = ? " +
                "GROUP BY merged_vcd, merged_vlabel",
                projectId, username);

            for (Map<String, Object> rule : mergedRules) {
                String mergedVcd = rule.get("merged_vcd").toString();
                String mergedVlabel = rule.get("merged_vlabel") != null ? rule.get("merged_vlabel").toString() : "";
                String[] originalVcds = rule.get("original_vcds").toString().split("\\|\\|");

                // Skip if any original VCD is already part of a detected group (not yet merged)
                boolean overlaps = false;
                for (String ov : originalVcds) {
                    if (alreadyInGroups.contains(ov.trim())) { overlaps = true; break; }
                }
                if (overlaps) continue;

                // Check if the merged target VCD exists (meaning the merge was done and data still exists)
                int targetCount = 0;
                try {
                    targetCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                        Integer.class, projectId, username, mergedVcd);
                } catch (Exception ignored) {}
                if (targetCount == 0) continue;

                // Get the real NCI Codelist Code from sas_codelist_data (authoritative)
                String logNciCode = rule.get("merged_nci_code") != null ? rule.get("merged_nci_code").toString() : "";
                String realNciCode = logNciCode;
                try {
                    String dbNciCode = jdbcTemplate.queryForObject(
                        "SELECT nci_codelist_code FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ? LIMIT 1",
                        String.class, projectId, username, mergedVcd);
                    if (dbNciCode != null && !dbNciCode.isEmpty()) {
                        realNciCode = dbNciCode;
                    }
                } catch (Exception ignored) {}

                // Build a "merged" group entry
                Map<String, Object> mergedGroup = new HashMap<>();
                mergedGroup.put("vcd_list", String.join("||", originalVcds));
                mergedGroup.put("vcd_count", originalVcds.length);
                mergedGroup.put("suggested_nci_code", realNciCode);
                mergedGroup.put("suggested_vlabel", mergedVlabel);
                mergedGroup.put("already_merged", true);
                mergedGroup.put("merged_target_vcd", mergedVcd);
                mergedGroup.put("merged_target_vlabel", mergedVlabel);
                mergedGroup.put("merged_target_nci_code", realNciCode);

                // Get terms from the merged codelist
                List<Map<String, Object>> termRows = jdbcTemplate.queryForList(
                    "SELECT code, nci_term_code, code_des FROM sas_codelist_data " +
                    "WHERE project_id = ? AND username = ? AND vcd = ? ORDER BY cdnum, sort_order",
                    projectId, username, mergedVcd);
                List<String> termCodes = new ArrayList<>();
                List<Map<String, Object>> mergedTermDetails = new ArrayList<>();
                for (Map<String, Object> tr : termRows) {
                    String code = tr.get("code") != null ? tr.get("code").toString().trim() : "";
                    if (!code.isEmpty()) termCodes.add(code);
                    mergedTermDetails.add(tr);
                }
                mergedGroup.put("term_list", String.join(", ", termCodes));
                mergedGroup.put("term_count", termCodes.size());
                mergedGroup.put("term_fingerprint", "merged_" + mergedVcd);
                mergedGroup.put("mergedTermDetails", mergedTermDetails);

                // Build vcdDetails showing original VCDs (they no longer exist, so refs = 0)
                List<Map<String, Object>> vcdDetails = new ArrayList<>();
                for (String ov : originalVcds) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("vcd", ov.trim());
                    detail.put("vlabel", mergedVlabel);
                    detail.put("specRefCount", 0);
                    detail.put("vlmRefCount", 0);
                    vcdDetails.add(detail);
                }
                mergedGroup.put("vcdDetails", vcdDetails);

                groups.add(mergedGroup);
            }

            return CommonResult.success(groups);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("分析合并候选失败: " + e.getMessage());
        }
    }

    /**
     * Unified analysis: groups VCDs by identical term sets, then detects subset relationships
     * between those groups, building clusters that show the full merge picture.
     */
    @GetMapping("/merge/analyze-unified/{projectId}")
    public CommonResult<List<Map<String, Object>>> analyzeUnified(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            ensureMergeLogTable();

            // Step 1: Get all VCDs with their term sets and metadata.
            // EXCLUDE merge products (rows whose vcd appears in sas_codelist_merge_log.merged_vcd)
            // so the freshly created COMxx don't show up as new candidate groups after refresh.
            // Their "已合并" entry is rebuilt separately further below from sas_codelist_merge_log.
            List<Map<String, Object>> vcdRows = jdbcTemplate.queryForList(
                "SELECT vcd, MAX(vlabel) AS vlabel, MAX(nci_codelist_code) AS nci_codelist_code, " +
                "  GROUP_CONCAT(TRIM(UPPER(code)) ORDER BY TRIM(UPPER(code)) SEPARATOR '||') AS term_key, " +
                "  GROUP_CONCAT(TRIM(code) ORDER BY TRIM(UPPER(code)) SEPARATOR ', ') AS term_list, " +
                "  COUNT(*) AS term_count " +
                "FROM sas_codelist_data " +
                "WHERE project_id = ? AND username = ? " +
                "  AND vcd NOT IN ( " +
                "    SELECT merged_vcd FROM sas_codelist_merge_log " +
                "    WHERE project_id = ? AND username = ? AND merged_vcd IS NOT NULL " +
                "  ) " +
                "GROUP BY vcd",
                projectId, username, projectId, username);

            // Step 2: Group VCDs by identical term fingerprint
            Map<String, List<Map<String, Object>>> fingerprintGroups = new LinkedHashMap<>();
            Map<String, Map<String, Object>> vcdInfoMap = new HashMap<>();

            for (Map<String, Object> row : vcdRows) {
                String vcd = row.get("vcd").toString();
                String termKey = row.get("term_key") != null ? row.get("term_key").toString() : "";
                Set<String> termSet = new LinkedHashSet<>(Arrays.asList(termKey.split("\\|\\|")));

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("vcd", vcd);
                info.put("vlabel", row.get("vlabel") != null ? row.get("vlabel").toString() : "");
                info.put("nciCode", row.get("nci_codelist_code") != null ? row.get("nci_codelist_code").toString() : "");
                info.put("termSet", termSet);
                info.put("termKey", termKey);
                info.put("termList", row.get("term_list") != null ? row.get("term_list").toString() : "");
                info.put("termCount", ((Number) row.get("term_count")).intValue());

                // Ref counts
                int specCount = 0, vlmCount = 0;
                try {
                    specCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sas_project_spec WHERE project_id = ? AND username = ? AND codelist = ?",
                        Integer.class, projectId, username, vcd);
                } catch (Exception ignored) {}
                try {
                    vlmCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sas_vlm_data WHERE project_id = ? AND username = ? AND codelist = ?",
                        Integer.class, projectId, username, vcd);
                } catch (Exception ignored) {}
                info.put("specRefCount", specCount);
                info.put("vlmRefCount", vlmCount);

                vcdInfoMap.put(vcd, info);

                String fingerprint = termKey;
                fingerprintGroups.computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(info);
            }

            // Step 3: Build identity groups (unique term sets) — each group represents one unique term set
            // identityGroup: { fingerprint, termSet, termList, termCount, vcds: [...] }
            List<Map<String, Object>> identityGroups = new ArrayList<>();
            Map<String, Map<String, Object>> fpToGroup = new LinkedHashMap<>();

            for (Map.Entry<String, List<Map<String, Object>>> entry : fingerprintGroups.entrySet()) {
                String fp = entry.getKey();
                List<Map<String, Object>> vcds = entry.getValue();

                Map<String, Object> firstVcd = vcds.get(0);
                Map<String, Object> ig = new LinkedHashMap<>();
                ig.put("fingerprint", fp);
                @SuppressWarnings("unchecked")
                Set<String> ts = (Set<String>) firstVcd.get("termSet");
                ig.put("termSet", ts);
                ig.put("termList", firstVcd.get("termList"));
                ig.put("termCount", firstVcd.get("termCount"));

                List<Map<String, Object>> vcdDetails = new ArrayList<>();
                for (Map<String, Object> v : vcds) {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("vcd", v.get("vcd"));
                    detail.put("vlabel", v.get("vlabel"));
                    detail.put("nciCode", v.get("nciCode"));
                    detail.put("specRefCount", v.get("specRefCount"));
                    detail.put("vlmRefCount", v.get("vlmRefCount"));
                    vcdDetails.add(detail);
                }
                ig.put("vcds", vcdDetails);
                ig.put("vcdCount", vcds.size());

                identityGroups.add(ig);
                fpToGroup.put(fp, ig);
            }

            // Step 4: Find subset relationships between identity groups
            // A group is a subset of another if its termSet ⊂ the other's termSet
            Map<String, List<String>> supersetToSubsets = new LinkedHashMap<>();
            Set<String> involvedAsSubset = new HashSet<>();

            List<String> fingerprints = new ArrayList<>(fpToGroup.keySet());
            for (int i = 0; i < fingerprints.size(); i++) {
                Map<String, Object> groupA = fpToGroup.get(fingerprints.get(i));
                @SuppressWarnings("unchecked")
                Set<String> setA = (Set<String>) groupA.get("termSet");
                int countA = (Integer) groupA.get("termCount");

                for (int j = 0; j < fingerprints.size(); j++) {
                    if (i == j) continue;
                    Map<String, Object> groupB = fpToGroup.get(fingerprints.get(j));
                    @SuppressWarnings("unchecked")
                    Set<String> setB = (Set<String>) groupB.get("termSet");
                    int countB = (Integer) groupB.get("termCount");

                    if (countA < countB && setB.containsAll(setA)) {
                        supersetToSubsets.computeIfAbsent(fingerprints.get(j), k -> new ArrayList<>())
                            .add(fingerprints.get(i));
                        involvedAsSubset.add(fingerprints.get(i));
                    }
                }
            }

            // Step 5: Build top-level supersets (not themselves subsets of another)
            Set<String> topLevelSupersets = new LinkedHashSet<>(supersetToSubsets.keySet());
            topLevelSupersets.removeAll(involvedAsSubset);

            // Step 6: Build clusters
            List<Map<String, Object>> clusters = new ArrayList<>();
            Set<String> processedFingerprints = new HashSet<>();

            // 6a: Subset chains — clusters with hierarchy
            for (String supFp : topLevelSupersets) {
                List<String> subFps = supersetToSubsets.get(supFp);
                if (subFps == null || subFps.isEmpty()) continue;

                // Collect all fingerprints recursively involved
                Set<String> allSubFps = new LinkedHashSet<>(subFps);
                // Also include transitive subsets (A⊂B⊂C: C's subFps has B, B's subFps has A)
                for (String sfp : subFps) {
                    List<String> transitive = supersetToSubsets.get(sfp);
                    if (transitive != null) allSubFps.addAll(transitive);
                }

                Map<String, Object> supGroup = fpToGroup.get(supFp);
                Map<String, Object> cluster = new LinkedHashMap<>();
                cluster.put("type", "subset_chain");
                cluster.put("cluster_key", "sc:" + supFp);

                // Superset group info
                Map<String, Object> supersetInfo = new LinkedHashMap<>();
                supersetInfo.put("termList", supGroup.get("termList"));
                supersetInfo.put("termCount", supGroup.get("termCount"));
                supersetInfo.put("vcds", supGroup.get("vcds"));
                supersetInfo.put("vcdCount", supGroup.get("vcdCount"));
                cluster.put("supersetGroup", supersetInfo);

                // Subset groups info
                List<Map<String, Object>> subsetGroupsList = new ArrayList<>();
                for (String sfp : allSubFps) {
                    Map<String, Object> subGroup = fpToGroup.get(sfp);
                    if (subGroup == null) continue;

                    @SuppressWarnings("unchecked")
                    Set<String> subTermSet = (Set<String>) subGroup.get("termSet");
                    @SuppressWarnings("unchecked")
                    Set<String> supTermSet = (Set<String>) supGroup.get("termSet");
                    Set<String> missingTerms = new LinkedHashSet<>(supTermSet);
                    missingTerms.removeAll(subTermSet);

                    Map<String, Object> subInfo = new LinkedHashMap<>();
                    subInfo.put("termList", subGroup.get("termList"));
                    subInfo.put("termCount", subGroup.get("termCount"));
                    subInfo.put("vcds", subGroup.get("vcds"));
                    subInfo.put("vcdCount", subGroup.get("vcdCount"));
                    subInfo.put("missingTerms", String.join(", ", missingTerms));
                    subInfo.put("missingCount", missingTerms.size());
                    subsetGroupsList.add(subInfo);
                }

                subsetGroupsList.sort((a, b) -> ((Integer) b.get("termCount")).compareTo((Integer) a.get("termCount")));
                cluster.put("subsetGroups", subsetGroupsList);

                // Collect all VCDs in this cluster
                List<String> allVcds = new ArrayList<>();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> supVcds = (List<Map<String, Object>>) supGroup.get("vcds");
                for (Map<String, Object> v : supVcds) allVcds.add(v.get("vcd").toString());
                for (Map<String, Object> subInfo : subsetGroupsList) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> subVcds = (List<Map<String, Object>>) subInfo.get("vcds");
                    for (Map<String, Object> v : subVcds) allVcds.add(v.get("vcd").toString());
                }
                cluster.put("allVcds", allVcds);
                cluster.put("totalVcdCount", allVcds.size());

                // Term details from the superset VCD
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> sVcds = (List<Map<String, Object>>) supGroup.get("vcds");
                if (!sVcds.isEmpty()) {
                    String refVcd = sVcds.get(0).get("vcd").toString();
                    try {
                        List<Map<String, Object>> termDetails = jdbcTemplate.queryForList(
                            "SELECT code, nci_term_code, code_des FROM sas_codelist_data " +
                            "WHERE project_id = ? AND username = ? AND vcd = ? ORDER BY cdnum, sort_order",
                            projectId, username, refVcd);
                        cluster.put("mergedTermDetails", termDetails);
                    } catch (Exception ignored) {}
                }

                // CT suggestion
                ProjectConfigPO config = projectConfigMapper.selectByProjectId(projectId);
                if (config == null) config = projectConfigMapper.selectByProjectId("DEFAULT");
                Long packageId = resolvePackageId(config);
                if (packageId != null) {
                    String termList = supGroup.get("termList") != null ? supGroup.get("termList").toString() : "";
                    String[] terms = termList.split(",\\s*");
                    if (terms.length > 0) {
                        Map<String, String> ctMatch = findCtCodelistByTerms(packageId, terms);
                        if (ctMatch != null) {
                            cluster.put("ct_codelist_code", ctMatch.get("codelist_code"));
                            cluster.put("ct_codelist_name", ctMatch.get("codelist_name"));
                        }
                    }
                }

                clusters.add(cluster);
                processedFingerprints.add(supFp);
                processedFingerprints.addAll(allSubFps);
            }

            // 6c (NEW — Scheme A): CT-aligned candidate groups.
            // For each unprocessed fingerprint, find the most specific CT codelist
            // that fully contains its term set (input ⊆ CT). Then group all fingerprints
            // that map to the SAME CT codelist into one "ct_aligned" cluster, proposing
            // to merge them into the CT's full standard term set.
            //
            // Example (Scheme A target case):
            //   fp_X = {AFTER, BEFORE, DURING, UNKNOWN}  (vcdA)
            //   fp_Y = {BEFORE, ONGOING, UNKNOWN}        (vcdB)
            //   Both fit inside CT codelist NRIND (CONCURRENT, AFTER, BEFORE, DURING, ONGOING, UNKNOWN)
            //   → produce ONE ct_aligned cluster suggesting merging both into NRIND.
            //
            // This step runs BEFORE 6b so that even fps with vcdCount==1 (which would be
            // dropped by 6b) get a chance to participate, and so that multi-vcd identity
            // fps can be "upgraded" to a ct_aligned cluster when they share a CT with another fp.
            ProjectConfigPO cfgForCt = projectConfigMapper.selectByProjectId(projectId);
            if (cfgForCt == null) cfgForCt = projectConfigMapper.selectByProjectId("DEFAULT");
            Long packageIdForCt = resolvePackageId(cfgForCt);
            if (packageIdForCt != null) {
                // ctCode → list of fingerprints whose term set ⊆ this CT codelist
                Map<String, List<String>> ctCodeToFps = new LinkedHashMap<>();
                Map<String, String> ctCodeToName = new HashMap<>();
                Map<String, Integer> ctCodeToTotal = new HashMap<>();

                for (Map.Entry<String, Map<String, Object>> entry : fpToGroup.entrySet()) {
                    String fp = entry.getKey();
                    if (processedFingerprints.contains(fp)) continue;
                    Map<String, Object> ig = entry.getValue();
                    @SuppressWarnings("unchecked")
                    Set<String> termSet = (Set<String>) ig.get("termSet");
                    // Skip groups with too few terms — single-term overlaps (e.g. only "OTHER"
                    // or "UNKNOWN") are too weak a signal and would create noisy false positives.
                    if (termSet == null || termSet.size() < 2) continue;

                    String[] termArr = termSet.toArray(new String[0]);
                    List<Map<String, Object>> matches = findCtCodelistsBySubset(packageIdForCt, termArr);
                    if (matches.isEmpty()) continue;

                    // Pick the smallest matching CT codelist (most specific).
                    // matches is ordered by total_terms ASC.
                    Map<String, Object> best = null;
                    for (Map<String, Object> m : matches) {
                        int total = toInt(m.get("total_terms"));
                        // Skip CTs that exactly match (handled by identity / subset_chain) — we want
                        // the case where the fp's terms are a STRICT subset of the CT.
                        if (total <= termSet.size()) continue;
                        // Guard against huge / catch-all CT codelists (e.g. dictionaries with hundreds
                        // of terms): a fp with 4 terms ⊆ a CT with 200 terms is too loose to suggest.
                        // Cap at 50 terms — typical CDISC CT codelists are well under this.
                        if (total > 50) continue;
                        best = m;
                        break;
                    }
                    if (best == null) continue;

                    String ctCode = best.get("codelist_code").toString();
                    String ctName = best.get("codelist_name") != null ? best.get("codelist_name").toString() : "";
                    int totalTerms = toInt(best.get("total_terms"));

                    ctCodeToFps.computeIfAbsent(ctCode, k -> new ArrayList<>()).add(fp);
                    ctCodeToName.putIfAbsent(ctCode, ctName);
                    ctCodeToTotal.putIfAbsent(ctCode, totalTerms);
                }

                // Build ct_aligned clusters: need ≥2 distinct fps mapping to the same CT
                for (Map.Entry<String, List<String>> entry : ctCodeToFps.entrySet()) {
                    String ctCode = entry.getKey();
                    List<String> fps = entry.getValue();
                    if (fps.size() < 2) continue;

                    // Build the PROJECT-SIDE UNION of all fps' term sets.
                    // CT is only used for grouping and NCI suggestion, not as the merge target.
                    Set<String> unionUpper = new LinkedHashSet<>();
                    List<Map<String, Object>> subGroupsList = new ArrayList<>();
                    List<String> allVcdsList = new ArrayList<>();
                    int totalVcdCount = 0;
                    // First pass: collect each fp's term set and build the union
                    Map<String, Set<String>> fpToTermUpper = new LinkedHashMap<>();
                    for (String fp : fps) {
                        Map<String, Object> ig = fpToGroup.get(fp);
                        if (ig == null) continue;
                        @SuppressWarnings("unchecked")
                        Set<String> ts = (Set<String>) ig.get("termSet");
                        Set<String> tsUpper = new LinkedHashSet<>();
                        if (ts != null) for (String t : ts) { tsUpper.add(t.trim().toUpperCase()); unionUpper.add(t.trim().toUpperCase()); }
                        fpToTermUpper.put(fp, tsUpper);
                    }
                    List<String> unionSorted = new ArrayList<>(unionUpper);
                    java.util.Collections.sort(unionSorted);

                    // Second pass: build sub-groups with missingTerms relative to union
                    for (String fp : fps) {
                        Map<String, Object> ig = fpToGroup.get(fp);
                        if (ig == null) continue;
                        Set<String> tsUpper = fpToTermUpper.get(fp);
                        List<String> missing = new ArrayList<>();
                        for (String t : unionSorted) {
                            if (tsUpper == null || !tsUpper.contains(t)) missing.add(t);
                        }

                        Map<String, Object> sub = new LinkedHashMap<>();
                        sub.put("termList", ig.get("termList"));
                        sub.put("termCount", ig.get("termCount"));
                        sub.put("vcds", ig.get("vcds"));
                        sub.put("vcdCount", ig.get("vcdCount"));
                        sub.put("missingTerms", String.join(", ", missing));
                        sub.put("missingCount", missing.size());
                        subGroupsList.add(sub);

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> vcds = (List<Map<String, Object>>) ig.get("vcds");
                        if (vcds != null) {
                            for (Map<String, Object> v : vcds) {
                                Object vcdObj = v.get("vcd");
                                if (vcdObj != null) allVcdsList.add(vcdObj.toString());
                            }
                            totalVcdCount += vcds.size();
                        }
                    }

                    if (subGroupsList.size() < 2 || totalVcdCount < 2) continue;

                    subGroupsList.sort((a, b) -> ((Integer) b.get("termCount")).compareTo((Integer) a.get("termCount")));

                    Map<String, Object> cluster = new LinkedHashMap<>();
                    cluster.put("type", "ct_aligned");
                    cluster.put("cluster_key", "ca:" + ctCode);
                    cluster.put("ct_codelist_code", ctCode);
                    cluster.put("ct_codelist_name", ctCodeToName.get(ctCode));

                    // supersetGroup = project-side union (virtual — no project vcds own it directly)
                    Map<String, Object> supInfo = new LinkedHashMap<>();
                    supInfo.put("termList", String.join(", ", unionSorted));
                    supInfo.put("termCount", unionSorted.size());
                    supInfo.put("vcds", new ArrayList<>());
                    supInfo.put("vcdCount", 0);
                    cluster.put("supersetGroup", supInfo);

                    cluster.put("subsetGroups", subGroupsList);
                    cluster.put("allVcds", allVcdsList);
                    cluster.put("totalVcdCount", allVcdsList.size());

                    // Build mergedTermDetails from the first (largest) sub-group's vcd rows,
                    // then merge in any extra terms from the other sub-groups' vcd rows.
                    // This gives the project-side union as the post-merge term set.
                    try {
                        Set<String> seenCodes = new LinkedHashSet<>();
                        List<Map<String, Object>> unionTermDetails = new ArrayList<>();
                        for (String fp : fps) {
                            Map<String, Object> ig = fpToGroup.get(fp);
                            if (ig == null) continue;
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> vcds = (List<Map<String, Object>>) ig.get("vcds");
                            if (vcds == null || vcds.isEmpty()) continue;
                            String refVcd = vcds.get(0).get("vcd").toString();
                            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                                "SELECT code, nci_term_code, code_des FROM sas_codelist_data " +
                                "WHERE project_id = ? AND username = ? AND vcd = ? ORDER BY cdnum, sort_order",
                                projectId, username, refVcd);
                            for (Map<String, Object> row : rows) {
                                String c = row.get("code") != null ? row.get("code").toString().trim().toUpperCase() : "";
                                if (!c.isEmpty() && seenCodes.add(c)) {
                                    unionTermDetails.add(row);
                                }
                            }
                        }
                        cluster.put("mergedTermDetails", unionTermDetails);
                    } catch (Exception ignored) {}

                    clusters.add(cluster);
                    for (String fp : fps) processedFingerprints.add(fp);
                }
            }

            // 6b: Pure identity groups (not part of any subset chain, with 2+ VCDs)
            for (Map.Entry<String, Map<String, Object>> entry : fpToGroup.entrySet()) {
                String fp = entry.getKey();
                if (processedFingerprints.contains(fp)) continue;
                Map<String, Object> ig = entry.getValue();
                int vcdCount = (Integer) ig.get("vcdCount");
                if (vcdCount < 2) continue;

                Map<String, Object> cluster = new LinkedHashMap<>();
                cluster.put("type", "identity");
                cluster.put("cluster_key", "fp:" + fp);

                Map<String, Object> supersetInfo = new LinkedHashMap<>();
                supersetInfo.put("termList", ig.get("termList"));
                supersetInfo.put("termCount", ig.get("termCount"));
                supersetInfo.put("vcds", ig.get("vcds"));
                supersetInfo.put("vcdCount", ig.get("vcdCount"));
                cluster.put("supersetGroup", supersetInfo);
                cluster.put("subsetGroups", new ArrayList<>());

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> igVcds = (List<Map<String, Object>>) ig.get("vcds");
                List<String> allVcds = new ArrayList<>();
                for (Map<String, Object> v : igVcds) allVcds.add(v.get("vcd").toString());
                cluster.put("allVcds", allVcds);
                cluster.put("totalVcdCount", allVcds.size());

                // Term details
                if (!igVcds.isEmpty()) {
                    String refVcd = igVcds.get(0).get("vcd").toString();
                    try {
                        List<Map<String, Object>> termDetails = jdbcTemplate.queryForList(
                            "SELECT code, nci_term_code, code_des FROM sas_codelist_data " +
                            "WHERE project_id = ? AND username = ? AND vcd = ? ORDER BY cdnum, sort_order",
                            projectId, username, refVcd);
                        cluster.put("mergedTermDetails", termDetails);
                    } catch (Exception ignored) {}
                }

                // CT suggestion
                ProjectConfigPO config = projectConfigMapper.selectByProjectId(projectId);
                if (config == null) config = projectConfigMapper.selectByProjectId("DEFAULT");
                Long packageId = resolvePackageId(config);
                String nciCode = "";
                for (Map<String, Object> v : igVcds) {
                    String nc = v.get("nciCode") != null ? v.get("nciCode").toString() : "";
                    if (!nc.isEmpty()) { nciCode = nc; break; }
                }
                if (nciCode.isEmpty() && packageId != null) {
                    String termList = ig.get("termList") != null ? ig.get("termList").toString() : "";
                    String[] terms = termList.split(",\\s*");
                    if (terms.length > 0) {
                        Map<String, String> ctMatch = findCtCodelistByTerms(packageId, terms);
                        if (ctMatch != null) {
                            cluster.put("ct_codelist_code", ctMatch.get("codelist_code"));
                            cluster.put("ct_codelist_name", ctMatch.get("codelist_name"));
                        }
                    }
                }
                if (!nciCode.isEmpty()) {
                    cluster.put("suggested_nci_code", nciCode);
                }

                clusters.add(cluster);
                processedFingerprints.add(fp);
            }

            // Step 6d (POST-PROCESSING): CT-aware cluster merging.
            // If 2+ non-merged clusters share the same ct_codelist_code, combine them
            // into a single ct_aligned cluster. This handles the common case where:
            //   Chain A: superset {AFTER, BEFORE, DURING, UNKNOWN}
            //   Chain B: superset {BEFORE, ONGOING, UNKNOWN}
            //   Chain C: superset {BEFORE, BEFORE/DURING, DURING, UNKNOWN}
            //   All three match CT C66728 → should be ONE candidate group.
            // It also resolves VCDs that appear as subsets of multiple chains.
            {
                Map<String, List<Integer>> ctCodeToIdx = new LinkedHashMap<>();
                for (int ci = 0; ci < clusters.size(); ci++) {
                    Map<String, Object> c = clusters.get(ci);
                    if (Boolean.TRUE.equals(c.get("already_merged"))) continue;
                    String ctCode = c.get("ct_codelist_code") != null ? c.get("ct_codelist_code").toString() : null;
                    if (ctCode != null && !ctCode.isEmpty()) {
                        ctCodeToIdx.computeIfAbsent(ctCode, k -> new ArrayList<>()).add(ci);
                    }
                }
                List<Integer> indicesToRemove = new ArrayList<>();
                List<Map<String, Object>> newClusters = new ArrayList<>();
                for (Map.Entry<String, List<Integer>> e2 : ctCodeToIdx.entrySet()) {
                    List<Integer> idxs = e2.getValue();
                    if (idxs.size() < 2) continue;
                    String ctCode = e2.getKey();

                    // Build union of all sub-groups (each fingerprint = one sub-group).
                    // Use fpToGroup to get canonical per-fingerprint info, avoiding duplicates
                    // when a VCD appears as a subset in multiple chains.
                    Set<String> seenFps = new LinkedHashSet<>();
                    Set<String> unionTermsUpper = new LinkedHashSet<>();
                    List<Map<String, Object>> subGroupsList = new ArrayList<>();
                    Set<String> allVcdsSet = new LinkedHashSet<>();
                    String ctName = "";

                    for (int ci : idxs) {
                        Map<String, Object> c = clusters.get(ci);
                        ctName = c.get("ct_codelist_name") != null ? c.get("ct_codelist_name").toString() : ctName;
                        // Add superset group VCDs
                        @SuppressWarnings("unchecked")
                        Map<String, Object> sup = (Map<String, Object>) c.get("supersetGroup");
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> supVcds = sup != null ? (List<Map<String, Object>>) sup.get("vcds") : null;
                        if (supVcds != null && !supVcds.isEmpty()) {
                            // Derive fingerprint from superset term list
                            String supTl = sup.get("termList") != null ? sup.get("termList").toString() : "";
                            String supFp = Arrays.stream(supTl.split(",\\s*")).map(t -> t.trim().toUpperCase()).sorted().collect(java.util.stream.Collectors.joining("||"));
                            if (seenFps.add(supFp)) {
                                Map<String, Object> sg = new LinkedHashMap<>();
                                sg.put("termList", supTl);
                                sg.put("termCount", sup.get("termCount"));
                                sg.put("vcds", supVcds);
                                sg.put("vcdCount", sup.get("vcdCount"));
                                sg.put("missingTerms", "");
                                sg.put("missingCount", 0);
                                subGroupsList.add(sg);
                            }
                            for (Map<String, Object> v : supVcds) allVcdsSet.add(v.get("vcd").toString());
                        }
                        // Collect union terms from superset
                        if (sup != null) {
                            String tl = sup.get("termList") != null ? sup.get("termList").toString() : "";
                            for (String t : tl.split(",\\s*")) { if (!t.trim().isEmpty()) unionTermsUpper.add(t.trim().toUpperCase()); }
                        }
                        // Add subset groups
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> subs = (List<Map<String, Object>>) c.get("subsetGroups");
                        if (subs != null) {
                            for (Map<String, Object> sg : subs) {
                                String sgTl = sg.get("termList") != null ? sg.get("termList").toString() : "";
                                String sgFp = Arrays.stream(sgTl.split(",\\s*")).map(t -> t.trim().toUpperCase()).sorted().collect(java.util.stream.Collectors.joining("||"));
                                if (seenFps.add(sgFp)) {
                                    subGroupsList.add(sg);
                                }
                                // Collect union terms from subset
                                for (String t : sgTl.split(",\\s*")) { if (!t.trim().isEmpty()) unionTermsUpper.add(t.trim().toUpperCase()); }
                                // Add VCDs (deduped by set)
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> sgVcds = (List<Map<String, Object>>) sg.get("vcds");
                                if (sgVcds != null) for (Map<String, Object> v : sgVcds) allVcdsSet.add(v.get("vcd").toString());
                            }
                        }
                    }

                    List<String> unionSorted = new ArrayList<>(unionTermsUpper);
                    java.util.Collections.sort(unionSorted);
                    // Recompute missingTerms for each sub-group relative to the union
                    for (Map<String, Object> sg : subGroupsList) {
                        Set<String> sgTerms = new HashSet<>();
                        String tl = sg.get("termList") != null ? sg.get("termList").toString() : "";
                        for (String t : tl.split(",\\s*")) { if (!t.trim().isEmpty()) sgTerms.add(t.trim().toUpperCase()); }
                        List<String> missing = new ArrayList<>();
                        for (String t : unionSorted) { if (!sgTerms.contains(t)) missing.add(t); }
                        sg.put("missingTerms", String.join(", ", missing));
                        sg.put("missingCount", missing.size());
                    }
                    subGroupsList.sort((a, b) -> ((Integer) b.get("termCount")).compareTo((Integer) a.get("termCount")));

                    Map<String, Object> combined = new LinkedHashMap<>();
                    combined.put("type", "ct_aligned");
                    combined.put("cluster_key", "ca:" + ctCode);
                    combined.put("ct_codelist_code", ctCode);
                    combined.put("ct_codelist_name", ctName);

                    Map<String, Object> supInfo = new LinkedHashMap<>();
                    supInfo.put("termList", String.join(", ", unionSorted));
                    supInfo.put("termCount", unionSorted.size());
                    supInfo.put("vcds", new ArrayList<>());
                    supInfo.put("vcdCount", 0);
                    combined.put("supersetGroup", supInfo);

                    combined.put("subsetGroups", subGroupsList);
                    List<String> allVcdsList = new ArrayList<>(allVcdsSet);
                    combined.put("allVcds", allVcdsList);
                    combined.put("totalVcdCount", allVcdsList.size());

                    // Build mergedTermDetails from the union of all VCDs' term rows
                    try {
                        Set<String> seenCodes = new LinkedHashSet<>();
                        List<Map<String, Object>> unionTermDetails = new ArrayList<>();
                        for (String vcd : allVcdsList) {
                            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                                "SELECT code, nci_term_code, code_des FROM sas_codelist_data " +
                                "WHERE project_id = ? AND username = ? AND vcd = ? ORDER BY cdnum, sort_order",
                                projectId, username, vcd);
                            for (Map<String, Object> row : rows) {
                                String cVal = row.get("code") != null ? row.get("code").toString().trim().toUpperCase() : "";
                                if (!cVal.isEmpty() && seenCodes.add(cVal)) unionTermDetails.add(row);
                            }
                        }
                        combined.put("mergedTermDetails", unionTermDetails);
                    } catch (Exception ignored) {}

                    newClusters.add(combined);
                    indicesToRemove.addAll(idxs);
                }
                if (!indicesToRemove.isEmpty()) {
                    java.util.Collections.sort(indicesToRemove, java.util.Collections.reverseOrder());
                    for (int idx : indicesToRemove) clusters.remove(idx);
                    clusters.addAll(newClusters);
                }
            }

            // Step 7: Append already-merged groups from merge_log
            Set<String> alreadyInClusters = new HashSet<>();
            for (Map<String, Object> c : clusters) {
                @SuppressWarnings("unchecked")
                List<String> av = (List<String>) c.get("allVcds");
                if (av != null) alreadyInClusters.addAll(av);
            }

            List<Map<String, Object>> mergedRules = jdbcTemplate.queryForList(
                "SELECT merged_vcd, merged_vlabel, MAX(merged_nci_code) AS merged_nci_code, " +
                "  MAX(merge_batch_id) AS merge_batch_id, " +
                "  MIN(merge_time) AS first_merge_time, " +
                "  GROUP_CONCAT(DISTINCT original_vcd ORDER BY original_vcd SEPARATOR '||') AS original_vcds " +
                "FROM sas_codelist_merge_log " +
                "WHERE project_id = ? AND username = ? " +
                "GROUP BY merged_vcd, merged_vlabel " +
                "ORDER BY MIN(merge_time) ASC, merged_vcd ASC",
                projectId, username);

            for (Map<String, Object> rule : mergedRules) {
                String mergedVcd = rule.get("merged_vcd").toString();
                String[] originalVcds = rule.get("original_vcds").toString().split("\\|\\|");
                String batchId = rule.get("merge_batch_id") != null ? rule.get("merge_batch_id").toString() : "";

                boolean overlaps = false;
                for (String ov : originalVcds) {
                    if (alreadyInClusters.contains(ov.trim())) { overlaps = true; break; }
                }
                if (overlaps) continue;

                int targetCount = 0;
                try {
                    targetCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                        Integer.class, projectId, username, mergedVcd);
                } catch (Exception ignored) {}
                if (targetCount == 0) continue;

                String mergedVlabel = rule.get("merged_vlabel") != null ? rule.get("merged_vlabel").toString() : "";
                String logNciCode = rule.get("merged_nci_code") != null ? rule.get("merged_nci_code").toString() : "";
                String realNciCode = logNciCode;
                try {
                    String dbNciCode = jdbcTemplate.queryForObject(
                        "SELECT nci_codelist_code FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ? LIMIT 1",
                        String.class, projectId, username, mergedVcd);
                    if (dbNciCode != null && !dbNciCode.isEmpty()) realNciCode = dbNciCode;
                } catch (Exception ignored) {}

                // Build term details
                List<Map<String, Object>> termRows = jdbcTemplate.queryForList(
                    "SELECT code, nci_term_code, code_des FROM sas_codelist_data " +
                    "WHERE project_id = ? AND username = ? AND vcd = ? ORDER BY cdnum, sort_order",
                    projectId, username, mergedVcd);
                List<String> termCodes = new ArrayList<>();
                for (Map<String, Object> tr : termRows) {
                    String code = tr.get("code") != null ? tr.get("code").toString().trim() : "";
                    if (!code.isEmpty()) termCodes.add(code);
                }

                Map<String, Object> cluster = new LinkedHashMap<>();
                cluster.put("type", "identity");
                cluster.put("already_merged", true);
                cluster.put("merged_target_vcd", mergedVcd);
                cluster.put("merged_target_vlabel", mergedVlabel);
                cluster.put("merged_target_nci_code", realNciCode);
                cluster.put("merge_batch_id", batchId);
                cluster.put("cluster_key", batchId.isEmpty() ? ("mv:" + mergedVcd) : ("bt:" + batchId));

                Map<String, Object> supersetInfo = new LinkedHashMap<>();
                supersetInfo.put("termList", String.join(", ", termCodes));
                supersetInfo.put("termCount", termCodes.size());

                // Look up each original VCD's pre-merge vlabel / nci_codelist_code from
                // the snapshot table so the "合并前" panel shows the TRUE original Names
                // (lets the user see who was merged, instead of seeing the merged Name everywhere).
                Map<String, String> origVlabelMap = new HashMap<>();
                Map<String, String> origNciMap = new HashMap<>();
                try {
                    List<Map<String, Object>> snapMeta = jdbcTemplate.queryForList(
                        "SELECT original_vcd, MAX(snap_vlabel) AS orig_vlabel, MAX(snap_nci_codelist_code) AS orig_nci " +
                        "FROM sas_codelist_merge_term_snapshot " +
                        "WHERE project_id = ? AND username = ? AND merged_vcd = ? GROUP BY original_vcd",
                        projectId, username, mergedVcd);
                    for (Map<String, Object> sm : snapMeta) {
                        String ov2 = sm.get("original_vcd") != null ? sm.get("original_vcd").toString() : "";
                        String ovl = sm.get("orig_vlabel") != null ? sm.get("orig_vlabel").toString() : "";
                        String onc = sm.get("orig_nci") != null ? sm.get("orig_nci").toString() : "";
                        if (!ov2.isEmpty()) {
                            origVlabelMap.put(ov2, ovl);
                            origNciMap.put(ov2, onc);
                        }
                    }
                } catch (Exception ignored) {}

                List<Map<String, Object>> vcdDetails = new ArrayList<>();
                for (String ov : originalVcds) {
                    String ovTrim = ov.trim();
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("vcd", ovTrim);
                    // Show the ORIGINAL vlabel (pre-merge) instead of the merged one.
                    detail.put("vlabel", origVlabelMap.getOrDefault(ovTrim, mergedVlabel));
                    detail.put("nciCode", origNciMap.getOrDefault(ovTrim, ""));
                    detail.put("specRefCount", 0);
                    detail.put("vlmRefCount", 0);
                    vcdDetails.add(detail);
                }
                supersetInfo.put("vcds", vcdDetails);
                supersetInfo.put("vcdCount", originalVcds.length);
                cluster.put("supersetGroup", supersetInfo);
                cluster.put("subsetGroups", new ArrayList<>());
                cluster.put("mergedTermDetails", termRows);
                cluster.put("allVcds", Arrays.asList(originalVcds));
                cluster.put("totalVcdCount", originalVcds.length);

                clusters.add(cluster);
            }

            // Sort by type weight, then content (no persistent numbering).
            clusters.sort((a, b) -> {
                int wA = typeWeight(a), wB = typeWeight(b);
                if (wA != wB) return Integer.compare(wA, wB);
                int vcA = a.get("totalVcdCount") != null ? ((Number) a.get("totalVcdCount")).intValue() : 0;
                int vcB = b.get("totalVcdCount") != null ? ((Number) b.get("totalVcdCount")).intValue() : 0;
                if (vcA != vcB) return Integer.compare(vcB, vcA);
                @SuppressWarnings("unchecked")
                Map<String, Object> supA = (Map<String, Object>) a.get("supersetGroup");
                @SuppressWarnings("unchecked")
                Map<String, Object> supB = (Map<String, Object>) b.get("supersetGroup");
                int tcA = supA != null && supA.get("termCount") != null ? ((Number) supA.get("termCount")).intValue() : 0;
                int tcB = supB != null && supB.get("termCount") != null ? ((Number) supB.get("termCount")).intValue() : 0;
                if (tcA != tcB) return Integer.compare(tcB, tcA);
                @SuppressWarnings("unchecked")
                List<String> avA = (List<String>) a.get("allVcds");
                @SuppressWarnings("unchecked")
                List<String> avB = (List<String>) b.get("allVcds");
                String firstA = (avA != null && !avA.isEmpty()) ? avA.get(0) : "";
                String firstB = (avB != null && !avB.isEmpty()) ? avB.get(0) : "";
                return firstA.compareTo(firstB);
            });

            return CommonResult.success(clusters);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("统一分析失败: " + e.getMessage());
        }
    }

    /**
     * Analyze codelists to find subset relationships (A's terms ⊆ B's terms).
     * Groups are built around the largest superset, with each subset listed.
     */
    @GetMapping("/merge/analyze-subsets/{projectId}")
    public CommonResult<List<Map<String, Object>>> analyzeSubsets(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();

            // Step 1: Get all VCDs and their term sets
            List<Map<String, Object>> vcdRows = jdbcTemplate.queryForList(
                "SELECT vcd, MAX(vlabel) AS vlabel, MAX(nci_codelist_code) AS nci_codelist_code, " +
                "  GROUP_CONCAT(TRIM(UPPER(code)) ORDER BY TRIM(UPPER(code)) SEPARATOR '||') AS term_key, " +
                "  GROUP_CONCAT(TRIM(code) ORDER BY TRIM(UPPER(code)) SEPARATOR ', ') AS term_list, " +
                "  COUNT(*) AS term_count " +
                "FROM sas_codelist_data WHERE project_id = ? AND username = ? GROUP BY vcd",
                projectId, username);

            // Build vcd -> term set map
            List<Map<String, Object>> vcdInfoList = new ArrayList<>();
            for (Map<String, Object> row : vcdRows) {
                String vcd = row.get("vcd").toString();
                String termKey = row.get("term_key") != null ? row.get("term_key").toString() : "";
                Set<String> termSet = new HashSet<>(Arrays.asList(termKey.split("\\|\\|")));
                Map<String, Object> info = new HashMap<>();
                info.put("vcd", vcd);
                info.put("vlabel", row.get("vlabel") != null ? row.get("vlabel").toString() : "");
                info.put("nciCode", row.get("nci_codelist_code") != null ? row.get("nci_codelist_code").toString() : "");
                info.put("termSet", termSet);
                info.put("termList", row.get("term_list") != null ? row.get("term_list").toString() : "");
                info.put("termCount", ((Number) row.get("term_count")).intValue());
                vcdInfoList.add(info);
            }

            // Step 2: Find subset relationships (skip identical sets - those are in the main analysis)
            // Build a map: supersetVcd -> list of subsetVcds
            // A is a subset of B if A ⊂ B (proper subset, not equal)
            Map<String, List<String>> supersetMap = new LinkedHashMap<>();
            Map<String, Map<String, Object>> infoMap = new HashMap<>();
            for (Map<String, Object> info : vcdInfoList) {
                infoMap.put(info.get("vcd").toString(), info);
            }

            Set<String> involvedAsSubset = new HashSet<>();

            for (int i = 0; i < vcdInfoList.size(); i++) {
                Map<String, Object> a = vcdInfoList.get(i);
                @SuppressWarnings("unchecked")
                Set<String> setA = (Set<String>) a.get("termSet");
                String vcdA = a.get("vcd").toString();

                for (int j = 0; j < vcdInfoList.size(); j++) {
                    if (i == j) continue;
                    Map<String, Object> b = vcdInfoList.get(j);
                    @SuppressWarnings("unchecked")
                    Set<String> setB = (Set<String>) b.get("termSet");

                    // Check if A ⊂ B (A is proper subset of B)
                    if (setA.size() < setB.size() && setB.containsAll(setA)) {
                        String vcdB = b.get("vcd").toString();
                        supersetMap.computeIfAbsent(vcdB, k -> new ArrayList<>()).add(vcdA);
                        involvedAsSubset.add(vcdA);
                    }
                }
            }

            // Step 3: Remove redundant superset entries (if A⊂B⊂C, only keep C as superset)
            // A superset entry should be removed if it is itself a subset of another superset
            Set<String> topLevelSupersets = new LinkedHashSet<>(supersetMap.keySet());
            topLevelSupersets.removeAll(involvedAsSubset);
            // Also include those that are subsets of others but still have their own subsets
            // (they'll be shown within the top-level group)

            // Step 4: Build result groups
            List<Map<String, Object>> groups = new ArrayList<>();
            Set<String> processed = new HashSet<>();

            for (String supVcd : topLevelSupersets) {
                if (processed.contains(supVcd)) continue;
                List<String> subsets = supersetMap.get(supVcd);
                if (subsets == null || subsets.isEmpty()) continue;

                // Deduplicate and remove any that are also top-level (chain handling)
                Set<String> uniqueSubsets = new LinkedHashSet<>(subsets);
                uniqueSubsets.remove(supVcd);

                Map<String, Object> group = new LinkedHashMap<>();
                Map<String, Object> supInfo = infoMap.get(supVcd);

                group.put("supersetVcd", supVcd);
                group.put("supersetVlabel", supInfo.get("vlabel"));
                group.put("supersetNciCode", supInfo.get("nciCode"));
                group.put("supersetTermList", supInfo.get("termList"));
                group.put("supersetTermCount", supInfo.get("termCount"));

                // Build subset details
                List<Map<String, Object>> subsetDetails = new ArrayList<>();
                for (String subVcd : uniqueSubsets) {
                    Map<String, Object> subInfo = infoMap.get(subVcd);
                    if (subInfo == null) continue;

                    @SuppressWarnings("unchecked")
                    Set<String> subTermSet = (Set<String>) subInfo.get("termSet");
                    @SuppressWarnings("unchecked")
                    Set<String> supTermSet = (Set<String>) supInfo.get("termSet");

                    // Find which terms in superset are missing from subset
                    Set<String> missingTerms = new LinkedHashSet<>(supTermSet);
                    missingTerms.removeAll(subTermSet);

                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("vcd", subVcd);
                    detail.put("vlabel", subInfo.get("vlabel"));
                    detail.put("nciCode", subInfo.get("nciCode"));
                    detail.put("termList", subInfo.get("termList"));
                    detail.put("termCount", subInfo.get("termCount"));
                    detail.put("missingTerms", String.join(", ", missingTerms));
                    detail.put("missingCount", missingTerms.size());
                    subsetDetails.add(detail);
                }

                // Sort subsets by term count descending
                subsetDetails.sort((x, y) -> ((Integer) y.get("termCount")).compareTo((Integer) x.get("termCount")));

                group.put("subsets", subsetDetails);
                group.put("subsetCount", subsetDetails.size());
                groups.add(group);

                processed.add(supVcd);
                processed.addAll(uniqueSubsets);
            }

            // Sort groups by subset count descending
            groups.sort((a, b) -> ((Integer) b.get("subsetCount")).compareTo((Integer) a.get("subsetCount")));

            return CommonResult.success(groups);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("子集分析失败: " + e.getMessage());
        }
    }

    /**
     * Suggest a CT codelist matching a given set of terms.
     * Returns diagnostic info when no exact match: reason, fuzzy suggestions.
     */
    @PostMapping("/merge/ct-suggest")
    public CommonResult<Map<String, Object>> ctSuggest(@RequestBody Map<String, Object> request) {
        try {
            String projectId = (String) request.get("projectId");
            @SuppressWarnings("unchecked")
            List<String> terms = (List<String>) request.get("terms");
            if (terms == null || terms.isEmpty()) {
                return CommonResult.failed("terms 不能为空");
            }

            ProjectConfigPO config = projectConfigMapper.selectByProjectId(projectId);
            if (config == null) config = projectConfigMapper.selectByProjectId("DEFAULT");

            Map<String, Object> result = new LinkedHashMap<>();

            if (config == null) {
                result.put("reason", "项目未配置，请在项目配置中设置Standard Type和CT Version");
                return CommonResult.success(result);
            }

            String standardType = config.getStandardType();
            String ctVersion = config.getCtVersion();
            result.put("standardType", standardType != null ? standardType : "未设置");
            result.put("ctVersion", ctVersion != null ? ctVersion : "未设置");

            Long packageId = resolvePackageId(config);
            if (packageId == null) {
                result.put("reason", "未找到CT数据包，请确认项目配置的Standard Type（当前: " +
                    (standardType != null ? standardType : "未设置") + "）和CT Version（当前: " +
                    (ctVersion != null ? ctVersion : "未设置") + "）是否正确，以及CT数据是否已导入");
                return CommonResult.success(result);
            }

            // Try exact match first
            Map<String, String> exactMatch = findCtCodelistByTerms(packageId, terms.toArray(new String[0]));
            if (exactMatch != null) {
                result.put("codelist_code", exactMatch.get("codelist_code"));
                result.put("codelist_name", exactMatch.get("codelist_name"));
                result.put("matchType", "exact");
                return CommonResult.success(result);
            }

            // Fuzzy match: find codelists where at least 1 input term matches, rank by match ratio
            StringBuilder placeholders = new StringBuilder();
            List<Object> fuzzyParams = new ArrayList<>();
            fuzzyParams.add(packageId);
            for (int i = 0; i < terms.size(); i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
                fuzzyParams.add(terms.get(i).trim().toUpperCase());
            }

            try {
                List<Map<String, Object>> fuzzyRows = jdbcTemplate.queryForList(
                    "SELECT ct.codelist_code, MAX(ct.codelist_name) AS codelist_name, " +
                    "  COUNT(DISTINCT UPPER(TRIM(ct.cdisc_submission_value))) AS matched_count, " +
                    "  (SELECT COUNT(*) FROM ct_term ct2 WHERE ct2.package_id = ct.package_id " +
                    "    AND ct2.codelist_code = ct.codelist_code AND ct2.codelist_code IS NOT NULL) AS total_count " +
                    "FROM ct_term ct " +
                    "WHERE ct.package_id = ? AND ct.codelist_code IS NOT NULL " +
                    "  AND UPPER(TRIM(ct.cdisc_submission_value)) IN (" + placeholders + ") " +
                    "GROUP BY ct.codelist_code, ct.package_id " +
                    "ORDER BY matched_count DESC " +
                    "LIMIT 5",
                    fuzzyParams.toArray()
                );

                if (!fuzzyRows.isEmpty()) {
                    List<Map<String, Object>> suggestions = new ArrayList<>();
                    for (Map<String, Object> row : fuzzyRows) {
                        Map<String, Object> s = new LinkedHashMap<>();
                        s.put("codelist_code", row.get("codelist_code").toString());
                        s.put("codelist_name", row.get("codelist_name") != null ? row.get("codelist_name").toString() : "");
                        s.put("matched_count", ((Number) row.get("matched_count")).intValue());
                        s.put("total_count", ((Number) row.get("total_count")).intValue());
                        suggestions.add(s);
                    }
                    result.put("suggestions", suggestions);
                    result.put("reason", "未找到精确匹配（所有" + terms.size() + "个Term完全一致的CT Codelist），以下是部分匹配的建议");
                    result.put("matchType", "fuzzy");
                } else {
                    result.put("reason", "在CT包中未找到包含任何输入Term的Codelist。请确认Term值是否与CDISC标准提交值一致");
                    result.put("matchType", "none");
                }
            } catch (Exception e) {
                result.put("reason", "CT模糊查询异常: " + e.getMessage());
                result.put("matchType", "error");
            }

            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("CT查询失败: " + e.getMessage());
        }
    }

    /**
     * Execute codelist merge: consolidate identical term groups into one codelist and backfill references.
     */
    @PostMapping("/merge/execute/{projectId}")
    public CommonResult<Map<String, Object>> executeMerge(@PathVariable String projectId, @RequestBody Map<String, Object> request) {
        try {
            String username = UserContext.getUsername();
            String targetVcd = (String) request.get("targetVcd");
            String targetVlabel = (String) request.get("targetVlabel");
            String targetNciCode = request.get("targetNciCode") != null ? (String) request.get("targetNciCode") : "";
            // Optional: a batchId to tie multiple merges from the same original candidate
            // group together (so analyzeUnified can re-group them as #N-1, #N-2, ...).
            String batchId = request.get("batchId") != null ? String.valueOf(request.get("batchId")) : "";
            if (batchId == null || batchId.trim().isEmpty()) {
                batchId = java.util.UUID.randomUUID().toString();
            }
            @SuppressWarnings("unchecked")
            List<String> sourceVcds = (List<String>) request.get("sourceVcds");

            if (targetVcd == null || targetVcd.isEmpty() || sourceVcds == null || sourceVcds.isEmpty()) {
                return CommonResult.failed("targetVcd 和 sourceVcds 不能为空");
            }

            ensureMergeLogTable();
            ensureMergeRefLogTable();
            ensureMergeTermSnapshotTable();

            // Deduplicate source VCDs (preserve order)
            LinkedHashSet<String> sourceSet = new LinkedHashSet<>(sourceVcds);

            // Step 0: Snapshot each source VCD's term rows BEFORE any modification, so undo
            // can restore each one to its EXACT original state (preserving subset relationships).
            for (String src : sourceSet) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT vlabel, nci_codelist_code, type, terminology, comment, cdnum, code, nci_term_code, code_des, code_ver, flag, sort_order " +
                    "FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                    projectId, username, src);
                for (Map<String, Object> row : rows) {
                    jdbcTemplate.update(
                        "INSERT INTO sas_codelist_merge_term_snapshot " +
                        "(project_id, username, merged_vcd, original_vcd, snap_vlabel, snap_nci_codelist_code, " +
                        " snap_type, snap_terminology, snap_comment, snap_cdnum, snap_code, snap_nci_term_code, " +
                        " snap_code_des, snap_code_ver, snap_flag, snap_sort_order) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        projectId, username, targetVcd, src,
                        row.get("vlabel"), row.get("nci_codelist_code"), row.get("type"),
                        row.get("terminology"), row.get("comment"), row.get("cdnum"),
                        row.get("code"), row.get("nci_term_code"), row.get("code_des"),
                        row.get("code_ver"), row.get("flag"), row.get("sort_order"));
                }
            }

            // Determine keepVcd: prefer one that already equals targetVcd (no rename needed),
            // otherwise pick the first source. This avoids the bug where Step 2 deletes the
            // newly-renamed target rows when targetVcd happens to be one of the sources.
            String keepVcd = null;
            for (String v : sourceSet) {
                if (v.equals(targetVcd)) { keepVcd = v; break; }
            }
            if (keepVcd == null) keepVcd = sourceSet.iterator().next();

            // Step 1: Update the kept VCD's rows to the target ID / name / nci_code (skip rename if equal)
            if (!keepVcd.equals(targetVcd)) {
                jdbcTemplate.update(
                    "UPDATE sas_codelist_data SET vcd = ?, vlabel = ?, nci_codelist_code = ?, updated_by = 'merge_codelist', updated_time = NOW() " +
                    "WHERE project_id = ? AND username = ? AND vcd = ?",
                    targetVcd, targetVlabel, targetNciCode, projectId, username, keepVcd);
            } else {
                jdbcTemplate.update(
                    "UPDATE sas_codelist_data SET vlabel = ?, nci_codelist_code = ?, updated_by = 'merge_codelist', updated_time = NOW() " +
                    "WHERE project_id = ? AND username = ? AND vcd = ?",
                    targetVlabel, targetNciCode, projectId, username, targetVcd);
            }

            // Step 2: Delete duplicate term rows from other source VCDs.
            // CRITICAL: skip targetVcd to avoid wiping the rows we just set in Step 1.
            for (String srcVcd : sourceSet) {
                if (srcVcd.equals(keepVcd)) continue;
                if (srcVcd.equals(targetVcd)) continue;
                jdbcTemplate.update(
                    "DELETE FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                    projectId, username, srcVcd);
            }

            // Step 3: Snapshot spec rows BEFORE updating, then backfill — for each source VCD
            // that will be redirected to targetVcd, capture (id, original_codelist) so we can
            // restore on undo. Only log rows that will actually be redirected (codelist != targetVcd).
            for (String srcVcd : sourceSet) {
                if (srcVcd.equals(targetVcd)) continue;
                List<Map<String, Object>> specRows = jdbcTemplate.queryForList(
                    "SELECT id FROM sas_project_spec WHERE project_id = ? AND username = ? AND codelist = ?",
                    projectId, username, srcVcd);
                for (Map<String, Object> row : specRows) {
                    jdbcTemplate.update(
                        "INSERT INTO sas_codelist_merge_ref_log (project_id, username, merged_vcd, ref_type, ref_row_id, original_codelist) VALUES (?, ?, ?, 'spec', ?, ?)",
                        projectId, username, targetVcd, ((Number) row.get("id")).longValue(), srcVcd);
                }
                jdbcTemplate.update(
                    "UPDATE sas_project_spec SET codelist = ?, updated_by = 'merge_codelist', updated_time = NOW() " +
                    "WHERE project_id = ? AND username = ? AND codelist = ?",
                    targetVcd, projectId, username, srcVcd);
            }

            // Step 4: Same for VLM rows
            for (String srcVcd : sourceSet) {
                if (srcVcd.equals(targetVcd)) continue;
                List<Map<String, Object>> vlmRows = jdbcTemplate.queryForList(
                    "SELECT id FROM sas_vlm_data WHERE project_id = ? AND username = ? AND codelist = ?",
                    projectId, username, srcVcd);
                for (Map<String, Object> row : vlmRows) {
                    jdbcTemplate.update(
                        "INSERT INTO sas_codelist_merge_ref_log (project_id, username, merged_vcd, ref_type, ref_row_id, original_codelist) VALUES (?, ?, ?, 'vlm', ?, ?)",
                        projectId, username, targetVcd, ((Number) row.get("id")).longValue(), srcVcd);
                }
                jdbcTemplate.update(
                    "UPDATE sas_vlm_data SET codelist = ?, updated_by = 'merge_codelist', updated_time = NOW() " +
                    "WHERE project_id = ? AND username = ? AND codelist = ?",
                    targetVcd, projectId, username, srcVcd);
            }

            // Step 5: Write merge log (with NCI Codelist Code and batch id)
            for (String srcVcd : sourceSet) {
                jdbcTemplate.update(
                    "INSERT INTO sas_codelist_merge_log (project_id, username, original_vcd, merged_vcd, merged_vlabel, merged_nci_code, merge_batch_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    projectId, username, srcVcd, targetVcd, targetVlabel, targetNciCode, batchId);
            }

            // Step 6: Re-read the actual term rows of the merged target so the frontend
            // can show the TRUE post-merge terms (important when source was a subset_chain
            // — the merged target's terms reflect ONLY what the kept VCD had, not the
            // union of all source VCDs' terms).
            List<Map<String, Object>> mergedTermRows = new ArrayList<>();
            try {
                mergedTermRows = jdbcTemplate.queryForList(
                    "SELECT code, nci_term_code, code_des FROM sas_codelist_data " +
                    "WHERE project_id = ? AND username = ? AND vcd = ? ORDER BY cdnum, sort_order",
                    projectId, username, targetVcd);
            } catch (Exception ignored) {}

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", String.format(
                "合并完成：%d 个Codelist合并为 [%s] %s，已回填Variables和ValueLevel引用",
                sourceSet.size(), targetVcd, targetVlabel));
            data.put("targetVcd", targetVcd);
            data.put("targetVlabel", targetVlabel);
            data.put("targetNciCode", targetNciCode);
            data.put("batchId", batchId);
            data.put("mergedTermDetails", mergedTermRows);
            return CommonResult.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("合并执行失败: " + e.getMessage());
        }
    }

    /**
     * Get merge history log for a project.
     */
    @GetMapping("/merge/log/{projectId}")
    public CommonResult<List<Map<String, Object>>> getMergeLog(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            ensureMergeLogTable();
            List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT original_vcd, merged_vcd, merged_vlabel, merged_nci_code, merge_time FROM sas_codelist_merge_log " +
                "WHERE project_id = ? AND username = ? ORDER BY merge_time DESC",
                projectId, username);
            return CommonResult.success(logs);
        } catch (Exception e) {
            return CommonResult.success(new ArrayList<>());
        }
    }

    /**
     * Get distinct merge rules from log: grouped by merged_vcd.
     * Returns rules where at least 2 of the original_vcds still exist in sas_codelist_data as separate VCDs
     * (i.e., they were re-extracted after a previous merge and can be merged again).
     */
    @GetMapping("/merge/pending-rules/{projectId}")
    public CommonResult<List<Map<String, Object>>> getPendingMergeRules(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            ensureMergeLogTable();

            // Get all distinct merge rules grouped by target
            List<Map<String, Object>> rules = jdbcTemplate.queryForList(
                "SELECT merged_vcd, merged_vlabel, MAX(merged_nci_code) AS merged_nci_code, " +
                "  GROUP_CONCAT(DISTINCT original_vcd ORDER BY original_vcd SEPARATOR '||') AS original_vcds " +
                "FROM sas_codelist_merge_log " +
                "WHERE project_id = ? AND username = ? " +
                "GROUP BY merged_vcd, merged_vlabel " +
                "ORDER BY MAX(merge_time) DESC",
                projectId, username);

            // Get all current VCDs in codelist
            Set<String> currentVcds = new HashSet<>(
                jdbcTemplate.queryForList(
                    "SELECT DISTINCT vcd FROM sas_codelist_data WHERE project_id = ? AND username = ?",
                    String.class, projectId, username));

            List<Map<String, Object>> pendingRules = new ArrayList<>();
            for (Map<String, Object> rule : rules) {
                String mergedVcd = rule.get("merged_vcd").toString();
                String mergedVlabel = rule.get("merged_vlabel") != null ? rule.get("merged_vlabel").toString() : "";
                String[] originalVcds = rule.get("original_vcds").toString().split("\\|\\|");

                // Find which original VCDs currently exist as separate VCDs (re-extracted)
                List<String> existingOriginals = new ArrayList<>();
                for (String ov : originalVcds) {
                    String trimmed = ov.trim();
                    if (!trimmed.equals(mergedVcd) && currentVcds.contains(trimmed)) {
                        existingOriginals.add(trimmed);
                    }
                }

                // Also check if the merged target VCD itself already exists (would be the "keep" one)
                boolean targetExists = currentVcds.contains(mergedVcd);

                String mergedNciCode = rule.get("merged_nci_code") != null ? rule.get("merged_nci_code").toString() : "";

                if (existingOriginals.size() >= 2 || (targetExists && existingOriginals.size() >= 1)) {
                    Map<String, Object> pending = new HashMap<>();
                    pending.put("mergedVcd", mergedVcd);
                    pending.put("mergedVlabel", mergedVlabel);
                    pending.put("mergedNciCode", mergedNciCode);
                    pending.put("originalVcds", String.join("||", originalVcds));
                    List<String> toMerge = new ArrayList<>(existingOriginals);
                    if (targetExists && !toMerge.contains(mergedVcd)) {
                        toMerge.add(0, mergedVcd);
                    }
                    pending.put("sourceVcds", toMerge);
                    pending.put("sourceCount", toMerge.size());
                    pendingRules.add(pending);
                }
            }

            return CommonResult.success(pendingRules);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("查询待合并规则失败: " + e.getMessage());
        }
    }

    /**
     * Re-apply all pending merge rules from history in one shot.
     */
    @PostMapping("/merge/reapply-all/{projectId}")
    public CommonResult<String> reapplyAllMerges(@PathVariable String projectId) {
        try {
            String username = UserContext.getUsername();
            ensureMergeLogTable();

            CommonResult<?> rulesResult = getPendingMergeRules(projectId);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pendingRules = (List<Map<String, Object>>) rulesResult.getData();

            if (pendingRules == null || pendingRules.isEmpty()) {
                return CommonResult.success("没有需要重新合并的规则");
            }

            int mergedCount = 0;
            int totalVcds = 0;
            StringBuilder details = new StringBuilder();

            for (Map<String, Object> rule : pendingRules) {
                String targetVcd = rule.get("mergedVcd").toString();
                String targetVlabel = rule.get("mergedVlabel") != null ? rule.get("mergedVlabel").toString() : "";
                String targetNciCode = rule.get("mergedNciCode") != null ? rule.get("mergedNciCode").toString() : "";
                @SuppressWarnings("unchecked")
                List<String> sourceVcds = (List<String>) rule.get("sourceVcds");

                if (sourceVcds == null || sourceVcds.size() < 2) continue;

                // Allocate a fresh batch_id for this rule so the merge participates in
                // the merge_log / snapshot infrastructure as a manual executeMerge call.
                String batchId = java.util.UUID.randomUUID().toString();

                // Pick the first source as the one to keep
                String keepVcd = sourceVcds.get(0);

                // Snapshot every source VCD's terms before mutation so undo can restore.
                ensureMergeLogTable();
                for (String srcVcd : sourceVcds) {
                    try {
                        jdbcTemplate.update(
                            "INSERT INTO sas_codelist_merge_term_snapshot " +
                            "(project_id, username, merged_vcd, original_vcd, snap_vcd, snap_vlabel, snap_nci_codelist_code, " +
                            " snap_type, snap_cdnum, snap_code, snap_nci_term_code, snap_code_des, snap_origin, snap_sort_order) " +
                            "SELECT project_id, username, ?, vcd, vcd, vlabel, nci_codelist_code, " +
                            "       type, cdnum, code, nci_term_code, code_des, origin, sort_order " +
                            "FROM sas_codelist_data " +
                            "WHERE project_id = ? AND username = ? AND vcd = ?",
                            targetVcd, projectId, username, srcVcd);
                    } catch (Exception ignored) {}
                }

                // Update kept VCD to target (including nci_codelist_code)
                jdbcTemplate.update(
                    "UPDATE sas_codelist_data SET vcd = ?, vlabel = ?, nci_codelist_code = ?, updated_by = 'merge_reapply', updated_time = NOW() " +
                    "WHERE project_id = ? AND username = ? AND vcd = ?",
                    targetVcd, targetVlabel, targetNciCode, projectId, username, keepVcd);

                // Delete other VCDs
                for (int i = 1; i < sourceVcds.size(); i++) {
                    String removeVcd = sourceVcds.get(i);
                    jdbcTemplate.update(
                        "DELETE FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                        projectId, username, removeVcd);
                }

                // Backfill spec and vlm + record the original codelist references for undo
                for (String srcVcd : sourceVcds) {
                    try {
                        jdbcTemplate.update(
                            "INSERT INTO sas_codelist_merge_ref_log " +
                            "(project_id, username, ref_table, ref_id, original_codelist, merged_vcd) " +
                            "SELECT project_id, username, 'spec', id, codelist, ? FROM sas_project_spec " +
                            "WHERE project_id = ? AND username = ? AND codelist = ?",
                            targetVcd, projectId, username, srcVcd);
                        jdbcTemplate.update(
                            "INSERT INTO sas_codelist_merge_ref_log " +
                            "(project_id, username, ref_table, ref_id, original_codelist, merged_vcd) " +
                            "SELECT project_id, username, 'vlm', id, codelist, ? FROM sas_vlm_data " +
                            "WHERE project_id = ? AND username = ? AND codelist = ?",
                            targetVcd, projectId, username, srcVcd);
                    } catch (Exception ignored) {}
                    jdbcTemplate.update(
                        "UPDATE sas_project_spec SET codelist = ?, updated_by = 'merge_reapply', updated_time = NOW() " +
                        "WHERE project_id = ? AND username = ? AND codelist = ?",
                        targetVcd, projectId, username, srcVcd);
                    jdbcTemplate.update(
                        "UPDATE sas_vlm_data SET codelist = ?, updated_by = 'merge_reapply', updated_time = NOW() " +
                        "WHERE project_id = ? AND username = ? AND codelist = ?",
                        targetVcd, projectId, username, srcVcd);
                }

                // Write merge log with the new batch_id
                for (String srcVcd : sourceVcds) {
                    jdbcTemplate.update(
                        "INSERT INTO sas_codelist_merge_log (project_id, username, original_vcd, merged_vcd, merged_vlabel, merged_nci_code, merge_batch_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        projectId, username, srcVcd, targetVcd, targetVlabel, targetNciCode, batchId);
                }

                mergedCount++;
                totalVcds += sourceVcds.size();
                details.append(String.format("[%s] %d个VCD; ", targetVcd, sourceVcds.size()));
            }

            return CommonResult.success(String.format(
                "一键合并完成：%d 组规则，共 %d 个Codelist。%s",
                mergedCount, totalVcds, details.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("一键合并失败: " + e.getMessage());
        }
    }

    /**
     * Undo a merge: restore original VCDs from merge log.
     */
    @PostMapping("/merge/undo/{projectId}")
    public CommonResult<String> undoMerge(@PathVariable String projectId, @RequestBody Map<String, Object> request) {
        try {
            String username = UserContext.getUsername();
            String mergedVcd = (String) request.get("mergedVcd");
            if (mergedVcd == null || mergedVcd.isEmpty()) {
                return CommonResult.failed("mergedVcd 不能为空");
            }

            ensureMergeLogTable();
            ensureMergeRefLogTable();
            ensureMergeTermSnapshotTable();

            List<Map<String, Object>> logs = jdbcTemplate.queryForList(
                "SELECT original_vcd, merged_vlabel FROM sas_codelist_merge_log " +
                "WHERE project_id = ? AND username = ? AND merged_vcd = ? ORDER BY id",
                projectId, username, mergedVcd);

            if (logs.isEmpty()) {
                return CommonResult.failed("未找到该合并记录");
            }

            // Collect all distinct original VCDs
            Set<String> originalVcdSet = new LinkedHashSet<>();
            for (Map<String, Object> log : logs) {
                originalVcdSet.add(log.get("original_vcd").toString());
            }

            // Step 1: Delete current mergedVcd rows (so we can recreate originals cleanly)
            // and any leftover original_vcd rows (defensive — in case undo ran twice or there are stragglers).
            if (!originalVcdSet.contains(mergedVcd)) {
                jdbcTemplate.update(
                    "DELETE FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                    projectId, username, mergedVcd);
            }
            for (String origVcd : originalVcdSet) {
                jdbcTemplate.update(
                    "DELETE FROM sas_codelist_data WHERE project_id = ? AND username = ? AND vcd = ?",
                    projectId, username, origVcd);
            }

            // Step 2: Restore each original VCD from the term snapshot (preserves original term sets,
            // including subset relationships). Fallback to copying mergedVcd's terms if no snapshot
            // exists (for legacy merges done before snapshot tracking was added).
            int restored = 0;
            List<Map<String, Object>> snaps = jdbcTemplate.queryForList(
                "SELECT * FROM sas_codelist_merge_term_snapshot " +
                "WHERE project_id = ? AND username = ? AND merged_vcd = ? " +
                "ORDER BY original_vcd, snap_cdnum, snap_sort_order",
                projectId, username, mergedVcd);
            if (!snaps.isEmpty()) {
                for (Map<String, Object> snap : snaps) {
                    CodelistDataPO row = new CodelistDataPO();
                    row.setProjectId(projectId);
                    row.setUsername(username);
                    row.setVcd(snap.get("original_vcd").toString());
                    row.setVlabel((String) snap.get("snap_vlabel"));
                    row.setNciCodelistCode((String) snap.get("snap_nci_codelist_code"));
                    row.setType((String) snap.get("snap_type"));
                    row.setTerminology((String) snap.get("snap_terminology"));
                    row.setComment((String) snap.get("snap_comment"));
                    Object cdnum = snap.get("snap_cdnum");
                    if (cdnum != null) row.setCdnum(((Number) cdnum).intValue());
                    row.setCode((String) snap.get("snap_code"));
                    row.setNciTermCode((String) snap.get("snap_nci_term_code"));
                    row.setCodeDes((String) snap.get("snap_code_des"));
                    Object sortOrder = snap.get("snap_sort_order");
                    if (sortOrder != null) row.setSortOrder(((Number) sortOrder).intValue());
                    row.setCreatedBy("merge_undo");
                    codelistDataMapper.insert(row);
                    restored++;
                }
            } else {
                // Legacy fallback: copy mergedVcd's terms to each originalVcd
                List<CodelistDataPO> mergedTerms = codelistDataMapper.selectByProjectIdAndVcd(projectId, mergedVcd, username);
                for (String originalVcd : originalVcdSet) {
                    if (originalVcd.equals(mergedVcd)) continue;
                    for (CodelistDataPO term : mergedTerms) {
                        CodelistDataPO newTerm = new CodelistDataPO();
                        newTerm.setProjectId(projectId);
                        newTerm.setUsername(username);
                        newTerm.setVcd(originalVcd);
                        newTerm.setVlabel(term.getVlabel());
                        newTerm.setNciCodelistCode(term.getNciCodelistCode());
                        newTerm.setType(term.getType());
                        newTerm.setTerminology(term.getTerminology());
                        newTerm.setComment(term.getComment());
                        newTerm.setCdnum(term.getCdnum());
                        newTerm.setCode(term.getCode());
                        newTerm.setNciTermCode(term.getNciTermCode());
                        newTerm.setCodeDes(term.getCodeDes());
                        newTerm.setSortOrder(term.getSortOrder());
                        newTerm.setCreatedBy("merge_undo");
                        codelistDataMapper.insert(newTerm);
                        restored++;
                    }
                }
            }

            // Step 3: Precise restore of spec/vlm references using ref_log.
            // Each ref_log row records (ref_row_id, original_codelist) — restore each row to its exact original.
            int specRestored = 0, vlmRestored = 0;
            List<Map<String, Object>> refLogs = jdbcTemplate.queryForList(
                "SELECT ref_type, ref_row_id, original_codelist FROM sas_codelist_merge_ref_log " +
                "WHERE project_id = ? AND username = ? AND merged_vcd = ?",
                projectId, username, mergedVcd);
            for (Map<String, Object> r : refLogs) {
                String refType = r.get("ref_type").toString();
                long refRowId = ((Number) r.get("ref_row_id")).longValue();
                String origCodelist = r.get("original_codelist").toString();
                if ("spec".equals(refType)) {
                    int n = jdbcTemplate.update(
                        "UPDATE sas_project_spec SET codelist = ?, updated_by = 'merge_undo' " +
                        "WHERE id = ? AND project_id = ? AND username = ?",
                        origCodelist, refRowId, projectId, username);
                    specRestored += n;
                } else if ("vlm".equals(refType)) {
                    int n = jdbcTemplate.update(
                        "UPDATE sas_vlm_data SET codelist = ?, updated_by = 'merge_undo' " +
                        "WHERE id = ? AND project_id = ? AND username = ?",
                        origCodelist, refRowId, projectId, username);
                    vlmRestored += n;
                }
            }

            // Fallback: any spec/vlm rows still pointing to mergedVcd (e.g., from very old merges
            // that lacked ref_log) — fall back to the first original VCD so at least nothing is orphaned.
            String firstOriginalVcd = originalVcdSet.iterator().next();
            if (!firstOriginalVcd.equals(mergedVcd)) {
                jdbcTemplate.update(
                    "UPDATE sas_project_spec SET codelist = ?, updated_by = 'merge_undo' " +
                    "WHERE project_id = ? AND username = ? AND codelist = ?",
                    firstOriginalVcd, projectId, username, mergedVcd);
                jdbcTemplate.update(
                    "UPDATE sas_vlm_data SET codelist = ?, updated_by = 'merge_undo' " +
                    "WHERE project_id = ? AND username = ? AND codelist = ?",
                    firstOriginalVcd, projectId, username, mergedVcd);
            }

            // Step 4: Cleanup logs (merge_log, ref_log, term snapshot).
            jdbcTemplate.update(
                "DELETE FROM sas_codelist_merge_log WHERE project_id = ? AND username = ? AND merged_vcd = ?",
                projectId, username, mergedVcd);
            jdbcTemplate.update(
                "DELETE FROM sas_codelist_merge_ref_log WHERE project_id = ? AND username = ? AND merged_vcd = ?",
                projectId, username, mergedVcd);
            jdbcTemplate.update(
                "DELETE FROM sas_codelist_merge_term_snapshot WHERE project_id = ? AND username = ? AND merged_vcd = ?",
                projectId, username, mergedVcd);

            return CommonResult.success(String.format(
                "撤销完成：恢复 %d 个原始Codelist（%d 条Term），还原 %d 条Variables引用、%d 条VLM引用",
                logs.size(), restored, specRestored, vlmRestored));
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("撤销合并失败: " + e.getMessage());
        }
    }

    /**
     * Undo an ENTIRE merge batch: undoes all merged_vcds tied to the same merge_batch_id.
     * Use this for multi-target merges so a single click restores the source cluster fully.
     */
    @PostMapping("/merge/undo-batch/{projectId}")
    public CommonResult<String> undoBatch(@PathVariable String projectId, @RequestBody Map<String, Object> request) {
        try {
            String username = UserContext.getUsername();
            String batchId = (String) request.get("batchId");
            if (batchId == null || batchId.isEmpty()) {
                return CommonResult.failed("batchId 不能为空");
            }
            ensureMergeLogTable();

            List<String> mergedVcds = jdbcTemplate.queryForList(
                "SELECT DISTINCT merged_vcd FROM sas_codelist_merge_log " +
                "WHERE project_id = ? AND username = ? AND merge_batch_id = ? ORDER BY merged_vcd",
                String.class, projectId, username, batchId);
            if (mergedVcds.isEmpty()) {
                return CommonResult.failed("未找到该批次的合并记录");
            }

            int success = 0;
            List<String> failed = new ArrayList<>();
            for (String mv : mergedVcds) {
                Map<String, Object> req = new HashMap<>();
                req.put("mergedVcd", mv);
                CommonResult<String> r = undoMerge(projectId, req);
                if (r != null && r.isSuccess()) success++;
                else failed.add(mv);
            }

            String msg = String.format("批次撤销完成：成功 %d/%d", success, mergedVcds.size());
            if (!failed.isEmpty()) msg += "，失败: " + String.join(", ", failed);
            return CommonResult.success(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return CommonResult.failed("批次撤销失败: " + e.getMessage());
        }
    }

    // --- Merge helper methods ---

    private void ensureMergeLogTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sas_codelist_merge_log (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  project_id VARCHAR(64) DEFAULT NULL," +
                "  username VARCHAR(100) DEFAULT NULL," +
                "  original_vcd VARCHAR(200) DEFAULT NULL," +
                "  merged_vcd VARCHAR(200) DEFAULT NULL," +
                "  merged_vlabel VARCHAR(500) DEFAULT NULL," +
                "  merged_nci_code VARCHAR(100) DEFAULT NULL," +
                "  merge_batch_id VARCHAR(64) DEFAULT NULL," +
                "  merge_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  KEY idx_project (project_id, username)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        } catch (Exception ignored) {}
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sas_codelist_merge_log' AND COLUMN_NAME = 'merged_nci_code'",
                Integer.class);
            if (cnt == null || cnt == 0) {
                jdbcTemplate.execute("ALTER TABLE sas_codelist_merge_log ADD COLUMN merged_nci_code VARCHAR(100) DEFAULT NULL AFTER merged_vlabel");
            }
        } catch (Exception ignored) {}
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sas_codelist_merge_log' AND COLUMN_NAME = 'merge_batch_id'",
                Integer.class);
            if (cnt == null || cnt == 0) {
                jdbcTemplate.execute("ALTER TABLE sas_codelist_merge_log ADD COLUMN merge_batch_id VARCHAR(64) DEFAULT NULL AFTER merged_nci_code");
            }
        } catch (Exception ignored) {}
    }

    /**
     * Tracking table for spec/vlm reference updates done by a merge.
     * Each row records that ref_row_id (in sas_project_spec or sas_vlm_data) used to point
     * to original_codelist before being redirected to merged_vcd.
     * On undo, we use this to restore each reference to its EXACT pre-merge value.
     */
    private void ensureMergeRefLogTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sas_codelist_merge_ref_log (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  project_id VARCHAR(64) DEFAULT NULL," +
                "  username VARCHAR(100) DEFAULT NULL," +
                "  merged_vcd VARCHAR(200) DEFAULT NULL," +
                "  ref_type VARCHAR(20) DEFAULT NULL COMMENT 'spec or vlm'," +
                "  ref_row_id BIGINT DEFAULT NULL," +
                "  original_codelist VARCHAR(200) DEFAULT NULL," +
                "  merge_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  KEY idx_lookup (project_id, username, merged_vcd, ref_type)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        } catch (Exception ignored) {}
    }

    /**
     * Snapshot of each source VCD's term rows BEFORE merge, for precise restore on undo.
     * Without this, undo would copy mergedVcd's terms to each originalVcd — which is wrong
     * for subset_chain merges where some originalVcds had fewer terms than the superset.
     */
    private void ensureMergeTermSnapshotTable() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS sas_codelist_merge_term_snapshot (" +
                "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "  project_id VARCHAR(64) DEFAULT NULL," +
                "  username VARCHAR(100) DEFAULT NULL," +
                "  merged_vcd VARCHAR(200) DEFAULT NULL," +
                "  original_vcd VARCHAR(200) DEFAULT NULL," +
                "  snap_vlabel VARCHAR(500) DEFAULT NULL," +
                "  snap_nci_codelist_code VARCHAR(100) DEFAULT NULL," +
                "  snap_type VARCHAR(20) DEFAULT NULL," +
                "  snap_terminology VARCHAR(200) DEFAULT NULL," +
                "  snap_comment TEXT," +
                "  snap_cdnum INT DEFAULT NULL," +
                "  snap_code VARCHAR(200) DEFAULT NULL," +
                "  snap_nci_term_code VARCHAR(100) DEFAULT NULL," +
                "  snap_code_des TEXT," +
                "  snap_code_ver VARCHAR(50) DEFAULT NULL," +
                "  snap_flag VARCHAR(10) DEFAULT NULL," +
                "  snap_sort_order INT DEFAULT NULL," +
                "  snap_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  KEY idx_lookup (project_id, username, merged_vcd, original_vcd)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        } catch (Exception ignored) {}
    }

    private static int typeWeight(Map<String, Object> cluster) {
        String type = cluster.get("type") != null ? cluster.get("type").toString() : "";
        boolean merged = cluster.get("already_merged") != null && (Boolean) cluster.get("already_merged");
        if (merged) return 4;
        switch (type) {
            case "subset_chain": return 1;
            case "ct_aligned":   return 2;
            case "identity":     return 3;
            default:             return 5;
        }
    }

    private Long resolvePackageId(ProjectConfigPO config) {
        if (config == null) return null;
        String standardType = config.getStandardType() != null ? config.getStandardType() : "SDTM";
        String ctVersionStr = config.getCtVersion();
        Long packageId = null;

        if (ctVersionStr != null && !ctVersionStr.isEmpty()) {
            String dateStr = ctVersionStr.replaceAll(".*?(\\d{4}-\\d{2}-\\d{2}).*", "$1");
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                List<Map<String, Object>> pkgRows = jdbcTemplate.queryForList(
                    "SELECT id FROM ct_package WHERE standard_type = ? AND CAST(release_date AS CHAR) LIKE CONCAT(?, '%') LIMIT 1",
                    standardType, dateStr);
                if (!pkgRows.isEmpty()) {
                    packageId = ((Number) pkgRows.get(0).get("id")).longValue();
                }
            }
        }
        if (packageId == null) {
            List<Map<String, Object>> pkgRows = jdbcTemplate.queryForList(
                "SELECT id FROM ct_package WHERE standard_type = ? AND language_code = 'EN' ORDER BY release_date DESC LIMIT 1",
                config.getStandardType() != null ? config.getStandardType() : "SDTM");
            if (!pkgRows.isEmpty()) {
                packageId = ((Number) pkgRows.get(0).get("id")).longValue();
            }
        }
        return packageId;
    }

    private Map<String, String> findCtCodelistByTerms(Long packageId, String[] terms) {
        if (terms == null || terms.length == 0) return null;

        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();
        params.add(packageId);
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
            params.add(terms[i].trim().toUpperCase());
        }
        params.add(terms.length);

        // Primary query: directly match against ct_term, no header JOIN.
        // Note: ct_term term-rows already carry codelist_name, so we don't need to JOIN header.
        // Order by total term count of the candidate codelist (smallest first) so we prefer
        // the most specific codelist that contains exactly the input terms.
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ct.codelist_code, MAX(ct.codelist_name) AS codelist_name, " +
                "  (SELECT COUNT(*) FROM ct_term ct2 WHERE ct2.package_id = ct.package_id " +
                "    AND ct2.codelist_code = ct.codelist_code AND ct2.cdisc_submission_value IS NOT NULL) AS total_terms " +
                "FROM ct_term ct " +
                "WHERE ct.package_id = ? AND ct.codelist_code IS NOT NULL " +
                "  AND UPPER(TRIM(ct.cdisc_submission_value)) IN (" + placeholders + ") " +
                "GROUP BY ct.codelist_code " +
                "HAVING COUNT(DISTINCT UPPER(TRIM(ct.cdisc_submission_value))) = ? " +
                "ORDER BY total_terms ASC " +
                "LIMIT 1",
                params.toArray()
            );
            if (!rows.isEmpty()) {
                Map<String, String> result = new HashMap<>();
                result.put("codelist_code", rows.get(0).get("codelist_code").toString());
                String name = rows.get(0).get("codelist_name") != null ? rows.get(0).get("codelist_name").toString() : "";
                result.put("codelist_name", name);
                return result;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }

    /**
     * Find ALL CT codelists in the given package that fully contain the input terms
     * (i.e., input terms ⊆ CT codelist's terms). Returns each codelist's code, name
     * and total_terms count, ordered by total_terms ASC so the most specific one
     * comes first.
     *
     * Used by analyzeUnified to detect "CT-aligned" candidate groups: two project
     * codelists with overlapping-but-not-equal term sets that both fit inside the
     * SAME CT codelist (e.g., {AFTER, BEFORE, DURING, UNKNOWN} and
     * {BEFORE, ONGOING, UNKNOWN} both ⊆ NRIND).
     */
    private List<Map<String, Object>> findCtCodelistsBySubset(Long packageId, String[] terms) {
        if (terms == null || terms.length == 0) return new ArrayList<>();

        StringBuilder placeholders = new StringBuilder();
        List<Object> params = new ArrayList<>();
        params.add(packageId);
        for (int i = 0; i < terms.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
            params.add(terms[i].trim().toUpperCase());
        }
        params.add(terms.length);

        try {
            return jdbcTemplate.queryForList(
                "SELECT ct.codelist_code, MAX(ct.codelist_name) AS codelist_name, " +
                "  (SELECT COUNT(*) FROM ct_term ct2 WHERE ct2.package_id = ct.package_id " +
                "    AND ct2.codelist_code = ct.codelist_code AND ct2.cdisc_submission_value IS NOT NULL) AS total_terms " +
                "FROM ct_term ct " +
                "WHERE ct.package_id = ? AND ct.codelist_code IS NOT NULL " +
                "  AND UPPER(TRIM(ct.cdisc_submission_value)) IN (" + placeholders + ") " +
                "GROUP BY ct.codelist_code " +
                "HAVING COUNT(DISTINCT UPPER(TRIM(ct.cdisc_submission_value))) = ? " +
                "ORDER BY total_terms ASC",
                params.toArray()
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

}