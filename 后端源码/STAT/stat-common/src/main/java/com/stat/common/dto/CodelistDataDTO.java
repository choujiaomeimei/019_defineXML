package com.stat.common.dto;

import java.io.Serializable;

/**
 * CodeList数据传输对象
 */
public class CodelistDataDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** CodeList数据ID */
    private Long id;
    
    /** 项目ID */
    private String projectId;
    
    /** 变量代码 */
    private String vcd;
    
    /** 变量标签 (Name) */
    private String vlabel;

    /** NCI Codelist Code */
    private String nciCodelistCode;
    
    /** 数据类型 (Data Type) */
    private String type;

    /** Terminology版本 */
    private String terminology;

    /** Comment备注 */
    private String comment;
    
    /** 代码序号 (Order) */
    private Integer cdnum;
    
    /** 代码值 (Term) */
    private String code;

    /** NCI Term Code */
    private String nciTermCode;
    
    /** 代码描述 (Decoded Value) */
    private String codeDes;

    /** 来源 */
    private String origin;
    
    /** 代码版本 */
    private String codeVer;
    
    /** 标记字段 */
    private String flag;
    
    /** 排序顺序 */
    private Integer sortOrder;

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

    public String getVcd() {
        return vcd;
    }

    public void setVcd(String vcd) {
        this.vcd = vcd;
    }

    public String getVlabel() {
        return vlabel;
    }

    public void setVlabel(String vlabel) {
        this.vlabel = vlabel;
    }

    public String getNciCodelistCode() { return nciCodelistCode; }
    public void setNciCodelistCode(String nciCodelistCode) { this.nciCodelistCode = nciCodelistCode; }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTerminology() { return terminology; }
    public void setTerminology(String terminology) { this.terminology = terminology; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Integer getCdnum() {
        return cdnum;
    }

    public void setCdnum(Integer cdnum) {
        this.cdnum = cdnum;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNciTermCode() { return nciTermCode; }
    public void setNciTermCode(String nciTermCode) { this.nciTermCode = nciTermCode; }

    public String getCodeDes() {
        return codeDes;
    }

    public void setCodeDes(String codeDes) {
        this.codeDes = codeDes;
    }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getCodeVer() {
        return codeVer;
    }

    public void setCodeVer(String codeVer) {
        this.codeVer = codeVer;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}