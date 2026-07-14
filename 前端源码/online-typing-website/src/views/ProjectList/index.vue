<template>
  <div class="project-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1>项目管理</h1>
        <p class="header-desc">管理您的所有 Define.XML 项目</p>
      </div>
      <div class="header-actions">
        <button type="button" class="reset-system-btn" :disabled="resetting" @click="resetDialogVisible = true">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path d="M2.5 8a5.5 5.5 0 0 1 9.4-3.9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M13.5 8A5.5 5.5 0 0 1 4.1 11.9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M11.5 2.5V5.5H8.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M4.5 13.5V10.5H7.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>{{ resetting ? '重置中…' : '重置系统' }}</span>
        </button>
        <el-button type="primary" class="create-btn" @click="goToCreateProject">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class="btn-icon">
            <path d="M8 3V13M3 8H13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          创建项目
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon variant-primary">
          <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
            <rect x="3" y="3" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/>
            <rect x="12" y="3" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/>
            <rect x="3" y="12" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/>
            <rect x="12" y="12" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ projectList.length }}</span>
          <span class="stat-label">项目总数</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon variant-success">
          <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
            <path d="M18 6L8.5 15.5L4 11" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ sdtmCount }}</span>
          <span class="stat-label">SDTM 项目</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon variant-warning">
          <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
            <circle cx="11" cy="11" r="8" stroke="currentColor" stroke-width="1.5"/>
            <path d="M11 7V11L14 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-num">{{ adamCount }}</span>
          <span class="stat-label">ADAM 项目</span>
        </div>
      </div>
    </div>

    <!-- 搜索 & 过滤 -->
    <div class="filter-bar">
      <div class="search-box">
        <svg class="search-icon" width="18" height="18" viewBox="0 0 18 18" fill="none">
          <circle cx="8" cy="8" r="5.5" stroke="currentColor" stroke-width="1.5"/>
          <path d="M12.5 12.5L16 16" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <input v-model="searchText" placeholder="搜索项目名称、编号..." />
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!filteredProjects.length && !projectList.length" class="empty-state">
      <div class="empty-icon">
        <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
          <rect x="8" y="12" width="48" height="40" rx="6" stroke="#D1D5DB" stroke-width="2"/>
          <path d="M8 24H56" stroke="#D1D5DB" stroke-width="2"/>
          <circle cx="16" cy="18" r="2" fill="#D1D5DB"/>
          <circle cx="22" cy="18" r="2" fill="#D1D5DB"/>
          <circle cx="28" cy="18" r="2" fill="#D1D5DB"/>
          <path d="M24 38L32 30L40 38" stroke="#D1D5DB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M32 30V46" stroke="#D1D5DB" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
      <h3>暂无项目</h3>
      <p>点击上方「创建项目」按钮开始您的第一个项目</p>
    </div>

    <!-- 无搜索结果 -->
    <div v-else-if="!filteredProjects.length && projectList.length" class="empty-state">
      <h3>未找到匹配的项目</h3>
      <p>尝试使用不同的关键词搜索</p>
    </div>

    <!-- 项目卡片网格 -->
    <div v-else class="project-grid">
      <div
        v-for="project in filteredProjects"
        :key="project.projectId"
        class="project-card"
        @click="enterProject(project)"
      >
        <div class="card-top">
          <div class="project-avatar" :style="{ background: getAvatarColor(project.projectId) }">
            {{ project.projectName ? project.projectName.charAt(0) : 'P' }}
          </div>
          <div class="card-actions" @click.stop>
            <el-dropdown trigger="click">
              <button class="more-btn">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <circle cx="8" cy="3" r="1.5" fill="currentColor"/>
                  <circle cx="8" cy="8" r="1.5" fill="currentColor"/>
                  <circle cx="8" cy="13" r="1.5" fill="currentColor"/>
                </svg>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="enterProject(project)">进入项目</el-dropdown-item>
                  <el-dropdown-item @click="deleteProject(project)" divided>
                    <span class="danger-text">删除项目</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <h3 class="project-name">{{ project.projectName }}</h3>
        <p class="project-id">{{ project.projectId }}</p>

        <div class="card-meta">
          <div class="meta-item">
            <span class="meta-label">方案</span>
            <span class="meta-value">{{ project.protocolNumber || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">申办方</span>
            <span class="meta-value">{{ project.sponsor || '-' }}</span>
          </div>
        </div>

        <div class="card-footer">
          <div class="tag-group">
            <el-tag
              v-for="type in getStandardTypes(project.standardType)"
              :key="type"
              :type="getTagType(type)"
              size="small"
              round
              effect="plain"
            >
              {{ type }}
            </el-tag>
          </div>
          <span class="card-date">{{ formatDate(project.createdTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 创建项目弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      width="640px"
      class="create-project-dialog"
      align-center
      destroy-on-close
      :show-close="true"
    >
      <template #header>
        <div class="create-dialog-header">
          <h2 class="create-dialog-title">创建新项目</h2>
          <p class="create-dialog-desc">填写项目基本信息，创建后可在工作台中编辑 Spec 与 Define</p>
        </div>
      </template>

      <el-form
        :model="projectForm"
        :rules="formRules"
        ref="projectFormRef"
        label-position="top"
        class="create-project-form"
        @submit.prevent
      >
        <section class="form-section">
          <h3 class="form-section-title">基本信息</h3>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="项目 ID" prop="projectId">
                <el-input v-model="projectForm.projectId" placeholder="如：P500_MR001" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="项目名称" prop="projectName">
                <el-input v-model="projectForm.projectName" placeholder="请输入项目名称" clearable />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="申办方 / 赞助方" prop="sponsor">
            <el-input v-model="projectForm.sponsor" placeholder="请输入申办方或赞助方名称" clearable />
          </el-form-item>
        </section>

        <section class="form-section">
          <h3 class="form-section-title">方案信息</h3>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="方案编号" prop="protocolNumber">
                <el-input v-model="projectForm.protocolNumber" placeholder="请输入方案编号" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="方案名称" prop="protocolName">
                <el-input v-model="projectForm.protocolName" placeholder="请输入方案名称" clearable />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="方案版本" prop="protocolVersion">
                <el-input v-model="projectForm.protocolVersion" placeholder="如：1.0" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="方案版本日期" prop="protocolDate">
                <el-date-picker
                  v-model="projectForm.protocolDate"
                  type="date"
                  placeholder="选择日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  class="full-width"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section form-section-last">
          <h3 class="form-section-title">标准类型</h3>
          <el-form-item prop="standardTypes" class="standard-types-item">
            <el-checkbox-group v-model="projectForm.standardTypes" class="standard-type-group">
              <el-checkbox value="SDTM" border class="standard-type-chip">SDTM</el-checkbox>
              <el-checkbox value="ADAM" border class="standard-type-chip">ADAM</el-checkbox>
              <el-checkbox value="SEND" border class="standard-type-chip">SEND</el-checkbox>
            </el-checkbox-group>
            <p class="form-hint">至少选择一种标准，将影响后续 Spec 与 Define 的处理规则</p>
          </el-form-item>
        </section>
      </el-form>

      <template #footer>
        <div class="create-dialog-footer">
          <el-button class="footer-cancel-btn" @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" class="footer-submit-btn" @click="confirmCreate" :loading="createLoading">
            创建项目
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 重置系统确认 -->
    <el-dialog
      v-model="resetDialogVisible"
      width="480px"
      class="reset-system-dialog"
      align-center
      destroy-on-close
      :close-on-click-modal="!resetting"
      :show-close="!resetting"
    >
      <template #header>
        <div class="reset-dialog-header">
          <div class="reset-dialog-icon" aria-hidden="true">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div>
            <h2 class="reset-dialog-title">重置系统</h2>
            <p class="reset-dialog-desc">此操作不可撤销，请确认后再继续</p>
          </div>
        </div>
      </template>

      <div class="reset-dialog-body">
        <p class="reset-dialog-lead">将清空以下数据：</p>
        <ul class="reset-dialog-list">
          <li>所有项目的 Spec、VLM、CodeList、Methods、Comments、Datasets、Pages 等 18 张业务表</li>
          <li>已上传的文件及项目文件夹</li>
        </ul>
        <p class="reset-dialog-warn">重置后项目列表将恢复为空，需重新创建项目。</p>
      </div>

      <template #footer>
        <div class="reset-dialog-footer">
          <el-button class="footer-cancel-btn" :disabled="resetting" @click="resetDialogVisible = false">取消</el-button>
          <el-button type="danger" class="footer-danger-btn" :loading="resetting" @click="confirmResetSystem">
            确定重置
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '@/axios'
import {
  ApiGetProjects,
  ApiCreateProject,
  ApiCopyProject,
  ApiDeleteProject
} from '@/api'
import { formatDate } from '@/utils/format'

const router = useRouter()
const projectList = ref([])
const dialogVisible = ref(false)
const resetDialogVisible = ref(false)
const createLoading = ref(false)
const resetting = ref(false)
const projectFormRef = ref(null)
const searchText = ref('')

const projectForm = ref({
  projectId: '',
  projectName: '',
  protocolNumber: '',
  protocolName: '',
  protocolVersion: '',
  protocolDate: '',
  sponsor: '',
  standardTypes: []
})

const formRules = {
  projectId: [
    { required: true, message: '请输入项目ID', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '项目ID只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  protocolNumber: [{ required: true, message: '请输入方案编号', trigger: 'blur' }],
  protocolName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  sponsor: [{ required: true, message: '请输入申办方/赞助方', trigger: 'blur' }],
  standardTypes: [{ required: true, type: 'array', min: 1, message: '请至少选择一种标准类型', trigger: 'change' }]
}

const filteredProjects = computed(() => {
  if (!searchText.value) return projectList.value
  const q = searchText.value.toLowerCase()
  return projectList.value.filter(p =>
    (p.projectName || '').toLowerCase().includes(q) ||
    (p.projectId || '').toLowerCase().includes(q) ||
    (p.protocolNumber || '').toLowerCase().includes(q) ||
    (p.sponsor || '').toLowerCase().includes(q)
  )
})

const sdtmCount = computed(() =>
  projectList.value.filter(p => p.standardType && p.standardType.includes('SDTM')).length
)
const adamCount = computed(() =>
  projectList.value.filter(p => p.standardType && p.standardType.includes('ADAM')).length
)

const avatarColors = [
  'linear-gradient(135deg, #6366f1, #818cf8)',
  'linear-gradient(135deg, #10b981, #34d399)',
  'linear-gradient(135deg, #f59e0b, #fbbf24)',
  'linear-gradient(135deg, #ef4444, #f87171)',
  'linear-gradient(135deg, #8b5cf6, #a78bfa)',
  'linear-gradient(135deg, #06b6d4, #22d3ee)',
  'linear-gradient(135deg, #ec4899, #f472b6)',
]

const getAvatarColor = (id: string) => {
  let hash = 0
  for (let i = 0; i < (id || '').length; i++) {
    hash = id.charCodeAt(i) + ((hash << 5) - hash)
  }
  return avatarColors[Math.abs(hash) % avatarColors.length]
}

const getStandardTypes = (type: string) => {
  if (!type) return []
  return type.split(',').map(t => t.trim()).filter(Boolean)
}

const getTagType = (type: string) => {
  const map: Record<string, string> = { SDTM: '', ADAM: 'success', SEND: 'warning' }
  return map[type.toUpperCase()] || 'info'
}

const getProjectList = async () => {
  try {
    const user = localStorage.getItem('user')
    const username = user ? JSON.parse(user).username : ''
    if (!username) {
      ElMessage.warning('用户未登录，请先登录')
      router.push('/login')
      return
    }
    const res = await ApiGetProjects(username)
    if (res && res.data && res.data.success) {
      projectList.value = res.data.data || []
    } else {
      ElMessage.error(res?.data?.message || '获取项目列表失败')
    }
  } catch (error) {
    ElMessage.error('获取项目列表失败：' + (error.message || '网络错误'))
  }
}

const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''

const resetSystem = async () => {
  resetting.value = true
  try {
    const res = await axios.post(`${baseUrl}/project-management/reset-all`)
    if (res.data?.success) {
      const d = res.data.data || {}
      const clearedTables = Object.keys(d).filter(k => k.startsWith('cleared_')).length
      const skippedTables = Object.keys(d).filter(k => k.startsWith('skip_')).length
      ElMessage({
        type: 'success',
        message: `系统重置完成: 清空 ${clearedTables} 张数据表，删除 ${d.upload_files_deleted || 0} 个上传文件和 ${d.project_files_deleted || 0} 个项目文件` + (skippedTables > 0 ? `（${skippedTables} 张表跳过）` : ''),
        duration: 5000
      })
      resetDialogVisible.value = false
      getProjectList()
      return true
    }
    ElMessage.error(res.data?.message || '重置失败')
    return false
  } catch (e) {
    ElMessage.error('重置失败: ' + (e.response?.data?.message || e.message))
    return false
  } finally {
    resetting.value = false
  }
}

const confirmResetSystem = () => {
  resetSystem()
}

const goToCreateProject = () => {
  projectForm.value = {
    projectId: '', projectName: '', protocolNumber: '', protocolName: '',
    protocolVersion: '', protocolDate: '', sponsor: '', standardTypes: []
  }
  dialogVisible.value = true
}

const confirmCreate = async () => {
  if (!projectFormRef.value) return
  try {
    await projectFormRef.value.validate()
    createLoading.value = true
    const user = localStorage.getItem('user')
    const username = user ? JSON.parse(user).username : ''
    if (!username) { ElMessage.error('用户未登录'); return }

    const res = await ApiCreateProject({ ...projectForm.value, username })
    if (res.data.success) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      await getProjectList()
    } else {
      ElMessage.error(res.data.message || '创建失败')
    }
  } catch (error) {
    ElMessage.error('创建失败')
  } finally {
    createLoading.value = false
  }
}

const enterProject = (project: { projectId: string }) => {
  router.push({ name: 'ProjectInfo', params: { projectId: project.projectId } })
}

const deleteProject = async (project: { projectId: string }) => {
  try {
    await ElMessageBox.confirm('确定要删除该项目吗？', '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })

    let deleteFiles = false
    try {
      await ElMessageBox.confirm('是否同时删除项目对应的文件夹？', '删除文件夹确认', {
        confirmButtonText: '删除文件夹', cancelButtonText: '仅删除项目记录',
        type: 'warning', distinguishCancelAndClose: true
      })
      deleteFiles = true
    } catch (action) {
      if (action === 'cancel') { deleteFiles = false }
      else { return }
    }

    const res = await ApiDeleteProject({ projectId: project.projectId, deleteFiles } as any)
    if (res.data.success) {
      ElMessage.success(deleteFiles ? '项目及文件夹删除成功' : '项目删除成功')
      getProjectList()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  const user = localStorage.getItem('user')
  if (!user) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  getProjectList()
})
</script>

<style scoped lang="less">
.project-list-page {
  max-width: var(--saas-content-max-width);
  margin: 0 auto;
  padding: var(--saas-content-padding);
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;

  h1 {
    font-size: 24px;
    font-weight: 700;
    color: var(--saas-text-primary);
    margin: 0 0 4px 0;
    letter-spacing: -0.02em;
  }

  .header-desc {
    font-size: 14px;
    color: var(--saas-text-secondary);
    margin: 0;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .reset-system-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 40px;
    padding: 0 16px;
    border: 1px solid var(--saas-danger-border);
    border-radius: var(--saas-radius-md);
    background: var(--saas-danger-bg);
    color: var(--saas-danger);
    font-size: 14px;
    font-weight: 500;
    font-family: var(--saas-font-rules, inherit);
    cursor: pointer;
    transition: background var(--saas-transition), border-color var(--saas-transition), color var(--saas-transition);

    svg {
      flex-shrink: 0;
    }

    &:hover:not(:disabled) {
      background: #fee2e2;
      border-color: #fca5a5;
      color: #dc2626;
    }

    &:disabled {
      opacity: 0.65;
      cursor: not-allowed;
    }
  }

  .create-btn {
    height: 40px;
    padding: 0 20px;
    border-radius: var(--saas-radius-md);
    font-weight: 600;
    font-size: 14px;
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  .stat-card {
    background: var(--saas-bg-card);
    border: 1px solid var(--saas-border-light);
    border-radius: var(--saas-radius-lg);
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    transition: all var(--saas-transition);

    &:hover {
      box-shadow: var(--saas-shadow-sm);
    }

    .stat-icon {
      width: 44px;
      height: 44px;
      border-radius: var(--saas-radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .stat-info {
      display: flex;
      flex-direction: column;

      .stat-num {
        font-size: 24px;
        font-weight: 700;
        color: var(--saas-text-primary);
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: var(--saas-text-secondary);
        margin-top: 2px;
      }
    }
  }
}

.filter-bar {
  margin-bottom: 20px;

  .search-box {
    position: relative;
    max-width: 400px;

    .search-icon {
      position: absolute;
      left: 14px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--saas-text-tertiary);
    }

    input {
      width: 100%;
      height: 40px;
      padding: 0 14px 0 42px;
      border: 1.5px solid var(--saas-border);
      border-radius: var(--saas-radius-md);
      font-size: 14px;
      font-family: inherit;
      color: var(--saas-text-primary);
      background: var(--saas-bg-card);
      outline: none;
      transition: all var(--saas-transition);

      &::placeholder { color: var(--saas-text-tertiary); }

      &:focus {
        border-color: var(--saas-primary);
        box-shadow: 0 0 0 3px var(--saas-primary-bg);
      }
    }
  }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    margin-bottom: 20px;
  }

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: var(--saas-text-primary);
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: var(--saas-text-secondary);
  }
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;

  .project-card {
    background: var(--saas-bg-card);
    border: 1px solid var(--saas-border-light);
    border-radius: var(--saas-radius-lg);
    padding: 20px;
    cursor: pointer;
    transition: all var(--saas-transition);

    &:hover {
      border-color: var(--saas-primary-lighter);
      box-shadow: var(--saas-shadow-md);
      transform: translateY(-2px);
    }

    .card-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 14px;

      .project-avatar {
        width: 40px;
        height: 40px;
        border-radius: var(--saas-radius-md);
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--saas-text-inverse);
        font-weight: 700;
        font-size: 16px;
      }

      .more-btn {
        width: 28px;
        height: 28px;
        border: none;
        background: transparent;
        border-radius: var(--saas-radius-sm);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--saas-text-tertiary);
        transition: all var(--saas-transition);

        &:hover {
          background: var(--saas-bg-hover);
          color: var(--saas-text-secondary);
        }
      }
    }

    .project-name {
      font-size: 16px;
      font-weight: 600;
      color: var(--saas-text-primary);
      margin: 0 0 4px 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .project-id {
      font-size: 12px;
      color: var(--saas-text-tertiary);
      margin: 0 0 14px 0;
      font-family: inherit;
    }

    .card-meta {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin-bottom: 16px;

      .meta-item {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .meta-label {
          font-size: 11px;
          color: var(--saas-text-tertiary);
          text-transform: uppercase;
          letter-spacing: 0.05em;
          font-weight: 500;
        }

        .meta-value {
          font-size: 13px;
          color: var(--saas-text-secondary);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 14px;
      border-top: 1px solid var(--saas-border-light);

      .tag-group {
        display: flex;
        gap: 6px;
      }

      .card-date {
        font-size: 12px;
        color: var(--saas-text-tertiary);
      }
    }
  }
}

.btn-icon {
  margin-right: var(--saas-space-1);
}

.danger-text {
  color: var(--saas-danger);
}

.full-width {
  width: 100%;
}

.variant-primary {
  background: var(--saas-primary-bg);
  color: var(--saas-primary);
}

.variant-success {
  background: var(--saas-success-bg);
  color: var(--saas-success);
}

.variant-warning {
  background: var(--saas-warning-bg);
  color: var(--saas-warning);
}
</style>

<style lang="less">
/* 创建项目弹窗（teleport 到 body，需全局样式） */
.create-project-dialog {
  font-family: var(--saas-font-rules, 'Microsoft YaHei', '微软雅黑', sans-serif);

  .el-dialog__header {
    margin: 0;
    padding: 24px 28px 0;
    border-bottom: none;
  }

  .el-dialog__headerbtn {
    top: 20px;
    right: 20px;
    width: 32px;
    height: 32px;
  }

  .el-dialog__body {
    padding: 20px 28px 8px;
  }

  .el-dialog__footer {
    padding: 16px 28px 24px;
    border-top: 1px solid var(--saas-border-light);
  }

  .create-dialog-header {
    padding-right: 32px;

    .create-dialog-title {
      margin: 0 0 6px;
      font-size: 20px;
      font-weight: 600;
      color: var(--saas-text-primary);
      letter-spacing: 0;
      font-family: inherit;
    }

    .create-dialog-desc {
      margin: 0;
      font-size: 13px;
      line-height: 1.5;
      color: var(--saas-text-secondary);
      font-family: inherit;
    }
  }

  .create-project-form {
    font-family: inherit;

    .form-section {
      margin-bottom: 20px;
      padding-bottom: 20px;
      border-bottom: 1px solid var(--saas-border-light);

      &.form-section-last {
        margin-bottom: 0;
        padding-bottom: 0;
        border-bottom: none;
      }
    }

    .form-section-title {
      margin: 0 0 14px;
      font-size: 13px;
      font-weight: 600;
      color: var(--saas-text-primary);
      font-family: inherit;
    }

    .el-form-item {
      margin-bottom: 16px;

      .el-form-item__label {
        padding-bottom: 6px;
        font-size: 13px;
        font-weight: 500;
        color: var(--saas-text-secondary);
        line-height: 1.4;
        font-family: inherit;

        &::before {
          color: var(--saas-danger);
        }
      }
    }

    .el-input__wrapper,
    .el-textarea__inner {
      border-radius: var(--saas-radius-md);
      font-family: inherit;
    }

    .standard-types-item {
      margin-bottom: 0;

      .el-form-item__content {
        flex-direction: column;
        align-items: flex-start;
      }
    }

    .standard-type-group {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      width: 100%;
    }

    .standard-type-chip {
      margin-right: 0 !important;
      height: 40px;
      padding: 0 20px;
      border-radius: var(--saas-radius-md) !important;

      .el-checkbox__label {
        font-size: 14px;
        font-weight: 600;
        font-family: inherit;
        padding-left: 8px;
      }

      &.is-checked {
        background: var(--saas-primary-bg);
        border-color: var(--saas-primary-lighter) !important;

        .el-checkbox__label {
          color: var(--saas-primary-dark);
        }
      }
    }

    .form-hint {
      margin: 10px 0 0;
      font-size: 12px;
      line-height: 1.5;
      color: var(--saas-text-tertiary);
      font-family: inherit;
    }
  }

  .create-dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;

    .footer-cancel-btn,
    .footer-submit-btn {
      min-width: 96px;
      height: 40px;
      border-radius: var(--saas-radius-md);
      font-size: 14px;
      font-weight: 500;
      font-family: inherit;
    }

    .footer-cancel-btn {
      border-color: var(--saas-border);
      color: var(--saas-text-secondary);

      &:hover {
        color: var(--saas-text-primary);
        border-color: var(--saas-text-tertiary);
        background: var(--saas-bg-hover);
      }
    }

    .footer-submit-btn {
      font-weight: 600;
      padding: 0 24px;
    }
  }
}

.reset-system-dialog {
  font-family: var(--saas-font-rules, 'Microsoft YaHei', '微软雅黑', sans-serif);

  .el-dialog__header {
    margin: 0;
    padding: 24px 28px 0;
  }

  .el-dialog__body {
    padding: 16px 28px 8px;
  }

  .el-dialog__footer {
    padding: 16px 28px 24px;
    border-top: 1px solid var(--saas-border-light);
  }

  .reset-dialog-header {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding-right: 28px;
  }

  .reset-dialog-icon {
    width: 44px;
    height: 44px;
    border-radius: var(--saas-radius-md);
    background: var(--saas-danger-bg);
    color: var(--saas-danger);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .reset-dialog-title {
    margin: 0 0 4px;
    font-size: 18px;
    font-weight: 600;
    color: var(--saas-text-primary);
    font-family: inherit;
  }

  .reset-dialog-desc {
    margin: 0;
    font-size: 13px;
    color: var(--saas-text-secondary);
    font-family: inherit;
  }

  .reset-dialog-body {
    font-family: inherit;
  }

  .reset-dialog-lead {
    margin: 0 0 10px;
    font-size: 14px;
    font-weight: 500;
    color: var(--saas-text-primary);
  }

  .reset-dialog-list {
    margin: 0 0 14px;
    padding-left: 20px;
    font-size: 13px;
    line-height: 1.65;
    color: var(--saas-text-secondary);

    li + li {
      margin-top: 6px;
    }
  }

  .reset-dialog-warn {
    margin: 0;
    padding: 10px 12px;
    border-radius: var(--saas-radius-md);
    background: var(--saas-warning-bg);
    font-size: 13px;
    line-height: 1.5;
    color: #92400e;
  }

  .reset-dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;

    .footer-cancel-btn,
    .footer-danger-btn {
      min-width: 96px;
      height: 40px;
      border-radius: var(--saas-radius-md);
      font-size: 14px;
      font-family: inherit;
    }

    .footer-danger-btn {
      font-weight: 600;
    }
  }
}
</style>
