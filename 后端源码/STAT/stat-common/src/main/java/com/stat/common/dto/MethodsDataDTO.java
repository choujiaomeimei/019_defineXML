package com.stat.common.dto;

public class MethodsDataDTO {

    private Long id;
    private String projectId;
    private String methodId;
    private String name;
    private String type;
    private String description;
    private String expressionContext;
    private String expressionCode;
    private String document;
    private String pages;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getMethodId() { return methodId; }
    public void setMethodId(String methodId) { this.methodId = methodId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExpressionContext() { return expressionContext; }
    public void setExpressionContext(String expressionContext) { this.expressionContext = expressionContext; }

    public String getExpressionCode() { return expressionCode; }
    public void setExpressionCode(String expressionCode) { this.expressionCode = expressionCode; }

    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }

    public String getPages() { return pages; }
    public void setPages(String pages) { this.pages = pages; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
