package com.stat.web.controller;

import com.stat.common.result.CommonResult;
import com.stat.dal.mapper.ProjectConfigMapper;
import com.stat.dal.po.ProjectConfigPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/project-config")
public class ProjectConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectConfigController.class);

    @Autowired
    private ProjectConfigMapper projectConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/get")
    public CommonResult<Map<String, Object>> getProjectConfig(@RequestParam("projectId") String projectId) {
        logger.info("获取项目配置, projectId: {}", projectId);
        try {
            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.fail("400", "项目ID不能为空");
            }

            ProjectConfigPO config = projectConfigMapper.selectByProjectId(projectId);
            if (config != null) {
                return CommonResult.success(toMap(config));
            } else {
                return CommonResult.success(null);
            }
        } catch (Exception e) {
            logger.error("获取项目配置失败", e);
            return CommonResult.fail("500", "获取项目配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    public CommonResult<Map<String, Object>> saveProjectConfig(@RequestBody Map<String, Object> data) {
        logger.info("保存项目配置: {}", data);
        try {
            String projectId = (String) data.get("projectId");
            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.fail("400", "项目ID不能为空");
            }

            ProjectConfigPO existing = projectConfigMapper.selectByProjectId(projectId);
            if (existing != null) {
                return CommonResult.fail("400", "项目配置已存在，请使用更新接口");
            }

            ProjectConfigPO po = fromMap(data);
            po.setCreateTime(LocalDateTime.now());
            po.setUpdateTime(LocalDateTime.now());
            projectConfigMapper.insert(po);

            logger.info("项目配置保存成功: {}", projectId);
            ProjectConfigPO saved = projectConfigMapper.selectByProjectId(projectId);
            return CommonResult.success(toMap(saved));

        } catch (Exception e) {
            logger.error("保存项目配置失败", e);
            return CommonResult.fail("500", "保存项目配置失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public CommonResult<Map<String, Object>> updateProjectConfig(@RequestBody Map<String, Object> data) {
        logger.info("更新项目配置: {}", data);
        try {
            String projectId = (String) data.get("projectId");
            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.fail("400", "项目ID不能为空");
            }

            ProjectConfigPO existing = projectConfigMapper.selectByProjectId(projectId);
            if (existing == null) {
                ProjectConfigPO po = fromMap(data);
                po.setCreateTime(LocalDateTime.now());
                po.setUpdateTime(LocalDateTime.now());
                projectConfigMapper.insert(po);
            } else {
                applyUpdates(existing, data);
                existing.setUpdateTime(LocalDateTime.now());
                projectConfigMapper.updateById(existing);
            }

            logger.info("项目配置更新成功: {}", projectId);
            ProjectConfigPO updated = projectConfigMapper.selectByProjectId(projectId);
            return CommonResult.success(toMap(updated));

        } catch (Exception e) {
            logger.error("更新项目配置失败", e);
            return CommonResult.fail("500", "更新项目配置失败: " + e.getMessage());
        }
    }

    /**
     * List available CT packages for dropdown selection.
     * Returns [{id, standardType, releaseDate, languageCode, label}]
     */
    @GetMapping("/ct-packages")
    public CommonResult<List<Map<String, Object>>> listCtPackages(
            @RequestParam(value = "standardType", required = false) String standardType) {
        try {
            String sql = "SELECT id, standard_type, release_date, language_code FROM ct_package";
            List<Object> params = new ArrayList<>();
            if (standardType != null && !standardType.isEmpty()) {
                sql += " WHERE standard_type = ?";
                params.add(standardType);
            }
            sql += " ORDER BY standard_type, release_date DESC";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", row.get("id"));
                item.put("standardType", row.get("standard_type"));
                Object rd = row.get("release_date");
                String dateStr = rd != null ? rd.toString().substring(0, 10) : "";
                item.put("releaseDate", dateStr);
                item.put("languageCode", row.get("language_code"));
                String st = row.get("standard_type") != null ? row.get("standard_type").toString() : "";
                String lang = row.get("language_code") != null ? row.get("language_code").toString() : "";
                item.put("label", st + " Terminology " + dateStr + " (" + lang + ")");
                result.add(item);
            }
            return CommonResult.success(result);
        } catch (Exception e) {
            logger.error("获取CT包列表失败", e);
            return CommonResult.fail("500", "获取CT包列表失败: " + e.getMessage());
        }
    }

    private ProjectConfigPO fromMap(Map<String, Object> data) {
        ProjectConfigPO po = new ProjectConfigPO();
        po.setProjectId((String) data.get("projectId"));
        po.setEncoding(getStr(data, "encoding", "UTF-8"));
        po.setLanguage(getStr(data, "language", "CN"));
        po.setStandardType(resolveStandardType(data));
        po.setStandardVersion(getStr(data, "standardVersion", getStr(data, "standard_version", null)));
        po.setCtVersion(getStr(data, "ctVersion", getStr(data, "ct_version", null)));
        po.setSourceFormat(getStr(data, "sourceFormat", getStr(data, "source_format", "SAS(XPT)")));
        po.setConfiguration(getStr(data, "configuration", null));
        po.setCreator(getStr(data, "creator", getStr(data, "username", null)));

        Object chn = data.get("chineseStandard");
        if (chn == null) chn = data.get("chinese_standard");
        po.setChineseStandard(toBool(chn, true));

        Object eng = data.get("englishStandard");
        if (eng == null) eng = data.get("english_standard");
        po.setEnglishStandard(toBool(eng, false));

        return po;
    }

    private void applyUpdates(ProjectConfigPO po, Map<String, Object> data) {
        if (data.containsKey("encoding")) po.setEncoding((String) data.get("encoding"));
        if (data.containsKey("language")) po.setLanguage((String) data.get("language"));
        if (data.containsKey("standardTypes") || data.containsKey("standardType") || data.containsKey("standard_type"))
            po.setStandardType(resolveStandardType(data));
        if (data.containsKey("standardVersion") || data.containsKey("standard_version"))
            po.setStandardVersion(getStr(data, "standardVersion", getStr(data, "standard_version", po.getStandardVersion())));
        if (data.containsKey("ctVersion") || data.containsKey("ct_version"))
            po.setCtVersion(getStr(data, "ctVersion", getStr(data, "ct_version", po.getCtVersion())));
        if (data.containsKey("sourceFormat") || data.containsKey("source_format"))
            po.setSourceFormat(getStr(data, "sourceFormat", getStr(data, "source_format", po.getSourceFormat())));
        if (data.containsKey("configuration"))
            po.setConfiguration((String) data.get("configuration"));

        Object chn = data.get("chineseStandard");
        if (chn == null) chn = data.get("chinese_standard");
        if (chn != null) po.setChineseStandard(toBool(chn, po.getChineseStandard()));

        Object eng = data.get("englishStandard");
        if (eng == null) eng = data.get("english_standard");
        if (eng != null) po.setEnglishStandard(toBool(eng, po.getEnglishStandard()));
    }

    private Map<String, Object> toMap(ProjectConfigPO po) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", po.getId());
        map.put("project_id", po.getProjectId());
        map.put("projectId", po.getProjectId());
        map.put("encoding", po.getEncoding());
        map.put("language", po.getLanguage());
        map.put("standard_type", po.getStandardType());
        map.put("standard_version", po.getStandardVersion());
        map.put("ct_version", po.getCtVersion());
        map.put("chinese_standard", po.getChineseStandard());
        map.put("english_standard", po.getEnglishStandard());
        map.put("source_format", po.getSourceFormat());
        map.put("configuration", po.getConfiguration());
        map.put("creator", po.getCreator());
        map.put("create_time", po.getCreateTime());
        map.put("update_time", po.getUpdateTime());
        return map;
    }

    @SuppressWarnings("unchecked")
    private static String resolveStandardType(Map<String, Object> data) {
        Object types = data.get("standardTypes");
        if (types instanceof List) {
            List<String> list = (List<String>) types;
            if (!list.isEmpty()) return String.join(",", list);
        }
        String st = getStr(data, "standardType", getStr(data, "standard_type", null));
        return st != null ? st : "SDTM";
    }

    private static String getStr(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v instanceof String ? (String) v : def;
    }

    private static Boolean toBool(Object val, Boolean def) {
        if (val == null) return def;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return "true".equalsIgnoreCase((String) val) || "1".equals(val);
        if (val instanceof Number) return ((Number) val).intValue() != 0;
        return def;
    }
}
