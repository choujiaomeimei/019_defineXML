<template>
  <div class="vlm-edit-page">
    <!-- 模式切换工具栏 -->
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h3 class="page-title">ValueLevel</h3>
        <span class="page-desc">变量级元数据 (Value Level Metadata)</span>
      </div>
      <div class="toolbar-right">
        <el-button type="warning" size="small" :loading="extracting" @click="extractWhereClause" round>
          提取Where Clause
        </el-button>
        <el-button type="primary" size="small" :loading="xptFieldsExtracting" @click="extractXptFields" plain round>
          <el-icon><DataAnalysis /></el-icon> 提取XPT字段
        </el-button>
        <el-button type="success" size="small" :loading="codelistExtracting" @click="extractCodelist" plain round>
          提取Codelist
        </el-button>
        <el-button type="warning" size="small" :loading="pagesExtracting" @click="extractVlmPages" plain round>
          <el-icon><Document /></el-icon> 提取Pages
        </el-button>
        <el-radio-group v-model="editMode" size="small">
          <el-radio-button value="table">表格模式</el-radio-button>
          <el-radio-button value="excel">Excel 模式</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 表格模式 -->
    <div v-if="editMode === 'table'" class="table-mode">
      <div class="table-layout">
        <!-- 左侧 Domain 导航 -->
        <div class="domain-sidebar">
          <div class="sidebar-title">Dataset 导航</div>
          <div v-if="datasetsLoading" class="sidebar-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
          <div v-else-if="datasets.length === 0" class="sidebar-empty">
            <span>暂无数据</span>
            <el-button type="primary" size="small" @click="extractWhereClause" :loading="extracting">提取Where Clause</el-button>
          </div>
          <template v-else>
            <div
              v-for="ds in datasets" :key="ds"
              class="domain-item"
              :class="{ active: selectedDataset === ds }"
              @click="selectDataset(ds)"
            >
              <span class="domain-name">{{ ds }}</span>
              <span class="domain-count">{{ datasetCounts[ds] || 0 }}</span>
            </div>
          </template>
        </div>

        <!-- 右侧表格 -->
        <div class="table-content">
          <div class="table-actions">
            <el-button type="primary" size="small" @click="showAddDialog" :disabled="!selectedDataset">
              <el-icon><Plus /></el-icon> 新增
            </el-button>
            <el-button size="small" @click="refreshData">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>

          <el-table
            :data="tableData"
            v-loading="tableLoading"
            border
            stripe
            size="small"
            max-height="calc(100vh - 260px)"
            @cell-dblclick="handleCellDblClick"
          >
            <el-table-column prop="sortOrder" label="Order" width="65" align="center">
              <template #default="{ row }"><span>{{ row.sortOrder }}</span></template>
            </el-table-column>
            <el-table-column prop="dataset" label="Dataset" width="90">
              <template #default="{ row }"><span>{{ row.dataset }}</span></template>
            </el-table-column>
            <el-table-column prop="variable" label="Variable" width="110">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'variable')" v-model="editForm.variable" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'variable')">{{ row.variable }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="whereClause" label="Where Clause" width="180">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'whereClause')" v-model="editForm.whereClause" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'whereClause')" :title="row.whereClause">{{ row.whereClause }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="label" label="Label" min-width="160">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'label')" v-model="editForm.label" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'label')" :title="row.label">{{ row.label }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="dataType" label="Data Type" width="90">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'dataType')" v-model="editForm.dataType" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'dataType')">{{ row.dataType||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="length" label="Length" width="70">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'length')" v-model="editForm.length" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'length')">{{ row.length||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="significantDigits" label="Sig. Digits" width="85">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'significantDigits')" v-model="editForm.significantDigits" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'significantDigits')">{{ row.significantDigits||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="format" label="Format" width="90">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'format')" v-model="editForm.format" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'format')">{{ row.format||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="mandatory" label="Mandatory" width="90" align="center">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'mandatory')" v-model="editForm.mandatory" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'mandatory')">{{ row.mandatory||'No' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="assignedValue" label="Assigned Value" width="120">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'assignedValue')" v-model="editForm.assignedValue" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'assignedValue')">{{ row.assignedValue||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="codelist" label="Codelist" width="110">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'codelist')" v-model="editForm.codelist" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'codelist')">{{ row.codelist||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="origin" label="Origin" width="100">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'origin')" v-model="editForm.origin" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'origin')">{{ row.origin||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="Source" width="110">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'source')" v-model="editForm.source" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'source')">{{ row.source||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pages" label="Pages" width="100">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'pages')" v-model="editForm.pages" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'pages')">{{ row.pages||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="method" label="Method" width="100">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'method')" v-model="editForm.method" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'method')">{{ row.method||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="predecessor" label="Predecessor" width="110">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'predecessor')" v-model="editForm.predecessor" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'predecessor')">{{ row.predecessor||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="Comment" width="120">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'comment')" v-model="editForm.comment" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'comment')">{{ row.comment||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="developerNotes" label="Developer Notes" width="140">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'developerNotes')" v-model="editForm.developerNotes" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'developerNotes')">{{ row.developerNotes||'-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <template v-if="editingRowId === row.id && editingField === '__row__'">
                  <el-button type="primary" link size="small" @click="saveRow(row)">保存</el-button>
                  <el-button link size="small" @click="cancelEdit">取消</el-button>
                </template>
                <template v-else>
                  <el-button type="primary" link size="small" @click="startEditRow(row)">编辑</el-button>
                  <el-popconfirm title="确定删除？" @confirm="deleteRow(row)">
                    <template #reference>
                      <el-button type="danger" link size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- Excel 模式 -->
    <div v-if="editMode === 'excel'" class="excel-mode">
      <div class="excel-toolbar">
        <el-button size="small" type="primary" :loading="excelSaving" @click="saveExcel" :disabled="!excelLoaded" round>
          <el-icon><DocumentChecked /></el-icon> 保存到数据库
        </el-button>
        <el-button size="small" type="success" @click="exportExcel" :disabled="!excelLoaded" round>
          <el-icon><Download /></el-icon> 导出 xlsx
        </el-button>
        <el-tag v-if="excelDirty" type="warning" size="small" effect="plain">未保存</el-tag>
      </div>
      <div class="excel-container">
        <div v-if="excelLoading" class="excel-loading">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div id="vlm-luckysheet" class="luckysheet-host"></div>
      </div>
    </div>

    <!-- 新增对话框 -->
    <el-dialog v-model="addDialogVisible" title="新增VLM数据" width="600px" destroy-on-close>
      <el-form :model="addForm" label-width="140px" size="small">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="Dataset"><el-input v-model="addForm.dataset" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Variable"><el-input v-model="addForm.variable" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="Where Clause"><el-input v-model="addForm.whereClause" /></el-form-item>
        <el-form-item label="Label"><el-input v-model="addForm.label" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="Data Type"><el-input v-model="addForm.dataType" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Length"><el-input v-model="addForm.length" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Sig. Digits"><el-input v-model="addForm.significantDigits" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="Format"><el-input v-model="addForm.format" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Mandatory"><el-input v-model="addForm.mandatory" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Codelist"><el-input v-model="addForm.codelist" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="Origin"><el-input v-model="addForm.origin" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Source"><el-input v-model="addForm.source" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="Category">
          <el-input v-model="addForm.category" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Loading, DocumentChecked, Download, Document, DataAnalysis } from '@element-plus/icons-vue'
import LuckyExcel from 'luckyexcel'
import service from '@/axios'

const props = defineProps({ projectId: String })
const route = useRoute()
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const currentProjectId = computed(() => props.projectId || route.params.projectId)

const editMode = ref('table')

// --- Table mode state ---
const datasets = ref([])
const datasetCounts = ref({})
const datasetsLoading = ref(false)
const selectedDataset = ref('')
const tableData = ref([])
const tableLoading = ref(false)
const extracting = ref(false)
const xptFieldsExtracting = ref(false)
const codelistExtracting = ref(false)
const pagesExtracting = ref(false)

const editingRowId = ref(null)
const editingField = ref(null)
const editForm = reactive({})

const addDialogVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({
  dataset: '', variable: '', whereClause: '', label: '',
  dataType: '', length: '', significantDigits: '', format: '',
  mandatory: 'No', assignedValue: '', codelist: '',
  origin: '', source: '', pages: '',
  method: '', predecessor: '', comment: '', developerNotes: ''
})

// --- Excel mode state ---
const excelLoading = ref(false)
const excelLoaded = ref(false)
const excelSaving = ref(false)
const excelDirty = ref(false)

const isEditing = (row, field) => {
  return editingRowId.value === row.id && (editingField.value === field || editingField.value === '__row__')
}

const startEdit = (row, field) => {
  editingRowId.value = row.id
  editingField.value = field
  Object.assign(editForm, row)
}

const startEditRow = (row) => {
  editingRowId.value = row.id
  editingField.value = '__row__'
  Object.assign(editForm, row)
}

const cancelEdit = () => {
  editingRowId.value = null
  editingField.value = null
}

const handleCellDblClick = (row, column) => {
  if (column.property) startEdit(row, column.property)
}

const loadDatasets = async () => {
  const pid = currentProjectId.value
  if (!pid) return
  datasetsLoading.value = true
  try {
    const res = await service.get(`${baseUrl}/api/vlm/datasets/${pid}`)
    if (res.data?.success) {
      datasets.value = res.data.data || []
      // Load counts
      const allRes = await service.get(`${baseUrl}/api/vlm/project/${pid}`)
      if (allRes.data?.success && Array.isArray(allRes.data.data)) {
        const counts = {}
        allRes.data.data.forEach(item => {
          counts[item.dataset] = (counts[item.dataset] || 0) + 1
        })
        datasetCounts.value = counts
      }
      if (datasets.value.length > 0 && !selectedDataset.value) {
        selectDataset(datasets.value[0])
      }
    }
  } catch (e) {
    console.error('加载Dataset列表失败:', e)
  } finally {
    datasetsLoading.value = false
  }
}

const selectDataset = async (ds) => {
  selectedDataset.value = ds
  tableLoading.value = true
  try {
    const res = await service.get(`${baseUrl}/api/vlm/project/${currentProjectId.value}/dataset/${ds}`)
    if (res.data?.success) {
      tableData.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载VLM数据失败:', e)
  } finally {
    tableLoading.value = false
  }
}

const refreshData = () => {
  cancelEdit()
  loadDatasets()
}

const saveRow = async (row) => {
  try {
    const payload = { ...editForm }
    delete payload.id
    const res = await service.put(`${baseUrl}/api/vlm/${row.id}`, payload)
    if (res.data?.success) {
      Object.assign(row, editForm)
      cancelEdit()
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

const deleteRow = async (row) => {
  try {
    const res = await service.delete(`${baseUrl}/api/vlm/${row.id}`)
    if (res.data?.success) {
      ElMessage.success('删除成功')
      selectDataset(selectedDataset.value)
    } else {
      ElMessage.error(res.data?.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const showAddDialog = () => {
  Object.assign(addForm, {
    dataset: selectedDataset.value, variable: '', whereClause: '', label: '',
    dataType: '', length: '', significantDigits: '', format: '',
    mandatory: 'No', assignedValue: '', codelist: '',
    origin: '', source: '', pages: '',
    method: '', predecessor: '', comment: '', developerNotes: ''
  })
  addDialogVisible.value = true
}

const submitAdd = async () => {
  addLoading.value = true
  try {
    const payload = { ...addForm, projectId: currentProjectId.value }
    const res = await service.post(`${baseUrl}/api/vlm`, payload)
    if (res.data?.success) {
      ElMessage.success('新增成功')
      addDialogVisible.value = false
      selectDataset(addForm.dataset)
      loadDatasets()
    } else {
      ElMessage.error(res.data?.message || '新增失败')
    }
  } catch (e) {
    ElMessage.error('新增失败: ' + (e.response?.data?.message || e.message))
  } finally {
    addLoading.value = false
  }
}

const extractWhereClause = async () => {
  extracting.value = true
  try {
    ElMessage.info('正在提取 Where Clause（VLM数据），请稍候（可能需要1-2分钟）...')
    const res = await service.post(`${baseUrl}/api/vlm/extract-vlm`, { projectId: currentProjectId.value }, { timeout: 600000 })
    if (res.data?.success) {
      ElMessage.success(res.data.data || 'Where Clause 提取完成')
      loadDatasets()
    } else {
      ElMessage.error(res.data?.message || '提取失败')
    }
  } catch (e) {
    ElMessage.error('提取失败: ' + (e.response?.data?.message || e.message))
  } finally {
    extracting.value = false
  }
}

const extractXptFields = async () => {
  xptFieldsExtracting.value = true
  try {
    ElMessage.info('正在提取 Data Type / Length / Significant Digits / Format ...')
    const res = await service.post(`${baseUrl}/api/vlm/extract-xpt-fields/${currentProjectId.value}`, {}, { timeout: 180000 })
    if (res.data?.success) {
      ElMessage.success(res.data.data || 'XPT字段提取完成')
      if (selectedDataset.value) selectDataset(selectedDataset.value)
    } else {
      ElMessage.error(res.data?.message || '提取失败')
    }
  } catch (e) {
    ElMessage.error('提取XPT字段失败: ' + (e.response?.data?.message || e.message))
  } finally {
    xptFieldsExtracting.value = false
  }
}

const extractCodelist = async () => {
  codelistExtracting.value = true
  try {
    ElMessage.info('正在提取 Codelist ...')
    const res = await service.post(`${baseUrl}/api/vlm/extract-vlm-codelist/${currentProjectId.value}`, {}, { timeout: 180000 })
    if (res.data?.success) {
      ElMessage.success(res.data.data || 'Codelist 提取完成')
      if (selectedDataset.value) selectDataset(selectedDataset.value)
    } else {
      ElMessage.error(res.data?.message || '提取失败')
    }
  } catch (e) {
    ElMessage.error('提取Codelist失败: ' + (e.response?.data?.message || e.message))
  } finally {
    codelistExtracting.value = false
  }
}

const extractVlmPages = async () => {
  pagesExtracting.value = true
  try {
    ElMessage.info('正在提取 VLM Pages...')
    const res = await service.post(`${baseUrl}/api/vlm/extract-vlm-pages/${currentProjectId.value}`, {}, { timeout: 120000 })
    if (res.data?.success) {
      ElMessage.success(res.data.data || 'VLM Pages 提取完成')
      if (selectedDataset.value) selectDataset(selectedDataset.value)
    } else {
      ElMessage.error(res.data?.message || '提取失败')
    }
  } catch (e) {
    ElMessage.error('提取Pages失败: ' + (e.response?.data?.message || e.message))
  } finally {
    pagesExtracting.value = false
  }
}

// --- Excel mode ---
const initExcelMode = async () => {
  excelLoading.value = true
  excelLoaded.value = false
  excelDirty.value = false
  try {
    const res = await service.get(`${baseUrl}/api/vlm/export-xlsx/${currentProjectId.value}`, {
      responseType: 'blob'
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const file = new File([blob], 'VLM.xlsx', { type: blob.type })

    LuckyExcel.transformExcelToLucky(file, (exportJson) => {
      if (!exportJson || !exportJson.sheets || exportJson.sheets.length === 0) {
        ElMessage.warning('无VLM数据可编辑，请先提取数据')
        excelLoading.value = false
        return
      }
      nextTick(() => {
        try {
          window.luckysheet.create({
            container: 'vlm-luckysheet',
            data: exportJson.sheets,
            title: 'VLM',
            showinfobar: false,
            showsheetbar: true,
            showstatisticBar: true,
            sheetFormulaBar: true,
            allowEdit: true,
            enableAddRow: true,
            enableAddBackTop: false,
            lang: 'zh',
            hook: {
              cellUpdated: () => { excelDirty.value = true }
            }
          })
          excelLoaded.value = true
          excelLoading.value = false
        } catch (e) {
          console.error('Luckysheet初始化失败:', e)
          ElMessage.error('编辑器初始化失败')
          excelLoading.value = false
        }
      })
    })
  } catch (e) {
    console.error('加载Excel失败:', e)
    ElMessage.error('加载数据失败')
    excelLoading.value = false
  }
}

const destroyLuckysheet = () => {
  if (window.luckysheet) {
    try { window.luckysheet.destroy() } catch (e) { /* ignore */ }
  }
}

const saveExcel = async () => {
  if (!window.luckysheet) return
  excelSaving.value = true
  try {
    const allSheets = window.luckysheet.getAllSheets()
    const res = await service.post(`${baseUrl}/api/vlm/import-xlsx/${currentProjectId.value}`, { sheets: allSheets })
    if (res.data?.success) {
      ElMessage.success(res.data.data || '保存成功')
      excelDirty.value = false
    } else {
      ElMessage.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  } finally {
    excelSaving.value = false
  }
}

const exportExcel = async () => {
  try {
    const res = await service.get(`${baseUrl}/api/vlm/export-xlsx/${currentProjectId.value}`, { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `VLM_${currentProjectId.value}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

watch(editMode, (newMode, oldMode) => {
  if (oldMode === 'excel') destroyLuckysheet()
  if (newMode === 'excel') nextTick(() => initExcelMode())
  if (newMode === 'table') loadDatasets()
})

onMounted(() => {
  if (currentProjectId.value) loadDatasets()
})

onBeforeUnmount(() => {
  destroyLuckysheet()
})
</script>

<style scoped lang="less">
.vlm-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--saas-topbar-height, 56px));
  background: var(--saas-bg-page, #f5f6fa);
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: var(--saas-bg-card, #fff);
  border-bottom: 1px solid var(--saas-border-light, #e5e7eb);

  .toolbar-left {
    display: flex;
    align-items: baseline;
    gap: 12px;
  }

  .page-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
    color: var(--saas-text-primary, #1f2937);
  }

  .page-desc {
    font-size: 13px;
    color: var(--saas-text-tertiary, #9ca3af);
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.table-mode {
  flex: 1;
  overflow: hidden;
}

.table-layout {
  display: flex;
  height: 100%;
}

.domain-sidebar {
  width: 200px;
  min-width: 200px;
  background: var(--saas-bg-card, #fff);
  border-right: 1px solid var(--saas-border-light, #e5e7eb);
  display: flex;
  flex-direction: column;
  overflow-y: auto;

  .sidebar-title {
    padding: 12px 16px;
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--saas-text-tertiary, #9ca3af);
    border-bottom: 1px solid var(--saas-border-light, #e5e7eb);
  }

  .sidebar-loading, .sidebar-empty {
    padding: 24px 16px;
    text-align: center;
    color: var(--saas-text-tertiary, #9ca3af);
    font-size: 13px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: center;
  }
}

.domain-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--saas-border-light, #f3f4f6);

  &:hover { background: var(--saas-bg-hover, #f9fafb); }
  &.active {
    background: var(--saas-primary-bg, #eff6ff);
    .domain-name { color: var(--saas-primary, #4f46e5); font-weight: 600; }
  }

  .domain-name {
    font-size: 13px;
    color: var(--saas-text-primary, #1f2937);
  }

  .domain-count {
    font-size: 11px;
    color: var(--saas-text-tertiary, #9ca3af);
    background: var(--saas-bg-input, #f3f4f6);
    padding: 1px 8px;
    border-radius: 10px;
  }
}

.table-content {
  flex: 1;
  padding: 16px;
  overflow: auto;

  .table-actions {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
  }
}

.excel-mode {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.excel-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: var(--saas-bg-card, #fff);
  border-bottom: 1px solid var(--saas-border-light, #e5e7eb);
}

.excel-container {
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

.excel-loading {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(255,255,255,0.9);
  z-index: 20;
  font-size: 14px;
  color: var(--saas-text-secondary, #6b7280);
}
</style>
