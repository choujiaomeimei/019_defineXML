<template>
  <div class="pages-edit-page">
    <!-- 表格模式 -->
    <div v-if="editMode === 'table'" class="table-mode">
      <div class="table-layout ws-edit-grid">
        <div class="domain-sidebar">
          <template v-if="datasetsLoading">
            <div class="sidebar-head-spacer" aria-hidden="true"></div>
            <div class="sidebar-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
          </template>
          <template v-else-if="datasets.length === 0">
            <div class="sidebar-head-spacer" aria-hidden="true"></div>
            <div class="sidebar-empty">
            <span>暂无数据</span>
            <el-button type="primary" size="small" @click="extractData" :loading="extracting">提取数据</el-button>
            </div>
          </template>
          <template v-else>
            <div class="sidebar-search">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索变量或 Pages..."
                size="small"
                clearable
                prefix-icon="Search"
              />
            </div>
            <div class="sidebar-list">
              <div
                v-for="ds in datasets" :key="ds"
                class="domain-item"
                :class="{ active: selectedDataset === ds }"
                @click="selectDataset(ds)"
              >
                <span class="domain-name">{{ ds }}</span>
                <span class="domain-count">{{ datasetCounts[ds] || 0 }}</span>
              </div>
            </div>
          </template>
        </div>

        <div class="table-content">
          <div class="table-actions ws-toolbar-zones">
            <div class="ws-toolbar-zone ws-toolbar-zone--start">
              <div class="ws-btn-group">
                <el-button type="primary" size="small" @click="showAddDialog" :disabled="!selectedDataset">
                  <el-icon><Plus /></el-icon> 新增
                </el-button>
                <el-button size="small" @click="refreshData">
                  <el-icon><Refresh /></el-icon> 刷新
                </el-button>
              </div>
            </div>
            <div class="ws-toolbar-zone ws-toolbar-zone--center">
              <div class="ws-btn-group ws-btn-group--extract">
                <el-button size="small" :loading="extracting" @click="extractData">
                  <el-icon><Document /></el-icon> 提取Pages
                </el-button>
              </div>
            </div>
            <div class="ws-toolbar-zone ws-toolbar-zone--end">
              <el-radio-group v-model="editMode" size="small" class="ws-mode-switch">
                <el-radio-button value="table">表格模式</el-radio-button>
                <el-radio-button value="excel">Excel 模式</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div class="table-body">
          <el-table
            :data="filteredTableData"
            v-loading="tableLoading"
            border
            stripe
            size="small"
            max-height="calc(100vh - 260px)"
            @cell-dblclick="handleCellDblClick"
          >
            <el-table-column prop="dataset" label="Dataset" width="90" class-name="col-dataset">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'dataset')" v-model="editForm.dataset" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else>{{ row.dataset }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="variable" label="Variable" width="150">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'variable')" v-model="editForm.variable" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else>{{ row.variable }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="whereClause" label="Where Clause" width="220">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'whereClause')" v-model="editForm.whereClause" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else>{{ row.whereClause }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pages" label="Pages" min-width="200">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'pages')" v-model="editForm.pages" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else>{{ row.pages }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="origin" label="Origin" width="140">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'origin')" v-model="editForm.origin" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else>{{ row.origin }}</span>
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
    </div>

    <!-- Excel 模式 -->
    <div v-if="editMode === 'excel'" class="excel-mode">
      <div class="excel-toolbar ws-toolbar-zones">
        <div class="ws-toolbar-zone ws-toolbar-zone--start">
          <div class="ws-btn-group">
            <el-button type="primary" size="small" :loading="excelSaving" @click="saveExcel" :disabled="!excelLoaded">
              <el-icon><DocumentChecked /></el-icon> 保存到数据库
            </el-button>
            <el-button size="small" @click="exportExcel" :disabled="!excelLoaded">
              <el-icon><Download /></el-icon> 导出 xlsx
            </el-button>
          </div>
          <el-tag v-if="excelDirty" type="warning" size="small" effect="plain">未保存</el-tag>
        </div>
        <div class="ws-toolbar-zone ws-toolbar-zone--center">
          <div class="ws-btn-group ws-btn-group--extract">
            <el-button size="small" :loading="extracting" @click="extractData">
              <el-icon><Document /></el-icon> 提取Pages
            </el-button>
          </div>
        </div>
        <div class="ws-toolbar-zone ws-toolbar-zone--end">
          <el-radio-group v-model="editMode" size="small" class="ws-mode-switch">
            <el-radio-button value="table">表格模式</el-radio-button>
            <el-radio-button value="excel">Excel 模式</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div class="excel-container">
        <div v-if="excelLoading" class="excel-loading">
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div id="pages-luckysheet" class="luckysheet-host"></div>
      </div>
    </div>

    <!-- 新增对话框 -->
    <el-dialog v-model="addDialogVisible" title="新增Pages数据" width="500px" destroy-on-close>
      <el-form :model="addForm" label-width="120px" size="small">
        <el-form-item label="Dataset">
          <el-input v-model="addForm.dataset" />
        </el-form-item>
        <el-form-item label="Variable">
          <el-input v-model="addForm.variable" />
        </el-form-item>
        <el-form-item label="Where Clause">
          <el-input v-model="addForm.whereClause" placeholder='e.g. EGTESTCD EQ "PRAG"' />
        </el-form-item>
        <el-form-item label="Pages">
          <el-input v-model="addForm.pages" placeholder="e.g. 12, 14, 15" />
        </el-form-item>
        <el-form-item label="Origin">
          <el-input v-model="addForm.origin" />
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
import { Plus, Refresh, Loading, DocumentChecked, Download, Document } from '@element-plus/icons-vue'
import LuckyExcel from 'luckyexcel'
import service from '@/axios'

