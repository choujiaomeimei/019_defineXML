<template>
  <div class="study-page">
    <div class="study-container">
      <div class="study-card" v-loading="loading">
        <div class="study-table">
          <div class="study-row" v-for="item in studyFields" :key="item.key">
            <div class="study-label">{{ item.label }}</div>
            <div class="study-value">{{ item.value || '-' }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import service from '@/axios'

const props = defineProps({ projectId: String })
const route = useRoute()
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const currentProjectId = computed(() => props.projectId || route.params.projectId)
const loading = ref(false)
const studyData = ref({})

const studyFields = computed(() => [
  { key: 'StudyName', label: 'Study Name', value: studyData.value.StudyName },
  { key: 'StudyDescription', label: 'Study Description', value: studyData.value.StudyDescription },
  { key: 'ProtocolName', label: 'Protocol Name', value: studyData.value.ProtocolName },
  { key: 'StandardName', label: 'Standard Name', value: studyData.value.StandardName },
  { key: 'StandardVersion', label: 'Standard Version', value: studyData.value.StandardVersion },
  { key: 'Language', label: 'Language', value: studyData.value.Language },
])

const loadStudyData = async () => {
  if (!currentProjectId.value) return
  loading.value = true
  try {
    const res = await service.get(`${baseUrl}/study/data`, { params: { projectId: currentProjectId.value } })
    if (res.data?.success && res.data.data) {
      studyData.value = res.data.data
    }
  } catch (e) {
    ElMessage.error('加载 Study 数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadStudyData())
</script>

<style scoped lang="less">
.study-page { padding: var(--saas-content-padding, 24px); }
.study-container { max-width: var(--saas-content-max-width, 960px); margin: 0 auto; }

.section-header {
  display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px;
  .section-title-group {
    h3 { font-size: 18px; font-weight: 600; color: var(--saas-text-primary, #1f2937); margin: 0 0 4px 0; }
    .section-desc { font-size: 13px; color: var(--saas-text-secondary, #6b7280); }
  }
}

.study-card {
  background: var(--saas-bg-card, #fff);
  border: 1px solid var(--saas-border-light, #e5e7eb);
  border-radius: 12px;
  overflow: hidden;
}

.study-table {
  .study-row {
    display: flex;
    border-bottom: 1px solid var(--saas-border-light, #e5e7eb);
    &:last-child { border-bottom: none; }
  }
  .study-label {
    width: 200px;
    min-width: 200px;
    padding: 16px 24px;
    font-size: 13px;
    font-weight: 600;
    color: var(--saas-text-secondary, #6b7280);
    background: var(--saas-bg-input, #f9fafb);
    display: flex;
    align-items: center;
  }
  .study-value {
    flex: 1;
    padding: 16px 24px;
    font-size: 14px;
    color: var(--saas-text-primary, #1f2937);
    display: flex;
    align-items: center;
  }
}
</style>
