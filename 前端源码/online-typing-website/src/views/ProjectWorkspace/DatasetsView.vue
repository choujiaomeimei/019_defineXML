<template>
  <div class="datasets-page">
    <!-- 表格模式 -->
    <div v-if="editMode === 'table'" class="table-mode">
      <div class="table-layout ws-flat-table">
        <div class="ws-flat-head">
          <div class="sidebar-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索数据集..."
              size="small"
              clearable
              prefix-icon="Search"
            />
          </div>
          <div class="table-actions ws-toolbar-zones">
            <div class="ws-toolbar-zone ws-toolbar-zone--start">
              <div class="ws-btn-group">
                <el-button type="primary" size="small" @click="showAddDialog"><el-icon><Plus /></el-icon> 新增</el-button>
                <el-button size="small" @click="refreshData"><el-icon><Refresh /></el-icon> 刷新</el-button>
              </div>
            </div>
            <div class="ws-toolbar-zone ws-toolbar-zone--center"></div>
            <div class="ws-toolbar-zone ws-toolbar-zone--end">
              <el-radio-group v-model="editMode" size="small" class="ws-mode-switch">
                <el-radio-button value="table">表格模式</el-radio-button>
                <el-radio-button value="excel">Excel 模式</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </div>
        <div class="table-body">
      <el-table :data="filteredData" v-loading="tableLoading" border stripe size="small" max-height="calc(100vh - 260px)" @cell-dblclick="handleCellDblClick">
        <el-table-column type="index" label="#" width="45" fixed />
        <el-table-column prop="dataset" label="Dataset" width="90" fixed class-name="col-dataset">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'dataset')" v-model="editForm.dataset" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else class="ds-name" @dblclick.stop="startEdit(row,'dataset')">{{row.dataset||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="label" label="Label" min-width="180">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'label')" v-model="editForm.label" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else @dblclick.stop="startEdit(row,'label')">{{row.label||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dataClass" label="Class" width="120">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'dataClass')" v-model="editForm.dataClass" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else @dblclick.stop="startEdit(row,'dataClass')">{{row.dataClass||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subClass" label="SubClass" width="110">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'subClass')" v-model="editForm.subClass" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else @dblclick.stop="startEdit(row,'subClass')">{{row.subClass||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="structure" label="Structure" width="140">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'structure')" v-model="editForm.structure" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else @dblclick.stop="startEdit(row,'structure')">{{row.structure||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="keyVariables" label="Key Variables" min-width="150">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'keyVariables')" v-model="editForm.keyVariables" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else @dblclick.stop="startEdit(row,'keyVariables')">{{row.keyVariables||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="standard" label="Standard" width="100">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'standard')" v-model="editForm.standard" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else @dblclick.stop="startEdit(row,'standard')">{{row.standard||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="hasNoData" label="Has No Data" width="100" align="center">
          <template #default="{row}">
            <el-select v-if="isEditing(row,'hasNoData')" v-model="editForm.hasNoData" size="small" clearable style="width:100%">
              <el-option label="Yes" value="Yes" /><el-option label="No" value="No" />
            </el-select>
            <span v-else @dblclick.stop="startEdit(row,'hasNoData')">{{row.hasNoData||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="repeating" label="Repeating" width="90" align="center">
          <template #default="{row}">
            <el-select v-if="isEditing(row,'repeating')" v-model="editForm.repeating" size="small" clearable style="width:100%">
              <el-option label="Yes" value="Yes" /><el-option label="No" value="No" />
            </el-select>
            <span v-else @dblclick.stop="startEdit(row,'repeating')">{{row.repeating||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="referenceData" label="Ref Data" width="90" align="center">
          <template #default="{row}">
            <el-select v-if="isEditing(row,'referenceData')" v-model="editForm.referenceData" size="small" clearable style="width:100%">
              <el-option label="Yes" value="Yes" /><el-option label="No" value="No" />
            </el-select>
            <span v-else @dblclick.stop="startEdit(row,'referenceData')">{{row.referenceData||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="Comment" min-width="140">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'comment')" v-model="editForm.comment" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else @dblclick.stop="startEdit(row,'comment')">{{row.comment||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="developerNotes" label="Developer Notes" min-width="140">
          <template #default="{row}">
            <el-input v-if="isEditing(row,'developerNotes')" v-model="editForm.developerNotes" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else @dblclick.stop="startEdit(row,'developerNotes')">{{row.developerNotes||'-'}}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{row}">
            <div style="display:flex;align-items:center;justify-content:center;white-space:nowrap">
              <template v-if="editingRowId === row.id">
                <el-button size="small" type="primary" text @click="saveRow(row)">保存</el-button>
                <el-button size="small" text @click="cancelEdit">取消</el-button>
              </template>
              <template v-else>
                <el-button size="small" type="primary" text @click="startEditRow(row)">编辑</el-button>
                <el-button size="small" type="danger" text @click="deleteRow(row)">删除</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
        </div>
      </div>
    </div>

    <!-- Excel 模式 -->
    <div v-if="editMode === 'excel'" class="excel-mode">
      <div class="excel-toolbar ws-toolbar-zones">
        <div class="ws-toolbar-zone ws-toolbar-zone--start">
          <div class="ws-btn-group">
            <el-button type="primary" size="small" @click="saveExcel" :loading="excelSaving" :disabled="!excelLoaded">
              保存到数据库
            </el-button>
          </div>
          <span v-if="excelDirty" class="dirty-hint">* 有未保存的修改</span>
        </div>
        <div class="ws-toolbar-zone ws-toolbar-zone--center"></div>
        <div class="ws-toolbar-zone ws-toolbar-zone--end">
          <el-radio-group v-model="editMode" size="small" class="ws-mode-switch">
            <el-radio-button value="table">表格模式</el-radio-button>
            <el-radio-button value="excel">Excel 模式</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div class="excel-container">
        <div v-if="excelLoading" class="excel-loading">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div id="datasets-luckysheet" class="luckysheet-host"></div>
      </div>
    </div>

    <!-- 新增对话框 -->
    <el-dialog v-model="addDialogVisible" title="新增数据集" width="600px" destroy-on-close>
      <el-form :model="addForm" label-width="120px" size="default">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="Dataset" required><el-input v-model="addForm.dataset" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Label"><el-input v-model="addForm.label" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="Class"><el-input v-model="addForm.dataClass" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Structure"><el-input v-model="addForm.structure" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="Key Variables"><el-input v-model="addForm.keyVariables" /></el-form-item>
        <el-form-item label="Comment"><el-input v-model="addForm.comment" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :disabled="!addForm.dataset">确定新增</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, Plus, Refresh, Search } from '@element-plus/icons-vue'
import service from '@/axios'

const props = defineProps({ projectId: String })
const route = useRoute()
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const currentProjectId = computed(() => props.projectId || route.params.projectId)
const currentUsername = computed(() => {
  const user = localStorage.getItem('user')
  return user ? JSON.parse(user).username : 'system'
})

const editMode = ref('table')
const tableLoading = ref(false)
const tableData = ref([])
const searchKeyword = ref('')
const editingRowId = ref(null)
const editingField = ref(null)
const editForm = ref({})
const addDialogVisible = ref(false)
const addForm = ref({})

const excelLoading = ref(false)
const excelLoaded = ref(false)
const excelSaving = ref(false)
const excelDirty = ref(false)

const filteredData = computed(() => {
  if (!searchKeyword.value) return tableData.value
  const kw = searchKeyword.value.toLowerCase()
  return tableData.value.filter(r => (r.dataset && r.dataset.toLowerCase().includes(kw)) || (r.label && r.label.toLowerCase().includes(kw)))
})

const loadData = async () => {
  if (!currentProjectId.value) return
  tableLoading.value = true
  try {
    const res = await service.get(`${baseUrl}/datasets/list`, { params: { projectId: currentProjectId.value } })
    if (res.data?.success && Array.isArray(res.data.data)) {
      tableData.value = res.data.data
    } else {
      tableData.value = []
    }
  } catch (e) {
    ElMessage.error('加载数据集失败')
    tableData.value = []
  } finally {
    tableLoading.value = false
  }
}

const refreshData = () => { cancelEdit(); loadData() }
const isEditing = (row, field) => editingRowId.value === row.id && (editingField.value === field || editingField.value === '__row__')
const startEdit = (row, field) => { editingRowId.value = row.id; editingField.value = field; editForm.value = { ...row } }
const startEditRow = (row) => { editingRowId.value = row.id; editingField.value = '__row__'; editForm.value = { ...row } }
const handleCellDblClick = (row, column) => { if (column.property) startEdit(row, column.property) }
const cancelEdit = () => { editingRowId.value = null; editingField.value = null; editForm.value = {} }

const saveRow = async (row) => {
  try {
    const res = await service.put(`${baseUrl}/datasets/update`, { ...editForm.value, id: row.id })
    if (res.data?.success) { Object.assign(row, editForm.value); ElMessage.success('保存成功'); cancelEdit() }
    else ElMessage.error(res.data?.message || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

const deleteRow = async (row) => {
  try {
    const res = await service.delete(`${baseUrl}/datasets/delete`, { params: { id: row.id } })
    if (res.data?.success) { tableData.value = tableData.value.filter(r => r.id !== row.id); ElMessage.success('删除成功') }
    else ElMessage.error(res.data?.message || '删除失败')
  } catch (e) { ElMessage.error('删除失败') }
}

const showAddDialog = () => {
  addForm.value = { projectId: currentProjectId.value, username: currentUsername.value, dataset: '', label: '', dataClass: '', subClass: '', structure: '', keyVariables: '', standard: '', hasNoData: '', repeating: '', referenceData: '', comment: '', developerNotes: '' }
  addDialogVisible.value = true
}

const submitAdd = async () => {
  if (!addForm.value.dataset) { ElMessage.warning('数据集名称为必填项'); return }
  try {
    const res = await service.post(`${baseUrl}/datasets/add`, addForm.value)
    if (res.data?.success) { tableData.value.push(res.data.data); addDialogVisible.value = false; ElMessage.success('新增成功') }
    else ElMessage.error(res.data?.message || '新增失败')
  } catch (e) { ElMessage.error('新增失败') }
}

const initExcelMode = async () => {
  excelLoading.value = true; excelLoaded.value = false; excelDirty.value = false
  try {
    await loadData()
    const cols = ['Dataset','Label','Class','SubClass','Structure','Key Variables','Standard','Has No Data','Repeating','Reference Data','Comment','Developer Notes']
    const fieldMap = ['dataset','label','dataClass','subClass','structure','keyVariables','standard','hasNoData','repeating','referenceData','comment','developerNotes']
    const headerRow = cols.map((c, i) => ({ r: 0, c: i, v: { m: c, v: c, ct: { fa: 'General', t: 'g' }, fc: '#333', bl: 1 } }))
    const dataRows = tableData.value.flatMap((row, ri) => fieldMap.map((f, ci) => ({ r: ri + 1, c: ci, v: { m: row[f] || '', v: row[f] || '', ct: { fa: 'General', t: 'g' } } })))
    const sheetData = [{ name: 'Datasets', color: '', status: 1, order: 0, row: Math.max(tableData.value.length + 10, 30), column: cols.length + 2, defaultRowHeight: 25, defaultColWidth: 120, celldata: [...headerRow, ...dataRows], config: { columnlen: Object.fromEntries(cols.map((_, i) => [i, i === 0 ? 90 : i === 1 ? 200 : 120])) } }]
    nextTick(() => {
      try {
        window.luckysheet.create({ container: 'datasets-luckysheet', data: sheetData, title: 'Datasets', showinfobar: false, showsheetbar: false, showstatisticBar: true, sheetFormulaBar: true, allowEdit: true, enableAddRow: true, enableAddBackTop: false, lang: 'zh', hook: { cellUpdated: () => { excelDirty.value = true } } })
        excelLoaded.value = true
      } catch (e) { ElMessage.error('编辑器初始化失败') }
      excelLoading.value = false
    })
  } catch (e) { ElMessage.error('加载数据失败'); excelLoading.value = false }
}

const destroyLuckysheet = () => { if (window.luckysheet) { try { window.luckysheet.destroy() } catch (e) { /* ignore */ } } }

const saveExcel = async () => {
  if (!window.luckysheet) return
  excelSaving.value = true
  try {
    const allSheets = window.luckysheet.getAllSheets()
    const sheet = allSheets[0]
    const data = sheet.data || []
    if (data.length < 2) { ElMessage.warning('无数据可保存'); excelSaving.value = false; return }
    const fieldMap = ['dataset','label','dataClass','subClass','structure','keyVariables','standard','hasNoData','repeating','referenceData','comment','developerNotes']
    const rows = []
    for (let r = 1; r < data.length; r++) {
      const row = {}
      let hasData = false
      fieldMap.forEach((f, c) => {
        const cell = data[r]?.[c]
        const val = cell?.v != null ? String(cell.v) : ''
        row[f] = val
        if (val) hasData = true
      })
      if (hasData) rows.push(row)
    }
    const res = await service.post(`${baseUrl}/datasets/save-all`, { projectId: currentProjectId.value, username: currentUsername.value, data: rows })
    if (res.data?.success) { ElMessage.success('保存成功'); excelDirty.value = false; await loadData() }
    else ElMessage.error(res.data?.message || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
  finally { excelSaving.value = false }
}

watch(editMode, (newMode, oldMode) => {
  if (oldMode === 'excel') destroyLuckysheet()
  if (newMode === 'excel') nextTick(() => initExcelMode())
  if (newMode === 'table') loadData()
})

onMounted(() => { if (currentProjectId.value) loadData() })
onBeforeUnmount(() => { destroyLuckysheet() })
</script>

<style scoped lang="less">
.datasets-page { display: flex; flex-direction: column; height: calc(100vh - var(--saas-topbar-height, 56px)); background: var(--saas-bg-page, #f5f6fa); }
.page-toolbar {
  display: flex; align-items: center; justify-content: space-between; padding: 16px 24px;
  background: var(--saas-bg-card, #fff); border-bottom: 1px solid var(--saas-border-light, #e5e7eb);
  .toolbar-left { display: flex; align-items: baseline; gap: 12px; }
  .page-title { font-size: 18px; font-weight: 600; margin: 0; color: var(--saas-text-primary, #1f2937); }
  .page-desc { font-size: 13px; color: var(--saas-text-tertiary, #9ca3af); }
}
.table-mode { flex: 1; min-height: 0; overflow: hidden; }
.ds-name { font-family: inherit; font-weight: 600; color: var(--saas-primary-dark, #2563eb); font-size: 12px; }
.excel-mode { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.excel-container { flex: 1; position: relative; }
.excel-loading { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; background: rgba(255,255,255,0.85); z-index: 10; color: var(--saas-text-tertiary, #9ca3af); }
.luckysheet-host { width: 100%; height: 100%; }
</style>
