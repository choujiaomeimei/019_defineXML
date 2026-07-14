import { ElMessage, ElMessageBox } from 'element-plus'
import service from '@/axios'

export const CODELIST_SCOPES = Object.freeze({
  VARIABLES: 'VARIABLES',
  VLM: 'VLM',
  ALL: 'ALL',
})

const scopeConfig = {
  [CODELIST_SCOPES.VARIABLES]: {
    label: 'Variables',
    endpoint: (baseUrl, projectId) => `${baseUrl}/project-spec-data/extract-var-codelist/${projectId}`,
    payload: scope => ({ scope }),
    timeout: 300000,
  },
  [CODELIST_SCOPES.VLM]: {
    label: 'VLM',
    endpoint: (baseUrl, projectId) => `${baseUrl}/api/vlm/extract-vlm-codelist/${projectId}`,
    payload: scope => ({ scope }),
    timeout: 300000,
  },
  [CODELIST_SCOPES.ALL]: {
    label: '全部',
    endpoint: baseUrl => `${baseUrl}/api/codelist/extract-codelist`,
    payload: (scope, projectId) => ({ projectId, scope }),
    timeout: 600000,
  },
}

const resultLabels = {
  codelistCount: 'Codelists',
  termCount: 'Terms',
  insertedCount: '新增 Terms',
  updatedCount: '更新 Terms',
  deletedCount: '清理旧 Terms',
  specReferenceCount: 'Variables 更新',
  vlmReferenceCount: 'VLM 更新',
  skippedDeletedCount: '跳过删除标记',
  warningCount: '警告',
  fallbackCount: 'EDC 回退',
  codelists: 'Codelists',
  terms: 'Terms',
  nciMatched: 'NCI 匹配',
  nciUnmatched: 'NCI 未匹配',
  preservedManual: '保留人工数据',
  dictionaries: 'Dictionaries',
  variablesUpdated: 'Variables 更新',
  vlmUpdated: 'VLM 更新',
  skippedDeleted: '跳过删除标记',
  reappliedMerges: '重新应用合并',
  source: '数据来源',
  sources: '数据来源',
}

const hasOwn = (value, key) => Object.prototype.hasOwnProperty.call(value, key)

const escapeHtml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#039;')

const displayValue = value => {
  if (value === null || value === undefined || value === '') return '0'
  if (Array.isArray(value)) return value.map(displayValue).join('、')
  if (typeof value === 'object') {
    return Object.entries(value)
      .map(([key, item]) => `${key}: ${displayValue(item)}`)
      .join('；')
  }
  return String(value)
}

const failedDatasetText = item => {
  if (typeof item === 'string') return item
  if (!item || typeof item !== 'object') return displayValue(item)
  const dataset = item.dataset || item.domain || item.name || '未知数据集'
  const reason = item.error || item.message || item.reason || item.status || '提取失败'
  return `${dataset}：${reason}`
}

export const getCodelistExtractionError = (error, scope) => {
  const label = scopeConfig[scope]?.label || 'Codelist'
  const responseMessage = error?.response?.data?.message
    || error?.response?.data?.data?.message
  return `${label} Codelist 提取失败：${responseMessage || error?.message || '服务器未返回错误详情'}`
}

export const extractCodelists = async ({ baseUrl = '', projectId, scope }) => {
  const config = scopeConfig[scope]
  if (!config) throw new Error(`不支持的提取范围：${scope}`)
  if (!projectId) throw new Error('缺少项目 ID')

  const response = await service.post(
    config.endpoint(baseUrl, projectId),
    config.payload(scope, projectId),
    { timeout: config.timeout },
  )

  if (!response.data?.success) {
    throw new Error(response.data?.message || '服务器返回提取失败')
  }

  const rawResult = response.data.data
  return typeof rawResult === 'object' && rawResult !== null
    ? rawResult
    : { message: rawResult || response.data.message || 'Codelist 提取完成' }
}

export const showCodelistExtractionResult = async (scope, result = {}) => {
  const label = scopeConfig[scope]?.label || 'Codelist'
  const failedDatasets = hasOwn(result, 'failedDatasets')
    ? (Array.isArray(result.failedDatasets) ? result.failedDatasets : [result.failedDatasets])
    : []
  const message = result.message || `${label} Codelist 提取完成`
  if (failedDatasets.length > 0) {
    ElMessage.warning(message)
  } else {
    ElMessage.success(message)
  }

  const summaryRows = Object.entries(resultLabels)
    .filter(([key]) => hasOwn(result, key))
    .map(([key, name]) => `
      <div style="display:flex;justify-content:space-between;gap:24px;padding:6px 0;border-bottom:1px solid var(--el-border-color-lighter);">
        <span style="color:var(--el-text-color-secondary);">${escapeHtml(name)}</span>
        <strong style="text-align:right;overflow-wrap:anywhere;">${escapeHtml(displayValue(result[key]))}</strong>
      </div>
    `)

  if (hasOwn(result, 'failedDatasets')) {
    summaryRows.push(`
      <div style="display:flex;justify-content:space-between;gap:24px;padding:6px 0;border-bottom:1px solid var(--el-border-color-lighter);">
        <span style="color:var(--el-text-color-secondary);">失败数据集</span>
        <strong>${failedDatasets.length}</strong>
      </div>
    `)
  }

  const failedSection = failedDatasets.length > 0
    ? `
      <div style="margin-top:14px;">
        <div style="margin-bottom:6px;font-weight:600;color:var(--el-color-warning-dark-2);">失败数据集（${failedDatasets.length}）</div>
        <ul style="margin:0;padding-left:20px;max-height:180px;overflow:auto;">
          ${failedDatasets.map(item => `<li style="margin:4px 0;">${escapeHtml(failedDatasetText(item))}</li>`).join('')}
        </ul>
      </div>
    `
    : ''

  if (summaryRows.length === 0 && !failedSection) return

  await ElMessageBox.alert(
    `<div>${summaryRows.join('')}${failedSection}</div>`,
    `${label} Codelist 提取结果`,
    {
      confirmButtonText: '确定',
      dangerouslyUseHTMLString: true,
      type: failedDatasets.length > 0 ? 'warning' : 'success',
    },
  ).catch(() => {})
}
