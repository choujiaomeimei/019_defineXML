package com.stat.dal.po;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("project_config")
public class ProjectConfigPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private String projectId;

    private String encoding;

    private String language;

    @TableField("standard_type")
    private String standardType;

    @TableField("standard_version")
    private String standardVersion;

    @TableField("ct_version")
    private String ctVersion;

    @TableField("chinese_standard")
    private Boolean chineseStandard;

    @TableField("english_standard")
    private Boolean englishStandard;

    @TableField("source_format")
    private String sourceFormat;

    private String configuration;

    private String creator;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getStandardType() { return standardType; }
    public void setStandardType(String standardType) { this.standardType = standardType; }

    public String getStandardVersion() { return standardVersion; }
    public void setStandardVersion(String standardVersion) { this.standardVersion = standardVersion; }

    public String getCtVersion() { return ctVersion; }
    public void setCtVersion(String ctVersion) { this.ctVersion = ctVersion; }

    public Boolean getChineseStandard() { return chineseStandard; }
    public void setChineseStandard(Boolean chineseStandard) { this.chineseStandard = chineseStandard; }

    public Boolean getEnglishStandard() { return englishStandard; }
    public void setEnglishStandard(Boolean englishStandard) { this.englishStandard = englishStandard; }

    public String getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(String sourceFormat) { this.sourceFormat = sourceFormat; }

    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
