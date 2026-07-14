/** SDTM — Datasets 处理规则 */
export default {
  standard: 'SDTM',
  module: 'datasets',
  sections: [
    {
      heading: '1. 数据来源',
      blocks: [
        {
          type: 'paragraph',
          html: 'Datasets 数据提取自上传的原始 Spec 的 <code>TOC</code> 工作表。TOC 表中的每一行代表一个数据集。',
        },
      ],
    },
    {
      heading: '2. 字段映射规则',
      blocks: [
        {
          type: 'table',
          columns: ['P21 字段', 'TOC 列来源', '说明'],
          rows: [
            ['<b>Dataset</b>', 'Dataset / Domain', '数据集名称（如 DM, AE, LB），必填'],
            ['<b>Label</b>', 'Description / Label', '数据集描述'],
            ['<b>Class</b>', 'Class / Observation Class', '如 Events, Findings, Interventions, Special Purpose 等'],
            ['<b>SubClass</b>', 'SubClass', '子分类，可选'],
            ['<b>Structure</b>', 'Structure', '数据结构，如 "One record per subject" 等'],
            ['<b>Key Variables</b>', 'Key Variables / Keys', '关键变量列表，逗号分隔'],
            ['<b>Standard</b>', 'Standard', '标准名称，如 SDTM-IG 3.4'],
            ['<b>Has No Data</b>', 'Has No Data', '是否无数据（Yes/No），默认空'],
            ['<b>Repeating</b>', 'Repeating', '是否可重复（Yes/No），默认空'],
            ['<b>Reference Data</b>', 'Reference Data', '是否为参考数据（Yes/No），默认空'],
            ['<b>Comment</b>', 'Comment / Comments', '备注说明'],
            ['<b>Developer Notes</b>', 'Developer Notes', '开发备注，可选'],
          ],
        },
      ],
    },
    {
      heading: '3. 数据存储',
      blocks: [
        {
          type: 'paragraph',
          html: '结果存入 <code>sas_datasets_data</code> 表（按用户隔离），用户可在 Datasets 页面的表格模式或 Excel 模式中编辑。',
        },
      ],
    },
  ],
}
