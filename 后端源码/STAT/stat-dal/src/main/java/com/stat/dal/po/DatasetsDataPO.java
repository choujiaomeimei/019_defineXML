package com.stat.dal.po;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("sas_datasets_data")
public class DatasetsDataPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private String projectId;

    private String username;
    private String dataset;
    private String label;

    @TableField("class")
    private String dataClass;

    @TableField("sub_class")
    private String subClass;

    private String structure;

    @TableField("key_variables")
    private String keyVariables;

    private String standard;

    @TableField("has_no_data")
    private String hasNoData;

    private String repeating;

    @TableField("reference_data")
    private String referenceData;

    private String comment;

    @TableField("developer_notes")
    private String developerNotes;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_by")
    private String createdBy;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDataClass() {
        return dataClass;
    }

    public void setDataClass(String dataClass) {
        this.dataClass = dataClass;
    }

    public String getSubClass() {
        return subClass;
    }

    public void setSubClass(String subClass) {
        this.subClass = subClass;
    }

    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
    }

    public String getKeyVariables() {
        return keyVariables;
    }

    public void setKeyVariables(String keyVariables) {
        this.keyVariables = keyVariables;
    }

    public String getStandard() {
        return standard;
    }

    public void setStandard(String standard) {
        this.standard = standard;
    }

    public String getHasNoData() {
        return hasNoData;
    }

    public void setHasNoData(String hasNoData) {
        this.hasNoData = hasNoData;
    }

    public String getRepeating() {
        return repeating;
    }

    public void setRepeating(String repeating) {
        this.repeating = repeating;
    }

    public String getReferenceData() {
        return referenceData;
    }

    public void setReferenceData(String referenceData) {
        this.referenceData = referenceData;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDeveloperNotes() {
        return developerNotes;
    }

    public void setDeveloperNotes(String developerNotes) {
        this.developerNotes = developerNotes;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
