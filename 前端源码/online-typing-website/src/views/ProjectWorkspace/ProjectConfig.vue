<template>
  <div class="config-page">
    <div class="config-container">
      <div class="section-header">
        <div class="section-title-group">
          <h3>文件上传</h3>
          <span class="section-desc">上传与管理项目所需文件</span>
        </div>
      </div>

      <!-- 未选择标准类型时的提示 -->
      <div v-if="!projectConfig.standardTypes || projectConfig.standardTypes.length === 0" class="empty-card">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
          <rect x="6" y="8" width="36" height="32" rx="4" stroke="#D1D5DB" stroke-width="2"/>
          <path d="M18 24H30M24 18V30" stroke="#D1D5DB" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <p>请先在项目信息中选择标准类型</p>
      </div>

      <!-- SDTM 文件管理 -->
      <template v-if="projectConfig.standardTypes && projectConfig.standardTypes.includes('SDTM')">
        <div class="config-card">
          <div class="card-title-bar">
            <div class="card-title">
              <div class="title-dot"></div>
              SDTM 文件管理
            </div>
          </div>
          <div class="card-body">
            <div class="upload-grid">
              <!-- aCRF (单文件替换) -->
              <div class="upload-card">
                <div class="upload-card-header">
                  <span class="upload-label">aCRF</span>
                  <el-tag v-if="processingCategory === 'ACRF'" type="warning" size="small" round>处理中...</el-tag>
                </div>
                <div v-if="currentFiles.ACRF" class="current-file-info">
                  <div class="file-name">{{ currentFiles.ACRF.originalName }}</div>
                  <div class="file-meta">{{ formatFileSize(currentFiles.ACRF.fileSize) }} · {{ formatDateTime(currentFiles.ACRF.uploadTime) }}
                    <el-tag v-if="currentFiles.ACRF.processStatus === 'completed'" type="success" size="small" style="margin-left:6px">已处理</el-tag>
                    <el-tag v-else-if="currentFiles.ACRF.processStatus === 'failed'" type="danger" size="small" style="margin-left:6px">处理失败</el-tag>
                  </div>
                  <div class="file-actions">
                    <el-button size="small" type="primary" text @click="triggerReplace('ACRF')" :disabled="!!processingCategory">替换文件</el-button>
                    <el-button size="small" type="danger" text @click="handleDelete(currentFiles.ACRF)" :disabled="!!processingCategory">删除</el-button>
                    <el-button v-if="currentFiles.ACRF.processStatus === 'failed'" size="small" type="warning" text @click="processFile(currentFiles.ACRF, 'acrf')" :loading="processingCategory === 'ACRF'">重新处理</el-button>
                    <el-button v-if="currentFiles.ACRF.outputFilePath" size="small" type="success" text @click="downloadProcessResult(currentFiles.ACRF)">下载结果</el-button>
                  </div>
                </div>
                <el-upload
                  v-show="!currentFiles.ACRF"
                  ref="acrfUploadRef"
                  class="saas-upload"
                  drag
                  :action="getUploadUrl('ACRF')"
                  :before-upload="beforeAcrfUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :headers="uploadHeaders"
                  accept=".pdf"
                  :limit="1"
                  :show-file-list="false"
                  :data="{ projectId: currentProjectId, fileCategory: 'ACRF', username: currentUsername }"
                >
                  <div class="upload-inner">
                    <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                      <path d="M14 18V7M14 7L9 12M14 7L19 12" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M5 18V21C5 22.1 5.9 23 7 23H21C22.1 23 23 22.1 23 21V18" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <span class="upload-text">拖拽文件或 <em>点击上传</em></span>
                    <span class="upload-hint">PDF 格式</span>
                  </div>
                </el-upload>
                <input ref="acrfReplaceInput" type="file" accept=".pdf" class="visually-hidden" @change="handleReplaceFile($event, 'ACRF')">
              </div>

              <!-- P21 空 Spec (单文件替换) -->
              <div class="upload-card">
                <div class="upload-card-header">
                  <span class="upload-label">P21空Spec</span>
                  <el-tag v-if="processingCategory === 'P21_SPEC'" type="warning" size="small" round>处理中...</el-tag>
                </div>
                <div v-if="currentFiles.P21_SPEC" class="current-file-info">
                  <div class="file-name">{{ currentFiles.P21_SPEC.originalName }}</div>
                  <div class="file-meta">{{ formatFileSize(currentFiles.P21_SPEC.fileSize) }} · {{ formatDateTime(currentFiles.P21_SPEC.uploadTime) }}
                    <el-tag v-if="currentFiles.P21_SPEC.processStatus === 'completed'" type="success" size="small" style="margin-left:6px">已处理</el-tag>
                    <el-tag v-else-if="currentFiles.P21_SPEC.processStatus === 'failed'" type="danger" size="small" style="margin-left:6px">处理失败</el-tag>
                  </div>
                  <div class="file-actions">
                    <el-button size="small" type="primary" text @click="triggerReplace('P21_SPEC')" :disabled="!!processingCategory">替换文件</el-button>
                    <el-button size="small" type="danger" text @click="handleDelete(currentFiles.P21_SPEC)" :disabled="!!processingCategory">删除</el-button>
                    <el-button v-if="currentFiles.P21_SPEC.processStatus === 'failed'" size="small" type="warning" text @click="processFile(currentFiles.P21_SPEC, 'p21spec')" :loading="processingCategory === 'P21_SPEC'">重新处理</el-button>
                    <el-button v-if="currentFiles.P21_SPEC.outputFilePath" size="small" type="success" text @click="downloadProcessResult(currentFiles.P21_SPEC)">下载结果</el-button>
                  </div>
                </div>
                <el-upload
                  v-show="!currentFiles.P21_SPEC"
                  class="saas-upload"
                  drag
                  :action="getUploadUrl('P21_SPEC')"
                  :before-upload="beforeSpecUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :headers="uploadHeaders"
                  accept=".xlsx,.xls"
                  :limit="1"
                  :show-file-list="false"
                  :data="{ projectId: currentProjectId, fileCategory: 'P21_SPEC', username: currentUsername }"
                >
                  <div class="upload-inner">
                    <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                      <path d="M14 18V7M14 7L9 12M14 7L19 12" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M5 18V21C5 22.1 5.9 23 7 23H21C22.1 23 23 22.1 23 21V18" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <span class="upload-text">拖拽文件或 <em>点击上传</em></span>
                    <span class="upload-hint">Excel 格式</span>
                  </div>
                </el-upload>
                <input ref="p21ReplaceInput" type="file" accept=".xlsx,.xls" class="visually-hidden" @change="handleReplaceFile($event, 'P21_SPEC')">
              </div>

              <!-- 项目 Spec (单文件替换) -->
              <div class="upload-card">
                <div class="upload-card-header">
                  <span class="upload-label">项目Spec</span>
                  <el-tag v-if="processingCategory === 'PROJECT_SPEC'" type="warning" size="small" round>处理中...</el-tag>
                </div>
                <div v-if="currentFiles.PROJECT_SPEC" class="current-file-info">
                  <div class="file-name">{{ currentFiles.PROJECT_SPEC.originalName }}</div>
                  <div class="file-meta">{{ formatFileSize(currentFiles.PROJECT_SPEC.fileSize) }} · {{ formatDateTime(currentFiles.PROJECT_SPEC.uploadTime) }}
                    <el-tag v-if="currentFiles.PROJECT_SPEC.processStatus === 'completed'" type="success" size="small" style="margin-left:6px">已处理</el-tag>
                    <el-tag v-else-if="currentFiles.PROJECT_SPEC.processStatus === 'failed'" type="danger" size="small" style="margin-left:6px">处理失败</el-tag>
                  </div>
                  <div class="file-actions">
                    <el-button size="small" type="primary" text @click="triggerReplace('PROJECT_SPEC')" :disabled="!!processingCategory">替换文件</el-button>
                    <el-button size="small" type="danger" text @click="handleDelete(currentFiles.PROJECT_SPEC)" :disabled="!!processingCategory">删除</el-button>
                    <el-button v-if="currentFiles.PROJECT_SPEC.processStatus === 'failed' || (currentFiles.PROJECT_SPEC.processStatus === 'completed' && !currentFiles.PROJECT_SPEC.outputFilePath)" size="small" type="warning" text @click="processFile(currentFiles.PROJECT_SPEC, 'projectspec')" :loading="processingCategory === 'PROJECT_SPEC'">{{ currentFiles.PROJECT_SPEC.processStatus === 'failed' ? '重新处理' : '导出Excel' }}</el-button>
                    <el-button v-if="currentFiles.PROJECT_SPEC.outputFilePath" size="small" type="success" text @click="downloadProcessResult(currentFiles.PROJECT_SPEC)">下载结果</el-button>
                  </div>
                </div>
                <el-upload
                  v-show="!currentFiles.PROJECT_SPEC"
                  class="saas-upload"
                  drag
                  :action="getUploadUrl('PROJECT_SPEC')"
                  :before-upload="beforeSpecUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :headers="uploadHeaders"
                  accept=".xlsx,.xls"
                  :limit="1"
                  :show-file-list="false"
                  :data="{ projectId: currentProjectId, fileCategory: 'PROJECT_SPEC', username: currentUsername }"
                >
                  <div class="upload-inner">
                    <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                      <path d="M14 18V7M14 7L9 12M14 7L19 12" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M5 18V21C5 22.1 5.9 23 7 23H21C22.1 23 23 22.1 23 21V18" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <span class="upload-text">拖拽文件或 <em>点击上传</em></span>
                    <span class="upload-hint">Excel 格式</span>
                  </div>
                </el-upload>
                <input ref="specReplaceInput" type="file" accept=".xlsx,.xls" class="visually-hidden" @change="handleReplaceFile($event, 'PROJECT_SPEC')">
              </div>

              <!-- EDC建库说明 (单文件替换) -->
              <div class="upload-card">
                <div class="upload-card-header">
                  <span class="upload-label">EDC建库说明</span>
                  <el-tag v-if="processingCategory === 'EDC_CODELIST'" type="warning" size="small" round>处理中...</el-tag>
                </div>
                <div v-if="currentFiles.EDC_CODELIST" class="current-file-info">
                  <div class="file-name">{{ currentFiles.EDC_CODELIST.originalName }}</div>
                  <div class="file-meta">{{ formatFileSize(currentFiles.EDC_CODELIST.fileSize) }} · {{ formatDateTime(currentFiles.EDC_CODELIST.uploadTime) }}</div>
                  <div class="file-actions">
                    <el-button size="small" type="primary" text @click="triggerReplace('EDC_CODELIST')" :disabled="!!processingCategory">替换文件</el-button>
                    <el-button size="small" type="danger" text @click="handleDelete(currentFiles.EDC_CODELIST)" :disabled="!!processingCategory">删除</el-button>
                  </div>
                </div>
                <el-upload
                  v-show="!currentFiles.EDC_CODELIST"
                  class="saas-upload"
                  drag
                  :action="getUploadUrl('EDC_CODELIST')"
                  :before-upload="beforeSpecUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :headers="uploadHeaders"
                  accept=".xlsx,.xls"
                  :limit="1"
                  :show-file-list="false"
                  :data="{ projectId: currentProjectId, fileCategory: 'EDC_CODELIST', username: currentUsername }"
                >
                  <div class="upload-inner">
                    <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                      <path d="M14 18V7M14 7L9 12M14 7L19 12" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M5 18V21C5 22.1 5.9 23 7 23H21C22.1 23 23 22.1 23 21V18" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <span class="upload-text">拖拽文件或 <em>点击上传</em></span>
                    <span class="upload-hint">Excel 格式 (含CODELIST sheet)</span>
                  </div>
                </el-upload>
                <input ref="edcReplaceInput" type="file" accept=".xlsx,.xls" class="visually-hidden" @change="handleReplaceFile($event, 'EDC_CODELIST')">
              </div>

              <!-- XPT (多文件模式，折叠表格) -->
              <div class="upload-card upload-card--xpt">
                <div class="upload-card-header">
                  <span class="upload-label">XPT 文件</span>
                  <div class="xpt-header-actions">
                    <el-tag v-if="xptFiles.length > 0" type="success" size="small" round>{{ xptFiles.length }} 个文件</el-tag>
                    <el-tag v-if="processingCategory === 'XPT'" type="warning" size="small" round>处理中...</el-tag>
                    <el-button v-if="xptFiles.length > 0" size="small" type="danger" text @click="handleDeleteAllXpt">全部删除</el-button>
                  </div>
                </div>
                <el-upload
                  class="saas-upload"
                  drag
                  :action="getUploadUrl('XPT')"
                  :before-upload="beforeXptUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :headers="uploadHeaders"
                  accept=".xpt"
                  :limit="50"
                  :show-file-list="false"
                  :data="{ projectId: currentProjectId, fileCategory: 'XPT', username: currentUsername }"
                  multiple
                >
                  <div class="upload-inner">
                    <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                      <path d="M14 18V7M14 7L9 12M14 7L19 12" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M5 18V21C5 22.1 5.9 23 7 23H21C22.1 23 23 22.1 23 21V18" stroke="var(--saas-primary-light)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <span class="upload-text">拖拽文件或 <em>点击上传</em></span>
                    <span class="upload-hint">XPT 格式，支持多文件</span>
                  </div>
                </el-upload>
                <div v-if="xptFiles.length > 0" class="xpt-table-section">
                  <div class="xpt-table-toggle" @click="xptExpanded = !xptExpanded">
                    <span class="toggle-icon">{{ xptExpanded ? '▼' : '▶' }}</span>
                    <span>已上传文件 ({{ xptFiles.length }})</span>
                    <span v-if="!xptExpanded && xptFiles.length > xptDisplayLimit" class="toggle-hint">
                      显示前 {{ xptDisplayLimit }} 条，还有 {{ xptFiles.length - xptDisplayLimit }} 条
                    </span>
                    <span class="toggle-action">{{ xptExpanded ? '收起' : '展开' }}</span>
                  </div>
                  <div class="xpt-table-wrapper" :class="{ 'xpt-table-wrapper--expanded': xptExpanded }">
                    <el-table :data="displayedXptFiles" size="small" :show-header="true" :border="false" class="xpt-compact-table">
                      <el-table-column prop="originalName" label="文件名" min-width="160" show-overflow-tooltip />
                      <el-table-column prop="fileSize" label="大小" width="100" align="center">
                        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
                      </el-table-column>
                      <el-table-column label="操作" width="70" align="center">
                        <template #default="{ row }">
                          <el-button size="small" type="danger" text @click="handleDelete(row)">删除</el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                    <div v-if="!xptExpanded && xptFiles.length > xptDisplayLimit" class="xpt-show-more" @click="xptExpanded = true">
                      查看全部 {{ xptFiles.length }} 个文件
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ApiGetProjectConfig, ApiSaveProjectConfig, ApiUpdateProjectConfig,
  ApiGetProjectFiles, ApiDeleteProjectFile, ApiProcessProjectFile, ApiGetProjects
} from '@/api'
import service from '@/axios'
import { formatFileSize, formatDateTime } from '@/utils/format'

