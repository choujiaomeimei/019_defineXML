<template>
  <div class="project-info-page">
    <div class="info-container">
      <!-- 标题行 -->
      <div class="section-header">
        <div class="section-title-group">
          <h3>项目基本信息</h3>
          <span class="section-desc">查看和编辑项目的基础配置信息</span>
        </div>
        <div class="header-actions">
          <el-button v-if="!isEditing" @click="startEdit" round>
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" class="btn-icon">
              <path d="M10 1.5L12.5 4M1.5 12.5L2 10L10 2L12 4L4 12L1.5 12.5Z" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            编辑
          </el-button>
          <template v-else>
            <el-button @click="cancelEdit" round>取消</el-button>
            <el-button type="primary" @click="saveProject" :loading="saving" round>保存</el-button>
          </template>
        </div>
      </div>

      <!-- 表单卡片 -->
      <div class="form-card">
        <el-form :model="projectForm" :rules="formRules" ref="projectFormRef" label-position="top">
          <!-- 基本信息 -->
          <div class="form-group">
            <div class="form-group-label">基本信息</div>
            <div class="form-grid">
              <el-form-item label="项目 ID" prop="projectId">
                <el-input v-model="projectForm.projectId" disabled />
                <span class="field-hint">项目 ID 创建后不可修改</span>
              </el-form-item>
              <el-form-item label="项目名称" prop="projectName">
                <el-input v-model="projectForm.projectName" :disabled="!isEditing" placeholder="请输入项目名称" />
              </el-form-item>
            </div>
          </div>

          <div class="form-divider" />

          <!-- 方案信息 -->
          <div class="form-group">
            <div class="form-group-label">方案信息</div>
            <div class="form-grid">
              <el-form-item label="方案编号" prop="protocolNumber">
                <el-input v-model="projectForm.protocolNumber" :disabled="!isEditing" placeholder="请输入方案编号" />
              </el-form-item>
              <el-form-item label="方案名称" prop="protocolName">
                <el-input v-model="projectForm.protocolName" :disabled="!isEditing" placeholder="请输入方案名称" />
              </el-form-item>
              <el-form-item label="方案版本" prop="protocolVersion">
                <el-input v-model="projectForm.protocolVersion" :disabled="!isEditing" placeholder="如：1.0" />
              </el-form-item>
              <el-form-item label="方案版本日期" prop="protocolDate">
                <el-date-picker
                  v-model="projectForm.protocolDate"
                  type="date"
                  :disabled="!isEditing"
                  placeholder="选择日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  class="full-width"
                />
              </el-form-item>
            </div>
          </div>

          <div class="form-divider" />

          <!-- 标准与配置 -->
          <div class="form-group">
            <div class="form-group-label">标准与配置</div>
            <div class="form-grid">
              <el-form-item label="标准类型" prop="standardTypes">
                <el-checkbox-group v-model="projectForm.standardTypes" :disabled="!isEditing">
                  <el-checkbox value="SDTM">SDTM</el-checkbox>
                  <el-checkbox value="ADAM">ADAM</el-checkbox>
                  <el-checkbox value="SEND">SEND</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="标准版本" prop="standardVersion">
                <el-select
                  v-model="projectForm.standardVersion"
                  :disabled="!isEditing"
                  placeholder="选择标准版本"
                  class="full-width"
                >
                  <el-option label="SDTM-IG 3.2" value="3.2" />
                  <el-option label="SDTM-IG 3.3" value="3.3" />
                  <el-option label="SDTM-IG 3.4" value="3.4" />
                </el-select>
              </el-form-item>
              <el-form-item label="CT版本 (Controlled Terminology)" prop="ctVersion">
                <el-select
                  v-model="projectForm.ctVersion"
                  :disabled="!isEditing"
                  placeholder="选择CT版本"
                  class="full-width"
                  clearable
                  filterable
                >
                  <el-option
                    v-for="pkg in ctPackages"
                    :key="pkg.id"
                    :label="pkg.label"
                    :value="pkg.releaseDate"
                  />
                </el-select>
                <span class="field-hint">用于Codelist匹配NCI代码的受控术语版本</span>
              </el-form-item>
              <el-form-item label="使用环境" prop="encoding">
                <el-select v-model="projectForm.encoding" :disabled="!isEditing" placeholder="选择编码">
                  <el-option label="UTF-8" value="UTF-8" />
                  <el-option label="GBK" value="GBK" />
                  <el-option label="ISO-8859-1" value="ISO-8859-1" />
                </el-select>
              </el-form-item>
              <el-form-item label="使用语言" prop="language">
                <el-select v-model="projectForm.language" :disabled="!isEditing" placeholder="选择语言">
                  <el-option label="中文" value="CN" />
                  <el-option label="英文" value="EN" />
                </el-select>
              </el-form-item>
            </div>
          </div>

          <div class="form-divider" />

          <!-- 归属信息 -->
          <div class="form-group">
            <div class="form-group-label">归属信息</div>
            <div class="form-grid">
              <el-form-item label="申办方 / 赞助方" prop="sponsor">
                <el-input v-model="projectForm.sponsor" :disabled="!isEditing" placeholder="请输入申办方或赞助方名称" />
              </el-form-item>
              <el-form-item label="创建用户" class="readonly-field">
                <el-input v-model="projectForm.username" disabled />
              </el-form-item>
              <el-form-item label="创建时间" class="readonly-field">
                <el-input v-model="projectForm.createdTime" disabled />
              </el-form-item>
            </div>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ApiGetProjects, ApiUpdateProject } from '@/api'
