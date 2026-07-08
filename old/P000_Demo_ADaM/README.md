# 项目模板 P000_Demo

这是项目文件夹的模板结构。新建项目时会复制这个结构。

## 文件夹说明

### uploads/
原始上传文件存储目录
- p21-spec/ - P21空SPEC文件
- xpt/ - XPT数据文件
- project-spec/ - 项目SPEC文件
- acrf/ - aCRF文件

### define/
Define相关处理文件目录
- p21空spec/ - P21空SPEC处理文件
- sdtm define package/ - SDTM Define Package
- 项目Spec/ - 项目SPEC处理文件
- SDTM注释CRF/ - SDTM注释CRF

### output/
处理结果输出目录
- define/ - Define相关输出
- reports/ - 报告输出

## 使用说明

1. 上传文件到对应的uploads子目录
2. Python工具处理时从define目录读取
3. 处理结果输出到output目录