const props = defineProps({ projectId: String })
const route = useRoute()
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const currentProjectId = computed(() => props.projectId || route.params.projectId)

const editMode = ref('table')

const datasets = ref([])
const datasetCounts = ref({})
const datasetsLoading = ref(false)
const selectedDataset = ref('')
const tableData = ref([])
const searchKeyword = ref('')
const tableLoading = ref(false)
const extracting = ref(false)

const editingRowId = ref(null)
const editingField = ref(null)
const editForm = reactive({})

const addDialogVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({ dataset: '', variable: '', whereClause: '', pages: '', origin: '' })

const excelLoading = ref(false)
const excelLoaded = ref(false)
const excelSaving = ref(false)
const excelDirty = ref(false)

const filteredTableData = computed(() => {
  if (!searchKeyword.value) return tableData.value
  const kw = searchKeyword.value.toLowerCase()
  return tableData.value.filter(row =>
    (row.variable && row.variable.toLowerCase().includes(kw)) ||
    (row.whereClause && row.whereClause.toLowerCase().includes(kw)) ||
    (row.pages && row.pages.toLowerCase().includes(kw))
  )
})

const isEditing = (row, field) =>
  editingRowId.value === row.id && (editingField.value === field || editingField.value === '__row__')

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
    const res = await service.get(`${baseUrl}/api/pages/datasets/${pid}`)
    if (res.data?.success) {
      datasets.value = res.data.data || []
      const allRes = await service.get(`${baseUrl}/api/pages/project/${pid}`)
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
  cancelEdit()
  selectedDataset.value = ds
  searchKeyword.value = ''
  tableLoading.value = true
  try {
    const res = await service.get(`${baseUrl}/api/pages/project/${currentProjectId.value}/dataset/${ds}`)
    if (res.data?.success) {
      tableData.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载Pages数据失败:', e)
  } finally {
    tableLoading.value = false
  }
}

const refreshData = () => { cancelEdit(); loadDatasets() }

const saveRow = async (row) => {
  try {
    const payload = { ...editForm }
    delete payload.id
    const res = await service.put(`${baseUrl}/api/pages/${row.id}`, payload)
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
    const res = await service.delete(`${baseUrl}/api/pages/${row.id}`)
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
  Object.assign(addForm, { dataset: selectedDataset.value, variable: '', whereClause: '', pages: '', origin: '' })
  addDialogVisible.value = true
}

const submitAdd = async () => {
  addLoading.value = true
  try {
    const payload = { ...addForm, projectId: currentProjectId.value }
    const res = await service.post(`${baseUrl}/api/pages`, payload)
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

const extractData = async () => {
  extracting.value = true
  try {
    ElMessage.info('正在提取Pages数据，请稍候（可能需要1-2分钟）...')
    const res = await service.post(`${baseUrl}/api/pages/extract-pages`, { projectId: currentProjectId.value }, { timeout: 300000 })
    if (res.data?.success) {
      ElMessage.success('Pages数据提取成功')
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

const initExcelMode = async () => {
  excelLoading.value = true
  excelLoaded.value = false
  excelDirty.value = false
  try {
    const res = await service.get(`${baseUrl}/api/pages/export-xlsx/${currentProjectId.value}`, { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const file = new File([blob], 'Pages.xlsx', { type: blob.type })

    LuckyExcel.transformExcelToLucky(file, (exportJson) => {
      if (!exportJson || !exportJson.sheets || exportJson.sheets.length === 0) {
        ElMessage.warning('无Pages数据可编辑，请先提取数据')
        excelLoading.value = false
        return
      }
      nextTick(() => {
        try {
          window.luckysheet.create({
            container: 'pages-luckysheet',
            data: exportJson.sheets,
            title: 'Pages',
            showinfobar: false,
            showsheetbar: true,
            showstatisticBar: true,
            sheetFormulaBar: true,
            allowEdit: true,
            enableAddRow: true,
            enableAddBackTop: false,
            lang: 'zh',
            hook: { cellUpdated: () => { excelDirty.value = true } }
          })
          excelLoaded.value = true
          excelLoading.value = false
        } catch (e) {
          ElMessage.error('编辑器初始化失败')
          excelLoading.value = false
        }
      })
    })
  } catch (e) {
    ElMessage.error('加载数据失败')
    excelLoading.value = false
  }
}

const destroyLuckysheet = () => {
  if (window.luckysheet) { try { window.luckysheet.destroy() } catch (e) { /* ignore */ } }
}

const saveExcel = async () => {
  if (!window.luckysheet) return
  excelSaving.value = true
  try {
    const allSheets = window.luckysheet.getAllSheets()
    const res = await service.post(`${baseUrl}/api/pages/import-xlsx/${currentProjectId.value}`, { sheets: allSheets })
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
    const res = await service.get(`${baseUrl}/api/pages/export-xlsx/${currentProjectId.value}`, { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `Pages_${currentProjectId.value}.xlsx`
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
.pages-edit-page {
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

.table-layout {
  height: 100%;
}

.domain-sidebar {
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

.excel-mode {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
