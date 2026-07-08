package com.stat.common.dto;

public class DictionariesDataDTO {

    private Long id;
    private String projectId;
    private String dictionaryId;
    private String name;
    private String dataType;
    private String dictionary;
    private String version;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

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
}
