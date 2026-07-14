/** SDTM — Pages 处理规则 */
export default {
  standard: 'SDTM',
  module: 'pages',
  sections: [
    {
      heading: '1. 前置依赖',
      blocks: [
        {
          type: 'paragraph',
          html: '需要已上传 <b>aCRF</b>（处理后生成 <code>Annots2.xlsx</code>）和<b>项目 Spec</b>（已处理）。',
        },
      ],
    },
    {
      heading: '2. 提取流程',
      blocks: [
        {
          type: 'ordered-list',
          items: [
            '读取 <code>Annots2.xlsx</code>，解析每条 PDF 注释的文本内容和所在页码',
            '读取 Spec 中所有域的变量列表',
            '将注释文本与变量名进行匹配（支持 <code>DOMAIN.VARIABLE</code> 和 <code>VARIABLE</code> 两种格式）',
            '汇总每个变量对应的所有页码，去重、排序后拼接为字符串（如 "12, 15, 23"）',
          ],
        },
      ],
    },
    {
      heading: '3. 字段说明',
      blocks: [
        {
          type: 'table',
          columns: ['字段', '来源与规则'],
          rows: [
            ['<b>dataset</b>', '域名（如 DM、AE），从 Spec 的 Domain Sheet 名获取'],
            ['<b>variable</b>', '变量名，来自 Spec'],
            ['<b>pages</b>', '注释文本匹配到的 PDF 页码，去重排序后逗号拼接'],
            ['<b>origin</b>', '来自 Spec 的 Origin 字段'],
          ],
        },
      ],
    },
    {
      heading: '4. 数据存储',
      blocks: [
        {
          type: 'paragraph',
          html: '结果存入 <code>sas_pages_data</code> 表，按用户隔离，用户可在 Pages 编辑页面审查修改。',
        },
      ],
    },
  ],
}
