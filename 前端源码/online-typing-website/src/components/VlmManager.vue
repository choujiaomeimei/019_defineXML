<template>
  <div class="vlm-manager">
    <div class="vlm-header">
      <div class="header-left">
        <h3>项目VLM管理</h3>
        <p>变量级元数据(Variable Level Metadata)管理</p>
      </div>
      <div class="header-actions">
        <el-button type="warning" @click="extractVlmData" :loading="extractLoading">
          <el-icon><Download /></el-icon>
          提取VLM数据
        </el-button>
        <el-button type="info" @click="testSimpleEnvironment" :loading="testLoading">
          <el-icon><Tools /></el-icon>
          简单测试
        </el-button>
        <el-button type="warning" size="small" @click="testExtractEnvironment" :loading="testLoading">
          完整测试
        </el-button>
        <el-button type="primary" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="success" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          新增VLM
        </el-button>
      </div>
    </div>

    <!-- 主内容区域：左侧Domain导航 + 右侧VLM列表 -->
    <div class="main-content">
      <!-- 左侧Domain导航 -->
      <div class="domain-sidebar">
        <div class="domain-header">
          <h4>Domain导航</h4>
          <div class="domain-stats">
            共 {{ domainList.length }} 个Domain
          </div>
        </div>
        <div class="domain-list">
          <div
            v-for="domain in domainList"
            :key="domain.dataset"
            :class="['domain-item', { active: selectedDomain === domain.dataset }]"
            @click="selectDomain(domain.dataset)"
          >
            <div class="domain-name">
              <el-icon><Document /></el-icon>
              {{ domain.dataset }}
            </div>
            <div class="domain-count">
              {{ domain.count }}条
            </div>
          </div>
          <div
            :class="['domain-item', { active: selectedDomain === 'ALL' }]"
            @click="selectDomain('ALL')"
          >
            <div class="domain-name">
              <el-icon><List /></el-icon>
              全部数据
            </div>
            <div class="domain-count">
              {{ totalRecords }}条
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧VLM详细列表 -->
      <div class="vlm-content">
        <!-- 当前选中Domain信息 -->
        <div class="selected-domain-info">
          <h4>
            <el-icon><Document /></el-icon>
            {{ selectedDomain === 'ALL' ? '全部VLM数据' : `${selectedDomain} Domain` }}
          </h4>
          <div class="domain-description">
            {{ getDomainDescription(selectedDomain) }}
          </div>
        </div>

        <!-- 筛选条件 -->
        <div class="filter-bar">
          <el-form inline :model="searchForm" class="search-form">
            <el-form-item label="项目ID:">
              <el-input
                v-model="searchForm.projectId"
                placeholder="请输入项目ID"
                clearable
                style="width: 150px"
              />
            </el-form-item>
            <el-form-item label="变量名:" v-if="selectedDomain !== 'ALL'">
              <el-input
                v-model="searchForm.variable"
                placeholder="请输入变量名"
                clearable
                style="width: 150px"
              />
            </el-form-item>
            <el-form-item label="标签:">
              <el-input
                v-model="searchForm.label"
                placeholder="请输入标签"
                clearable
                style="width: 150px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch" size="small">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
              <el-button @click="resetSearch" size="small">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- VLM数据表格 -->
        <div class="table-container">
          <el-table
            :data="tableData"
            v-loading="loading"
            stripe
            border
            height="500px"
            @sort-change="handleSortChange"
            row-key="id"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="dataset" label="数据集" width="80" sortable v-if="selectedDomain === 'ALL'" />
            <el-table-column prop="variable" label="变量名" width="120" sortable />
            <el-table-column label="所在Domain" width="120">
              <template #default="{ row }">
                <el-tag v-if="getVariableDomains(row.variable).length > 0" type="info" size="small">
                  {{ formatDomainList(getVariableDomains(row.variable)) }}
                </el-tag>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="whereClause" label="WHERE条件" width="200" show-overflow-tooltip />
            <el-table-column prop="label" label="标签" width="200" show-overflow-tooltip />
            <el-table-column prop="controlledTermsOrFormat" label="受控术语/格式" width="150" show-overflow-tooltip />
            <el-table-column prop="origin" label="来源" width="80" />
            <el-table-column prop="pages" label="页面" width="100" show-overflow-tooltip />
            <el-table-column prop="derivationComment" label="派生/注释" width="200" show-overflow-tooltip />
            <el-table-column prop="method" label="方法" width="60" />
            <el-table-column prop="category" label="类别" width="100" show-overflow-tooltip />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="editRow(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="deleteRow(row)">删除</el-button>
                <el-button type="info" size="small" @click="moveUp(row)">上移</el-button>
                <el-button type="info" size="small" @click="moveDown(row)">下移</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[20, 50, 100, 200]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            class="pagination"
          />
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form
        :model="editForm"
        :rules="formRules"
        ref="editFormRef"
        label-width="140px"
        class="edit-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目ID:" prop="projectId">
              <el-input v-model="editForm.projectId" placeholder="请输入项目ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据集:" prop="dataset">
              <el-input v-model="editForm.dataset" placeholder="请输入数据集名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="变量名:" prop="variable">
              <el-input v-model="editForm.variable" placeholder="请输入变量名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源:" prop="origin">
              <el-select v-model="editForm.origin" placeholder="选择来源">
                <el-option label="CRF" value="CRF" />
                <el-option label="Assigned" value="Assigned" />
                <el-option label="Derived" value="Derived" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="WHERE条件:" prop="whereClause">
          <el-input v-model="editForm.whereClause" placeholder="请输入WHERE条件子句" />
        </el-form-item>
        <el-form-item label="标签:" prop="label">
          <el-input v-model="editForm.label" placeholder="请输入变量标签/描述" />
        </el-form-item>
        <el-form-item label="受控术语/格式:">
          <el-input v-model="editForm.controlledTermsOrFormat" placeholder="请输入受控术语或格式" />
        </el-form-item>
        <el-form-item label="页面信息:">
          <el-input v-model="editForm.pages" placeholder="请输入页面信息" />
        </el-form-item>
        <el-form-item label="方法标识:">
          <el-input v-model="editForm.method" placeholder="请输入方法标识" />
        </el-form-item>
        <el-form-item label="类别:">
          <el-input v-model="editForm.category" placeholder="请输入类别" />
        </el-form-item>
        <el-form-item label="派生/注释:">
          <el-input
            v-model="editForm.derivationComment"
            type="textarea"
            :rows="3"
            placeholder="请输入派生/注释信息"
          />
        </el-form-item>
        <el-form-item label="备注:">
          <el-input
            v-model="editForm.comment"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveVlmData" :loading="saveLoading">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus, Search, Download, Tools, Document, List } from '@element-plus/icons-vue'
