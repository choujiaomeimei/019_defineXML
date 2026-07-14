<template>
  <div class="documents-edit-page">
    <div class="table-mode">
      <div class="table-layout ws-flat-table">
        <div class="ws-flat-head">
          <div class="sidebar-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索 Document..."
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
        <el-table-column prop="documentId" label="ID" width="220">
          <template #default="{ row }">
            <el-input v-if="isEditing(row, 'documentId')" v-model="editForm.documentId" size="small"
              @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else :title="row.documentId">{{ row.documentId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="Title" min-width="320">
          <template #default="{ row }">
            <el-input v-if="isEditing(row, 'title')" v-model="editForm.title" size="small"
              @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else :title="row.title">{{ row.title || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="href" label="Href" min-width="300">
          <template #default="{ row }">
            <el-input v-if="isEditing(row, 'href')" v-model="editForm.href" size="small"
              @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" />
            <span v-else :title="row.href">{{ row.href || '-' }}</span>
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

    <el-dialog v-model="addDialogVisible" title="新增Document" width="600px" destroy-on-close>
      <el-form :model="addForm" label-width="80px" size="small">
        <el-form-item label="ID"><el-input v-model="addForm.documentId" placeholder="e.g. blankcrf" /></el-form-item>
        <el-form-item label="Title"><el-input v-model="addForm.title" placeholder="e.g. Annotated Case Report Form" /></el-form-item>
        <el-form-item label="Href"><el-input v-model="addForm.href" placeholder="e.g. blankcrf.pdf" /></el-form-item>
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
    [r.documentId, r.title, r.href]
      .some(v => v && String(v).toLowerCase().includes(kw))
  )
})

const editingRowId = ref(null)
const editingField = ref(null)
const editForm = reactive({})

const addDialogVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive({ documentId: '', title: '', href: '' })

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
    const res = await service.get(`${baseUrl}/api/documents/project/${pid}`)
    if (res.data?.success) {
      tableData.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载Documents数据失败:', e)
  } finally {
    tableLoading.value = false
  }
}

const refreshData = () => { cancelEdit(); loadData() }

const saveRow = async (row) => {
  try {
    const payload = { ...editForm }
    delete payload.id
    const res = await service.put(`${baseUrl}/api/documents/${row.id}`, payload)
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
    const res = await service.delete(`${baseUrl}/api/documents/${row.id}`)
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
  Object.assign(addForm, { documentId: '', title: '', href: '' })
  addDialogVisible.value = true
}

const submitAdd = async () => {
  addLoading.value = true
  try {
    const payload = { ...addForm, projectId: currentProjectId.value }
    const res = await service.post(`${baseUrl}/api/documents`, payload)
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
.documents-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--saas-topbar-height, 56px));
  background: var(--saas-bg-page, #f5f6fa);
}
</style>
