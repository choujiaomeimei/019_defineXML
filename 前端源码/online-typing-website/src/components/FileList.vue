<template>
  <div class="file-list">
    <div v-if="files.length === 0" class="empty-state">
      <el-icon class="empty-icon"><Document /></el-icon>
      <p class="empty-text">暂无文件</p>
    </div>

    <div v-else class="file-items">
      <div v-for="(file, index) in files" :key="file.fileId" class="file-item">
        <div class="file-info">
          <div class="file-icon-wrapper">
            <el-icon class="file-icon">
              <Document v-if="type === 'acrf'" />
              <Files v-else />
            </el-icon>
          </div>

          <div class="file-details">
            <div class="file-name">{{ file.originalName }}</div>
            <div class="file-meta">
              <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
              <span class="file-time">{{ formatUploadTime(file.uploadTime) }}</span>
            </div>
          </div>

          <div class="file-status">
            <el-tag v-if="!file.processResult" type="info" size="small">未处理</el-tag>
            <el-tag v-else-if="file.processResult.success" type="success" size="small">处理完成</el-tag>
            <el-tag v-else type="danger" size="small">处理失败</el-tag>
          </div>

          <div class="file-actions">
            <el-button
              v-if="!file.processResult || !file.processResult.success"
              type="primary"
              size="small"
              :loading="processing && processingFileId === file.fileId"
              @click="handleProcess(file)"
            >
              <el-icon><MagicStick /></el-icon>
              处理
            </el-button>

            <el-button
              v-if="file.processResult && file.processResult.success && type === 'acrf'"
              type="success"
              size="small"
              @click="downloadResult(file)"
            >
              <el-icon><Download /></el-icon>
              下载
            </el-button>

            <el-dropdown trigger="click" @command="handleCommand">
              <el-button size="small" type="text">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="{ action: 'reupload', file }">
                    <el-icon><Upload /></el-icon>
                    重新上传
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'delete', file, index }" divided>
                    <el-icon><Delete /></el-icon>
                    删除文件
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <!-- 处理结果展示 -->
        <div v-if="file.processResult && !file.processResult.success" class="error-info">
          <el-alert
            :title="file.processResult.error || '处理失败'"
            type="error"
            size="small"
            show-icon
            :closable="false"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Files,
  MagicStick,
  Download,
  Upload,
  Delete,
  MoreFilled
} from '@element-plus/icons-vue'

// Props
const props = defineProps({
  files: {
    type: Array,
    default: () => []
  },
  type: {
    type: String,
    required: true,
    validator: (value) => ['acrf', 'p21spec', 'projectspec', 'xpt', 'send-p21spec', 'send-projectspec', 'send-xpt', 'adam-p21spec', 'adam-projectspec', 'adam-xpt'].includes(value)
  }
})

// Emits
const emit = defineEmits(['process', 'delete', 'reupload', 'process-complete', 'file-deleted'])

// 响应式数据
const processing = ref(false)
const processingFileId = ref(null)

// 基础URL配置
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''

// 方法
const handleProcess = async (file) => {
  processing.value = true
  processingFileId.value = file.fileId

  try {
    emit('process', file, props.type)

    let processUrl = ''
    let message = ''

    switch (props.type) {
      case 'acrf':
        processUrl = `${baseUrl}/acrf/process`
        message = '正在解析PDF注释，请稍候...'
        break
      case 'p21spec':
        processUrl = `${baseUrl}/p21-spec/process`
        message = '正在处理P21空SPEC文件，请稍候...'
        break
      case 'projectspec':
        processUrl = `${baseUrl}/project-spec/process`
        message = '正在处理项目SPEC文件，请稍候...'
        break
      case 'xpt':
        processUrl = `${baseUrl}/xpt/process`
        message = '正在处理XPT文件，请稍候...'
        break
    }

    ElMessage.info(message)

    const response = await fetch(processUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        fileId: file.fileId,
        dateSuffix: props.type === 'acrf' ? generateDateSuffix() : undefined
      })
    })

    const result = await response.json()

    if (result.success && result.data) {
      const processResultData = {
        success: result.data.success,
        message: result.data.message,
        output_file: result.data.output_file,
        processTime: new Date(result.data.processTime)
      }

      emit('process-complete', { fileId: file.fileId, processResult: processResultData })

      ElMessage.success('文件处理成功!')
    } else {
      const errorResult = {
        success: false,
        error: result.data?.error || result.message || '处理失败'
      }

      emit('process-complete', { fileId: file.fileId, processResult: errorResult })

      ElMessage.error('处理失败: ' + (result.data?.error || result.message || '未知错误'))
    }
  } catch (error) {
    console.error('处理错误:', error)
    const errorResult = {
      success: false,
      error: '网络请求失败，请检查后端服务是否正常运行'
    }

    emit('process-complete', { fileId: file.fileId, processResult: errorResult })

    ElMessage.error('处理失败，请稍后重试')
  } finally {
    processing.value = false
    processingFileId.value = null
  }
}

