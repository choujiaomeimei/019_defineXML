<template>
  <div class="define-editor-page">
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-button size="small" @click="goBack" round>
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <span class="file-name-display">{{ fileName || '加载中...' }}</span>
        <el-tag v-if="hasUnsavedChanges" type="warning" size="small" effect="plain">未保存</el-tag>
        <el-tag v-if="saveSuccess" type="success" size="small" effect="plain">已保存</el-tag>
      </div>
      <div class="toolbar-right">
        <el-button size="small" type="primary" :loading="saving" @click="saveData" :disabled="!sheetLoaded" round>
          <el-icon><DocumentChecked /></el-icon> 保存
        </el-button>
        <el-button size="small" type="success" @click="exportFile" :disabled="!sheetLoaded" round>
          <el-icon><Download /></el-icon> 导出 xlsx
        </el-button>
      </div>
    </div>
    <div class="editor-container">
      <div v-if="loading" class="editor-loading">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <span>正在加载 Excel 文件...</span>
      </div>
      <div v-if="loadError" class="editor-error">
        <el-icon :size="32"><WarningFilled /></el-icon>
        <span>{{ loadError }}</span>
        <el-button size="small" type="primary" @click="goBack">返回</el-button>
      </div>
      <div id="luckysheet" class="luckysheet-host"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, DocumentChecked, Download, Loading, WarningFilled } from '@element-plus/icons-vue'
import service from '@/axios'

const props = defineProps({ projectId: String })
const route = useRoute()
const router = useRouter()
const baseUrl = ''

const loading = ref(true)
const loadError = ref('')
const saving = ref(false)
const sheetLoaded = ref(false)
const hasUnsavedChanges = ref(false)
const saveSuccess = ref(false)

const fileName = computed(() => route.query.file || '')
const currentProjectId = computed(() => props.projectId || route.params.projectId)

const goBack = () => {
  if (window.luckysheet) {
    try { window.luckysheet.destroy() } catch (e) { /* ignore */ }
  }
  router.push({ name: 'DefineXlsx', params: { projectId: currentProjectId.value } })
}

const initLuckysheetFromData = (sheets, title) => {
  if (!sheets || sheets.length === 0) {
    loadError.value = '文件没有有效的工作表数据'
    loading.value = false
    return
  }
  nextTick(() => {
    try {
      window.luckysheet.create({
        container: 'luckysheet',
        data: sheets,
        title: title || fileName.value,
        showinfobar: false,
        showsheetbar: true,
        showstatisticBar: true,
        sheetFormulaBar: true,
        allowEdit: true,
        enableAddRow: true,
        enableAddBackTop: false,
        showConfigWindowResize: false,
        forceCalculation: false,
        lang: 'zh',
        hook: {
          cellUpdated: () => {
            hasUnsavedChanges.value = true
            saveSuccess.value = false
          },
          sheetActivate: () => {},
        }
      })
      sheetLoaded.value = true
      loading.value = false
    } catch (e) {
      console.error('Luckysheet 初始化失败:', e)
      loadError.value = 'Excel 编辑器初始化失败: ' + (e.message || '未知错误')
      loading.value = false
    }
  })
}

const loadFromSavedData = async () => {
  try {
    const res = await service.get(`${baseUrl}/project/getDefineSavedData`, {
      params: { projectId: currentProjectId.value, fileName: fileName.value }
    })
    if (res.data?.success && res.data.data?.sheetData) {
      const sheets = JSON.parse(res.data.data.sheetData)
      if (Array.isArray(sheets) && sheets.length > 0) {
        initLuckysheetFromData(sheets, fileName.value)
        return true
      }
    }
  } catch (e) {
    console.log('无已保存数据，从文件加载')
  }
  return false
}

const loadFromFile = async () => {
  try {
    // Try the new generated Define first, fallback to old downloadDefine
    let res
    try {
      res = await service.get(`${baseUrl}/project/generateDefineXlsx/${currentProjectId.value}`, {
        responseType: 'blob',
        timeout: 120000,
      })
    } catch (e) {
      res = await service.get(`${baseUrl}/project/downloadDefine`, {
        params: { file: fileName.value },
        responseType: 'blob'
      })
    }
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const file = new File([blob], fileName.value, { type: blob.type })

    const LuckyExcel = (await import('luckyexcel')).default
    LuckyExcel.transformExcelToLucky(file, (exportJson) => {
      if (!exportJson || !exportJson.sheets || exportJson.sheets.length === 0) {
        loadError.value = '文件解析失败，未找到工作表数据'
        loading.value = false
        return
      }
      initLuckysheetFromData(exportJson.sheets, exportJson.info?.name || fileName.value)
    })
  } catch (e) {
    console.error('加载文件失败:', e)
    loadError.value = '加载文件失败: ' + (e.response?.data?.message || e.message || '网络错误')
    loading.value = false
  }
}

const saveData = async () => {
  if (!window.luckysheet) return
  saving.value = true
  try {
    const allSheets = window.luckysheet.getAllSheets()
    const payload = {
      projectId: currentProjectId.value,
      fileName: fileName.value,
      sheetData: JSON.stringify(allSheets)
    }
    const res = await service.post(`${baseUrl}/project/saveDefine`, payload)
    if (res.data?.success) {
      ElMessage.success('保存成功')
      hasUnsavedChanges.value = false
      saveSuccess.value = true
    } else {
      ElMessage.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    console.error('保存失败:', e)
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message || '网络错误'))
  } finally {
    saving.value = false
  }
}

const exportFile = async () => {
  try {
    const res = await service.get(`${baseUrl}/project/downloadDefine`, {
      params: { file: fileName.value },
      responseType: 'blob'
    })
    const contentType = res.headers['content-type'] || 'application/octet-stream'
    const blob = new Blob([res.data], { type: contentType })
    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = fileName.value || 'define.xlsx'
    link.click()
    window.URL.revokeObjectURL(blobUrl)
  } catch (e) {
    console.error('导出失败:', e)
    ElMessage.error('导出失败')
  }
}

onMounted(async () => {
  if (!fileName.value) {
    loadError.value = '未指定文件名'
    loading.value = false
    return
  }

  const loadedFromDB = await loadFromSavedData()
  if (!loadedFromDB) {
    await loadFromFile()
  }
})

onBeforeUnmount(() => {
  if (window.luckysheet) {
    try { window.luckysheet.destroy() } catch (e) { /* ignore */ }
  }
})
</script>

<style scoped lang="less">
.define-editor-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--saas-topbar-height, 56px));
  background: #fff;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid var(--saas-border-light, #e5e7eb);
  background: var(--saas-bg-card, #fff);
  flex-shrink: 0;
  z-index: 10;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .file-name-display {
    font-size: 14px;
    font-weight: 600;
    color: var(--saas-text-primary, #1f2937);
    max-width: 400px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.editor-container {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.luckysheet-host {
  position: absolute;
  width: 100%;
  height: 100%;
  left: 0;
  top: 0;
}

.editor-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.9);
  z-index: 20;
  font-size: 14px;
  color: var(--saas-text-secondary, #6b7280);
}

.editor-error {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: #fff;
  z-index: 20;
  font-size: 14px;
  color: var(--saas-danger, #ef4444);
}
</style>
