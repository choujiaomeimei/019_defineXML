package com.stat.common.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目数据传输对象
 */
public class ProjectDTO {

    private Long id;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 方案编号
     */
    private String protocolNumber;

    /**
     * 方案名称
     */
    private String protocolName;

    /**
     * 方案版本
     */
    private String protocolVersion;

    /**
     * 方案版本日期
     */
    private LocalDate protocolDate;

    /**
     * 标准类型：SDTM、ADAM、SEND
     */
    private String standardType;

    private String encoding;

    private String language;

    private String standardVersion;

    private String sponsor;

    /**
     * 用户名
     */
    private String username;

    /**
     * 状态：1-活跃，0-归档
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusText;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 文件统计信息
     */
    private FileStats fileStats;

    public static class FileStats {
        private Integer p21SpecCount = 0;
        private Integer xptCount = 0;
        private Integer projectSpecCount = 0;
        private Integer acrfCount = 0;

        // Getters and Setters
        public Integer getP21SpecCount() {
            return p21SpecCount;
        }

        public void setP21SpecCount(Integer p21SpecCount) {
            this.p21SpecCount = p21SpecCount;
        }

        public Integer getXptCount() {
            return xptCount;
        }

        public void setXptCount(Integer xptCount) {
            this.xptCount = xptCount;
        }

        public Integer getProjectSpecCount() {
            return projectSpecCount;
        }

        public void setProjectSpecCount(Integer projectSpecCount) {
            this.projectSpecCount = projectSpecCount;
        }

        public Integer getAcrfCount() {
            return acrfCount;
        }

        public void setAcrfCount(Integer acrfCount) {
            this.acrfCount = acrfCount;
        }
    }

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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProtocolNumber() {
        return protocolNumber;
    }

    public void setProtocolNumber(String protocolNumber) {
        this.protocolNumber = protocolNumber;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public LocalDate getProtocolDate() {
        return protocolDate;
    }

    public void setProtocolDate(LocalDate protocolDate) {
        this.protocolDate = protocolDate;
    }

    public String getStandardType() {
        return standardType;
    }

    public void setStandardType(String standardType) {
        this.standardType = standardType;
    }

    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getStandardVersion() { return standardVersion; }
    public void setStandardVersion(String standardVersion) { this.standardVersion = standardVersion; }

    public String getSponsor() {
        return sponsor;
    }

    public void setSponsor(String sponsor) {
        this.sponsor = sponsor;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
        this.statusText = status == 1 ? "活跃" : "归档";
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
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

    public FileStats getFileStats() {
        return fileStats;
    }

    public void setFileStats(FileStats fileStats) {
        this.fileStats = fileStats;
    }
}