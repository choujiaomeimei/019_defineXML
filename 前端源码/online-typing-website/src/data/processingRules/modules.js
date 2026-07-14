/** 处理规则模块元数据 */
export const RULE_MODULES = {
  datasets: {
    key: 'datasets',
    letter: 'D',
    title: 'Datasets 处理规则',
    tooltip: 'Datasets 处理规则',
  },
  variables: {
    key: 'variables',
    letter: 'V',
    title: 'Variables 映射规则',
    tooltip: 'Variables 映射规则',
  },
  vlm: {
    key: 'vlm',
    letter: 'L',
    title: 'ValueLevel 生成规则',
    tooltip: 'ValueLevel 生成规则',
  },
  codelist: {
    key: 'codelist',
    letter: 'C',
    title: 'Codelists 处理规则',
    tooltip: 'Codelists 处理规则',
  },
  pages: {
    key: 'pages',
    letter: 'P',
    title: 'Pages 处理规则',
    tooltip: 'Pages 处理规则',
  },
}

export const RULE_MODULE_KEYS = Object.keys(RULE_MODULES)

/** 数据标准分类 */
export const PROCESSING_STANDARDS = [
  { key: 'SDTM', label: 'SDTM', available: true },
  { key: 'ADaM', label: 'ADaM', available: false },
  { key: 'SEND', label: 'SEND', available: false },
]
