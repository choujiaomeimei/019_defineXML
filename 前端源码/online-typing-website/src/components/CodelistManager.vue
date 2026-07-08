<template>
  <div class="codelist-manager">
    <!-- 头部 -->
    <div class="codelist-header">
      <div class="header-left">
        <h3>项目CodeList管理</h3>
        <p>基于VCD的代码列表和受控术语管理</p>
      </div>
      <div class="header-actions">
        <el-button type="warning" @click="extractCodelistData" :loading="extractLoading">
          <el-icon><Download /></el-icon>
          提取CodeList数据
        </el-button>
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- VCD导航侧边栏 -->
      <div class="vcd-sidebar">
        <div class="sidebar-header">
          <h4>变量代码 (VCD)</h4>
          <el-input 
            v-model="vcdSearchText" 
            placeholder="搜索VCD..." 
            size="small"
            clearable
            @input="filterVcds"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="vcd-list" v-loading="vcdLoading">
          <div 
            v-for="vcdItem in filteredVcdList" 
            :key="vcdItem.vcd"
            class="vcd-item"
            :class="{ 'active': selectedVcd === vcdItem.vcd }"
            @click="selectVcd(vcdItem.vcd, vcdItem.vlabel)"
          >
            <div class="vcd-name">
              {{ vcdItem.vcd }}
              <span v-if="vcdItem.domains && vcdItem.domains.length > 0" class="vcd-domains">
                [{{ vcdItem.domains.join(', ') }}]
              </span>
            </div>
            <div class="vcd-label">{{ vcdItem.vlabel }}</div>
            <div class="vcd-count">{{ vcdItem.count }} 项</div>
          </div>
        </div>
      </div>

      <!-- 主内容区域 -->
      <div class="content-area">
        <!-- 选中VCD的详细信息和操作 -->
        <div class="content-header" v-if="selectedVcd">
          <div class="selected-vcd-info">
            <h3>{{ selectedVcd }}</h3>
            <p class="vcd-description">{{ selectedVcdLabel }}</p>
            <div class="vcd-tags">
              <el-tag size="small">共 {{ currentVcdData.length }} 项代码</el-tag>
              <el-tag v-if="selectedVcdDomains.length > 0" type="info" size="small" class="domain-tag">
                涉及域: [{{ selectedVcdDomains.join(', ') }}]
              </el-tag>
              <span v-else class="no-domain-text">未关联任何域</span>
            </div>
          </div>
          
          <div class="content-actions">
            <el-input 
              v-model="codeSearchText" 
              placeholder="搜索代码值或描述..." 
              size="small"
              style="width: 250px; margin-right: 10px;"
              clearable
              @input="searchCodelistData"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="success" size="small" @click="addCodeToVcd">
              <el-icon><Plus /></el-icon>
              新增代码
            </el-button>
          </div>
        </div>

        <!-- 数据表格 -->
        <div class="data-table" v-if="selectedVcd">
          <el-table 
            :data="filteredTableData" 
            :loading="loading" 
            border 
            style="width: 100%"
            empty-text="该VCD下暂无代码数据"
            :height="tableHeight"
            row-key="id"
          >
            <el-table-column prop="cdnum" label="序号" width="80" sortable />
            <el-table-column prop="code" label="代码值" width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <el-text type="primary">{{ row.code }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="codeDes" label="代码描述" min-width="250" show-overflow-tooltip />
            <el-table-column prop="type" label="数据类型" width="100" />
            <el-table-column prop="codeVer" label="版本" width="100" />
            <el-table-column prop="flag" label="标记" width="80" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="editRow(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="deleteRow(row)">删除</el-button>
                <el-button type="info" size="small" @click="moveUp(row)">上移</el-button>
                <el-button type="info" size="small" @click="moveDown(row)">下移</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 空状态 -->
        <div class="empty-state" v-else>
          <el-empty description="请从左侧选择一个VCD查看其代码列表">
            <el-button type="primary" @click="refreshData">刷新VCD列表</el-button>
          </el-empty>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog 
      :model-value="showEditDialog" 
      :title="editDialogTitle" 
      width="600px"
      @close="closeEditDialog"
    >
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="变量代码" prop="vcd">
          <el-input v-model="editForm.vcd" :disabled="editForm.id" />
        </el-form-item>
        <el-form-item label="变量标签" prop="vlabel">
          <el-input v-model="editForm.vlabel" />
        </el-form-item>
        <el-form-item label="数据类型" prop="type">
          <el-select v-model="editForm.type" style="width: 100%">
            <el-option label="Char" value="Char" />
            <el-option label="Num" value="Num" />
          </el-select>
        </el-form-item>
        <el-form-item label="代码序号" prop="cdnum">
          <el-input-number v-model="editForm.cdnum" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="代码值" prop="code">
          <el-input v-model="editForm.code" />
        </el-form-item>
        <el-form-item label="代码描述" prop="codeDes">
          <el-input v-model="editForm.codeDes" type="textarea" rows="3" />
        </el-form-item>
        <el-form-item label="代码版本" prop="codeVer">
          <el-input v-model="editForm.codeVer" />
        </el-form-item>
        <el-form-item label="标记" prop="flag">
          <el-input v-model="editForm.flag" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="closeEditDialog">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Refresh, Plus, Search } from '@element-plus/icons-vue'
