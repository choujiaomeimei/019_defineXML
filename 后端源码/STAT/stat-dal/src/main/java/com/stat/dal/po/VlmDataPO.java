package com.stat.dal.po;

import java.util.Date;

/**
 * VLM数据表实体类
 */
public class VlmDataPO {
    
    /** VLM数据ID */
    private Long id;
    
    /** 项目ID */
    private String projectId;
    
    /** 用户名 */
    private String username;
    
    /** 数据集名称 */
    private String dataset;
    
    /** 变量名 */
    private String variable;
    
    /** WHERE条件子句 */
    private String whereClause;
    
    /** 变量标签/描述 */
    private String label;

    private String dataType;
    private String length;
    private String significantDigits;
    private String format;
    private String mandatory;
    private String assignedValue;
    private String codelist;
    
    /** 受控术语或格式 */
    private String controlledTermsOrFormat;
    
    /** 数据来源 */
    private String origin;

    private String source;
    
    /** 页面信息 */
    private String pages;
    
    /** 派生/注释信息 */
    private String derivationComment;
    
    /** 方法标识 */
    private String method;

    private String predecessor;
    
    /** 备注 */
    private String comment;

    private String developerNotes;
    
    /** 类别 */
    private String category;
    
    /** 排序顺序 */
    private Integer sortOrder;
    
    /** 创建时间 */
    private Date createdTime;
    
    /** 更新时间 */
    private Date updatedTime;
    
    /** 创建人 */
    private String createdBy;
    
    /** 更新人 */
    private String updatedBy;

    // Getters and Setters
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

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public void setWhereClause(String whereClause) {
        this.whereClause = whereClause;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getLength() { return length; }
    public void setLength(String length) { this.length = length; }

    public String getSignificantDigits() { return significantDigits; }
    public void setSignificantDigits(String significantDigits) { this.significantDigits = significantDigits; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getMandatory() { return mandatory; }
    public void setMandatory(String mandatory) { this.mandatory = mandatory; }

    public String getAssignedValue() { return assignedValue; }
    public void setAssignedValue(String assignedValue) { this.assignedValue = assignedValue; }

    public String getCodelist() { return codelist; }
    public void setCodelist(String codelist) { this.codelist = codelist; }

    public String getControlledTermsOrFormat() {
        return controlledTermsOrFormat;
    }

    public void setControlledTermsOrFormat(String controlledTermsOrFormat) {
        this.controlledTermsOrFormat = controlledTermsOrFormat;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getDerivationComment() {
        return derivationComment;
    }

    public void setDerivationComment(String derivationComment) {
        this.derivationComment = derivationComment;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPredecessor() { return predecessor; }
    public void setPredecessor(String predecessor) { this.predecessor = predecessor; }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDeveloperNotes() { return developerNotes; }
    public void setDeveloperNotes(String developerNotes) { this.developerNotes = developerNotes; }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}