import service from '@/axios'

const route = useRoute()
const isEditing = ref(false)
const saving = ref(false)
const projectFormRef = ref(null)
const projectId = computed(() => route.params.projectId as string)
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const ctPackages = ref<any[]>([])

const projectForm = ref({
  projectId: '', projectName: '', protocolNumber: '', protocolName: '',
  protocolVersion: '', protocolDate: '', standardTypes: [] as string[], sponsor: '',
  encoding: 'UTF-8', language: 'CN', standardVersion: '', ctVersion: '',
  username: '', createdTime: ''
})

const originalForm = ref({ ...projectForm.value })

const formRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  protocolNumber: [{ required: true, message: '请输入方案编号', trigger: 'blur' }],
  protocolName: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  sponsor: [{ required: true, message: '请输入申办方/赞助方', trigger: 'blur' }],
  standardTypes: [{ required: true, type: 'array', min: 1, message: '请至少选择一种标准类型', trigger: 'change' }]
}

const loadCtPackages = async () => {
  try {
    const res = await service.get(`${baseUrl}/project-config/ct-packages`)
    if (res.data?.success && Array.isArray(res.data.data)) {
      ctPackages.value = res.data.data
    }
  } catch (e) {
    console.error('加载CT版本列表失败:', e)
  }
}

const loadProjectInfo = async () => {
  if (!projectId.value) return
  try {
    const user = localStorage.getItem('user')
    const username = user ? JSON.parse(user).username : ''
    const res = await ApiGetProjects(username)
    if (res.data.success && res.data.data) {
      const projects = Array.isArray(res.data.data) ? res.data.data : [res.data.data]
      const cur = projects.find(p => p.projectId === projectId.value)
      if (cur) {
        let standardTypes: string[] = []
        if (cur.standardType) {
          standardTypes = cur.standardType.split(',').filter((type: string) => type.trim())
        }
        projectForm.value = {
          projectId: cur.projectId || '',
          projectName: cur.projectName || '',
          protocolNumber: cur.protocolNumber || '',
          protocolName: cur.protocolName || '',
          protocolVersion: cur.protocolVersion || '',
          protocolDate: cur.protocolDate || '',
          standardTypes,
          sponsor: cur.sponsor || '',
          encoding: cur.encoding || 'UTF-8',
          language: cur.language || 'CN',
          standardVersion: cur.standardVersion || '',
          ctVersion: cur.ctVersion || '',
          username: cur.username || '',
          createdTime: cur.createdTime || ''
        }
        originalForm.value = { ...projectForm.value }
      }
    }

    // Also load ctVersion and standardVersion from project_config
    try {
      const configRes = await service.get(`${baseUrl}/project-config/get`, { params: { projectId: projectId.value } })
      if (configRes.data?.success && configRes.data.data) {
        const cfg = configRes.data.data
        if (cfg.ct_version) {
          projectForm.value.ctVersion = cfg.ct_version
          originalForm.value.ctVersion = cfg.ct_version
        }
        if (cfg.standard_version) {
          projectForm.value.standardVersion = cfg.standard_version
          originalForm.value.standardVersion = cfg.standard_version
        }
      }
    } catch (e) { /* ignore */ }
  } catch (error) {
    ElMessage.error('加载项目信息失败')
  }
}

