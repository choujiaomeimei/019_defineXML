/** SDTM — ValueLevel 生成规则 */
export default {
  standard: 'SDTM',
  module: 'vlm',
  sections: [
    {
      heading: '1. 前置依赖',
      blocks: [
        {
          type: 'paragraph',
          html: '需要已上传 <b>XPT 数据集</b>和<b>项目 Spec</b>（已处理）。系统扫描 XPT 目录下可用的域名来确定哪些域参与提取。',
        },
      ],
    },
    {
      heading: '2. 提取配置',
      blocks: [
        {
          type: 'paragraph',
          html: '系统按预置的 <code>vlm_configs</code> 逐项处理，每项包含：<code>(域名, 排序变量, 分组变量列表, 码表参数)</code>。典型配置如下：',
        },
        {
          type: 'table',
          columns: ['域', '排序变量', '分组变量', '识别模式'],
          rows: [
            ['TS', 'TSSEQ', 'TSPARMCD, TSPARM', 'TSPARMCD 模式'],
            ['TX', 'TXSEQ', 'TXPARMCD, TXPARM', 'PARMCD 模式'],
            ['LB', 'LBSEQ', 'LBCAT, LBTESTCD, LBTEST', 'TESTCD 模式'],
            ['EG / PC / PP / VS / IE 等', '各自 SEQ', '各自 TESTCD + TEST', 'TESTCD 模式'],
          ],
        },
      ],
    },
    {
      heading: '3. 字段生成规则',
      blocks: [
        {
          type: 'table',
          columns: ['字段', '生成规则'],
          rows: [
            ['<b>Dataset</b>', '配置中的域名转大写（如 <code>lb → LB</code>）'],
            [
              '<b>Variable</b>',
              '<b>TSPARMCD 模式</b>：<code>{DATASET}VAL</code>（如 TSVAL）<br/><b>PARMCD 模式</b>：<code>{DATASET}VAL</code>（如 TXVAL）<br/><b>TESTCD 模式</b>：<code>{DATASET}ORRES</code>（如 LBORRES）',
            ],
            [
              '<b>Where Clause</b>',
              '<b>TSPARMCD</b>：<code>TSPARMCD EQ "{该行 TSPARMCD 值}"</code><br/><b>PARMCD</b>：<code>{域}PARMCD EQ "{该行 PARMCD 值}"</code><br/><b>TESTCD</b>：<code>{域}TESTCD EQ "{该行 TESTCD 值}"</code>',
            ],
            ['<b>Label</b>', '取对应参数/测试名称列的值（如 TSPARM、LBTEST），即 <code>*CD</code> 列去掉 CD 的同名列'],
            ['<b>Controlled Terms or Format</b>', '不自动填充（留空，用户可手动编辑）'],
            ['<b>Origin</b>', '<b>TSPARMCD / PARMCD</b>：<code>Assigned</code>；<b>TESTCD</b>：<code>CRF</code>'],
            ['<b>Pages</b>', '不自动填充（后续由 Pages 流程补充）'],
            ['<b>Derivation/Comment</b>', '仅 TSPARMCD 模式取 <code>TSVAL</code> 列的值作为注释'],
            ['<b>Method</b>', '仅 TSPARMCD 模式设为 <code>"Y"</code>'],
            ['<b>Comment / 类别</b>', '不自动填充'],
          ],
        },
      ],
    },
    {
      heading: '4. 去重与排序',
      blocks: [
        {
          type: 'paragraph',
          html: '对每组 XPT 数据按分组变量 <code>drop_duplicates</code> 后逐行生成，最终按 Dataset、Variable、Where Clause 排序。结果存入 <code>sas_vlm_data</code> 表供用户审查编辑。',
        },
      ],
    },
  ],
}
