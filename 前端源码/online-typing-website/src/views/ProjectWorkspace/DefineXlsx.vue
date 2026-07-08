<template>
  <div class="define-page">
    <div class="define-container">
      <div class="section-header">
        <div class="section-title-group">
          <h3>Define 制作</h3>
          <span class="section-desc">合并所有数据表并生成 Define.xlsx</span>
        </div>
        <div class="header-actions">
          <el-button v-if="generatedFile" type="warning" @click="openOnlineEditor" round>
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" class="btn-icon">
              <path d="M11.5 1.5L14.5 4.5M1 15L1.5 12.5L12 2L14 4L3.5 14.5L1 15Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            在线编辑
          </el-button>
          <el-button type="primary" :loading="generating" @click="generateDefine" round>
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" class="btn-icon">
              <path d="M8 2V10M8 10L5 7M8 10L11 7M2 12V13C2 13.6 2.4 14 3 14H13C13.6 14 14 13.6 14 13V12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            生成并下载 Define.xlsx
          </el-button>
        </div>
      </div>

      <div class="define-layout">
        <div class="op-card">
          <div class="op-card-title">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <rect x="3" y="2" width="12" height="14" rx="2" stroke="var(--saas-primary)" stroke-width="1.5"/>
              <path d="M6 6H12M6 9H10" stroke="var(--saas-primary)" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            包含的数据表 (Sheets)
          </div>
          <div class="op-card-body">
            <div class="sheets-grid">
              <div v-for="s in sheetList" :key="s.name" class="sheet-item">
                <div class="sheet-icon">{{ s.icon }}</div>
                <div class="sheet-info">
                  <span class="sheet-name">{{ s.name }}</span>
                  <span class="sheet-desc">{{ s.desc }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="generateResult" class="result-card" :class="{ success: generateResult.success, error: !generateResult.success }">
          <div class="result-header">
            <svg v-if="generateResult.success" width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="8" stroke="var(--saas-success)" stroke-width="1.5"/>
              <path d="M7 10L9.5 12.5L13.5 7.5" stroke="var(--saas-success)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="8" stroke="var(--saas-danger)" stroke-width="1.5"/>
              <path d="M7.5 7.5L12.5 12.5M12.5 7.5L7.5 12.5" stroke="var(--saas-danger)" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            <span>{{ generateResult.success ? 'Define.xlsx 生成成功' : '生成失败' }}</span>
          </div>
          <div v-if="!generateResult.success" class="result-body">
            <el-alert :title="generateResult.error" type="error" show-icon :closable="false" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import service from '@/axios'

const props = defineProps({ projectId: String })
const route = useRoute()
const router = useRouter()

const generating = ref(false)
const generateResult = ref(null)
const generatedFile = ref(null)

const currentProjectId = computed(() => props.projectId || route.params.projectId)
const baseUrl = ''

const sheetList = [
  { name: 'Study', desc: '研究级别元数据', icon: 'S' },
  { name: 'Datasets', desc: '数据集定义', icon: 'DS' },
  { name: 'Variables', desc: '变量定义', icon: 'Var' },
  { name: 'ValueLevel', desc: '变量级元数据', icon: 'VL' },
  { name: 'Codelists', desc: '代码列表', icon: 'CL' },
  { name: 'Dictionaries', desc: '外部字典引用', icon: 'Di' },
  { name: 'Methods', desc: '方法定义', icon: 'Mt' },
  { name: 'Comments', desc: '注释定义', icon: 'Cm' },
  { name: 'Documents', desc: '文档引用', icon: 'Do' },
]

const generateDefine = async () => {
  const pid = currentProjectId.value
  if (!pid) { ElMessage.warning('项目ID不能为空'); return }
  generating.value = true
  generateResult.value = null
  try {
    ElMessage.info('正在合并数据表，生成 Define.xlsx...')
    const res = await service.get(`${baseUrl}/project/generateDefineXlsx/${pid}`, {
      responseType: 'blob',
      timeout: 120000,
    })
    const contentType = res.headers['content-type'] || 'application/octet-stream'
    const blob = new Blob([res.data], { type: contentType })
    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    const disposition = res.headers['content-disposition']
    let fileName = `Define_${pid}.xlsx`
    if (disposition) {
      const match = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^";\n]+)/)
      if (match) fileName = decodeURIComponent(match[1])
    }
    link.download = fileName
    link.click()
    window.URL.revokeObjectURL(blobUrl)
    generatedFile.value = `Define_${pid}.xlsx`
    generateResult.value = { success: true }
    ElMessage.success('Define.xlsx 生成并下载成功!')
  } catch (e) {
    const msg = e.response?.status === 500 ? '服务器生成失败' : (e.message || '未知错误')
    generateResult.value = { success: false, error: msg }
    ElMessage.error('生成失败: ' + msg)
  } finally {
    generating.value = false
  }
}

const openOnlineEditor = () => {
  const file = generatedFile.value
  if (!file) return
  router.push({
    name: 'DefineEditor',
    params: { projectId: currentProjectId.value },
    query: { file }
  })
}

const checkExistingDefineFile = async () => {
  try {
    const res = await service.get(`${baseUrl}/project/getLatestDefineFile`)
    if (res.data?.success && res.data.data?.exists) {
      generatedFile.value = res.data.data.output_file
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  if (currentProjectId.value) checkExistingDefineFile()
})
</script>

<style scoped lang="less">
.define-page { padding: var(--saas-content-padding); }
.define-container { max-width: var(--saas-content-max-width); margin: 0 auto; }

.section-header {
  display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--saas-space-5);
  .section-title-group {
    h3 { font-size: var(--saas-text-xl); font-weight: var(--saas-font-bold); color: var(--saas-text-primary); margin: 0 0 var(--saas-space-1) 0; }
    .section-desc { font-size: var(--saas-text-sm); color: var(--saas-text-secondary); }
  }
  .header-actions { display: flex; gap: var(--saas-space-2); flex-shrink: 0; }
}

.define-layout { display: flex; flex-direction: column; gap: 20px; }

.op-card {
  background: var(--saas-bg-card); border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg); margin-bottom: 16px; overflow: hidden;
  .op-card-title {
    display: flex; align-items: center; gap: 8px; padding: 14px 20px;
    border-bottom: 1px solid var(--saas-border-light); font-size: 14px;
    font-weight: 600; color: var(--saas-text-primary); background: var(--saas-bg-input);
  }
  .op-card-body { padding: 20px; }
}