const route = useRoute()
const saving = ref(false)
const loading = ref(false)
const configExists = ref(false)
const currentProjectId = computed(() => route.params.projectId)
const currentUsername = computed(() => {
  const user = localStorage.getItem('user')
  return user ? JSON.parse(user).username : 'system'
})

const projectConfig = ref({
  projectId: '', encoding: 'UTF-8', language: 'CN',
  standardTypes: ['SDTM'], standardVersion: '', ctVersion: '',
  standardLanguage: 'chinese', sourceFormat: 'SAS(XPT)', configuration: ''
})

const currentFiles = ref({ ACRF: null, P21_SPEC: null, PROJECT_SPEC: null, EDC_CODELIST: null })
const xptFiles = ref([])
const processingCategory = ref(null)
const xptExpanded = ref(false)
const xptDisplayLimit = 5

const displayedXptFiles = computed(() => {
  if (xptExpanded.value) return xptFiles.value
  return xptFiles.value.slice(0, xptDisplayLimit)
})

const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const uploadHeaders = computed(() => {
  const headers = { 'Accept': 'application/json' }
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      if (user.token) {
        headers['Authorization'] = `Bearer ${user.token}`
      }
    } catch (e) { /* ignore */ }
  }
  return headers
})

const acrfReplaceInput = ref(null)
const p21ReplaceInput = ref(null)
const specReplaceInput = ref(null)
const edcReplaceInput = ref(null)