import { getVlmList, getVlmByProject, getVlmDatasets, createVlmData, updateVlmData, deleteVlmData, updateVlmSortOrder } from '@/api/vlm'

// 响应式数据
const loading = ref(false)
const saveLoading = ref(false)
const extractLoading = ref(false)
const testLoading = ref(false)
const dialogVisible = ref(false)
const tableData = ref([])
const datasetOptions = ref([])

// Domain导航相关数据
const allVlmData = ref([]) // 存储所有VLM数据
const domainList = ref([]) // domain列表及统计信息
const selectedDomain = ref('ALL') // 当前选中的domain
const totalRecords = ref(0) // 总记录数

// 变量Domain映射数据
const variableDomainMapping = ref({}) // 存储变量对应的Domain列表映射

// 搜索表单
const searchForm = reactive({
  projectId: 'MJR-MR001-01',
  dataset: '',
  variable: '',
  label: ''
})

// 分页信息
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 编辑表单
const editForm = reactive({
  id: null,
  projectId: 'MJR-MR001-01',
  dataset: '',
  variable: '',
  whereClause: '',
  label: '',
  controlledTermsOrFormat: '',
  origin: '',
  pages: '',
  derivationComment: '',
  method: '',
  comment: '',
  category: '',
  sortOrder: 0
})

// 表单引用
const editFormRef = ref()

// 表单验证规则
const formRules = {
  projectId: [
    { required: true, message: '请输入项目ID', trigger: 'blur' }
  ],
  dataset: [
    { required: true, message: '请输入数据集名称', trigger: 'blur' }
  ],
  variable: [
    { required: true, message: '请输入变量名', trigger: 'blur' }
  ]
}

// 计算属性
const dialogTitle = computed(() => {
  return editForm.id ? '编辑VLM数据' : '新增VLM数据'
})

// 方法
const refreshData = () => {
  fetchAllVlmData()
}