import { 
  getCodelistList, 
  getCodelistByProject, 
  getCodelistVcds,
  getVcdDomainsMapping,
  createCodelistData,
  updateCodelistData,
  deleteCodelistData,
  updateCodelistSortOrder
} from '@/api/codelist'

// 响应式数据
const loading = ref(false)
const vcdLoading = ref(false)
const extractLoading = ref(false)
const showEditDialog = ref(false)

// VCD相关数据
const vcdList = ref([])
const filteredVcdList = ref([])
const vcdSearchText = ref('')
const selectedVcd = ref('')
const selectedVcdLabel = ref('')
const selectedVcdDomains = ref([])

// 表格数据
const tableData = ref([])
const currentVcdData = ref([])
const filteredTableData = ref([])
const codeSearchText = ref('')

// 编辑表单
const editForm = reactive({
  id: null,
  vcd: '',
  vlabel: '',
  type: 'Char',
  cdnum: 1,
  code: '',
  codeDes: '',
  codeVer: '',
  flag: ''
})

const editFormRef = ref()

// 表单验证规则
const editRules = {
  vcd: [{ required: true, message: '请输入变量代码', trigger: 'blur' }],
  vlabel: [{ required: true, message: '请输入变量标签', trigger: 'blur' }],
  code: [{ required: true, message: '请输入代码值', trigger: 'blur' }],
  codeDes: [{ required: true, message: '请输入代码描述', trigger: 'blur' }]
}

// 计算属性
const editDialogTitle = computed(() => {
  return editForm.id ? '编辑CodeList数据' : '新增CodeList数据'
})

const tableHeight = computed(() => {
  return window.innerHeight - 300
})

// 方法
const refreshData = async () => {
  await Promise.all([fetchAllCodelistData(), fetchVcdList()])
}