.sheets-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px;
  .sheet-item {
    display: flex; align-items: center; gap: 12px;
    padding: 12px 16px; border-radius: var(--saas-radius-md);
    border: 1px solid var(--saas-border-light); background: var(--saas-bg-page);
    transition: all 0.15s;
    &:hover { border-color: var(--saas-primary-lighter); background: var(--saas-primary-bg); }
    .sheet-icon {
      width: 36px; height: 36px; border-radius: 8px;
      background: var(--saas-primary-bg); color: var(--saas-primary);
      display: flex; align-items: center; justify-content: center;
      font-size: 13px; font-weight: 700; flex-shrink: 0;
    }
    .sheet-info { display: flex; flex-direction: column; gap: 2px; }
    .sheet-name { font-size: 14px; font-weight: 600; color: var(--saas-text-primary); }
    .sheet-desc { font-size: 12px; color: var(--saas-text-tertiary); }
  }
}

.result-card {
  background: var(--saas-bg-card); border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg); padding: 20px; margin-bottom: 16px;
  &.success { border-left: 3px solid var(--saas-success); }
  &.error { border-left: 3px solid var(--saas-danger); }
  .result-header {
    display: flex; align-items: center; gap: 8px;
    font-weight: 600; font-size: 15px; color: var(--saas-text-primary);
  }
  .result-body { margin-top: 14px; }
}

.btn-icon { margin-right: var(--saas-space-1); }
</style>