// Domain相关方法
const fetchAllVlmData = async () => {
  loading.value = true
  try {
    const response = await getVlmByProject(searchForm.projectId)
    
    if (response.data && response.data.success && response.data.code === "0") {
      allVlmData.value = response.data.data || []
      totalRecords.value = allVlmData.value.length
      
      // 生成domain统计信息
      generateDomainList()
      
      // 获取变量Domain映射
      await fetchVariableDomainMapping()
      
      // 根据当前选中的domain显示数据
      filterDataByDomain()
      
    } else {
      ElMessage.error(response.data?.message || '获取VLM数据失败')
    }
  } catch (error) {
    ElMessage.error('获取VLM数据失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const generateDomainList = () => {
  const domainMap = new Map()
  
  allVlmData.value.forEach(item => {
    const dataset = item.dataset
    if (domainMap.has(dataset)) {
      domainMap.set(dataset, domainMap.get(dataset) + 1)
    } else {
      domainMap.set(dataset, 1)
    }
  })
  
  domainList.value = Array.from(domainMap.entries()).map(([dataset, count]) => ({
    dataset,
    count
  })).sort((a, b) => a.dataset.localeCompare(b.dataset))
}

// 获取变量Domain映射
const fetchVariableDomainMapping = async () => {
  try {
    const response = await fetch('/api/project-spec-data/variable-domain-mapping', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      }
    })
    
    if (response.ok) {
      const result = await response.json()
      if (result.success && result.code === "0") {
        variableDomainMapping.value = result.data || {}
      } else {
        console.error('获取变量Domain映射失败:', result.message)
      }
    }
  } catch (error) {
    console.error('获取变量Domain映射异常:', error)
  }
}

// 根据变量名获取其所在的Domain列表
const getVariableDomains = (variableName) => {
  if (!variableName || !variableDomainMapping.value) return []
  
  // 首先尝试直接匹配变量名
  if (variableDomainMapping.value[variableName]) {
    return variableDomainMapping.value[variableName]
  }
  
  // 如果直接匹配不到，尝试在所有CDISC Submission Value中查找
  for (const [cdiscValue, domains] of Object.entries(variableDomainMapping.value)) {
    if (cdiscValue === variableName) {
      return domains
    }
  }
  
  return []
}

// 格式化Domain列表显示
const formatDomainList = (domains) => {
  if (!domains || domains.length === 0) return '-'
  return `[${domains.join(',')}]`
}

const selectDomain = (domain) => {
  selectedDomain.value = domain
  
  // 重置分页到第一页
  pagination.page = 1
  
  // 清空搜索条件（除了projectId）
  searchForm.dataset = domain === 'ALL' ? '' : domain
  searchForm.variable = ''
  searchForm.label = ''
  
  // 过滤数据
  filterDataByDomain()
}

const filterDataByDomain = () => {
  let filteredData = allVlmData.value
  
  // 根据选中的domain过滤
  if (selectedDomain.value !== 'ALL') {
    filteredData = filteredData.filter(item => item.dataset === selectedDomain.value)
  }
  
  // 应用搜索条件
  if (searchForm.variable) {
    filteredData = filteredData.filter(item => 
      item.variable && item.variable.toLowerCase().includes(searchForm.variable.toLowerCase())
    )
  }
  
  if (searchForm.label) {
    filteredData = filteredData.filter(item => 
      item.label && item.label.toLowerCase().includes(searchForm.label.toLowerCase())
    )
  }
  
  // 手动分页
  const startIndex = (pagination.page - 1) * pagination.size
  const endIndex = startIndex + pagination.size
  tableData.value = filteredData.slice(startIndex, endIndex)
  pagination.total = filteredData.length
}

const getDomainDescription = (domain) => {
  const descriptions = {
    'TS': '试验摘要数据集 - 包含试验的基本信息和参数',
    'LB': '实验室数据集 - 包含实验室检查结果和相关测试',
    'VS': '生命体征数据集 - 包含受试者的生命体征测量',
    'EG': '心电图数据集 - 包含心电图检查结果',
    'PE': '体格检查数据集 - 包含体格检查的结果',
    'PC': '药代动力学浓度数据集 - 包含药物浓度测定结果',
    'PP': '药代动力学参数数据集 - 包含药代动力学参数',
    'ALL': '显示所有Domain的VLM数据'
  }
  return descriptions[domain] || `${domain} 数据集的变量级元数据`
}