const getUploadUrl = (category) => `${baseUrl}/files/upload`

const beforeAcrfUpload = (file) => {
  if (!file.name.toLowerCase().endsWith('.pdf')) { ElMessage.error('只能上传PDF格式文件!'); return false }
  if (file.size / 1024 / 1024 >= 100) { ElMessage.error('文件大小不能超过 100MB!'); return false }
  return true
}
const beforeSpecUpload = (file) => {
  if (!file.name.toLowerCase().endsWith('.xlsx') && !file.name.toLowerCase().endsWith('.xls')) { ElMessage.error('只能上传Excel格式文件!'); return false }
  if (file.size / 1024 / 1024 >= 50) { ElMessage.error('文件大小不能超过 50MB!'); return false }
  return true
}
const beforeXptUpload = (file) => {
  if (!file.name.toLowerCase().endsWith('.xpt')) { ElMessage.error('只能上传XPT格式文件!'); return false }
  if (file.size / 1024 / 1024 >= 100) { ElMessage.error('文件大小不能超过 100MB!'); return false }
  return true
}

let _uploadMsgTimer = null
let _uploadPendingCount = 0

const handleUploadSuccess = async (response) => {
  if (response && response.success) {
    const fileCategory = response.data?.fileCategory
    const fileId = response.data?.fileId

    _uploadPendingCount++
    if (_uploadMsgTimer) clearTimeout(_uploadMsgTimer)
    _uploadMsgTimer = setTimeout(() => {
      const n = _uploadPendingCount
      ElMessage.success(n > 1 ? `${n} 个文件上传成功，正在自动处理...` : '文件上传成功，正在自动处理...')
      _uploadPendingCount = 0
      _uploadMsgTimer = null
    }, 300)

    await loadProjectFiles()
    if (fileCategory && fileId) {
      await autoProcess({ fileId, fileCategory }, fileCategory)
    }
  } else {
    ElMessage.error(response?.message || '上传失败')
  }
}

