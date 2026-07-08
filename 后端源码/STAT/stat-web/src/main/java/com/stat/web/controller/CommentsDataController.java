package com.stat.web.controller;

import com.stat.common.dto.CommentsDataDTO;
import com.stat.common.result.CommonResult;
import com.stat.service.ICommentsDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentsDataController {

    @Autowired
    private ICommentsDataService commentsDataService;

    @GetMapping("/project/{projectId}")
    public CommonResult<List<CommentsDataDTO>> getByProject(@PathVariable String projectId) {
        try {
            return CommonResult.success(commentsDataService.getCommentsDataByProjectId(projectId));
        } catch (Exception e) {
            return CommonResult.failed("查询Comments数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public CommonResult<CommentsDataDTO> getById(@PathVariable Long id) {
        try {
            CommentsDataDTO result = commentsDataService.getCommentsDataById(id);
            return result != null ? CommonResult.success(result) : CommonResult.failed("数据不存在");
        } catch (Exception e) {
            return CommonResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    public CommonResult<String> add(@RequestBody CommentsDataDTO dto) {
        try {
            return commentsDataService.addCommentsData(dto)
                    ? CommonResult.success("新增成功")
                    : CommonResult.failed("新增失败");
        } catch (Exception e) {
            return CommonResult.failed("新增失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public CommonResult<String> update(@PathVariable Long id, @RequestBody CommentsDataDTO dto) {
        try {
            dto.setId(id);
            return commentsDataService.updateCommentsData(dto)
                    ? CommonResult.success("更新成功")
                    : CommonResult.failed("更新失败");
        } catch (Exception e) {
            return CommonResult.failed("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> delete(@PathVariable Long id) {
        try {
            return commentsDataService.deleteCommentsData(id)
                    ? CommonResult.success("删除成功")
                    : CommonResult.failed("删除失败");
        } catch (Exception e) {
            return CommonResult.failed("删除失败: " + e.getMessage());
        }
    }
}
