<template>
  <div class="project-spec-upload">
    <el-card class="upload-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">项目SPEC上传器</span>
          <el-icon class="title-icon"><Files /></el-icon>
        </div>
      </template>
      
      <div class="upload-content">
        <!-- 项目SPEC上传区域 -->
        <div class="upload-section">
          <div class="section-title">
            <el-icon class="section-icon"><Upload /></el-icon>
            <span>上传项目SPEC文件</span>
          </div>
          
          <el-upload
            ref="upload"
            class="upload-demo"
            drag
            :action="uploadUrl"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :headers="uploadHeaders"
            name="file"
            accept=".xlsx,.xls"
            :limit="10"
            :on-exceed="handleExceed"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将项目SPEC文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持上传Excel格式文件(.xlsx, .xls)，每个文件大小不超过50MB，最多10个文件
              </div>
            </template>
          </el-upload>
        </div>

        <!-- 文件列表 -->
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
            
            <el-table-column label="上传时间" width="140">
              <template #default="scope">
                <span class="upload-time">{{ formatUploadTime(scope.row.uploadTime) }}</span>
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
                    @click="processProjectSpec(scope.row)"
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

        <!-- 使用说明 -->
        <div class="help-section">
          <el-collapse>
            <el-collapse-item title="使用说明" name="help">
              <div class="help-content">
                <h5>什么是项目SPEC？</h5>
                <p>项目SPEC是项目规格说明文档，包含数据结构定义、变量映射和业务规则等信息。</p>
                
                <h5>使用步骤：</h5>
                <ol>
                  <li>上传Excel格式的项目SPEC文件</li>
                  <li>点击"处理"按钮开始解析SPEC</li>
                  <li>系统将自动解析文件中的规格信息</li>
                  <li>查看处理结果</li>
                </ol>
                
                <h5>注意事项：</h5>
                <ul>
                  <li>确保文件是Excel格式(.xlsx或.xls)</li>
                  <li>文件大小不超过50MB</li>
                  <li>文件内容应包含标准的SPEC结构</li>
                </ul>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Files,
  Upload,
  UploadFilled,
  DocumentChecked,
  Delete,
  MagicStick,
  Document
} from '@element-plus/icons-vue'

// 响应式数据
const uploadedFiles = ref([])
const processing = ref(false)
const processingFileId = ref(null)
const reUploading = ref(false)
const reUploadingFileId = ref(null)

// 配置
const baseUrl = 'http://localhost:9201'
const uploadUrl = `${baseUrl}/project-spec/upload`
const uploadHeaders = {
  'Accept': 'application/json'
}

// 方法
const beforeUpload = (file) => {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
                  file.type === 'application/vnd.ms-excel' ||
                  file.name.toLowerCase().endsWith('.xlsx') ||
                  file.name.toLowerCase().endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('只能上传Excel格式文件!')
    return false
  }
  
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过 50MB!')
    return false
  }
  
  return true
}

const handleUploadSuccess = (response, file) => {
  console.log('上传成功响应:', response)
  if (response && response.success) {
    uploadedFiles.value.push(response.data)
    ElMessage.success(`${file.name} 上传成功!`)
  } else {
    ElMessage.error(`${file.name} 上传失败: ${response?.message || '未知错误'}`)
  }
}

const handleUploadError = (error, file) => {
  console.error('上传失败:', error)
  ElMessage.error(`${file.name} 上传失败!`)
}

const handleExceed = () => {
  ElMessage.warning('最多只能上传10个文件')
}

