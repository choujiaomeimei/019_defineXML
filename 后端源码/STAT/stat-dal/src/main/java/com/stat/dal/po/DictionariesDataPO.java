package com.stat.dal.po;

import java.util.Date;

public class DictionariesDataPO {

    private Long id;
    private String projectId;
    private String username;
    private String dictionaryId;
    private String name;
    private String dataType;
    private String dictionary;
    private String version;
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

    public String getDictionaryId() { return dictionaryId; }
    public void setDictionaryId(String dictionaryId) { this.dictionaryId = dictionaryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getDictionary() { return dictionary; }
    public void setDictionary(String dictionary) { this.dictionary = dictionary; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

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
