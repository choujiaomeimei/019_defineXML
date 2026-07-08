package com.stat.dal.po;

import java.util.Date;

public class PagesDataPO {

    private Long id;
    private String projectId;
    private String username;
    private String dataset;
    private String variable;
    private String whereClause;
    private String pages;
    private String origin;
    private Integer sortOrder;
    private Date createdTime;
    private Date updatedTime;
    private String createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDataset() { return dataset; }
    public void setDataset(String dataset) { this.dataset = dataset; }

    public String getVariable() { return variable; }
    public void setVariable(String variable) { this.variable = variable; }

    public String getPages() { return pages; }
    public void setPages(String pages) { this.pages = pages; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }

    public Date getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Date updatedTime) { this.updatedTime = updatedTime; }

    public String getWhereClause() { return whereClause; }
    public void setWhereClause(String whereClause) { this.whereClause = whereClause; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