const removeFile = async (index) => {
  const file = uploadedFiles.value[index]
  const fileName = file.originalName
  
  try {
    await ElMessageBox.confirm(
      `确认删除文件 "${fileName}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    // 调用后端删除API
    const response = await fetch(`${baseUrl}/project-spec/delete?fileId=${encodeURIComponent(file.fileId)}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    
    if (result.success) {
      uploadedFiles.value.splice(index, 1)
      ElMessage.success('文件已删除')
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

// 触发重新上传
const triggerReUpload = (file) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.xlsx,.xls'
  input.style.display = 'none'
  
  input.onchange = async (event) => {
    const selectedFile = event.target.files[0]
    if (selectedFile) {
      await handleReUpload(file, selectedFile)
    }
    document.body.removeChild(input)
  }
  
  document.body.appendChild(input)
  input.click()
}

// 处理重新上传
const handleReUpload = async (originalFile, newFile) => {
  if (!beforeUpload(newFile)) {
    return
  }
  
  reUploading.value = true
  reUploadingFileId.value = originalFile.fileId
  
  try {
    const formData = new FormData()
    formData.append('file', newFile)
    formData.append('fileId', originalFile.fileId)
    
    const response = await fetch(`${baseUrl}/project-spec/reupload`, {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: formData
    })
    
    const result = await response.json()
    
    if (result.success) {
      const fileIndex = uploadedFiles.value.findIndex(f => f.fileId === originalFile.fileId)
      if (fileIndex !== -1) {
        const updatedFile = {
          ...result.data,
          fileId: originalFile.fileId,
          originalName: originalFile.originalName,
          processResult: null
        }
        uploadedFiles.value[fileIndex] = updatedFile
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

const processProjectSpec = async (file) => {
  if (!file) {
    ElMessage.warning('请选择要处理的文件')
    return
  }
  
  processing.value = true
  processingFileId.value = file.fileId
  
  try {
    ElMessage.info('正在处理项目SPEC文件，请稍候...')
    
    const response = await fetch(`${baseUrl}/project-spec/process`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        fileId: file.fileId
      })
    })
    
    const result = await response.json()
    
    if (result.success && result.data) {
      const processResultData = {
        success: result.data.success,
        message: result.data.message,
        processTime: new Date(result.data.processTime)
      }
      
      const fileIndex = uploadedFiles.value.findIndex(f => f.fileId === file.fileId)
      if (fileIndex !== -1) {
        uploadedFiles.value[fileIndex].processResult = processResultData
      }
      
      ElMessage.success('项目SPEC文件处理成功!')
    } else {
      const errorResult = {
        success: false,
        error: result.data?.error || result.message || '处理失败'
      }
      
      const fileIndex = uploadedFiles.value.findIndex(f => f.fileId === file.fileId)
      if (fileIndex !== -1) {
        uploadedFiles.value[fileIndex].processResult = errorResult
      }
      
      ElMessage.error('处理失败: ' + (result.data?.error || result.message || '未知错误'))
    }
  } catch (error) {
    console.error('处理错误:', error)
    const errorResult = {
      success: false,
      error: '网络请求失败，请检查后端服务是否正常运行'
    }
    
    const fileIndex = uploadedFiles.value.findIndex(f => f.fileId === file.fileId)
    if (fileIndex !== -1) {
      uploadedFiles.value[fileIndex].processResult = errorResult
    }
    
    ElMessage.error('处理失败，请稍后重试')
  } finally {
    processing.value = false
    processingFileId.value = null
  }
}

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatUploadTime = (uploadTime) => {
  if (!uploadTime) return ''
  const time = new Date(uploadTime)
  const month = String(time.getMonth() + 1).padStart(2, '0')
  const day = String(time.getDate()).padStart(2, '0')
  const hours = String(time.getHours()).padStart(2, '0')
  const minutes = String(time.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

// 页面加载时从数据库恢复上传记录
const loadUploadRecords = async () => {
  try {
    const response = await fetch(`${baseUrl}/project-spec/records`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    
    if (result.success && Array.isArray(result.data)) {
      uploadedFiles.value = result.data
      console.log('已加载', result.data.length, '条项目SPEC上传记录')
    } else {
      console.warn('获取上传记录失败:', result.message)
    }
  } catch (error) {
    console.error('加载上传记录错误:', error)
    ElMessage.warning('加载历史记录失败，但不影响正常使用')
  }
}

// 组件挂载时加载历史记录
onMounted(() => {
  loadUploadRecords()
})
</script>

<style scoped lang="less">
.project-spec-upload {
  padding: 16px;
  
  .upload-card {
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      
      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
      
      .title-icon {
        color: #409EFF;
        font-size: 20px;
      }
    }
  }
  
  .upload-content {
    .section-title {
      display: flex;
      align-items: center;
      margin-bottom: 12px;
      font-size: 14px;
      font-weight: 500;
      color: #303133;
      
      .section-icon {
        color: #409EFF;
        margin-right: 8px;
      }
    }
    
    .upload-section {
      margin-bottom: 20px;
      
      .upload-demo {
        .el-upload {
          width: 100%;
        }
        
        .el-upload-dragger {
          height: 60px !important;
          
          .el-icon--upload {
            font-size: 24px !important;
            margin-bottom: 6px !important;
          }
          
          .el-upload__text {
            font-size: 14px !important;
            margin-bottom: 8px !important;
          }
        }
      }
    }
    
    .file-list-section {
      margin-bottom: 20px;
      
      .list-header {
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        
        .list-icon {
          color: #67C23A;
          margin-right: 8px;
        }
        
        .list-title {
          font-weight: 500;
          color: #303133;
          font-size: 14px;
        }
      }
      
      .file-table {
        .file-name-cell {
          display: flex;
          align-items: center;
          
          .file-icon {
            color: #409EFF;
            margin-right: 8px;
            flex-shrink: 0;
          }
          
          .file-name {
            font-weight: 500;
            color: #303133;
            word-break: break-all;
          }
        }
        
        .file-size {
          color: #606266;
          font-size: 13px;
        }
        
        .result-file {
          display: flex;
          align-items: center;
          gap: 8px;
          
          .result-icon {
            color: #67C23A;
            flex-shrink: 0;
          }
          
          .result-name {
            font-weight: 500;
            color: #303133;
            font-size: 13px;
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
      margin-top: 40px;
      
      .help-content {
        h5 {
          margin: 16px 0 8px 0;
          color: #303133;
          font-size: 14px;
        }
        
        p, li {
          margin: 8px 0;
          color: #606266;
          font-size: 14px;
          line-height: 1.6;
        }
        
        ol, ul {
          padding-left: 20px;
        }
      }
    }
  }
}
</style>