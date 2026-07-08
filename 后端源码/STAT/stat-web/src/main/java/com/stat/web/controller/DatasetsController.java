package com.stat.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stat.common.result.CommonResult;
import com.stat.common.security.UserContext;
import com.stat.dal.mapper.DatasetsDataMapper;
import com.stat.dal.po.DatasetsDataPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datasets（数据集定义）数据接口
 */
@RestController
@RequestMapping("/datasets")
public class DatasetsController {

    @Autowired
    private DatasetsDataMapper datasetsDataMapper;

    /**
     * 按项目与当前登录用户列出数据集行
     */
    @GetMapping("/list")
    public CommonResult<List<DatasetsDataPO>> list(@RequestParam("projectId") String projectId) {
        try {
            if (projectId == null || projectId.trim().isEmpty()) {
                return CommonResult.failed("projectId 不能为空");
            }
            String username = UserContext.getUsername();
            if (username == null || username.trim().isEmpty()) {
                return CommonResult.failed("无法获取当前用户，请重新登录");
            }
            LambdaQueryWrapper<DatasetsDataPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DatasetsDataPO::getProjectId, projectId.trim())
                    .eq(DatasetsDataPO::getUsername, username.trim())
                    .orderByAsc(DatasetsDataPO::getSortOrder)
                    .orderByAsc(DatasetsDataPO::getId);
            List<DatasetsDataPO> list = datasetsDataMapper.selectList(wrapper);
            return CommonResult.success(list != null ? list : new ArrayList<>());
        } catch (Exception e) {
            return CommonResult.failed("查询 Datasets 数据失败: " + e.getMessage());
        }
    }

    /**
     * 全量保存：先删除该项目下该用户的全部行，再批量插入
     */
    @PostMapping("/save-all")
    public CommonResult<String> saveAll(@RequestBody Map<String, Object> body) {
        try {
            String projectId = asString(body.get("projectId"));
            String username = asString(body.get("username"));
            if (username == null || username.isEmpty()) {
                username = UserContext.getUsername();
            }
            if (projectId == null || projectId.isEmpty()) {
                return CommonResult.failed("projectId 不能为空");
            }
            if (username == null || username.isEmpty()) {
                return CommonResult.failed("username 不能为空");
            }
            Object dataObj = body.get("data");
            if (!(dataObj instanceof List)) {
                return CommonResult.failed("data 必须为数组");
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) dataObj;

            LambdaQueryWrapper<DatasetsDataPO> del = new LambdaQueryWrapper<>();
            del.eq(DatasetsDataPO::getProjectId, projectId).eq(DatasetsDataPO::getUsername, username);
            datasetsDataMapper.delete(del);

            int n = 0;
            for (Map<String, Object> row : rows) {
                DatasetsDataPO po = mapRowToPo(row, projectId, username);
                po.setId(null);
                datasetsDataMapper.insert(po);
                n++;
            }
            return CommonResult.success("保存成功，共写入 " + n + " 条");
        } catch (Exception e) {
            return CommonResult.failed("批量保存失败: " + e.getMessage());
        }
    }

    /**
     * 按主键更新单行
     */
    @PutMapping("/update")
    public CommonResult<String> update(@RequestBody DatasetsDataPO po) {
        try {
            if (po == null || po.getId() == null) {
                return CommonResult.failed("id 不能为空");
            }
            int rows = datasetsDataMapper.updateById(po);
            if (rows > 0) {
                return CommonResult.success("更新成功");
            }
            return CommonResult.failed("未找到对应记录或更新失败");
        } catch (Exception e) {
            return CommonResult.failed("更新失败: " + e.getMessage());
        }
    }

    /**
     * 按主键删除单行
     */
    @DeleteMapping("/delete")
    public CommonResult<String> delete(@RequestParam("id") Long id) {
        try {
            if (id == null) {
                return CommonResult.failed("id 不能为空");
            }
            int rows = datasetsDataMapper.deleteById(id);
            if (rows > 0) {
                return CommonResult.success("删除成功");
            }
            return CommonResult.failed("未找到对应记录");
        } catch (Exception e) {
            return CommonResult.failed("删除失败: " + e.getMessage());
        }
    }

    /**
     * 新增单行
     */
    @PostMapping("/add")
    public CommonResult<String> add(@RequestBody DatasetsDataPO po) {
        try {
            if (po == null) {
                return CommonResult.failed("请求体不能为空");
            }
            po.setId(null);
            if (po.getUsername() == null || po.getUsername().trim().isEmpty()) {
                String u = UserContext.getUsername();
                if (u != null && !u.trim().isEmpty()) {
                    po.setUsername(u.trim());
                }
            }
            int rows = datasetsDataMapper.insert(po);
            if (rows > 0) {
                return CommonResult.success("新增成功");
            }
            return CommonResult.failed("新增失败");
        } catch (Exception e) {
            return CommonResult.failed("新增失败: " + e.getMessage());
        }
    }

    private static String asString(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Object firstNonNull(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            if (row.containsKey(k) && row.get(k) != null) {
                return row.get(k);
            }
        }
        return null;
    }

    private static Integer asInteger(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            String s = o.toString().trim();
            if (s.isEmpty()) {
                return null;
            }
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static DatasetsDataPO mapRowToPo(Map<String, Object> row, String projectId, String username) {
        DatasetsDataPO po = new DatasetsDataPO();
        po.setProjectId(projectId);
        po.setUsername(username);

        Object v;

        v = firstNonNull(row, "dataset", "Dataset");
        po.setDataset(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "label", "Label");
        po.setLabel(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "dataClass", "class", "Class");
        po.setDataClass(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "subClass", "sub_class", "SubClass");
        po.setSubClass(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "structure", "Structure");
        po.setStructure(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "keyVariables", "key_variables", "KeyVariables");
        po.setKeyVariables(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "standard", "Standard");
        po.setStandard(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "hasNoData", "has_no_data", "HasNoData");
        po.setHasNoData(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "repeating", "Repeating");
        po.setRepeating(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "referenceData", "reference_data", "ReferenceData");
        po.setReferenceData(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "comment", "Comment");
        po.setComment(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "developerNotes", "developer_notes", "DeveloperNotes");
        po.setDeveloperNotes(v != null ? String.valueOf(v) : null);

        v = firstNonNull(row, "sortOrder", "sort_order", "SortOrder");
        po.setSortOrder(asInteger(v));

        v = firstNonNull(row, "createdBy", "created_by", "CreatedBy");
        po.setCreatedBy(v != null ? String.valueOf(v) : null);

        return po;
    }
}
