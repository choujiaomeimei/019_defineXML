<template>
  <div class="file-uploader">
    <el-card class="upload-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">{{ title }}</span>
          <el-icon class="title-icon"><component :is="headerIcon" /></el-icon>
        </div>
      </template>

      <div class="upload-content">
        <!-- Upload area -->
        <div class="upload-section">
          <div class="section-title">
            <el-icon class="section-icon"><Upload /></el-icon>
            <span>{{ uploadLabel }}</span>
          </div>

          <el-upload
            ref="uploadRef"
            class="upload-demo"
            drag
            :multiple="limit > 1"
            :action="uploadUrl"
            :before-upload="handleBeforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :headers="uploadHeaders"
            name="file"
            :accept="accept"
            :limit="limit"
            :on-exceed="handleExceed"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                <slot name="tip">
                  {{ defaultTipText }}
                </slot>
              </div>
            </template>
          </el-upload>
        </div>

        <!-- File list -->
        <div v-if="uploadedFiles.length > 0" class="file-list-section">
          <div class="list-header">
            <el-icon class="list-icon"><DocumentChecked /></el-icon>
            <span class="list-title">已上传文件 ({{ uploadedFiles.length }})</span>
          </div>

          <el-table :data="uploadedFiles" stripe border size="small" class="file-table">
            <el-table-column label="重新上传" width="100" align="center">
              <template #default="scope">
                <el-button
                  type="warning"
                  size="small"
                  :loading="reUploading && reUploadingFileId === scope.row.fileId"
                  @click="triggerReUpload(scope.row)"
                >
                  <el-icon><Upload /></el-icon>
                </el-button>
              </template>
            </el-table-column>

            <el-table-column label="文件名" min-width="200">
              <template #default="scope">
                <div class="file-name-cell">
                  <el-icon class="file-icon"><Document /></el-icon>
                  <span class="file-name">{{ scope.row.originalName }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="文件大小" width="120">
              <template #default="scope">
                <span class="file-size">{{ formatFileSize(scope.row.fileSize) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="上传时间" width="180">
              <template #default="scope">
                <span class="upload-time">{{ formatDateTime(scope.row.uploadTime) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="处理状态" width="120">
              <template #default="scope">
                <el-tag v-if="!scope.row.processResult" type="info" size="small">未处理</el-tag>
                <el-tag v-else-if="scope.row.processResult.success" type="success" size="small">处理成功</el-tag>
                <el-tag v-else type="danger" size="small">处理失败</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="处理结果" min-width="180">
              <template #default="scope">
                <div v-if="scope.row.processResult && scope.row.processResult.success" class="result-file">
                  <el-icon class="result-icon"><DocumentChecked /></el-icon>
                  <span class="result-name">处理完成</span>
                </div>
                <div v-else class="parse-action">
                  <el-button
                    type="primary"
                    size="small"
                    :loading="processing && processingFileId === scope.row.fileId"
                    @click="processFile(scope.row)"
                  >
                    <el-icon><MagicStick /></el-icon>
                    处理
                  </el-button>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="80" fixed="right">
              <template #default="scope">
                <el-button
                  type="danger"
                  size="small"
                  @click="removeFile(scope.$index)"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- Help section (slot-based so each consumer can provide its own content) -->
        <div v-if="$slots.help" class="help-section">
          <el-collapse>
            <el-collapse-item title="使用说明" name="help">
              <div class="help-content">
                <slot name="help" />
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Files,
  Folder,
  Upload,
  UploadFilled,
  Document,
  DocumentChecked,
  Delete,
  MagicStick
} from '@element-plus/icons-vue'
import { formatFileSize, formatDateTime } from '@/utils/format'

const props = defineProps({
  title: { type: String, required: true },
  accept: { type: String, required: true },
  limit: { type: Number, default: 1 },
  apiPrefix: { type: String, required: true },
  projectId: { type: String, default: '' },
  fileValidator: { type: Function, default: null },
  uploadLabel: { type: String, default: '上传文件' },
  maxSizeMB: { type: Number, default: 50 },
  headerIcon: { type: [String, Object], default: () => Files }
})

const emit = defineEmits(['upload-success', 'upload-error', 'file-removed', 'process-success', 'process-error'])

const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''

const uploadRef = ref(null)
const uploadedFiles = ref([])
const processing = ref(false)
const processingFileId = ref(null)
const reUploading = ref(false)
const reUploadingFileId = ref(null)

const normalizedPrefix = computed(() => {
  let p = props.apiPrefix
  if (!p.startsWith('/')) p = '/' + p
  if (!p.endsWith('/')) p = p + '/'
  return p
})

const uploadUrl = computed(() => `${baseUrl}${normalizedPrefix.value}upload`)

const uploadHeaders = { 'Accept': 'application/json' }

const defaultTipText = computed(() => {
  const extensions = props.accept.split(',').map(e => e.trim()).join(', ')
  const limitText = props.limit > 1 ? `，最多${props.limit}个文件` : ''
  return `支持上传${extensions}格式文件，文件大小不超过${props.maxSizeMB}MB${limitText}`
})

const defaultValidator = (file) => {
  const name = file.name.toLowerCase()
  const acceptList = props.accept.split(',').map(a => a.trim().toLowerCase())
  const matchesExtension = acceptList.some(ext => name.endsWith(ext))
  if (!matchesExtension) {
    ElMessage.error(`只能上传 ${props.accept} 格式文件!`)
    return false
  }
  const isUnderLimit = file.size / 1024 / 1024 < props.maxSizeMB
  if (!isUnderLimit) {
    ElMessage.error(`文件大小不能超过 ${props.maxSizeMB}MB!`)
    return false
  }
  return true
}

const handleBeforeUpload = (file) => {
  if (props.fileValidator) return props.fileValidator(file)
  return defaultValidator(file)
}

const handleUploadSuccess = (response, file) => {
  if (response && response.success) {
    uploadedFiles.value.push(response.data)
    ElMessage.success(`${file.name} 上传成功!`)
    emit('upload-success', response.data, file)
  } else {
    ElMessage.error(`${file.name} 上传失败: ${response?.message || '未知错误'}`)
    emit('upload-error', response, file)
  }
}

const handleUploadError = (error, file) => {
  console.error('上传失败:', error)
  ElMessage.error(`${file.name} 上传失败!`)
  emit('upload-error', error, file)
}

const handleExceed = () => {
  ElMessage.warning(`最多只能上传${props.limit}个文件`)
}

const removeFile = async (index) => {
  const file = uploadedFiles.value[index]
  try {
    await ElMessageBox.confirm(
      `确认删除文件 "${file.originalName}" 吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )

    const response = await fetch(
      `${baseUrl}${normalizedPrefix.value}delete?fileId=${encodeURIComponent(file.fileId)}`,
      { method: 'DELETE', headers: { 'Content-Type': 'application/json' } }
    )
    const result = await response.json()

    if (result.success) {
      uploadedFiles.value.splice(index, 1)
      ElMessage.success('文件已删除')
      emit('file-removed', file)
    } else {
      ElMessage.error('删除失败: ' + (result.message || '未知错误'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文件错误:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

const triggerReUpload = (file) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = props.accept
  input.style.display = 'none'

  input.onchange = async (event) => {
    const selectedFile = event.target.files[0]
    if (selectedFile) await handleReUpload(file, selectedFile)
    document.body.removeChild(input)
  }

  document.body.appendChild(input)
  input.click()
}

const handleReUpload = async (originalFile, newFile) => {
  if (!handleBeforeUpload(newFile)) return

  reUploading.value = true
  reUploadingFileId.value = originalFile.fileId

  try {
    const formData = new FormData()
    formData.append('file', newFile)
    formData.append('fileId', originalFile.fileId)

    const response = await fetch(`${baseUrl}${normalizedPrefix.value}reupload`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: formData
    })
    const result = await response.json()

    if (result.success) {
      const idx = uploadedFiles.value.findIndex(f => f.fileId === originalFile.fileId)
      if (idx !== -1) {
        uploadedFiles.value[idx] = {
          ...result.data,
          fileId: originalFile.fileId,
          originalName: originalFile.originalName,
          processResult: null
        }
      }
      ElMessage.success(`${originalFile.originalName} 重新上传成功!`)
    } else {
      ElMessage.error(`重新上传失败: ${result.message || '未知错误'}`)
    }
  } catch (error) {
    console.error('重新上传错误:', error)
    ElMessage.error('重新上传失败，请稍后重试')
  } finally {
    reUploading.value = false
    reUploadingFileId.value = null
  }
}

const processFile = async (file) => {
  if (!file) {
    ElMessage.warning('请选择要处理的文件')
    return
  }

  processing.value = true
  processingFileId.value = file.fileId

  try {
    ElMessage.info('正在处理文件，请稍候...')

    const response = await fetch(`${baseUrl}${normalizedPrefix.value}process`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fileId: file.fileId })
    })
    const result = await response.json()

    const idx = uploadedFiles.value.findIndex(f => f.fileId === file.fileId)

    if (result.success && result.data) {
      const processResultData = {
        success: result.data.success,
        message: result.data.message,
        processTime: new Date(result.data.processTime)
      }
      if (idx !== -1) uploadedFiles.value[idx].processResult = processResultData
      ElMessage.success('文件处理成功!')
      emit('process-success', processResultData, file)
    } else {
      const errorResult = {
        success: false,
        error: result.data?.error || result.message || '处理失败'
      }
      if (idx !== -1) uploadedFiles.value[idx].processResult = errorResult
      ElMessage.error('处理失败: ' + (result.data?.error || result.message || '未知错误'))
      emit('process-error', errorResult, file)
    }
  } catch (error) {
    console.error('处理错误:', error)
    const errorResult = {
      success: false,
      error: '网络请求失败，请检查后端服务是否正常运行'
    }
    const idx = uploadedFiles.value.findIndex(f => f.fileId === file.fileId)
    if (idx !== -1) uploadedFiles.value[idx].processResult = errorResult
    ElMessage.error('处理失败，请稍后重试')
    emit('process-error', errorResult, file)
  } finally {
    processing.value = false
    processingFileId.value = null
  }
}

const loadUploadRecords = async () => {
  try {
    const response = await fetch(`${baseUrl}${normalizedPrefix.value}records`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    })
    const result = await response.json()

    if (result.success && Array.isArray(result.data)) {
      uploadedFiles.value = result.data
    } else {
      console.warn('获取上传记录失败:', result.message)
    }
  } catch (error) {
    console.error('加载上传记录错误:', error)
    ElMessage.warning('加载历史记录失败，但不影响正常使用')
  }
}

const getFiles = () => uploadedFiles.value
const clearFiles = () => { uploadedFiles.value = [] }

defineExpose({ getFiles, clearFiles, loadUploadRecords })

onMounted(() => {
  loadUploadRecords()
})
</script>

<style scoped lang="less">
.file-uploader {
  padding: var(--saas-space-4, 16px);

  .upload-card {
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .card-title {
        font-size: var(--saas-text-base, 16px);
        font-weight: var(--saas-font-semibold, 600);
        color: var(--saas-text-primary, #303133);
      }

      .title-icon {
        color: var(--saas-primary, #409EFF);
        font-size: 20px;
      }
    }
  }

  .upload-content {
    .section-title {
      display: flex;
      align-items: center;
      margin-bottom: var(--saas-space-3, 12px);
      font-size: var(--saas-text-sm, 14px);
      font-weight: 500;
      color: var(--saas-text-primary, #303133);

      .section-icon {
        color: var(--saas-primary, #409EFF);
        margin-right: var(--saas-space-2, 8px);
      }
    }

    .upload-section {
      margin-bottom: var(--saas-space-5, 20px);

      .upload-demo {
        :deep(.el-upload) {
          width: 100%;
        }

        :deep(.el-upload-dragger) {
          .el-icon--upload {
            font-size: 24px !important;
            margin-bottom: 6px !important;
          }

          .el-upload__text {
            font-size: var(--saas-text-sm, 14px) !important;
            margin-bottom: var(--saas-space-2, 8px) !important;
          }
        }
      }
    }

    .file-list-section {
      margin-bottom: var(--saas-space-5, 20px);

      .list-header {
        display: flex;
        align-items: center;
        margin-bottom: var(--saas-space-3, 12px);

        .list-icon {
          color: var(--saas-success, #67C23A);
          margin-right: var(--saas-space-2, 8px);
        }

        .list-title {
          font-weight: 500;
          color: var(--saas-text-primary, #303133);
          font-size: var(--saas-text-sm, 14px);
        }
      }

      .file-table {
        .file-name-cell {
          display: flex;
          align-items: center;

          .file-icon {
            color: var(--saas-primary, #409EFF);
            margin-right: var(--saas-space-2, 8px);
            flex-shrink: 0;
          }

          .file-name {
            font-weight: 500;
            color: var(--saas-text-primary, #303133);
            word-break: break-all;
          }
        }

        .file-size {
          color: var(--saas-text-secondary, #606266);
          font-size: var(--saas-text-xs, 13px);
        }

        .upload-time {
          color: var(--saas-text-secondary, #606266);
          font-size: var(--saas-text-xs, 13px);
        }

        .result-file {
          display: flex;
          align-items: center;
          gap: var(--saas-space-2, 8px);

          .result-icon {
            color: var(--saas-success, #67C23A);
            flex-shrink: 0;
          }

          .result-name {
            font-weight: 500;
            color: var(--saas-text-primary, #303133);
            font-size: var(--saas-text-xs, 13px);
          }
        }

        .parse-action {
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }
    }

    .help-section {
      margin-top: var(--saas-space-10, 40px);

      .help-content {
        :deep(h5) {
          margin: var(--saas-space-4, 16px) 0 var(--saas-space-2, 8px) 0;
          color: var(--saas-text-primary, #303133);
          font-size: var(--saas-text-sm, 14px);
        }

        :deep(p),
        :deep(li) {
          margin: var(--saas-space-2, 8px) 0;
          color: var(--saas-text-secondary, #606266);
          font-size: var(--saas-text-sm, 14px);
          line-height: 1.6;
        }

        :deep(ol),
        :deep(ul) {
          padding-left: 20px;
        }
      }
    }
  }
}
</style>
