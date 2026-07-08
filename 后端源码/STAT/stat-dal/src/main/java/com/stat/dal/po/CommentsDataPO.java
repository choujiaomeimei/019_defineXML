package com.stat.dal.po;

import java.util.Date;

public class CommentsDataPO {

    private Long id;
    private String projectId;
    private String username;
    private String commentId;
    private String description;
    private String document;
    private String pages;
    private Integer sortOrder;
    private Date createdTime;
    private Date updatedTime;
    private String createdBy;
    private String updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }

    public String getPages() { return pages; }
    public void setPages(String pages) { this.pages = pages; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Date getCreatedTime() { return createdTime; }
    public void setCreatedTime(Date createdTime) { this.createdTime = createdTime; }

    public Date getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(Date updatedTime) { this.updatedTime = updatedTime; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
