package com.stat.dal.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("project") // 数据库表名
public class ProjectPO {
    
    @TableId
    private Long id; // 项目ID

    private String applicationUnit; // 申办方单位
    private String planName; // 方案名称
    private String planNumber; // 方案编号
    private String createTime; // 创建时间
    private Integer deleteFlag; // 删除标志
    private String projectId; // 项目ID
    // 其他字段...
} 