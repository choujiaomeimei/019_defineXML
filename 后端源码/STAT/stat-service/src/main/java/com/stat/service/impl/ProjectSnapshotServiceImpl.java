package com.stat.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stat.common.entity.FileUploadRecord;
import com.stat.common.entity.ProjectSnapshot;
import com.stat.common.entity.SnapshotFile;
import com.stat.dal.mapper.*;
import com.stat.dal.po.CodelistDataPO;
import com.stat.dal.po.ProjectSpecPO;
import com.stat.dal.po.VlmDataPO;
import com.stat.common.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectSnapshotServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ProjectSnapshotServiceImpl.class);

    @Autowired
    private ProjectSnapshotMapper projectSnapshotMapper;

    @Autowired
    private SnapshotFileMapper snapshotFileMapper;

    @Autowired
    private ProjectSpecMapper projectSpecMapper;

    @Autowired
    private VlmDataMapper vlmDataMapper;

    @Autowired
    private CodelistDataMapper codelistDataMapper;

    @Autowired
    private FileUploadRecordMapper fileUploadRecordMapper;

    @Transactional(rollbackFor = Exception.class)
    public ProjectSnapshot createSnapshot(String projectId, String snapshotName,
                                          String description, String createdBy) {
        log.info("创建项目快照: projectId={}, name={}", projectId, snapshotName);
        ProjectSnapshot snapshot = createSnapshotInternal(projectId, snapshotName, description, createdBy, "manual");
        log.info("项目快照创建成功: id={}", snapshot.getId());
        return snapshot;
    }

    public List<ProjectSnapshot> listSnapshots(String projectId) {
        return projectSnapshotMapper.selectByProjectId(projectId);
    }

    public Map<String, Object> getSnapshotDetail(Long snapshotId) {
        ProjectSnapshot snapshot = projectSnapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            return null;
        }

        List<SnapshotFile> files = snapshotFileMapper.selectBySnapshotId(snapshotId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", snapshot.getId());
        detail.put("projectId", snapshot.getProjectId());
        detail.put("snapshotName", snapshot.getSnapshotName());
        detail.put("snapshotType", snapshot.getSnapshotType());
        detail.put("description", snapshot.getDescription());
        detail.put("createdBy", snapshot.getCreatedBy());
        detail.put("createdTime", snapshot.getCreatedTime());
        detail.put("files", files);

        if (snapshot.getSpecDataJson() != null) {
            detail.put("specData", JSON.parseArray(snapshot.getSpecDataJson()));
        }
        if (snapshot.getVlmDataJson() != null) {
            detail.put("vlmData", JSON.parseArray(snapshot.getVlmDataJson()));
        }
        if (snapshot.getCodelistDataJson() != null) {
            detail.put("codelistData", JSON.parseArray(snapshot.getCodelistDataJson()));
        }

        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSnapshot(Long snapshotId) {
        ProjectSnapshot snapshot = projectSnapshotMapper.selectById(snapshotId);
        if (snapshot != null && Boolean.TRUE.equals(snapshot.getLocked())) {
            throw new RuntimeException("已锁定的版本不能删除");
        }

        LambdaQueryWrapper<SnapshotFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(SnapshotFile::getSnapshotId, snapshotId);
        snapshotFileMapper.delete(fileWrapper);

        return projectSnapshotMapper.deleteById(snapshotId) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProjectSnapshot createAutoSnapshot(String projectId, String triggerAction) {
        String name = "auto-" + triggerAction + "-" + System.currentTimeMillis();
        ProjectSnapshot snapshot = createSnapshotInternal(projectId, name, 
                "自动快照: " + triggerAction, "system", "auto");
        log.info("自动快照创建成功: projectId={}, trigger={}", projectId, triggerAction);
        return snapshot;
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreSnapshot(Long snapshotId) {
        ProjectSnapshot snapshot = projectSnapshotMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new RuntimeException("快照不存在");
        }

        String projectId = snapshot.getProjectId();
        String username = UserContext.getUsername();

        // Create an auto-snapshot of current state before restoring
        createAutoSnapshot(projectId, "pre-restore");

        // Restore spec data
        projectSpecMapper.deleteByProjectId(projectId, username);
        if (snapshot.getSpecDataJson() != null) {
            List<ProjectSpecPO> specList = JSON.parseArray(snapshot.getSpecDataJson(), ProjectSpecPO.class);
            if (specList != null && !specList.isEmpty()) {
                for (ProjectSpecPO spec : specList) {
                    spec.setId(null);
                    spec.setUsername(username);
                    projectSpecMapper.insert(spec);
                }
            }
        }

        // Restore VLM data
        vlmDataMapper.deleteByProjectId(projectId, username);
        if (snapshot.getVlmDataJson() != null) {
            List<VlmDataPO> vlmList = JSON.parseArray(snapshot.getVlmDataJson(), VlmDataPO.class);
            if (vlmList != null && !vlmList.isEmpty()) {
                for (VlmDataPO vlm : vlmList) {
                    vlm.setId(null);
                    vlm.setUsername(username);
                    vlmDataMapper.insert(vlm);
                }
            }
        }

        // Restore codelist data
        codelistDataMapper.deleteByProjectId(projectId, username);
        if (snapshot.getCodelistDataJson() != null) {
            List<CodelistDataPO> codelistList = JSON.parseArray(snapshot.getCodelistDataJson(), CodelistDataPO.class);
            if (codelistList != null && !codelistList.isEmpty()) {
                for (CodelistDataPO cl : codelistList) {
                    cl.setId(null);
                    cl.setUsername(username);
                    codelistDataMapper.insert(cl);
                }
            }
        }

        log.info("快照恢复成功: snapshotId={}, projectId={}", snapshotId, projectId);
    }

    public boolean lockSnapshot(Long snapshotId) {
        ProjectSnapshot snapshot = projectSnapshotMapper.selectById(snapshotId);
        if (snapshot == null) return false;
        snapshot.setLocked(true);
        return projectSnapshotMapper.updateById(snapshot) > 0;
    }

    public boolean unlockSnapshot(Long snapshotId) {
        ProjectSnapshot snapshot = projectSnapshotMapper.selectById(snapshotId);
        if (snapshot == null) return false;
        snapshot.setLocked(false);
        return projectSnapshotMapper.updateById(snapshot) > 0;
    }

    public Map<String, Object> compareSnapshots(Long snapshotIdA, Long snapshotIdB) {
        ProjectSnapshot a = projectSnapshotMapper.selectById(snapshotIdA);
        ProjectSnapshot b = projectSnapshotMapper.selectById(snapshotIdB);
        if (a == null || b == null) return null;

        Map<String, Object> diff = new HashMap<>();
        diff.put("snapshotA", buildSnapshotSummary(a));
        diff.put("snapshotB", buildSnapshotSummary(b));

        List<?> specA = a.getSpecDataJson() != null ? JSON.parseArray(a.getSpecDataJson()) : List.of();
        List<?> specB = b.getSpecDataJson() != null ? JSON.parseArray(b.getSpecDataJson()) : List.of();
        diff.put("specCountA", specA.size());
        diff.put("specCountB", specB.size());
        diff.put("specChanged", specA.size() != specB.size());

        List<?> vlmA = a.getVlmDataJson() != null ? JSON.parseArray(a.getVlmDataJson()) : List.of();
        List<?> vlmB = b.getVlmDataJson() != null ? JSON.parseArray(b.getVlmDataJson()) : List.of();
        diff.put("vlmCountA", vlmA.size());
        diff.put("vlmCountB", vlmB.size());
        diff.put("vlmChanged", vlmA.size() != vlmB.size());

        List<?> clA = a.getCodelistDataJson() != null ? JSON.parseArray(a.getCodelistDataJson()) : List.of();
        List<?> clB = b.getCodelistDataJson() != null ? JSON.parseArray(b.getCodelistDataJson()) : List.of();
        diff.put("codelistCountA", clA.size());
        diff.put("codelistCountB", clB.size());
        diff.put("codelistChanged", clA.size() != clB.size());

        return diff;
    }

    private Map<String, Object> buildSnapshotSummary(ProjectSnapshot s) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", s.getId());
        summary.put("name", s.getSnapshotName());
        summary.put("versionLabel", s.getVersionLabel());
        summary.put("createdTime", s.getCreatedTime());
        summary.put("locked", s.getLocked());
        return summary;
    }

    private ProjectSnapshot createSnapshotInternal(String projectId, String snapshotName,
                                                    String description, String createdBy, String type) {
        String username = UserContext.getUsername();
        List<ProjectSpecPO> specData = projectSpecMapper.selectByProjectId(projectId, username);
        List<VlmDataPO> vlmData = vlmDataMapper.selectByProjectId(projectId, username);
        List<CodelistDataPO> codelistData = codelistDataMapper.selectByProjectId(projectId, username);

        ProjectSnapshot snapshot = new ProjectSnapshot();
        snapshot.setProjectId(projectId);
        snapshot.setSnapshotName(snapshotName);
        snapshot.setSnapshotType(type);
        snapshot.setDescription(description);
        snapshot.setSpecDataJson(JSON.toJSONString(specData));
        snapshot.setVlmDataJson(JSON.toJSONString(vlmData));
        snapshot.setCodelistDataJson(JSON.toJSONString(codelistData));
        snapshot.setCreatedBy(createdBy);
        snapshot.setCreatedTime(LocalDateTime.now());
        snapshot.setLocked(false);

        projectSnapshotMapper.insert(snapshot);

        List<FileUploadRecord> currentFiles = fileUploadRecordMapper.selectByProjectId(projectId);
        for (FileUploadRecord file : currentFiles) {
            SnapshotFile sf = new SnapshotFile();
            sf.setSnapshotId(snapshot.getId());
            sf.setFileCategory(file.getFileCategory());
            sf.setOriginalName(file.getOriginalName());
            sf.setFilePath(file.getFilePath());
            sf.setFileSize(file.getFileSize());
            sf.setFileMd5(file.getFileMd5());
            sf.setVersionNumber(file.getVersionNumber());
            snapshotFileMapper.insert(sf);
        }
        return snapshot;
    }
}