const autoProcess = async (fileInfo, category) => {
  processingCategory.value = category
  try {
    const res = await service.post(`${baseUrl}/files/process`, {
      fileId: fileInfo.fileId
    })
    const result = res.data
    if (result.success) {
      ElMessage.success('文件处理完成')
    } else {
      ElMessage.warning(`自动处理失败：${result.message || '未知错误'}，可手动重新处理`)
    }
    await loadProjectFiles()
  } catch (error) {
    const msg = error.response?.data?.message || error.message
    ElMessage.warning(`自动处理失败：${msg}，可手动重新处理`)
    await loadProjectFiles()
  } finally {
    processingCategory.value = null
  }
}
const handleUploadError = (error, file) => ElMessage.error(`${file.name} 上传失败!`)

const triggerReplace = (category) => {
  const inputMap = { ACRF: acrfReplaceInput, P21_SPEC: p21ReplaceInput, PROJECT_SPEC: specReplaceInput, EDC_CODELIST: edcReplaceInput }
  const input = inputMap[category]
  if (input?.value) input.value.click()
}

const handleReplaceFile = async (event, category) => {
  const file = event.target.files[0]
  if (!file) return
  event.target.value = ''

  const formData = new FormData()
  formData.append('file', file)
  formData.append('projectId', currentProjectId.value)
  formData.append('fileCategory', category)
  formData.append('username', currentUsername.value)

  try {
    const res = await service.post(`${baseUrl}/files/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.success) {
      const fileId = res.data.data?.fileId
      ElMessage.success('文件替换成功，正在自动处理...')
      await loadProjectFiles()
      if (fileId) {
        await autoProcess({ fileId, fileCategory: category }, category)
      }
    } else {
      ElMessage.error(res.data.message || '替换失败')
    }
  } catch (e) {
    ElMessage.error('替换失败: ' + (e.message || ''))
  }
}

const handleDelete = async (file) => {
  try {
    await ElMessageBox.confirm(`确定要删除文件 "${file.originalName}" 吗？`, '确认删除', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const res = await ApiDeleteProjectFile(file.fileId)
    if (res.data.success) { ElMessage.success('文件删除成功'); await loadProjectFiles() }
    else ElMessage.error(res.data.message || '删除失败')
  } catch (error) { if (error !== 'cancel') ElMessage.error('删除失败') }
}

const processFile = async (file, type) => {
  const categoryMap = { acrf: 'ACRF', p21spec: 'P21_SPEC', projectspec: 'PROJECT_SPEC', xpt: 'XPT' }
  const category = categoryMap[type]
  processingCategory.value = category
  try {
    ElMessage.info('开始处理文件...')
    const res = await service.post(`${baseUrl}/files/process`, { fileId: file.fileId })
    const result = res.data
    if (result.success) { ElMessage.success('文件处理成功'); await loadProjectFiles() }
    else ElMessage.error(result.message || '处理失败')
  } catch (error) {
    const msg = error.response?.data?.message || error.message
    ElMessage.error('处理失败: ' + msg)
  }
  finally { processingCategory.value = null }
}

const downloadProcessResult = (file) => {
  const url = `${baseUrl}/files/download?fileId=${file.fileId}`
  const userStr = localStorage.getItem('user')
  let token = ''
  if (userStr) {
    try { token = JSON.parse(userStr).token || '' } catch (e) { /* ignore */ }
  }
  const link = document.createElement('a')
  if (token) {
    service.get(url, { responseType: 'blob' }).then(res => {
      const contentType = res.headers['content-type'] || 'application/octet-stream'
      const blob = new Blob([res.data], { type: contentType })
      const blobUrl = window.URL.createObjectURL(blob)
      link.href = blobUrl
      const disposition = res.headers['content-disposition']
      let fileName = 'download'
      if (disposition) {
        const match = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^"';\n]+)/)
        if (match) fileName = decodeURIComponent(match[1])
      }
      link.download = fileName
      link.click()
      window.URL.revokeObjectURL(blobUrl)
    }).catch(() => {
      ElMessage.error('下载失败')
    })
  } else {
    link.href = url
    link.click()
  }
}

const handleDeleteAllXpt = async () => {
  if (xptFiles.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要删除全部 ${xptFiles.value.length} 个 XPT 文件吗？`,
      '确认批量删除',
      { confirmButtonText: '全部删除', cancelButtonText: '取消', type: 'warning' }
    )
    let successCount = 0
    let failCount = 0
    for (const f of xptFiles.value) {
      try {
        const res = await ApiDeleteProjectFile(f.fileId)
        if (res.data.success) successCount++
        else failCount++
      } catch { failCount++ }
    }
    if (failCount === 0) {
      ElMessage.success(`已删除全部 ${successCount} 个文件`)
    } else {
      ElMessage.warning(`成功删除 ${successCount} 个，失败 ${failCount} 个`)
    }
    await loadProjectFiles()
    xptExpanded.value = false
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('批量删除失败')
  }
}

