<template>
  <div class="codelist-edit-page">
    <!-- 表格模式 -->
    <div v-if="editMode === 'table'" class="table-mode">
      <div class="table-layout ws-edit-grid">
        <!-- 左侧 VCD 导航 -->
        <div class="domain-sidebar">
          <template v-if="vcdsLoading">
            <div class="sidebar-head-spacer" aria-hidden="true"></div>
            <div class="sidebar-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
          </template>
          <template v-else-if="vcds.length === 0">
            <div class="sidebar-head-spacer" aria-hidden="true"></div>
            <div class="sidebar-empty">
            <span>暂无数据</span>
            <el-button type="primary" size="small" @click="extractData" :loading="extracting" :disabled="extracting">提取Codelist</el-button>
            </div>
          </template>
          <template v-else>
            <div class="sidebar-search">
              <el-input
                v-model="vcdSearch"
                placeholder="搜索 VCD..."
                size="small"
                clearable
                prefix-icon="Search"
              />
            </div>
            <div class="sidebar-list">
              <div v-if="filteredVcds.length === 0" class="sidebar-empty-inline">无匹配 VCD</div>
              <div
                v-for="vcd in filteredVcds" :key="vcd"
                class="domain-item"
                :class="{ active: selectedVcd === vcd, 'was-deleted': deletedVcdsSet.has(vcd) }"
                @click="selectVcd(vcd)"
                @mouseenter="hoveredVcd = vcd"
                @mouseleave="hoveredVcd = ''"
              >
                <span class="domain-name">
                  <el-icon v-if="deletedVcdsSet.has(vcd)" class="deleted-marker" title="曾被删除，请检查"><WarningFilled /></el-icon>
                  {{ vcd }}
                </span>
                <div class="domain-right">
                  <span class="domain-count" v-show="hoveredVcd !== vcd">{{ vcdCounts[vcd] || 0 }}</span>
                  <el-icon
                    v-show="hoveredVcd === vcd"
                    class="vcd-delete-icon"
                    @click.stop="confirmDeleteVcd(vcd)"
                  ><Delete /></el-icon>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 右侧表格 -->
        <div class="table-content">
          <div class="table-actions ws-toolbar-zones">
            <div class="ws-toolbar-zone ws-toolbar-zone--start">
              <div class="ws-btn-group">
                <el-button type="primary" size="small" @click="showAddDialog" :disabled="!selectedVcd">
                  <el-icon><Plus /></el-icon> 新增
                </el-button>
                <el-button size="small" @click="refreshData">
                  <el-icon><Refresh /></el-icon> 刷新
                </el-button>
              </div>
            </div>
            <div class="ws-toolbar-zone ws-toolbar-zone--center">
              <div v-if="markedVcdsInList.length > 0" class="ws-btn-group ws-btn-group--danger">
                <el-button type="danger" size="small" :loading="batchDeleteLoading" @click="confirmBatchDeleteMarked">
                  一键删除已标记 ({{ markedVcdsInList.length }})
                </el-button>
              </div>
              <div class="ws-btn-group ws-btn-group--success">
                <el-button size="small" :loading="nciLoading" @click="fillNciCodes">
                  匹配NCI码
                </el-button>
                <el-button size="small" :loading="edcCompareLoading" @click="compareEdc">
                  比对EDC
                </el-button>
                <el-button size="small" @click="goToMerge">
                  合并Codelist
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
            :data="tableData"
            v-loading="tableLoading"
            border
            stripe
            size="small"
            max-height="calc(100vh - 260px)"
            @cell-dblclick="handleCellDblClick"
          >
            <el-table-column prop="vcd" label="ID" width="150">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'vcd')" v-model="editForm.vcd" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'vcd')">{{ row.vcd }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="vlabel" label="Name" min-width="140">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'vlabel')" v-model="editForm.vlabel" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'vlabel')">{{ row.vlabel }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nciCodelistCode" label="NCI Codelist Code" width="140">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'nciCodelistCode')" v-model="editForm.nciCodelistCode" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'nciCodelistCode')">{{ row.nciCodelistCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="Data Type" width="90">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'type')" v-model="editForm.type" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'type')">{{ row.type }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="terminology" label="Terminology" width="180">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'terminology')" v-model="editForm.terminology" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'terminology')">{{ row.terminology }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="Comment" width="120">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'comment')" v-model="editForm.comment" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'comment')">{{ row.comment }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="cdnum" label="Order" width="70">
              <template #default="{ row }">
                <el-input-number v-if="isEditing(row, 'cdnum')" v-model="editForm.cdnum" size="small" :controls="false" style="width:100%"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'cdnum')">{{ row.cdnum }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="code" label="Term" width="160">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'code')" v-model="editForm.code" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'code')">{{ row.code }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nciTermCode" label="NCI Term Code" width="120">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'nciTermCode')" v-model="editForm.nciTermCode" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'nciTermCode')">{{ row.nciTermCode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="codeDes" label="Decoded Value" min-width="160">
              <template #default="{ row }">
                <el-input v-if="isEditing(row, 'codeDes')" v-model="editForm.codeDes" size="small"
                  @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
                <span v-else @dblclick.stop="startEdit(row, 'codeDes')">{{ row.codeDes }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="origin" label="Origin" width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span :title="row.origin">{{ row.origin || '-' }}</span>
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
          <el-icon class="is-loading" :size="28"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div id="codelist-luckysheet" class="luckysheet-host"></div>
      </div>
    </div>

    <!-- 新增对话框 -->
    <el-dialog v-model="addDialogVisible" title="新增CodeList数据" width="550px" destroy-on-close>
      <el-form :model="addForm" label-width="140px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="ID">
              <el-input v-model="addForm.vcd" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Name">
              <el-input v-model="addForm.vlabel" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="NCI Codelist Code">
              <el-input v-model="addForm.nciCodelistCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Data Type">
              <el-input v-model="addForm.type" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Order">
              <el-input-number v-model="addForm.cdnum" :controls="false" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Term">
              <el-input v-model="addForm.code" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="NCI Term Code">
              <el-input v-model="addForm.nciTermCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Decoded Value">
              <el-input v-model="addForm.codeDes" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Terminology">
          <el-input v-model="addForm.terminology" />
        </el-form-item>
        <el-form-item label="Comment">
          <el-input v-model="addForm.comment" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- EDC比对结果对话框 -->
    <el-dialog v-model="edcDialogVisible" title="EDC Codelist 比对结果" width="800px" destroy-on-close>
      <div v-if="edcDiffData.missing.length > 0" style="margin-bottom: 20px;">
        <h4 style="margin: 0 0 10px; color: #e6a23c;">EDC中有、当前Codelist缺失的Term（{{ edcDiffData.missing.length }}条）</h4>
        <el-table :data="edcDiffData.missing" border size="small" max-height="250">
          <el-table-column prop="vcd" label="Codelist ID" width="140" />
          <el-table-column prop="edcName" label="EDC编码名" width="120" />
          <el-table-column prop="code" label="编码值" width="100" />
          <el-table-column prop="label" label="编码标签" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>
      <div v-if="edcDiffData.extra.length > 0">
        <h4 style="margin: 0 0 10px; color: #909399;">当前Codelist有、EDC中没有的Term（{{ edcDiffData.extra.length }}条，仅供参考）</h4>
        <el-table :data="edcDiffData.extra" border size="small" max-height="250">
          <el-table-column prop="vcd" label="Codelist ID" width="140" />
          <el-table-column prop="code" label="Term" width="140" />
          <el-table-column prop="codeDes" label="Decoded Value" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="edcDialogVisible = false">关闭</el-button>
        <el-button v-if="edcDiffData.missing.length > 0" type="primary" :loading="edcApplyLoading" @click="applyEdcMissing">
          补充缺失的 {{ edcDiffData.missing.length }} 条Term
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Loading, DocumentChecked, Download, Delete, WarningFilled } from '@element-plus/icons-vue'
import LuckyExcel from 'luckyexcel'
import service from '@/axios'
import {
  CODELIST_SCOPES,
  extractCodelists,
  getCodelistExtractionError,
  showCodelistExtractionResult,
} from '@/utils/codelistExtraction'

const props = defineProps({ projectId: String })
const route = useRoute()
const router = useRouter()
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const currentProjectId = computed(() => props.projectId || route.params.projectId)

function goToMerge() {
  router.push(`/project/${currentProjectId.value}/codelist-merge`)
}

const editMode = ref('table')

// --- Table mode state ---
const vcds = ref([])
const vcdCounts = ref({})
const vcdsLoading = ref(false)
const vcdSearch = ref('')
const selectedVcd = ref('')
const tableData = ref([])
const tableLoading = ref(false)
const extracting = ref(false)

const hoveredVcd = ref('')
const deletedVcds = ref([])
const deletedVcdsSet = computed(() => new Set(deletedVcds.value))
const markedVcdsInList = computed(() => deletedVcds.value.filter(v => vcds.value.includes(v)))
const batchDeleteLoading = ref(false)

const editingRowId = ref(null)
const editingField = ref(null)
const editForm = reactive({})

const addDialogVisible = ref(false)
const addLoading = ref(false)
const nciLoading = ref(false)
const edcCompareLoading = ref(false)
const edcDialogVisible = ref(false)
const edcDiffData = ref({ missing: [], extra: [] })
const edcApplyLoading = ref(false)
const addForm = reactive({
  vcd: '', vlabel: '', nciCodelistCode: '', type: '', terminology: '', comment: '',
  cdnum: null, code: '', nciTermCode: '', codeDes: ''
})

// --- Excel mode state ---
const excelLoading = ref(false)
const excelLoaded = ref(false)
const excelSaving = ref(false)
const excelDirty = ref(false)

const filteredVcds = computed(() => {
  if (!vcdSearch.value) return vcds.value
  const q = vcdSearch.value.toLowerCase()
  return vcds.value.filter(v => v.toLowerCase().includes(q))
})

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

const loadDeletedVcds = async () => {
  try {
    const res = await service.get(`${baseUrl}/api/codelist/deleted-vcds/${currentProjectId.value}`)
    if (res.data?.success) {
      deletedVcds.value = res.data.data || []
    }
  } catch (e) { /* ignore */ }
}

const loadVcds = async () => {
  const pid = currentProjectId.value
  if (!pid) return
  vcdsLoading.value = true
  try {
    const res = await service.get(`${baseUrl}/api/codelist/vcds/${pid}`)
    if (res.data?.success) {
      vcds.value = res.data.data || []
      const allRes = await service.get(`${baseUrl}/api/codelist/project/${pid}`)
      if (allRes.data?.success && Array.isArray(allRes.data.data)) {
        const counts = {}
        allRes.data.data.forEach(item => {
          counts[item.vcd] = (counts[item.vcd] || 0) + 1
        })
        vcdCounts.value = counts
      }
      if (vcds.value.length > 0 && !selectedVcd.value) {
        selectVcd(vcds.value[0])
      }
    }
    await loadDeletedVcds()
  } catch (e) {
    console.error('加载VCD列表失败:', e)
  } finally {
    vcdsLoading.value = false
  }
}

const selectVcd = async (vcd) => {
  cancelEdit()
  selectedVcd.value = vcd
  tableLoading.value = true
  try {
    const res = await service.get(`${baseUrl}/api/codelist/project/${currentProjectId.value}/vcd/${vcd}`)
    if (res.data?.success) {
      tableData.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载CodeList数据失败:', e)
  } finally {
    tableLoading.value = false
  }
}

const refreshData = () => {
  cancelEdit()
  loadVcds()
}

const saveRow = async (row) => {
  try {
    const payload = { ...editForm }
    delete payload.id
    const res = await service.put(`${baseUrl}/api/codelist/${row.id}`, payload)
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
    const res = await service.delete(`${baseUrl}/api/codelist/${row.id}`)
    if (res.data?.success) {
      ElMessage.success('删除成功')
      selectVcd(selectedVcd.value)
    } else {
      ElMessage.error(res.data?.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const confirmDeleteVcd = (vcd) => {
  ElMessageBox.confirm(
    `确定删除整个 Codelist「${vcd}」及其所有 Term 吗？此操作不可恢复。`,
    '删除确认',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  ).then(() => deleteVcd(vcd)).catch(() => {})
}

const deleteVcd = async (vcd) => {
  try {
    const res = await service.delete(`${baseUrl}/api/codelist/by-vcd/${currentProjectId.value}`, { params: { vcd } })
    if (res.data?.success) {
      ElMessage.success(res.data.data || '删除成功')
      if (selectedVcd.value === vcd) {
        selectedVcd.value = ''
        tableData.value = []
      }
      loadVcds()
    } else {
      ElMessage.error(res.data?.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

const clearDeletedRecord = async (vcd) => {
  try {
    await service.delete(`${baseUrl}/api/codelist/deleted-vcds/${currentProjectId.value}`, { params: { vcd } })
  } catch (e) { /* ignore */ }
}

const confirmBatchDeleteMarked = () => {
  const count = markedVcdsInList.value.length
  ElMessageBox.confirm(
    `将一键删除 ${count} 个曾被删除过的 Codelist（标记为⚠️的），确定继续吗？`,
    '批量删除确认',
    { confirmButtonText: '全部删除', cancelButtonText: '取消', type: 'warning' }
  ).then(() => batchDeleteMarked()).catch(() => {})
}

const batchDeleteMarked = async () => {
  batchDeleteLoading.value = true
  try {
    const res = await service.post(`${baseUrl}/api/codelist/batch-delete-marked/${currentProjectId.value}`)
    if (res.data?.success) {
      ElMessage.success(res.data.data || '批量删除成功')
      if (markedVcdsInList.value.includes(selectedVcd.value)) {
        selectedVcd.value = ''
        tableData.value = []
      }
      loadVcds()
    } else {
      ElMessage.error(res.data?.message || '批量删除失败')
    }
  } catch (e) {
    ElMessage.error('批量删除失败: ' + (e.response?.data?.message || e.message))
  } finally {
    batchDeleteLoading.value = false
  }
}

const showAddDialog = () => {
  Object.assign(addForm, {
    vcd: selectedVcd.value, vlabel: '', nciCodelistCode: '', type: '', terminology: '', comment: '',
    cdnum: null, code: '', nciTermCode: '', codeDes: ''
  })
  addDialogVisible.value = true
}

const submitAdd = async () => {
  addLoading.value = true
  try {
    const payload = { ...addForm, projectId: currentProjectId.value }
    const res = await service.post(`${baseUrl}/api/codelist`, payload)
    if (res.data?.success) {
      ElMessage.success('新增成功')
      addDialogVisible.value = false
      selectVcd(addForm.vcd)
      loadVcds()
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
  if (extracting.value) return
  try {
    await ElMessageBox.confirm(
      '该操作会重建 Variables 与 VLM 范围的系统提取 Codelist。已标记删除、合并及人工维护的数据将按服务端规则保留。请确认当前编辑已保存。是否继续？',
      '确认重建数据',
      { confirmButtonText: '继续执行', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  extracting.value = true
  try {
    ElMessage.info('正在提取全部 Codelist，请稍候...')
    const result = await extractCodelists({
      baseUrl,
      projectId: currentProjectId.value,
      scope: CODELIST_SCOPES.ALL,
    })
    await loadVcds()
    if (selectedVcd.value) await selectVcd(selectedVcd.value)
    await showCodelistExtractionResult(CODELIST_SCOPES.ALL, result)
  } catch (e) {
    ElMessage.error(getCodelistExtractionError(e, CODELIST_SCOPES.ALL))
  } finally {
    extracting.value = false
  }
}

const fillNciCodes = async () => {
  nciLoading.value = true
  try {
    ElMessage.info('正在从CT表中填充NCI代码...')
    const res = await service.post(`${baseUrl}/api/codelist/fill-nci-codes/${currentProjectId.value}`, {}, { timeout: 120000 })
    if (res.data?.success) {
      ElMessage.success(res.data.data || 'NCI代码填充成功')
      if (selectedVcd.value) selectVcd(selectedVcd.value)
    } else {
      ElMessage.error(res.data?.message || '填充失败')
    }
  } catch (e) {
    ElMessage.error('NCI代码填充失败: ' + (e.response?.data?.message || e.message))
  } finally {
    nciLoading.value = false
  }
}

const compareEdc = async () => {
  edcCompareLoading.value = true
  try {
    ElMessage.info('正在比对EDC建库说明...')
    const res = await service.post(`${baseUrl}/api/codelist/compare-edc/${currentProjectId.value}`, {}, { timeout: 120000 })
    if (res.data?.success) {
      edcDiffData.value = res.data.data || { missing: [], extra: [] }
      if (edcDiffData.value.missing.length === 0 && edcDiffData.value.extra.length === 0) {
        ElMessage.success('Codelist与EDC完全一致，无差异')
      } else {
        edcDialogVisible.value = true
      }
    } else {
      ElMessage.error(res.data?.message || '比对失败')
    }
  } catch (e) {
    ElMessage.error('比对EDC失败: ' + (e.response?.data?.message || e.message))
  } finally {
    edcCompareLoading.value = false
  }
}

const applyEdcMissing = async () => {
  edcApplyLoading.value = true
  try {
    const res = await service.post(`${baseUrl}/api/codelist/apply-edc-diff/${currentProjectId.value}`,
      { items: edcDiffData.value.missing }, { timeout: 60000 })
    if (res.data?.success) {
      ElMessage.success(res.data.data || '补充成功')
      edcDialogVisible.value = false
      loadVcds()
      if (selectedVcd.value) selectVcd(selectedVcd.value)
    } else {
      ElMessage.error(res.data?.message || '补充失败')
    }
  } catch (e) {
    ElMessage.error('补充失败: ' + (e.response?.data?.message || e.message))
  } finally {
    edcApplyLoading.value = false
  }
}

// --- Excel mode ---
const initExcelMode = async () => {
  excelLoading.value = true
  excelLoaded.value = false
  excelDirty.value = false
  try {
    const res = await service.get(`${baseUrl}/api/codelist/export-xlsx/${currentProjectId.value}`, {
      responseType: 'blob'
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const file = new File([blob], 'CodeList.xlsx', { type: blob.type })

    LuckyExcel.transformExcelToLucky(file, (exportJson) => {
      if (!exportJson || !exportJson.sheets || exportJson.sheets.length === 0) {
        ElMessage.warning('无CodeList数据可编辑，请先提取数据')
        excelLoading.value = false
        return
      }
      nextTick(() => {
        try {
          window.luckysheet.create({
            container: 'codelist-luckysheet',
            data: exportJson.sheets,
            title: 'CodeList',
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
    const res = await service.post(`${baseUrl}/api/codelist/import-xlsx/${currentProjectId.value}`, { sheets: allSheets })
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
    const res = await service.get(`${baseUrl}/api/codelist/export-xlsx/${currentProjectId.value}`, { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `CodeList_${currentProjectId.value}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

watch(editMode, (newMode, oldMode) => {
  if (oldMode === 'excel') destroyLuckysheet()
  if (newMode === 'excel') nextTick(() => initExcelMode())
  if (newMode === 'table') loadVcds()
})

onMounted(() => {
  if (currentProjectId.value) loadVcds()
})

onBeforeUnmount(() => {
  destroyLuckysheet()
})
</script>

<style scoped lang="less">
.codelist-edit-page {
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

.domain-item {
  .domain-right {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
  }

  .domain-count {
    padding: 1px 8px;
    flex-shrink: 0;
  }

  .vcd-delete-icon {
    font-size: 14px;
    color: var(--saas-danger);
    cursor: pointer;
    transition: color 0.15s;
    &:hover { color: #dc2626; }
  }

  .deleted-marker {
    font-size: 13px;
    color: var(--saas-warning);
    margin-right: 2px;
    vertical-align: -1px;
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