const fetchAllCodelistData = async () => {
  loading.value = true
  try {
    const response = await getCodelistByProject('MJR-MR001-01')
    
    if (response.code === 200 || response.code === '200' || response.data) {
      tableData.value = response.data || []
    } else {
      ElMessage.error(response.message || '获取CodeList数据失败')
    }
  } catch (error) {
    console.error('获取CodeList数据异常:', error)
    ElMessage.error('获取CodeList数据失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const fetchVcdList = async () => {
  vcdLoading.value = true
  try {
    await fetchVcdListFallback()
  } catch (error) {
    console.error('获取VCD映射失败:', error)
    // 如果出错，创建基础的VCD列表（无Domain信息）
    const vcdMap = new Map()
    
    tableData.value.forEach(item => {
      if (item.vcd) {
        if (!vcdMap.has(item.vcd)) {
          vcdMap.set(item.vcd, {
            vcd: item.vcd,
            vlabel: item.vlabel || '',
            count: 0,
            domains: []
          })
        }
        vcdMap.get(item.vcd).count++
      }
    })
    
    vcdList.value = Array.from(vcdMap.values()).sort((a, b) => a.vcd.localeCompare(b.vcd))
    filteredVcdList.value = [...vcdList.value]
  } finally {
    vcdLoading.value = false
  }
}

// 降级方案：从表格数据中统计VCD，同时获取Domain映射
const fetchVcdListFallback = async () => {
  const vcdDomainMapping = await fetchVariableDomainMapping()
  
  const vcdMap = new Map()
  
  tableData.value.forEach(item => {
    if (item.vcd) {
      if (!vcdMap.has(item.vcd)) {
        const domains = vcdDomainMapping[item.vcd] || []
        
        vcdMap.set(item.vcd, {
          vcd: item.vcd,
          vlabel: item.vlabel || '',
          count: 0,
          domains: Array.isArray(domains) ? domains : []
        })
      }
      vcdMap.get(item.vcd).count++
    }
  })
  
  vcdList.value = Array.from(vcdMap.values()).sort((a, b) => a.vcd.localeCompare(b.vcd))
  filteredVcdList.value = [...vcdList.value]
}

// 获取VCD到域的映射数据
const fetchVariableDomainMapping = async () => {
  try {
    const response = await fetch('/api/project-spec-data/vcd-domain-mapping?projectId=MJR-MR001-01', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      }
    })
    
    if (response.ok) {
      const result = await response.json()
      if (result.success && result.code === "0") {
        return result.data || {}
      } else {
        console.error('获取VCD-Domain映射失败:', result.message)
        return {}
      }
    } else {
      console.error('API请求失败:', response.status, response.statusText)
      return {}
    }
  } catch (error) {
    console.error('获取变量Domain映射异常:', error)
    return {}
  }
}

const filterVcds = () => {
  if (!vcdSearchText.value.trim()) {
    filteredVcdList.value = [...vcdList.value]
  } else {
    const searchText = vcdSearchText.value.toLowerCase()
    filteredVcdList.value = vcdList.value.filter(item => 
      item.vcd.toLowerCase().includes(searchText) ||
      item.vlabel.toLowerCase().includes(searchText)
    )
  }
}

const selectVcd = (vcd, vlabel) => {
  selectedVcd.value = vcd
  selectedVcdLabel.value = vlabel || ''
  
  const vcdItem = vcdList.value.find(item => item.vcd === vcd)
  selectedVcdDomains.value = vcdItem ? (vcdItem.domains || []) : []
  
  currentVcdData.value = tableData.value.filter(item => item.vcd === vcd)
  filteredTableData.value = [...currentVcdData.value]
  
  codeSearchText.value = ''
}

const searchCodelistData = () => {
  if (!codeSearchText.value.trim()) {
    filteredTableData.value = [...currentVcdData.value]
  } else {
    const searchText = codeSearchText.value.toLowerCase()
    filteredTableData.value = currentVcdData.value.filter(item =>
      (item.code && item.code.toLowerCase().includes(searchText)) ||
      (item.codeDes && item.codeDes.toLowerCase().includes(searchText))
    )
  }
}

const addCodeToVcd = () => {
  if (!selectedVcd.value) {
    ElMessage.warning('请先选择一个VCD')
    return
  }
  
  // 重置表单
  Object.assign(editForm, {
    id: null,
    vcd: selectedVcd.value,
    vlabel: selectedVcdLabel.value,
    type: 'Char',
    cdnum: currentVcdData.value.length + 1,
    code: '',
    codeDes: '',
    codeVer: '',
    flag: ''
  })
  
  showEditDialog.value = true
}

const editRow = (row) => {
  Object.assign(editForm, { ...row })
  showEditDialog.value = true
}

const deleteRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除代码"${row.code}"吗？`, '确认删除', {
      type: 'warning'
    })
    
    const response = await deleteCodelistData(row.id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      refreshData()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

const moveUp = (row) => {
  const index = filteredTableData.value.findIndex(item => item.id === row.id)
  if (index > 0) {
    // 实现上移逻辑
    ElMessage.info('上移功能开发中...')
  }
}

const moveDown = (row) => {
  const index = filteredTableData.value.findIndex(item => item.id === row.id)
  if (index < filteredTableData.value.length - 1) {
    // 实现下移逻辑
    ElMessage.info('下移功能开发中...')
  }
}

const saveEdit = async () => {
  try {
    await editFormRef.value?.validate()
    
    const data = { ...editForm }
    let response
    
    if (editForm.id) {
      response = await updateCodelistData(editForm.id, data)
    } else {
      data.projectId = 'MJR-MR001-01'
      response = await createCodelistData(data)
    }
    
    if (response.code === 200) {
      ElMessage.success(editForm.id ? '更新成功' : '新增成功')
      closeEditDialog()
      refreshData()
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败：' + error.message)
  }
}

const closeEditDialog = () => {
  showEditDialog.value = false
  editFormRef.value?.clearValidate()
}

const extractCodelistData = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重新提取CodeList数据吗？这将覆盖现有数据。',
      '确认提取',
      { type: 'warning' }
    )
    
    extractLoading.value = true
    
    const response = await fetch('/api/codelist/extract-codelist', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ projectId: 'MJR-MR001-01' })
    })
    
    const result = await response.json()
    
    if (result.code === 200) {
      ElMessage.success('CodeList数据提取成功！')
      refreshData()
    } else {
      ElMessage.error(result.message || '数据提取失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('数据提取失败：' + error.message)
    }
  } finally {
    extractLoading.value = false
  }
}

// 生命周期
onMounted(() => {
  refreshData()
})
</script>

<style scoped lang="less">
.codelist-manager {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  
  .codelist-header {
    background: white;
    padding: 20px;
    border-bottom: 1px solid #e6e6e6;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-left {
      h3 {
        margin: 0 0 5px 0;
        color: #303133;
        font-size: 18px;
      }
      
      p {
        margin: 0;
        color: #909399;
        font-size: 14px;
      }
    }
    
    .header-actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .main-content {
    flex: 1;
    display: flex;
    overflow: hidden;
    
    .vcd-sidebar {
      width: 300px;
      background: white;
      border-right: 1px solid #e6e6e6;
      display: flex;
      flex-direction: column;
      
      .sidebar-header {
        padding: 20px;
        border-bottom: 1px solid #e6e6e6;
        
        h4 {
          margin: 0 0 15px 0;
          color: #303133;
          font-size: 16px;
        }
      }
      
      .vcd-list {
        flex: 1;
        overflow-y: auto;
        padding: 10px 0;
        
        .vcd-item {
          padding: 15px 20px;
          border-bottom: 1px solid #f0f0f0;
          cursor: pointer;
          transition: all 0.3s;
          
          &:hover {
            background: #f8f9fa;
          }
          
          &.active {
            background: #e8f4fd;
            border-left: 3px solid #409eff;
          }
          
          .vcd-name {
            font-weight: 600;
            color: #303133;
            font-size: 14px;
            margin-bottom: 5px;
            line-height: 1.4;
            
            .vcd-domains {
              font-weight: 400;
              color: #909399;
              font-size: 11px;
              margin-left: 8px;
            }
          }
          
          .vcd-label {
            color: #606266;
            font-size: 12px;
            margin-bottom: 8px;
            line-height: 1.4;
          }
          
          .vcd-count {
            color: #909399;
            font-size: 11px;
          }
        }
      }
    }
    
    .content-area {
      flex: 1;
      display: flex;
      flex-direction: column;
      background: white;
      
      .content-header {
        padding: 20px;
        border-bottom: 1px solid #e6e6e6;
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        
        .selected-vcd-info {
          h3 {
            margin: 0 0 5px 0;
            color: #303133;
            font-size: 18px;
          }
          
          .vcd-description {
            margin: 0 0 10px 0;
            color: #606266;
            font-size: 14px;
          }
        }
        
        .content-actions {
          display: flex;
          align-items: center;
        }
      }
      
      .data-table {
        flex: 1;
        padding: 0 20px 20px 20px;
      }
      
      .empty-state {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
}

:deep(.el-table) {
  .el-table__body tr:hover > td {
    background-color: #f8f9fa !important;
  }
}

:deep(.el-empty) {
  padding: 60px 0;
}

// Domain标签相关样式
.domain-tag {
  font-size: 12px !important;
  padding: 2px 6px !important;
  border-radius: 4px !important;
  white-space: nowrap !important;
}

.no-domain-text {
  color: #c0c4cc;
  font-size: 12px;
}
</style>