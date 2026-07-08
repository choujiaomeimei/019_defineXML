<template>
  <div class="acrf-upload">
    <el-card class="upload-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">aCRF注释解析器</span>
          <el-icon class="title-icon"><DocumentCopy /></el-icon>
        </div>
      </template>
      
      <div class="upload-content">
        <!-- PDF上传区域 -->
        <div class="upload-section">
          <div class="section-title">
            <el-icon class="section-icon"><Upload /></el-icon>
            <span>上传aCRF PDF文件</span>
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
            accept=".pdf"
            :limit="1"
            :on-exceed="handleExceed"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将aCRF.pdf文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持上传1个PDF文件，文件大小不超过100MB
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
            
            <el-table-column label="解析状态" width="120">
              <template #default="scope">
                <el-tag v-if="!scope.row.processResult" type="info" size="small">未解析</el-tag>
                <el-tag v-else-if="scope.row.processResult.success" type="success" size="small">解析成功</el-tag>
                <el-tag v-else type="danger" size="small">解析失败</el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="解析结果" min-width="180">
              <template #default="scope">
                <div v-if="scope.row.processResult && scope.row.processResult.success" class="result-file">
                  <el-icon class="result-icon"><DocumentChecked /></el-icon>
                  <span class="result-name">{{ getResultFileName(scope.row.processResult.outputFile || scope.row.outputFilePath) }}</span>
                  <el-button 
                    type="primary" 
                    size="small" 
                    link 
                    @click="downloadResult(scope.row.processResult.outputFile || scope.row.outputFilePath)"
                  >
                    <el-icon><Download /></el-icon>
                    下载
                  </el-button>
                </div>
                <div v-else class="parse-action">
                  <el-button 
                    type="primary" 
                    size="small"
                    :loading="processing && processingFileId === scope.row.fileId"
                    @click="processAnnotations(scope.row)"
                  >
                    <el-icon><MagicStick /></el-icon>
                    解析
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

        <!-- 处理结果展示 -->
        <div v-if="processResult" class="result-section">
          <div class="result-header">
            <el-icon class="result-icon" :class="processResult.success ? 'success' : 'error'">
              <CircleCheck v-if="processResult.success" />
              <CircleClose v-else />
            </el-icon>
            <span class="result-title">处理结果</span>
          </div>
          
          <div v-if="processResult.success" class="result-success">
            <div class="result-info">
              <p><strong>处理状态:</strong> <el-tag type="success">成功</el-tag></p>
              <p><strong>输出文件:</strong> {{ processResult.output_file }}</p>
              <p><strong>处理时间:</strong> {{ formatTime(processResult.processTime) }}</p>
            </div>
            
            <div class="result-actions">
              <el-button type="success" @click="downloadResult">
                <el-icon style="margin-right: 8px;"><Download /></el-icon>
                下载Annots2.xlsx
              </el-button>
              <el-button type="primary" @click="previewResult">
                <el-icon style="margin-right: 8px;"><View /></el-icon>
                预览数据
              </el-button>
            </div>
          </div>
          
          <div v-else class="result-error">
            <el-alert
              title="处理失败"
              :description="processResult.error"
              type="error"
              show-icon
              :closable="false"
            />
          </div>
        </div>

        <!-- 数据预览 -->
        <div v-if="showPreview && previewData" class="preview-section">
          <div class="preview-header">
            <h4>注释数据预览</h4>
            <el-button size="small" @click="closePreview">
              <el-icon><Close /></el-icon>
              关闭预览
            </el-button>
          </div>
          
          <div class="preview-content">
            <el-table :data="previewData" stripe style="width: 100%" max-height="300" size="small">
              <el-table-column prop="page" label="页码" width="80" />
              <el-table-column prop="field" label="字段名" min-width="120" />
              <el-table-column prop="annotation" label="注释内容" min-width="200" />
              <el-table-column prop="type" label="注释类型" width="100" />
              <el-table-column prop="position" label="位置" width="120" />
            </el-table>
          </div>
        </div>

        <!-- 使用说明 -->
        <div class="help-section">
          <el-collapse>
            <el-collapse-item title="使用说明" name="help">
              <div class="help-content">
                <h5>什么是aCRF？</h5>
                <p>aCRF（Annotated Case Report Form）是带有注释的病例报告表，用于临床试验数据收集的标准化表单。</p>
                
                <h5>使用步骤：</h5>
                <ol>
                  <li>上传包含注释的aCRF PDF文件</li>
                  <li>点击"解析PDF注释"按钮开始处理</li>
                  <li>系统将自动提取PDF中的注释信息</li>
                  <li>下载生成的Annots2.xlsx文件</li>
                </ol>
                
                <h5>注意事项：</h5>
                <ul>
                  <li>确保PDF文件包含标准的注释标记</li>
                  <li>文件大小不超过100MB</li>
                  <li>处理时间可能需要几分钟，请耐心等待</li>
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
  DocumentCopy,
  Upload,
  UploadFilled,
  DocumentChecked,
  Delete,
  MagicStick,
  CircleCheck,
  CircleClose,
  Download,
  View,
  Close
} from '@element-plus/icons-vue'

