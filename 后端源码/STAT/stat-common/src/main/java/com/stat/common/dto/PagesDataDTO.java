package com.stat.common.dto;

public class PagesDataDTO {

    private Long id;
    private String projectId;
    private String dataset;
    private String variable;
    private String whereClause;
    private String pages;
    private String origin;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getDataset() { return dataset; }
    public void setDataset(String dataset) { this.dataset = dataset; }

    public String getVariable() { return variable; }
    public void setVariable(String variable) { this.variable = variable; }

    public String getPages() { return pages; }
    public void setPages(String pages) { this.pages = pages; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getWhereClause() { return whereClause; }
    public void setWhereClause(String whereClause) { this.whereClause = whereClause; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
