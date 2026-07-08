package com.stat.common.dto;

public class CommentsDataDTO {

    private Long id;
    private String projectId;
    private String commentId;
    private String description;
    private String document;
    private String pages;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

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
}
