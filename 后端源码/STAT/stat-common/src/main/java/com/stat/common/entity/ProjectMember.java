package com.stat.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("project_member")
public class ProjectMember implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectId;
    private String username;
    private String role;
    private Date createdTime;
    private Date updatedTime;
}
