/** SDTM — Codelists 处理规则 */
export default {
  standard: 'SDTM',
  module: 'codelist',
  sections: [
    {
      heading: '1. 前置依赖',
      blocks: [
        {
          type: 'paragraph',
          html: '需要已上传 <b>XPT 数据集</b>和<b>项目 Spec</b>（已处理）。',
        },
      ],
    },
    {
      heading: '2. 提取流程',
      blocks: [
        {
          type: 'ordered-list',
          items: [
            '<b>从 Spec 识别需要码表的变量</b>：遍历每个域的 Spec，读取 <code>CDISC Submission Value</code> / <code>Controlled Terms or Format</code> / <code>Codelist</code> 列（按优先级），有值的变量需要生成 CodeList',
            '<b>从 XPT 提取实际值</b>：读取对应 XPT 文件，收集该变量的所有唯一值（<code>#</code> 分隔拼接）',
            '<b>拆分为逐行码表</b>：将拼接的值按 <code>#</code> 拆开，每个值为一条 code 行',
            '<b>合并 VLM 阶段产生的附加码表</b>（如 ARM/ARMCD 等域配置生成的码表行）',
            '<b>后处理</b>：按 <code>vcd → cdnum</code> 重排序号；对 <code>type=Num</code> 且有 <code>codeDes</code> 的，把 code 设为序号',
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
            ['<b>vcd</b>', '码表标识符，取自 Spec 的 <code>CDISC Submission Value</code> 或 <code>Controlled Terms or Format</code> 列值'],
            ['<b>vlabel</b>', '变量标签，来自 Spec 对应变量的 Label'],
            ['<b>type</b>', '数据类型，来自 Spec（Char / Num），默认 Char'],
            ['<b>cdnum</b>', '代码序号，在每个 vcd 组内从 1 开始自动编号'],
            ['<b>code</b>', '代码值，从 XPT 中该变量的实际唯一值提取；<code>type=Num</code> 且有 codeDes 时设为序号'],
            ['<b>code_des</b>', '代码描述（Decoded Value），目前留空由用户填写'],
            ['<b>code_ver</b>', '代码版本（如 CDISC CT 版本号），留空由用户填写'],
            ['<b>flag</b>', '固定域标记：<code>ARM, ARMCD, VISIT, VISITNUM, IETEST, IETESTCD</code> 等预设域的 flag 为 <code>"Y"</code>，其余为空'],
          ],
        },
      ],
    },
    {
      heading: '4. 过滤规则',
      blocks: [
        {
          type: 'paragraph',
          html: 'Spec 中变量名包含 <code>CATN</code> 或 <code>TESTN</code> 的行会被自动跳过（非标准编码变量）。Spec 中受控术语列为空的行也会被跳过。',
        },
      ],
    },
    {
      heading: '5. 数据存储',
      blocks: [
        {
          type: 'paragraph',
          html: '结果存入 <code>sas_codelist_data</code> 表，按用户隔离，用户可在 CodeList 编辑页面审查修改。',
        },
      ],
    },
  ],
}
