/** SDTM — Variables 映射规则 */
export default {
  standard: 'SDTM',
  module: 'variables',
  sections: [
    {
      heading: '1. 上传与解析',
      blocks: [
        {
          type: 'paragraph',
          html: '上传项目 Spec（Excel），系统自动识别工作表名为 2-4 个大写字母的 Domain Sheet（如 DM、AE、LB），跳过 TOC 等非域名 Sheet。匹配到 3 个及以上标准字段的 Sheet 才会被解析。',
        },
      ],
    },
    {
      heading: '2. Spec → P21 Variables 映射',
      blocks: [
        {
          type: 'paragraph',
          html: '原始 Spec 字段经过处理后映射到 P21 标准的 Variables 字段：',
        },
        {
          type: 'table',
          columns: ['P21 Variables', 'Spec 来源', '映射规则'],
          rows: [
            ['<b>Order</b>', 'Seq', '自动编号'],
            ['<b>Dataset</b>', 'Domain', '直接映射'],
            ['<b>Variable</b>', 'Variable Name', '直接映射'],
            ['<b>Label</b>', 'Variable Label', '直接映射'],
            ['<b>Data Type</b>', 'Type', 'Char → text, Num → integer/float'],
            ['<b>Length</b>', 'Length', '直接映射（自动去 .0 后缀）'],
            ['<b>Significant Digits</b>', '—', '默认空，用户填写'],
            ['<b>Format</b>', 'Controlled Terms or Format', '直接映射'],
            ['<b>Mandatory</b>', 'Core', 'Req → Yes, Exp/Perm → No'],
            ['<b>Assigned Value</b>', '—', '置空，用户填写'],
            ['<b>Codelist</b>', 'Controlled Terms or Format', '直接映射'],
            ['<b>Submission Value</b>', 'CDISC Submission Value', '直接映射'],
            ['<b>Common</b>', '—', '置空，用户填写'],
            ['<b>Origin</b>', 'Origin', '直接映射'],
            ['<b>Source</b>', 'Source', '直接映射'],
            ['<b>Pages</b>', 'CRF Page', '直接映射'],
            ['<b>Text</b>', 'Text', 'Spec "Text" 列（原 Derived Method）'],
            ['<b>Predecessor</b>', '—', '置空，用户填写'],
            ['<b>Role</b>', 'Role', '直接映射'],
            ['<b>Has No Data</b>', '—', '置空，用户填写'],
            ['<b>Comment</b>', 'Comment', '直接映射'],
            ['<b>Developer Notes</b>', '—', '置空，用户填写'],
            ['<b>SUPP</b>', 'SUPP', '直接映射'],
            ['<b>QEVAL</b>', 'QEVAL', '直接映射'],
          ],
        },
      ],
    },
    {
      heading: '3. 附加映射说明',
      blocks: [
        {
          type: 'paragraph',
          html: '<b>Method</b>：Spec "Method" 列值映射到 Method 字段（标记变量是否有方法描述）。<b>Codelist</b> 和 <b>Format</b> 均来自 Spec "Controlled Terms or Format" 列。',
        },
      ],
    },
    {
      heading: '4. 数据流向',
      blocks: [
        {
          type: 'paragraph',
          html: '解析结果存入 <code>sas_project_spec</code> 表（按用户隔离） → 用户在 Variables 页面可修改 → 同步生成 <code>spec_synced_{projectId}.xlsx</code> → 下游 VLM / Codelists / Pages / Define 提取均读取此同步文件。',
        },
      ],
    },
  ],
}
