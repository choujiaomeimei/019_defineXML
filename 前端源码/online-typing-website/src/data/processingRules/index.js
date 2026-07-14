import datasets from './sdtm/datasets.js'
import variables from './sdtm/variables.js'
import vlm from './sdtm/vlm.js'
import codelist from './sdtm/codelist.js'
import pages from './sdtm/pages.js'
import { RULE_MODULES } from './modules.js'

/** SDTM 规则注册表：{ moduleKey: ruleDoc } */
const SDTM_RULES = {
  datasets,
  variables,
  vlm,
  codelist,
  pages,
}

/** 未上线标准的占位配置 */
const PLACEHOLDER_CONFIG = {
  ADaM: {
    title: 'ADaM 规则开发中',
    description: 'ADaM 分析数据集的处理规则正在整理，上线后将覆盖 Datasets、Variables、ValueLevel、Codelists、Pages 等模块的映射与生成逻辑。',
    plannedSections: ['数据来源与依赖', '字段映射规则', '派生逻辑', '数据存储'],
  },
  SEND: {
    title: 'SEND 规则开发中',
    description: 'SEND 非临床数据标准的处理规则正在整理，上线后将覆盖 Datasets、Variables、ValueLevel、Codelists、Pages 等模块的映射与生成逻辑。',
    plannedSections: ['数据来源与依赖', '字段映射规则', '实验设计关联', '数据存储'],
  },
}

/**
 * 获取指定标准与模块的处理规则
 * @param {string} standard - SDTM | ADaM | SEND
 * @param {string} moduleKey - datasets | variables | vlm | codelist | pages
 * @returns {{ type: 'content' | 'placeholder', rule?: object, placeholder?: object }}
 */
export function getProcessingRule(standard, moduleKey) {
  if (standard === 'SDTM') {
    const rule = SDTM_RULES[moduleKey]
    if (!rule) return { type: 'placeholder', placeholder: { title: '规则未找到', description: '' } }
    return { type: 'content', rule }
  }

  const placeholder = PLACEHOLDER_CONFIG[standard]
  if (placeholder) {
    return {
      type: 'placeholder',
      placeholder: {
        ...placeholder,
        moduleTitle: RULE_MODULES[moduleKey]?.title || moduleKey,
      },
    }
  }

  return { type: 'placeholder', placeholder: PLACEHOLDER_CONFIG.ADaM }
}

export { RULE_MODULES, RULE_MODULE_KEYS, PROCESSING_STANDARDS } from './modules.js'