const loadProjectFiles = async () => {
  if (!currentProjectId.value) return
  try {
    const categories = ['ACRF', 'P21_SPEC', 'PROJECT_SPEC', 'EDC_CODELIST']
    const results = await Promise.all(categories.map(c => ApiGetProjectFiles(currentProjectId.value, c)))

    categories.forEach((cat, i) => {
      const data = results[i]?.data
      if (data?.success && Array.isArray(data.data) && data.data.length > 0) {
        currentFiles.value[cat] = data.data[0]
      } else {
        currentFiles.value[cat] = null
      }
    })

    const xptRes = await ApiGetProjectFiles(currentProjectId.value, 'XPT')
    if (xptRes.data?.success && Array.isArray(xptRes.data.data)) {
      xptFiles.value = xptRes.data.data
    } else {
      xptFiles.value = []
    }
  } catch (error) { console.error('加载文件列表失败:', error) }
}

const saveConfig = async () => {
  if (!currentProjectId.value) { ElMessage.warning('项目ID不能为空'); return }
  saving.value = true
  try {
    projectConfig.value.projectId = currentProjectId.value
    projectConfig.value.chineseStandard = projectConfig.value.standardLanguage === 'chinese'
    projectConfig.value.englishStandard = projectConfig.value.standardLanguage === 'english'
    let response
    if (configExists.value) response = await ApiUpdateProjectConfig(projectConfig.value)
    else response = await ApiSaveProjectConfig(projectConfig.value)
    if (response.data.success) { ElMessage.success('配置保存成功!'); configExists.value = true; await loadProjectConfig() }
    else ElMessage.error(response.data.message || '保存配置失败')
  } catch (error) { ElMessage.error('保存配置失败，请稍后重试') }
  finally { saving.value = false }
}