const handleCommand = (command) => {
  const { action, file, index } = command

  switch (action) {
    case 'reupload':
      emit('reupload', file, props.type)
      break
    case 'delete':
      handleDelete(file, index)
      break
  }
}

const handleDelete = async (file, index) => {
  try {
    await ElMessageBox.confirm(
      `确认删除文件 "${file.originalName}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    let deleteUrl = ''
    switch (props.type) {
      case 'acrf':
        deleteUrl = `${baseUrl}/acrf/delete?fileId=${encodeURIComponent(file.fileId)}`
        break
      case 'p21spec':
        deleteUrl = `${baseUrl}/p21-spec/delete?fileId=${encodeURIComponent(file.fileId)}`
        break
      case 'projectspec':
        deleteUrl = `${baseUrl}/project-spec/delete?fileId=${encodeURIComponent(file.fileId)}`
        break
      case 'xpt':
        deleteUrl = `${baseUrl}/xpt/delete?fileId=${encodeURIComponent(file.fileId)}`
        break
    }

    const response = await fetch(deleteUrl, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    const result = await response.json()

    if (result.success) {
      emit('file-deleted', { fileId: file.fileId, index })
      ElMessage.success('文件已删除')
      emit('delete', file, props.type)
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

const downloadResult = (file) => {
  const outputFile = file.processResult?.output_file
  if (outputFile) {
    const link = document.createElement('a')
    link.href = `${baseUrl}/acrf/download?file=${encodeURIComponent(outputFile)}`
    link.download = getResultFileName(outputFile)
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

// 工具方法
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatUploadTime = (uploadTime) => {
  if (!uploadTime) return '-'

  const time = new Date(uploadTime)
  if (isNaN(time.getTime())) {
    return '-'
  }

  const month = String(time.getMonth() + 1).padStart(2, '0')
  const day = String(time.getDate()).padStart(2, '0')
  const hours = String(time.getHours()).padStart(2, '0')
  const minutes = String(time.getMinutes()).padStart(2, '0')

  return `${month}-${day} ${hours}:${minutes}`
}

const generateDateSuffix = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}${month}${day}`
}

const getResultFileName = (filePath) => {
  if (!filePath) return ''
  const actualFileName = filePath.split('/').pop() || filePath
  if (actualFileName.includes('_') && actualFileName.includes('.xlsx')) {
    return actualFileName
  }
  const dateSuffix = generateDateSuffix()
  return `Annots2_${dateSuffix}.xlsx`
}
</script>

<style scoped lang="less">
.file-list {
  .empty-state {
    text-align: center;
    padding: 40px 20px;
    color: #909399;

    .empty-icon {
      font-size: 48px;
      margin-bottom: 16px;
    }

    .empty-text {
      margin: 0;
      font-size: 14px;
    }
  }

  .file-items {
    .file-item {
      border: 1px solid #EBEEF5;
      border-radius: 6px;
      margin-bottom: 12px;
      overflow: hidden;
      transition: all 0.3s;

      &:last-child {
        margin-bottom: 0;
      }

      &:hover {
        border-color: #C6E2FF;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
      }

      .file-info {
        display: flex;
        align-items: center;
        padding: 12px 16px;
        background: #fff;

        .file-icon-wrapper {
          margin-right: 12px;

          .file-icon {
            font-size: 24px;
            color: #409EFF;
          }
        }

        .file-details {
          flex: 1;
          min-width: 0;

          .file-name {
            font-weight: 500;
            color: #303133;
            font-size: 14px;
            margin-bottom: 4px;
            word-break: break-all;
          }

          .file-meta {
            display: flex;
            gap: 12px;
            font-size: 12px;
            color: #909399;

            .file-size {
              &:after {
                content: '|';
                margin-left: 12px;
                color: #E4E7ED;
              }
            }
          }
        }

        .file-status {
          margin-right: 12px;
        }

        .file-actions {
          display: flex;
          align-items: center;
          gap: 8px;

          .el-button {
            padding: 5px 8px;
          }
        }
      }

      .error-info {
        padding: 8px 16px;
        background: #FEF0F0;
        border-top: 1px solid #EBEEF5;

        :deep(.el-alert) {
          padding: 6px 12px;

          .el-alert__content {
            padding: 0;
          }

          .el-alert__title {
            font-size: 12px;
            line-height: 1.4;
          }
        }
      }
    }
  }
}
</style>