const startEdit = () => {
  isEditing.value = true
  originalForm.value = { ...projectForm.value }
}

const cancelEdit = () => {
  isEditing.value = false
  projectForm.value = { ...originalForm.value }
}

const saveProject = async () => {
  if (!projectFormRef.value) return
  try {
    await projectFormRef.value.validate()
    saving.value = true
    const updateData: any = { ...projectForm.value, standardType: projectForm.value.standardTypes.join(',') }
    delete updateData.standardTypes
    delete updateData.createdTime
    const response = await ApiUpdateProject(projectId.value, updateData)
    if (response.data.success) {
      // Also update ctVersion and standardVersion in project_config
      if (projectForm.value.ctVersion !== originalForm.value.ctVersion ||
          projectForm.value.standardVersion !== originalForm.value.standardVersion) {
        try {
          await service.put(`${baseUrl}/project-config/update`, {
            projectId: projectId.value,
            ctVersion: projectForm.value.ctVersion,
            standardVersion: projectForm.value.standardVersion
          })
        } catch (e) { console.error('更新配置失败:', e) }
      }
      ElMessage.success('项目信息更新成功')
      isEditing.value = false
      originalForm.value = { ...projectForm.value }
    } else {
      ElMessage.error(response.data.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error('更新失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadCtPackages()
  loadProjectInfo()
})
</script>

<style scoped lang="less">
.project-info-page {
  padding: var(--saas-content-padding);
}

.info-container {
  max-width: var(--saas-content-max-width);
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--saas-space-5);

  .section-title-group {
    h3 {
      font-size: var(--saas-text-xl);
      font-weight: var(--saas-font-bold);
      color: var(--saas-text-primary);
      margin: 0 0 var(--saas-space-1) 0;
    }

    .section-desc {
      font-size: var(--saas-text-sm);
      color: var(--saas-text-secondary);
    }
  }

  .header-actions {
    display: flex;
    gap: var(--saas-space-2);
  }
}

.form-card {
  background: var(--saas-bg-card);
  border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg);
  padding: 28px;

  .form-group {
    .form-group-label {
      font-size: var(--saas-text-sm);
      font-weight: var(--saas-font-semibold);
      color: var(--saas-text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: var(--saas-space-4);
    }
  }

  .form-divider {
    height: 1px;
    background: var(--saas-border-light);
    margin: var(--saas-space-5) 0 var(--saas-space-6);
  }

  .form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 4px 24px;

    :deep(.el-form-item) {
      margin-bottom: 20px;

      .el-form-item__label {
        font-weight: 600;
        font-size: 13px;
        color: var(--saas-text-primary);
        padding-bottom: 6px;
      }

      .el-input__wrapper {
        border-radius: var(--saas-radius-md);
        box-shadow: 0 0 0 1px var(--saas-border);
        transition: all var(--saas-transition);

        &:hover {
          box-shadow: 0 0 0 1px var(--saas-primary-lighter);
        }

        &.is-focus {
          box-shadow: 0 0 0 1px var(--saas-primary), 0 0 0 3px var(--saas-primary-bg);
        }
      }

      .el-input.is-disabled .el-input__wrapper {
        background: var(--saas-bg-input);
        box-shadow: 0 0 0 1px var(--saas-border-light);
      }
    }

    :deep(.readonly-field) {
      .el-form-item__label {
        color: var(--saas-text-tertiary);
        font-weight: var(--saas-font-normal);
      }
    }
  }

  .field-hint {
    display: block;
    margin-top: var(--saas-space-1);
    font-size: var(--saas-text-xs);
    color: var(--saas-text-tertiary);
  }
}

.btn-icon {
  margin-right: var(--saas-space-1);
}

.full-width {
  width: 100%;
}
</style>