// 响应式数据
const uploadedFiles = ref([])  // 改为文件列表
const processing = ref(false)
const processingFileId = ref(null)  // 正在处理的文件ID
const processResult = ref(null)
const showPreview = ref(false)
const previewData = ref([])
const reUploading = ref(false)
const reUploadingFileId = ref(null)  // 正在重新上传的文件ID
const reUploadInput = ref(null)  // 隐藏的文件输入元素

// 配置
const baseUrl = 'http://localhost:9201'
const uploadUrl = `${baseUrl}/acrf/upload`
const uploadHeaders = {
  'Accept': 'application/json'
}

// 方法
const beforeUpload = (file) => {
  const isPDF = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
  if (!isPDF) {
    ElMessage.error('只能上传PDF格式文件!')
    return false
  }
  
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过 100MB!')
    return false
  }
  
  return true
}

const handleUploadSuccess = (response, file) => {
  console.log('上传成功响应:', response)
  if (response && response.success) {
    // 直接使用服务器返回的完整记录
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
  ElMessage.warning('只能上传1个文件')
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
    const response = await fetch(`${baseUrl}/acrf/delete?fileId=${encodeURIComponent(file.fileId)}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    
    if (result.success) {
      uploadedFiles.value.splice(index, 1)
      // 如果删除的是当前显示结果的文件，清空结果
      if (uploadedFiles.value.length === 0) {
        processResult.value = null
        showPreview.value = false
        previewData.value = []
      }
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

// 生成日期后缀（格式：YYYYMMDD）
const generateDateSuffix = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}${month}${day}`
}

const processAnnotations = async (file) => {
  if (!file) {
    ElMessage.warning('请选择要解析的文件')
    return
  }
  
  processing.value = true
  processingFileId.value = file.fileId
  
  try {
    ElMessage.info('正在解析PDF注释，请稍候...')
    
    const response = await fetch(`${baseUrl}/acrf/process`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        fileId: file.fileId,
        dateSuffix: generateDateSuffix()  // 传递日期后缀
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
      
      // 更新对应文件对象的处理结果
      const fileIndex = uploadedFiles.value.findIndex(f => f.fileId === file.fileId)
      if (fileIndex !== -1) {
        uploadedFiles.value[fileIndex].processResult = processResultData
      }
      processResult.value = processResultData
      
      ElMessage.success('PDF注释解析成功!')
    } else {
      const errorResult = {
        success: false,
        error: result.data?.error || result.message || '处理失败'
      }
      
      const fileIndex = uploadedFiles.value.findIndex(f => f.fileId === file.fileId)
      if (fileIndex !== -1) {
        uploadedFiles.value[fileIndex].processResult = errorResult
      }
      processResult.value = errorResult
      
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
    processResult.value = errorResult
    
    ElMessage.error('处理失败，请稍后重试')
  } finally {
    processing.value = false
    processingFileId.value = null
  }
}

// 获取结果文件名（显示为Annots2_20250821.xlsx格式）
const getResultFileName = (filePath) => {
  if (!filePath) return ''
  // 如果文件路径包含实际文件名，提取出来，否则使用默认名称
  const actualFileName = filePath.split('/').pop() || filePath
  // 如果已经包含日期后缀，直接返回
  if (actualFileName.includes('_') && actualFileName.includes('.xlsx')) {
    return actualFileName
  }
  // 否则添加当前日期后缀
  const dateSuffix = generateDateSuffix()
  return `Annots2_${dateSuffix}.xlsx`
}

// 下载结果文件
const downloadResult = (outputFile) => {
  const fileName = outputFile || (processResult.value && processResult.value.output_file)
  if (fileName) {
    const link = document.createElement('a')
    link.href = `${baseUrl}/acrf/download?file=${encodeURIComponent(fileName)}`
    link.download = getResultFileName(fileName)
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

const previewResult = async () => {
  try {
    // 模拟预览数据
    previewData.value = [
      { page: 1, field: 'SUBJID', annotation: '受试者编号', type: '文本', position: '10,20' },
      { page: 1, field: 'VISITNUM', annotation: '访问次数', type: '数字', position: '10,40' },
      { page: 2, field: 'AEDECOD', annotation: '不良事件编码', type: '文本', position: '15,30' },
      { page: 2, field: 'AESER', annotation: '严重不良事件', type: '选择', position: '15,50' },
      { page: 3, field: 'VSTEST', annotation: '生命体征检查', type: '文本', position: '20,25' }
    ]
    showPreview.value = true
  } catch (error) {
    ElMessage.error('预览失败')
  }
}

const closePreview = () => {
  showPreview.value = false
  previewData.value = null
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

const formatTime = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

// 页面加载时从数据库恢复上传记录
const loadUploadRecords = async () => {
  try {
    const response = await fetch(`${baseUrl}/acrf/records`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    
    if (result.success && Array.isArray(result.data)) {
      uploadedFiles.value = result.data
      console.log('已加载', result.data.length, '条aCRF上传记录')
    } else {
      console.warn('获取上传记录失败:', result.message)
    }
  } catch (error) {
    console.error('加载上传记录错误:', error)
    ElMessage.warning('加载历史记录失败，但不影响正常使用')
  }
}

// 触发重新上传
const triggerReUpload = (file) => {
  // 创建隐藏的文件输入元素
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.pdf'
  input.style.display = 'none'
  
  input.onchange = async (event) => {
    const selectedFile = event.target.files[0]
    if (selectedFile) {
      await handleReUpload(file, selectedFile)
    }
    // 移除临时创建的input元素
    document.body.removeChild(input)
  }
  
  // 添加到DOM并触发点击
  document.body.appendChild(input)
  input.click()
}

// 处理重新上传
const handleReUpload = async (originalFile, newFile) => {
  // 验证新文件
  if (!beforeUpload(newFile)) {
    return
  }
  
  reUploading.value = true
  reUploadingFileId.value = originalFile.fileId
  
  try {
    const formData = new FormData()
    formData.append('file', newFile)
    formData.append('fileId', originalFile.fileId)  // 传递原文件ID用于替换
    
    const response = await fetch(`${baseUrl}/acrf/reupload`, {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: formData
    })
    
    const result = await response.json()
    
    if (result.success) {
      // 更新对应的文件记录
      const fileIndex = uploadedFiles.value.findIndex(f => f.fileId === originalFile.fileId)
      if (fileIndex !== -1) {
        // 保持原有的fileId和originalName，但更新其他信息，清除处理结果
        const updatedFile = {
          ...result.data,
          fileId: originalFile.fileId,
          originalName: originalFile.originalName,
          processResult: null  // 清除之前的处理结果
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

// 组件挂载时加载历史记录
onMounted(() => {
  loadUploadRecords()
})
</script>

<style scoped lang="less">
.acrf-upload {
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
            flex: 1;
            word-break: break-all;
          }
        }
        
        .no-result {
          color: #C0C4CC;
          font-style: italic;
        }
        
        .parse-action {
          display: flex;
          align-items: center;
          justify-content: center;
        }
      }
    }
    
    .result-section {
      margin-bottom: 20px;
      padding: 16px;
      border-radius: 6px;
      border: 1px solid #EBEEF5;
      
      .result-header {
        display: flex;
        align-items: center;
        margin-bottom: 16px;
        
        .result-icon {
          margin-right: 8px;
          font-size: 18px;
          
          &.success {
            color: #67C23A;
          }
          
          &.error {
            color: #F56C6C;
          }
        }
        
        .result-title {
          font-weight: 500;
          color: #303133;
        }
      }
      
      .result-success {
        .result-info {
          margin-bottom: 20px;
          
          p {
            margin: 8px 0;
            color: #606266;
            
            strong {
              color: #303133;
            }
          }
        }
        
        .result-actions {
          display: flex;
          gap: 12px;
        }
      }
    }
    
    .preview-section {
      margin-bottom: 30px;
      
      .preview-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;
        padding-bottom: 12px;
        border-bottom: 1px solid #EBEEF5;
        
        h4 {
          margin: 0;
          color: #303133;
        }
      }
      
      .preview-content {
        background: #FAFBFC;
        border-radius: 8px;
        padding: 16px;
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