const resetConfig = () => {
  projectConfig.value = { projectId: currentProjectId.value || '', encoding: 'UTF-8', language: 'CN', standardTypes: ['SDTM'], standardVersion: 'SDTM-IG3.2', ctVersion: 'CT2021-12-17', standardLanguage: 'chinese', sourceFormat: 'SAS(XPT)', configuration: '' }
  ElMessage.info('配置已重置为默认值')
}

const loadProjectConfig = async () => {
  if (!currentProjectId.value) return
  try {
    loading.value = true
    const user = localStorage.getItem('user')
    const username = user ? JSON.parse(user).username : ''
    const projectsRes = await ApiGetProjects(username)
    let projectStandardTypes = ['SDTM']
    if (projectsRes.data.success && projectsRes.data.data) {
      const projects = Array.isArray(projectsRes.data.data) ? projectsRes.data.data : [projectsRes.data.data]
      const cur = projects.find(p => p.projectId === currentProjectId.value)
      if (cur && cur.standardType) projectStandardTypes = cur.standardType.split(',').filter(t => t.trim())
    }
    const response = await ApiGetProjectConfig(currentProjectId.value)
    if (response.data.success && response.data.data) {
      const c = response.data.data
      projectConfig.value = { projectId: c.project_id || currentProjectId.value, encoding: c.encoding || 'UTF-8', language: c.language || 'CN', standardTypes: projectStandardTypes, standardVersion: c.standard_version || '', ctVersion: c.ct_version || '', standardLanguage: c.chinese_standard ? 'chinese' : 'english', sourceFormat: c.source_format || 'SAS(XPT)', configuration: c.configuration || '' }
      configExists.value = true
    } else {
      projectConfig.value = { projectId: currentProjectId.value, encoding: 'UTF-8', language: 'CN', standardTypes: projectStandardTypes, standardVersion: '', ctVersion: '', standardLanguage: 'chinese', sourceFormat: 'SAS(XPT)', configuration: '' }
      configExists.value = false
    }
  } catch (error) {
    projectConfig.value = { projectId: currentProjectId.value, encoding: 'UTF-8', language: 'CN', standardTypes: ['SDTM'], standardVersion: '', ctVersion: '', standardLanguage: 'chinese', sourceFormat: 'SAS(XPT)', configuration: '' }
    configExists.value = false
  } finally { loading.value = false }
}

