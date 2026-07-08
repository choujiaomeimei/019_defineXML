package com.stat.web.controller;

import com.stat.common.dto.DocumentsDataDTO;
import com.stat.common.result.CommonResult;
import com.stat.service.IDocumentsDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentsDataController {

    @Autowired
    private IDocumentsDataService documentsDataService;

    @GetMapping("/project/{projectId}")
    public CommonResult<List<DocumentsDataDTO>> getByProject(@PathVariable String projectId) {
        try {
            return CommonResult.success(documentsDataService.getDocumentsDataByProjectId(projectId));
        } catch (Exception e) {
            return CommonResult.failed("查询Documents数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public CommonResult<DocumentsDataDTO> getById(@PathVariable Long id) {
        try {
            DocumentsDataDTO result = documentsDataService.getDocumentsDataById(id);
            return result != null ? CommonResult.success(result) : CommonResult.failed("数据不存在");
        } catch (Exception e) {
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    public CommonResult<String> add(@RequestBody DocumentsDataDTO dto) {
        try {
            return documentsDataService.addDocumentsData(dto)
                    ? CommonResult.success("新增成功")
                    : CommonResult.failed("新增失败");
        } catch (Exception e) {
            return CommonResult.failed("新增失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public CommonResult<String> update(@PathVariable Long id, @RequestBody DocumentsDataDTO dto) {
        try {
            dto.setId(id);
            return documentsDataService.updateDocumentsData(dto)
                    ? CommonResult.success("更新成功")
                    : CommonResult.failed("更新失败");
        } catch (Exception e) {
            return CommonResult.failed("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> delete(@PathVariable Long id) {
        try {
            return documentsDataService.deleteDocumentsData(id)
                    ? CommonResult.success("删除成功")
                    : CommonResult.failed("删除失败");
        } catch (Exception e) {
            return CommonResult.failed("删除失败: " + e.getMessage());
        }
    }
}