const testSimpleEnvironment = async () => {
  try {
    testLoading.value = true
    ElMessage.info('正在进行简单环境测试...')
    
    try {
      const response = await fetch('/api/test/python-env', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          projectId: searchForm.projectId
        })
      })
      
      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status} ${response.statusText}`)
      }
      
      const responseText = await response.text()
      
      let result
      try {
        result = JSON.parse(responseText)
      } catch (parseError) {
        throw new Error('服务器返回了无效的JSON格式数据')
      }
      
      if (result.code === '200' && result.success) {
        ElMessage.success('简单环境测试通过！')
      } else {
        ElMessage.error('简单环境测试失败')
      }
      
      // 显示详细结果
      ElMessageBox.alert(result.message || result.data, '环境测试结果', {
        dangerouslyUseHTMLString: true,
        customStyle: { 'white-space': 'pre-wrap' }
      })
      
    } catch (fetchError) {
      ElMessage.error('简单环境测试API调用失败：' + fetchError.message)
    }
    
  } catch (error) {
    ElMessage.error('简单环境测试操作失败：' + (error.message || error))
  } finally {
    testLoading.value = false
  }
}

const testExtractEnvironment = async () => {
  try {
    testLoading.value = true
    ElMessage.info('正在测试提取环境...')
    
    try {
      const response = await fetch('/api/vlm/test-extract', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          projectId: searchForm.projectId
        })
      })
      
      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status} ${response.statusText}`)
      }
      
      const responseText = await response.text()
      
      let result
      try {
        result = JSON.parse(responseText)
      } catch (parseError) {
        throw new Error('服务器返回了无效的JSON格式数据')
      }
      
      if (result.code === '200' && result.success) {
        ElMessage.success('环境测试通过！')
        // 显示详细结果
        ElMessageBox.alert(result.message || result.data, '环境测试结果', {
          dangerouslyUseHTMLString: true,
          customStyle: { 'white-space': 'pre-wrap' }
        })
      } else {
        ElMessage.error('环境测试失败')
        ElMessageBox.alert(result.message || '环境测试失败', '环境测试结果', {
          type: 'error',
          dangerouslyUseHTMLString: true,
          customStyle: { 'white-space': 'pre-wrap' }
        })
      }
      
    } catch (fetchError) {
      ElMessage.error('环境测试API调用失败：' + fetchError.message)
    }
    
  } catch (error) {
    ElMessage.error('环境测试操作失败：' + (error.message || error))
  } finally {
    testLoading.value = false
  }
}

const extractVlmData = async () => {
  try {
    await ElMessageBox.confirm(
      '此操作将从XPT文件中提取VLM数据并导入到数据库中。\n注意：会先清空当前项目的VLM数据，然后重新导入。\n确认继续吗？', 
      '确认VLM数据提取和导入', 
      {
        type: 'warning',
        confirmButtonText: '确认提取',
        cancelButtonText: '取消'
      }
    )
    
    extractLoading.value = true
    ElMessage.info('正在提取和导入VLM数据，请稍候...')
    
    try {
      // 调用后端API触发VLM数据提取
      const response = await fetch('/api/vlm/extract-vlm', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          projectId: searchForm.projectId
        })
      })
      
      // 检查响应状态
      if (!response.ok) {
        throw new Error(`HTTP Error: ${response.status} ${response.statusText}`)
      }
      
      // 获取响应文本
      const responseText = await response.text()
      
      // 尝试解析JSON
      let result
      try {
        result = JSON.parse(responseText)
      } catch (parseError) {
        throw new Error('服务器返回了无效的JSON格式数据')
      }
      
      if (result.code === 200) {
        ElMessage.success('数据提取和导入成功！')
        refreshData()
      } else {
        ElMessage.error(result.message || '数据提取和导入失败')
      }
      
    } catch (fetchError) {
      ElMessage.error('API调用失败：' + fetchError.message)
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('数据提取和导入失败：' + error.message)
    }
  } finally {
    extractLoading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  filterDataByDomain()
}

const resetSearch = () => {
  searchForm.variable = ''
  searchForm.label = ''
  pagination.page = 1
  filterDataByDomain()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  filterDataByDomain()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  filterDataByDomain()
}

const handleSortChange = ({ column, prop, order }) => {
}

const showAddDialog = () => {
  resetEditForm()
  dialogVisible.value = true
}

const editRow = (row) => {
  Object.assign(editForm, { ...row })
  dialogVisible.value = true
}

const deleteRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除VLM数据 "${row.variable}" 吗？`, '确认删除', {
      type: 'warning'
    })
    
    const response = await deleteVlmData(row.id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      fetchVlmData()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

const moveUp = async (row) => {
  const currentIndex = tableData.value.findIndex(item => item.id === row.id)
  if (currentIndex > 0) {
    const targetRow = tableData.value[currentIndex - 1]
    await swapSortOrder(row, targetRow)
  }
}

const moveDown = async (row) => {
  const currentIndex = tableData.value.findIndex(item => item.id === row.id)
  if (currentIndex < tableData.value.length - 1) {
    const targetRow = tableData.value[currentIndex + 1]
    await swapSortOrder(row, targetRow)
  }
}

const swapSortOrder = async (row1, row2) => {
  try {
    const sortOrderList = [
      { id: row1.id, sortOrder: row2.sortOrder },
      { id: row2.id, sortOrder: row1.sortOrder }
    ]
    
    const response = await updateVlmSortOrder(sortOrderList)
    if (response.code === 200) {
      ElMessage.success('排序更新成功')
      fetchVlmData()
    } else {
      ElMessage.error(response.message || '排序更新失败')
    }
  } catch (error) {
    ElMessage.error('排序更新失败：' + error.message)
  }
}

const saveVlmData = async () => {
  try {
    await editFormRef.value.validate()
    saveLoading.value = true
    
    let response
    if (editForm.id) {
      response = await updateVlmData(editForm.id, editForm)
    } else {
      response = await createVlmData(editForm)
    }
    
    if (response.code === 200) {
      ElMessage.success(editForm.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      fetchVlmData()
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    if (error.message) {
      ElMessage.error('保存失败：' + error.message)
    }
  } finally {
    saveLoading.value = false
  }
}

const resetEditForm = () => {
  Object.assign(editForm, {
    id: null,
    projectId: searchForm.projectId || 'MJR-MR001-01',
    dataset: '',
    variable: '',
    whereClause: '',
    label: '',
    controlledTermsOrFormat: '',
    origin: '',
    pages: '',
    derivationComment: '',
    method: '',
    comment: '',
    category: '',
    sortOrder: 0
  })
  editFormRef.value?.clearValidate()
}

// 生命周期
onMounted(() => {
  refreshData()
})
</script>

<style scoped lang="less">
.vlm-manager {
  // 主内容布局
  .main-content {
    display: flex;
    gap: 20px;
    height: calc(100vh - 200px);
  }

  // Domain侧边栏样式
  .domain-sidebar {
    width: 280px;
    background: #f8f9fa;
    border-radius: 8px;
    padding: 16px;
    border: 1px solid #e1e6f0;
    flex-shrink: 0;

    .domain-header {
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #e1e6f0;

      h4 {
        margin: 0 0 8px 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .domain-stats {
        font-size: 12px;
        color: #909399;
      }
    }

    .domain-list {
      max-height: calc(100vh - 300px);
      overflow-y: auto;

      .domain-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px;
        margin-bottom: 8px;
        background: white;
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          border-color: #409eff;
          box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
        }

        &.active {
          background: #409eff;
          border-color: #409eff;
          color: white;

          .domain-count {
            background: rgba(255, 255, 255, 0.2);
            color: white;
          }
        }

        .domain-name {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 14px;
          font-weight: 500;
        }

        .domain-count {
          background: #f0f2f5;
          color: #606266;
          padding: 2px 8px;
          border-radius: 12px;
          font-size: 12px;
          font-weight: 500;
        }
      }
    }
  }

  // VLM内容区样式
  .vlm-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;

    .selected-domain-info {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 16px;

      h4 {
        margin: 0 0 8px 0;
        font-size: 18px;
        font-weight: 600;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .domain-description {
        font-size: 14px;
        opacity: 0.9;
      }
    }

    .filter-bar {
      margin-bottom: 16px;
      padding: 16px;
      background: #f8f9fa;
      border-radius: 8px;
      border: 1px solid #e1e6f0;

      .search-form {
        margin: 0;
      }
    }

    .table-container {
      flex: 1;
      display: flex;
      flex-direction: column;

      .el-table {
        flex: 1;
      }

      .pagination {
        margin-top: 16px;
        display: flex;
        justify-content: flex-end;
      }

      // Domain标签样式
      .domain-tag {
        font-size: 12px;
        padding: 2px 6px;
        border-radius: 4px;
        white-space: nowrap;
      }

      .text-muted {
        color: #999;
        font-size: 12px;
      }
    }
  }
  .vlm-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #EBEEF5;

    .header-left {
      h3 {
        margin: 0 0 8px 0;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }

      p {
        margin: 0;
        font-size: 14px;
        color: #606266;
      }
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .filter-bar {
    background: #f5f7fa;
    padding: 16px;
    border-radius: 6px;
    margin-bottom: 16px;

    .search-form {
      .el-form-item {
        margin-bottom: 0;
      }
    }
  }

  .table-container {
    .pagination {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .edit-form {
    .el-form-item {
      margin-bottom: 20px;
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>