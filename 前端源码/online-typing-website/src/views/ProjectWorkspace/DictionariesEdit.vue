<template>
  <div class="dictionaries-edit-page">
    <div class="table-mode">
      <div class="table-layout ws-flat-table">
        <div class="ws-flat-head">
          <div class="sidebar-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索 Dictionary..."
              size="small"
              clearable
              prefix-icon="Search"
            />
          </div>
          <div class="table-actions ws-toolbar-zones">
            <div class="ws-toolbar-zone ws-toolbar-zone--start">
              <div class="ws-btn-group">
                <el-button type="primary" size="small" @click="showAddDialog">
                  <el-icon><Plus /></el-icon> 新增
                </el-button>
                <el-button size="small" @click="refreshData">
                  <el-icon><Refresh /></el-icon> 刷新
                </el-button>
              </div>
            </div>
            <div class="ws-toolbar-zone ws-toolbar-zone--center"></div>
            <div class="ws-toolbar-zone ws-toolbar-zone--end"></div>
          </div>
        </div>
        <div class="table-body">
      <el-table
        :data="filteredData"
        v-loading="tableLoading"
        border
        stripe
        size="small"
        max-height="calc(100vh - 260px)"
        @cell-dblclick="handleCellDblClick"
      >
        <el-table-column prop="dictionaryId" label="ID" width="200">
          <template #default="{ row }">
            <el-input v-if="isEditing(row, 'dictionaryId')" v-model="editForm.dictionaryId" size="small"
              @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else :title="row.dictionaryId">{{ row.dictionaryId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="Name" min-width="260">
          <template #default="{ row }">
            <el-input v-if="isEditing(row, 'name')" v-model="editForm.name" size="small"
              @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else :title="row.name">{{ row.name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dataType" label="Data Type" width="140">
          <template #default="{ row }">
            <el-input v-if="isEditing(row, 'dataType')" v-model="editForm.dataType" size="small"
              @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else>{{ row.dataType || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dictionary" label="Dictionary" width="200">
          <template #default="{ row }">
            <el-input v-if="isEditing(row, 'dictionary')" v-model="editForm.dictionary" size="small"
              @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else>{{ row.dictionary || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="Version" width="160">
          <template #default="{ row }">
            <el-input v-if="isEditing(row, 'version')" v-model="editForm.version" size="small"
              @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else>{{ row.version || '-' }}</span>
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

    <el-dialog v-model="addDialogVisible" title="新增Dictionary" width="600px" destroy-on-close>
      <el-form :model="addForm" label-width="120px" size="small">
        <el-form-item label="ID"><el-input v-model="addForm.dictionaryId" placeholder="e.g. DRUGDICT_F" /></el-form-item>
        <el-form-item label="Name"><el-input v-model="addForm.name" placeholder="e.g. Drug Dictionary" /></el-form-item>
        <el-form-item label="Data Type"><el-input v-model="addForm.dataType" placeholder="e.g. text" /></el-form-item>
        <el-form-item label="Dictionary"><el-input v-model="addForm.dictionary" placeholder="e.g. WHODRUG" /></el-form-item>
        <el-form-item label="Version"><el-input v-model="addForm.version" placeholder="e.g. 220204" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import service from '@/axios'

const props = defineProps({ projectId: String })
const route = useRoute()
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const currentProjectId = computed(() => props.projectId || route.params.projectId)

const tableData = ref([])
const tableLoading = ref(false)
const searchKeyword = ref('')

const filteredData = computed(() => {
  if (!searchKeyword.value) return tableData.value
  const kw = searchKeyword.value.toLowerCase()
  return tableData.value.filter(r =>
    [r.dictionaryId, r.name, r.dataType, r.dictionary, r.version]
      .some(v => v && String(v).toLowerCase().includes(kw))
  )
})

const editingRowId = ref(null)
const editingField = ref(null)
const editForm = reactive({})

const addDialogVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({ dictionaryId: '', name: '', dataType: 'text', dictionary: '', version: '' })

const isEditing = (row, field) =>
  editingRowId.value === row.id && (editingField.value === field || editingField.value === '__row__')

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
  if (column.property) {
    editingRowId.value = row.id
    editingField.value = column.property
    Object.assign(editForm, row)
  }
}

const loadData = async () => {
  const pid = currentProjectId.value
  if (!pid) return
  tableLoading.value = true
  try {
    const res = await service.get(`${baseUrl}/api/dictionaries/project/${pid}`)
    if (res.data?.success) {
      tableData.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载Dictionaries数据失败:', e)
  } finally {
    tableLoading.value = false
  }
}

const refreshData = () => { cancelEdit(); loadData() }

const saveRow = async (row) => {
  try {
    const payload = { ...editForm }
    delete payload.id
    const res = await service.put(`${baseUrl}/api/dictionaries/${row.id}`, payload)
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
    const res = await service.delete(`${baseUrl}/api/dictionaries/${row.id}`)
    if (res.data?.success) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.data?.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const showAddDialog = () => {
  Object.assign(addForm, { dictionaryId: '', name: '', dataType: 'text', dictionary: '', version: '' })
  addDialogVisible.value = true
}

const submitAdd = async () => {
  addLoading.value = true
  try {
    const payload = { ...addForm, projectId: currentProjectId.value }
    const res = await service.post(`${baseUrl}/api/dictionaries`, payload)
    if (res.data?.success) {
      ElMessage.success('新增成功')
      addDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.data?.message || '新增失败')
    }
  } catch (e) {
    ElMessage.error('新增失败: ' + (e.response?.data?.message || e.message))
  } finally {
    addLoading.value = false
  }
}

onMounted(() => {
  if (currentProjectId.value) loadData()
})
</script>

<style scoped lang="less">
.dictionaries-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--saas-topbar-height, 56px));
  background: var(--saas-bg-page, #f5f6fa);
}
</style>