onMounted(() => { if (currentProjectId.value) Promise.all([loadProjectConfig(), loadProjectFiles()]) })
</script>

<style scoped lang="less">
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
}

.config-page { padding: var(--saas-content-padding); }
.config-container { max-width: var(--saas-content-max-width); margin: 0 auto; }

.section-header {
  display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: var(--saas-space-5);
  .section-title-group {
    h3 { font-size: var(--saas-text-xl); font-weight: var(--saas-font-bold); color: var(--saas-text-primary); margin: 0 0 var(--saas-space-1) 0; }
    .section-desc { font-size: var(--saas-text-sm); color: var(--saas-text-secondary); }
  }
}

.config-card {
  background: var(--saas-bg-card); border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg); margin-bottom: 20px; overflow: hidden;
  .card-title-bar {
    display: flex; align-items: center; justify-content: space-between;
    padding: 16px 24px; border-bottom: 1px solid var(--saas-border-light); background: var(--saas-bg-input);
    .card-title {
      display: flex; align-items: center; gap: 10px; font-size: 15px; font-weight: 600; color: var(--saas-text-primary);
      .title-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--saas-primary); }
    }
    .card-actions { display: flex; gap: 8px; }
  }
  .card-body { padding: 24px; }
}

.config-strip {
  display: flex;
  align-items: flex-end;
  gap: var(--saas-space-5);
  background: var(--saas-bg-card);
  border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg);
  padding: var(--saas-space-4) var(--saas-space-5);
  margin-bottom: 20px;

  .config-strip-fields {
    display: flex;
    gap: var(--saas-space-5);
    flex: 1;

    .strip-field {
      .strip-label {
        display: block;
        font-size: var(--saas-text-xs);
        font-weight: var(--saas-font-medium);
        color: var(--saas-text-secondary);
        margin-bottom: var(--saas-space-1);
      }
      :deep(.el-select) { width: 180px; }
    }
  }

  .config-strip-actions {
    display: flex;
    gap: var(--saas-space-2);
    flex-shrink: 0;
  }
}

