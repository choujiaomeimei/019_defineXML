package com.stat.dal.po;

import java.util.Date;

/**
 * 项目Spec数据表实体类
 */
public class ProjectSpecPO {
    
    /** Spec数据ID */
    private Long id;
    
    /** 项目ID */
    private String projectId;
    
    /** 用户名 */
    private String username;
    
    /** 数据域/数据集名称 */
    private String domain;
    
    /** 变量名称 */
    private String variable;
    
    /** 变量标签/描述 */
    private String label;
    
    /** 数据类型 */
    private String type;
    
    /** 变量长度（保留原始字符串） */
    private String length;

    /** 有效位数 */
    private String significantDigits;
    
    /** 受控术语或格式 */
    private String controlledTermsOrFormat;
    
    /** CDISC提交值 */
    private String cdiscSubmissionValue;
    
    /** 小数位数 */
    private Integer decimalPlaces;
    
    /** 数据来源 */
    private String origin;

    /** 数据Source */
    private String source;
    
    /** 变量角色 */
    private String role;
    
    /** CDISC注释 */
    private String cdiscNotes;
    
    /** 核心级别 */
    private String core;
    
    /** 代码列表引用 */
    private String codelist;

    /** 赋值 */
    private String assignedValue;

    /** 公共变量标识 */
    private String common;
    
    /** 显示格式 */
    private String format;
    
    /** 备注说明 */
    private String comment;

    /** 开发者注释 */
    private String developerNotes;

    /** SUPP标识 */
    private String supp;

    /** QEVAL值 */
    private String qeval;
    
    /** 是否必填 (Yes/No) */
    private String mandatory;
    
    /** 主键序号 */
    private Integer keySequence;
    
    /** 受控术语 */
    private String controlledTerms;
    
    /** 派生逻辑 */
    private String derivation;
    
    /** 前置变量 */
    private String predecessor;

    /** 无数据标识 */
    private String hasNoData;
    
    /** Text内容(原Derived Method) */
    private String textContent;

    /** Method标识 */
    private String method;
    
    /** 相关页面 */
    private String pages;
    
    /** 问题文本 */
    private String questionText;
    
    /** 提示信息 */
    private String prompt;
    
    /** 数据集类别 */
    private String datasetClass;
    
    /** 数据结构 */
    private String structure;
    
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getSignificantDigits() { return significantDigits; }
    public void setSignificantDigits(String significantDigits) { this.significantDigits = significantDigits; }

    public String getControlledTermsOrFormat() {
        return controlledTermsOrFormat;
    }

    public void setControlledTermsOrFormat(String controlledTermsOrFormat) {
        this.controlledTermsOrFormat = controlledTermsOrFormat;
    }

    public String getCdiscSubmissionValue() {
        return cdiscSubmissionValue;
    }

    public void setCdiscSubmissionValue(String cdiscSubmissionValue) {
        this.cdiscSubmissionValue = cdiscSubmissionValue;
    }

    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCdiscNotes() {
        return cdiscNotes;
    }

    public void setCdiscNotes(String cdiscNotes) {
        this.cdiscNotes = cdiscNotes;
    }

    public String getCore() {
        return core;
    }

    public void setCore(String core) {
        this.core = core;
    }

    public String getCodelist() {
        return codelist;
    }

    public void setCodelist(String codelist) {
        this.codelist = codelist;
    }

    public String getAssignedValue() { return assignedValue; }
    public void setAssignedValue(String assignedValue) { this.assignedValue = assignedValue; }

    public String getCommon() { return common; }
    public void setCommon(String common) { this.common = common; }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDeveloperNotes() { return developerNotes; }
    public void setDeveloperNotes(String developerNotes) { this.developerNotes = developerNotes; }

    public String getSupp() { return supp; }
    public void setSupp(String supp) { this.supp = supp; }

    public String getQeval() { return qeval; }
    public void setQeval(String qeval) { this.qeval = qeval; }

    public String getMandatory() {
        return mandatory;
    }

    public void setMandatory(String mandatory) {
        this.mandatory = mandatory;
    }

    public Integer getKeySequence() {
        return keySequence;
    }

    public void setKeySequence(Integer keySequence) {
        this.keySequence = keySequence;
    }

    public String getControlledTerms() {
        return controlledTerms;
    }

    public void setControlledTerms(String controlledTerms) {
        this.controlledTerms = controlledTerms;
    }

    public String getDerivation() {
        return derivation;
    }

    public void setDerivation(String derivation) {
        this.derivation = derivation;
    }

    public String getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(String predecessor) {
        this.predecessor = predecessor;
    }

    public String getHasNoData() { return hasNoData; }
    public void setHasNoData(String hasNoData) { this.hasNoData = hasNoData; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getDatasetClass() {
        return datasetClass;
    }

    public void setDatasetClass(String datasetClass) {
        this.datasetClass = datasetClass;
    }

    public String getStructure() {
        return structure;
    }

    public void setStructure(String structure) {
        this.structure = structure;
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