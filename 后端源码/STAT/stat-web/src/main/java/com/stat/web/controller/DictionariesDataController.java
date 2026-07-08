package com.stat.web.controller;

import com.stat.common.dto.DictionariesDataDTO;
import com.stat.common.result.CommonResult;
import com.stat.service.IDictionariesDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dictionaries")
public class DictionariesDataController {

    @Autowired
    private IDictionariesDataService dictionariesDataService;

    @GetMapping("/project/{projectId}")
    public CommonResult<List<DictionariesDataDTO>> getByProject(@PathVariable String projectId) {
        try {
            return CommonResult.success(dictionariesDataService.getDictionariesDataByProjectId(projectId));
        } catch (Exception e) {
            return CommonResult.failed("查询Dictionaries数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public CommonResult<DictionariesDataDTO> getById(@PathVariable Long id) {
        try {
            DictionariesDataDTO result = dictionariesDataService.getDictionariesDataById(id);
            return result != null ? CommonResult.success(result) : CommonResult.failed("数据不存在");
        } catch (Exception e) {
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    public CommonResult<String> add(@RequestBody DictionariesDataDTO dto) {
        try {
            return dictionariesDataService.addDictionariesData(dto)
                    ? CommonResult.success("新增成功")
                    : CommonResult.failed("新增失败");
        } catch (Exception e) {
            return CommonResult.failed("新增失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public CommonResult<String> update(@PathVariable Long id, @RequestBody DictionariesDataDTO dto) {
        try {
            dto.setId(id);
            return dictionariesDataService.updateDictionariesData(dto)
                    ? CommonResult.success("更新成功")
                    : CommonResult.failed("更新失败");
        } catch (Exception e) {
            return CommonResult.failed("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> delete(@PathVariable Long id) {
        try {
            return dictionariesDataService.deleteDictionariesData(id)
                    ? CommonResult.success("删除成功")
                    : CommonResult.failed("删除失败");
        } catch (Exception e) {
            return CommonResult.failed("删除失败: " + e.getMessage());
        }
    }
}