.empty-card {
  text-align: center; padding: 60px 20px; background: var(--saas-bg-card);
  border: 1px solid var(--saas-border-light); border-radius: var(--saas-radius-lg); margin-bottom: 20px;
  p { margin-top: 12px; color: var(--saas-text-secondary); font-size: 14px; }
}

.upload-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px;

  .upload-card {
    border: 1px solid var(--saas-border); border-radius: var(--saas-radius-md);
    padding: 16px; background: var(--saas-bg-page);

    .upload-card-header {
      display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
      .upload-label { font-size: 13px; font-weight: 600; color: var(--saas-text-primary); }
    }

    .saas-upload {
      :deep(.el-upload) { width: 100%; }
      :deep(.el-upload-dragger) {
        border: 1.5px dashed var(--saas-border); border-radius: var(--saas-radius-md);
        background: var(--saas-bg-card); padding: 16px; transition: all var(--saas-transition);
        &:hover { border-color: var(--saas-primary-lighter); background: var(--saas-primary-bg); }
      }
    }

    .upload-inner {
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      .upload-text { font-size: 13px; color: var(--saas-text-secondary); em { color: var(--saas-primary); font-style: normal; font-weight: 500; } }
      .upload-hint { font-size: 11px; color: var(--saas-text-tertiary); }
    }
  }
}

.current-file-info {
  background: var(--saas-bg-card); border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-md); padding: 12px;

  .file-name {
    font-size: 13px; font-weight: 600; color: var(--saas-text-primary);
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  }
  .file-meta { font-size: 11px; color: var(--saas-text-tertiary); margin-top: 4px; }
  .file-actions { margin-top: 8px; display: flex; gap: 4px; }
}

.upload-card--xpt {
  grid-column: 1 / -1;
}

.xpt-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.xpt-table-section {
  margin-top: 12px;
  border-top: 1px dashed var(--saas-border);
  padding-top: 8px;
}

.xpt-table-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  font-size: 12px;
  color: var(--saas-text-secondary);
  cursor: pointer;
  user-select: none;
  border-radius: var(--saas-radius-sm);
  transition: background var(--saas-transition);

  &:hover {
    background: var(--saas-bg-input);
  }

  .toggle-icon {
    font-size: 10px;
    width: 14px;
    text-align: center;
    transition: transform 0.2s;
  }

  .toggle-hint {
    color: var(--saas-text-tertiary);
    font-size: 11px;
  }

  .toggle-action {
    margin-left: auto;
    color: var(--saas-primary);
    font-size: 11px;
    font-weight: 500;
  }
}

.xpt-table-wrapper {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;

  &--expanded {
    max-height: 400px;
    overflow-y: auto;
  }
}

.xpt-table-wrapper:not(.xpt-table-wrapper--expanded) {
  max-height: 220px;
  overflow: hidden;
}

.xpt-compact-table {
  :deep(.el-table__header th) {
    font-size: 11px;
    padding: 4px 0;
    background: var(--saas-bg-input);
  }
  :deep(.el-table__body td) {
    font-size: 12px;
    padding: 3px 0;
  }
  :deep(.el-table__row) {
    height: 32px;
  }
}

.xpt-show-more {
  text-align: center;
  padding: 6px 0;
  font-size: 12px;
  color: var(--saas-primary);
  cursor: pointer;
  border-top: 1px dashed var(--saas-border-light);

  &:hover {
    color: var(--saas-primary-dark);
    text-decoration: underline;
  }
}
</style>
