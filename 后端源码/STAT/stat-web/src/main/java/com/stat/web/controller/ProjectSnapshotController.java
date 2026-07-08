package com.stat.web.controller;

import com.stat.common.entity.ProjectSnapshot;
import com.stat.common.result.CommonResult;
import com.stat.common.security.RequireProjectAccess;
import com.stat.service.impl.ProjectSnapshotServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/project-snapshot")
public class ProjectSnapshotController {

    private static final Logger log = LoggerFactory.getLogger(ProjectSnapshotController.class);

    @Autowired
    private ProjectSnapshotServiceImpl projectSnapshotService;

    @RequireProjectAccess("projectId")
    @PostMapping("/create")
    public CommonResult<?> createSnapshot(@RequestBody Map<String, String> request) {
        try {
            String projectId = request.get("projectId");
            String snapshotName = request.get("snapshotName");
            String description = request.get("description");
            String username = request.get("username");

            if (projectId == null || projectId.isEmpty()) {
                return CommonResult.failed("项目ID不能为空");
            }
            if (snapshotName == null || snapshotName.isEmpty()) {
                return CommonResult.failed("版本名称不能为空");
            }

            ProjectSnapshot snapshot = projectSnapshotService.createSnapshot(
                    projectId, snapshotName, description, username);

            return CommonResult.success(snapshot);
        } catch (Exception e) {
            log.error("创建项目快照失败: {}", e.getMessage(), e);
            return CommonResult.failed("创建快照失败: " + e.getMessage());
        }
    }

    @RequireProjectAccess("projectId")
    @GetMapping("/list")
    public CommonResult<?> listSnapshots(@RequestParam("projectId") String projectId) {
        try {
            List<ProjectSnapshot> snapshots = projectSnapshotService.listSnapshots(projectId);
            return CommonResult.success(snapshots);
        } catch (Exception e) {
            log.error("查询快照列表失败: {}", e.getMessage(), e);
            return CommonResult.failed("查询快照列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/detail")
    public CommonResult<?> getSnapshotDetail(@RequestParam("snapshotId") Long snapshotId) {
        try {
            Map<String, Object> detail = projectSnapshotService.getSnapshotDetail(snapshotId);
            if (detail == null) {
                return CommonResult.failed("快照不存在");
            }
            return CommonResult.success(detail);
        } catch (Exception e) {
            log.error("查询快照详情失败: {}", e.getMessage(), e);
            return CommonResult.failed("查询快照详情失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public CommonResult<?> deleteSnapshot(@RequestParam("snapshotId") Long snapshotId) {
        try {
            boolean deleted = projectSnapshotService.deleteSnapshot(snapshotId);
            if (deleted) {
                return CommonResult.success("快照删除成功");
            }
            return CommonResult.failed("快照不存在");
        } catch (Exception e) {
            log.error("删除快照失败: {}", e.getMessage(), e);
            return CommonResult.failed("删除快照失败: " + e.getMessage());
        }
    }

    @PostMapping("/restore/{snapshotId}")
    public CommonResult<?> restoreSnapshot(@PathVariable Long snapshotId) {
        try {
            projectSnapshotService.restoreSnapshot(snapshotId);
            return CommonResult.success("快照恢复成功");
        } catch (Exception e) {
            log.error("恢复快照失败: {}", e.getMessage(), e);
            return CommonResult.failed("恢复快照失败: " + e.getMessage());
        }
    }

    @PutMapping("/lock/{snapshotId}")
    public CommonResult<?> lockSnapshot(@PathVariable Long snapshotId) {
        try {
            boolean locked = projectSnapshotService.lockSnapshot(snapshotId);
            return locked ? CommonResult.success("版本已锁定") : CommonResult.failed("快照不存在");
        } catch (Exception e) {
            log.error("锁定快照失败: {}", e.getMessage(), e);
            return CommonResult.failed("锁定失败: " + e.getMessage());
        }
    }

    @PutMapping("/unlock/{snapshotId}")
    public CommonResult<?> unlockSnapshot(@PathVariable Long snapshotId) {
        try {
            boolean unlocked = projectSnapshotService.unlockSnapshot(snapshotId);
            return unlocked ? CommonResult.success("版本已解锁") : CommonResult.failed("快照不存在");
        } catch (Exception e) {
            log.error("解锁快照失败: {}", e.getMessage(), e);
            return CommonResult.failed("解锁失败: " + e.getMessage());
        }
    }

    @GetMapping("/compare")
    public CommonResult<?> compareSnapshots(@RequestParam("snapshotIdA") Long snapshotIdA,
                                            @RequestParam("snapshotIdB") Long snapshotIdB) {
        try {
            Map<String, Object> diff = projectSnapshotService.compareSnapshots(snapshotIdA, snapshotIdB);
            if (diff == null) {
                return CommonResult.failed("快照不存在");
            }
            return CommonResult.success(diff);
        } catch (Exception e) {
            log.error("对比快照失败: {}", e.getMessage(), e);
            return CommonResult.failed("对比失败: " + e.getMessage());
        }
    }
}
