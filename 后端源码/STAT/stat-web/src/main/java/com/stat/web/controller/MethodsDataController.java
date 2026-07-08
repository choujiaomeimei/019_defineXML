package com.stat.web.controller;

import com.stat.common.dto.MethodsDataDTO;
import com.stat.common.result.CommonResult;
import com.stat.service.IMethodsDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/methods")
public class MethodsDataController {

    @Autowired
    private IMethodsDataService methodsDataService;

    @GetMapping("/project/{projectId}")
    public CommonResult<List<MethodsDataDTO>> getByProject(@PathVariable String projectId) {
        try {
            return CommonResult.success(methodsDataService.getMethodsDataByProjectId(projectId));
        } catch (Exception e) {
            return CommonResult.failed("查询Methods数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public CommonResult<MethodsDataDTO> getById(@PathVariable Long id) {
        try {
            MethodsDataDTO result = methodsDataService.getMethodsDataById(id);
            return result != null ? CommonResult.success(result) : CommonResult.failed("数据不存在");
        } catch (Exception e) {
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    public CommonResult<String> add(@RequestBody MethodsDataDTO dto) {
        try {
            return methodsDataService.addMethodsData(dto)
                    ? CommonResult.success("新增成功")
                    : CommonResult.failed("新增失败");
        } catch (Exception e) {
            return CommonResult.failed("新增失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public CommonResult<String> update(@PathVariable Long id, @RequestBody MethodsDataDTO dto) {
        try {
            dto.setId(id);
            return methodsDataService.updateMethodsData(dto)
                    ? CommonResult.success("更新成功")
                    : CommonResult.failed("更新失败");
        } catch (Exception e) {
            return CommonResult.failed("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> delete(@PathVariable Long id) {
        try {
            return methodsDataService.deleteMethodsData(id)
                    ? CommonResult.success("删除成功")
                    : CommonResult.failed("删除失败");
        } catch (Exception e) {
            return CommonResult.failed("删除失败: " + e.getMessage());
        }
    }
}
