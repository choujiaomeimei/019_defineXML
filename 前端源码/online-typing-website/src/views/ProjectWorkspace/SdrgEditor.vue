<template>
  <div class="sdrg-page">
    <div class="sdrg-container">
      <!-- 页头 -->
      <div class="section-header">
        <div class="section-title-group">
          <h3>SDRG 撰写</h3>
          <span class="section-desc">Study Data Reviewer's Guide 章节编辑与导出</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" :loading="exporting" @click="exportSdrg" round>
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" class="btn-icon">
              <path d="M8 2V10M8 10L5 7M8 10L11 7M2 12V13C2 13.6 2.4 14 3 14H13C13.6 14 14 13.6 14 13V12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            导出 Word
          </el-button>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div v-if="!loading && sections.length > 0" class="stats-row">
        <div class="stat-card">
          <div class="stat-icon variant-primary">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M4 3H16V17H4V3Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
              <path d="M7 7H13M7 10H13M7 13H10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ sections.length }}</span>
            <span class="stat-label">章节总数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon variant-success">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/>
              <path d="M7 10L9.5 12.5L13.5 7.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ filledCount }}</span>
            <span class="stat-label">已填写</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon variant-warning">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/>
              <path d="M10 6V10.5L13 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ lastUpdateDisplay }}</span>
            <span class="stat-label">最后更新</span>
          </div>
        </div>
      </div>

      <!-- 骨架屏 -->
      <el-skeleton v-if="loading" :rows="12" animated />

      <!-- 空状态 -->
      <div v-else-if="sections.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
            <rect x="12" y="8" width="40" height="48" rx="4" stroke="#D1D5DB" stroke-width="2"/>
            <path d="M22 24H42M22 32H36M22 40H40" stroke="#D1D5DB" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <h4>暂无章节数据</h4>
        <p>加载 SDRG 章节时出错，请刷新重试</p>
        <el-button type="primary" round @click="loadSections">重新加载</el-button>
      </div>

      <!-- 主编辑区：左导航 + 右编辑 -->
      <div v-else class="sdrg-workspace">
        <!-- 左侧章节导航 -->
        <div class="chapter-nav">
          <div class="chapter-nav-title">章节列表</div>
          <div class="chapter-list">
            <button
              v-for="section in sections"
              :key="section.sectionKey"
              class="chapter-item"
              :class="{ active: activeSection === section.sectionKey }"
              @click="selectSection(section.sectionKey)"
            >
              <span class="chapter-status" :class="{ filled: isFilled(section) }">
                <svg v-if="isFilled(section)" width="10" height="10" viewBox="0 0 10 10" fill="none">
                  <path d="M2 5L4.5 7.5L8 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              <span class="chapter-label">{{ section.sectionTitle }}</span>
            </button>
          </div>
        </div>

        <!-- 右侧编辑面板 -->
        <div class="editor-panel">
          <template v-if="currentSection">
            <div class="op-card">
              <div class="op-card-title">
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                  <path d="M10 1.5H4.5C3.4 1.5 2.5 2.4 2.5 3.5V14.5C2.5 15.6 3.4 16.5 4.5 16.5H13.5C14.6 16.5 15.5 15.6 15.5 14.5V7L10 1.5Z" stroke="var(--saas-primary)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M10 1.5V7H15.5" stroke="var(--saas-primary)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                {{ currentSection.sectionTitle }}
                <span class="section-order">第 {{ currentSection.sectionOrder }} 章</span>
              </div>
              <div class="op-card-body">
                <div class="editor-field">
                  <label class="field-label">章节标题</label>
                  <el-input
                    v-model="currentSection.sectionTitle"
                    placeholder="请输入章节标题"
                  />
                </div>
                <div class="editor-field">
                  <label class="field-label">章节内容</label>
                  <!-- 富文本工具栏 -->
                  <div v-if="editor" class="rich-toolbar">
                    <div class="toolbar-group">
                      <button
                        class="tb-btn"
                        :class="{ active: editor.isActive('heading', { level: 2 }) }"
                        @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
                        title="标题"
                      >H2</button>
                      <button
                        class="tb-btn"
                        :class="{ active: editor.isActive('heading', { level: 3 }) }"
                        @click="editor.chain().focus().toggleHeading({ level: 3 }).run()"
                        title="小标题"
                      >H3</button>
                    </div>
                    <span class="toolbar-sep" />
                    <div class="toolbar-group">
                      <button
                        class="tb-btn"
                        :class="{ active: editor.isActive('bold') }"
                        @click="editor.chain().focus().toggleBold().run()"
                        title="加粗 (Ctrl+B)"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M4 2.5H9.5C11.1 2.5 12.5 3.9 12.5 5.5C12.5 7.1 11.1 8.5 9.5 8.5H4V2.5Z" stroke="currentColor" stroke-width="2"/><path d="M4 8.5H10.5C12.1 8.5 13.5 9.9 13.5 11.5C13.5 13.1 12.1 14.5 10.5 14.5H4V8.5Z" stroke="currentColor" stroke-width="2"/></svg>
                      </button>
                      <button
                        class="tb-btn"
                        :class="{ active: editor.isActive('italic') }"
                        @click="editor.chain().focus().toggleItalic().run()"
                        title="斜体 (Ctrl+I)"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M6 2.5H12M4 13.5H10M9.5 2.5L6.5 13.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                      </button>
                      <button
                        class="tb-btn"
                        :class="{ active: editor.isActive('underline') }"
                        @click="editor.chain().focus().toggleUnderline().run()"
                        title="下划线 (Ctrl+U)"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M4 2.5V8C4 10.2 5.8 12 8 12C10.2 12 12 10.2 12 8V2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M3 14.5H13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                      </button>
                      <button
                        class="tb-btn"
                        :class="{ active: editor.isActive('highlight') }"
                        @click="editor.chain().focus().toggleHighlight().run()"
                        title="高亮"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><rect x="2" y="10" width="12" height="4" rx="1" fill="currentColor" opacity="0.3"/><path d="M3 2.5L8 9L13 2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                      </button>
                    </div>
                    <span class="toolbar-sep" />
                    <div class="toolbar-group">
                      <button
                        class="tb-btn"
                        :class="{ active: editor.isActive('bulletList') }"
                        @click="editor.chain().focus().toggleBulletList().run()"
                        title="无序列表"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><circle cx="3" cy="4" r="1.5" fill="currentColor"/><circle cx="3" cy="8" r="1.5" fill="currentColor"/><circle cx="3" cy="12" r="1.5" fill="currentColor"/><path d="M7 4H14M7 8H14M7 12H14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                      </button>
                      <button
                        class="tb-btn"
                        :class="{ active: editor.isActive('orderedList') }"
                        @click="editor.chain().focus().toggleOrderedList().run()"
                        title="有序列表"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><text x="1" y="5.5" font-size="5" font-weight="700" fill="currentColor">1</text><text x="1" y="9.5" font-size="5" font-weight="700" fill="currentColor">2</text><text x="1" y="13.5" font-size="5" font-weight="700" fill="currentColor">3</text><path d="M7 4H14M7 8H14M7 12H14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                      </button>
                    </div>
                    <span class="toolbar-sep" />
                    <div class="toolbar-group">
                      <button
                        class="tb-btn"
                        @click="insertTable"
                        title="插入表格"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><rect x="1.5" y="1.5" width="13" height="13" rx="1.5" stroke="currentColor" stroke-width="1.5"/><path d="M1.5 5.5H14.5M1.5 10.5H14.5M6 1.5V14.5M11 1.5V14.5" stroke="currentColor" stroke-width="1"/></svg>
                      </button>
                      <button
                        class="tb-btn"
                        @click="editor.chain().focus().toggleBlockquote().run()"
                        :class="{ active: editor.isActive('blockquote') }"
                        title="引用"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M3 4H7V8H5L3 12V8" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M9 4H13V8H11L9 12V8" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
                      </button>
                      <button
                        class="tb-btn"
                        @click="editor.chain().focus().setHorizontalRule().run()"
                        title="分隔线"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M2 8H14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
                      </button>
                    </div>
                    <span class="toolbar-sep" />
                    <div class="toolbar-group">
                      <button
                        class="tb-btn"
                        @click="editor.chain().focus().undo().run()"
                        :disabled="!editor.can().undo()"
                        title="撤销 (Ctrl+Z)"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M3 6H10C12.2 6 14 7.8 14 10C14 12.2 12.2 14 10 14H7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M6 3L3 6L6 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                      </button>
                      <button
                        class="tb-btn"
                        @click="editor.chain().focus().redo().run()"
                        :disabled="!editor.can().redo()"
                        title="重做 (Ctrl+Shift+Z)"
                      >
                        <svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M13 6H6C3.8 6 2 7.8 2 10C2 12.2 3.8 14 6 14H9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M10 3L13 6L10 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                      </button>
                    </div>
                  </div>
                  <!-- 富文本编辑区 -->
                  <div class="rich-editor-wrap">
                    <editor-content :editor="editor" class="rich-editor" />
                  </div>
                </div>
                <div class="editor-actions">
                  <el-button
                    type="primary"
                    :loading="currentSection._saving"
                    @click="saveSection(currentSection)"
                    round
                  >
                    <svg width="14" height="14" viewBox="0 0 16 16" fill="none" class="btn-icon">
                      <path d="M13 5V13C13 13.6 12.6 14 12 14H4C3.4 14 3 13.6 3 13V3C3 2.4 3.4 2 4 2H10L13 5Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M10 2V5H13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M6 9H10M6 11.5H9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                    </svg>
                    保存章节
                  </el-button>
                  <span v-if="currentSection.updatedBy" class="update-meta">
                    <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                      <circle cx="6" cy="6" r="5" stroke="currentColor" stroke-width="1"/>
                      <path d="M6 3.5V6L7.5 7" stroke="currentColor" stroke-width="1" stroke-linecap="round"/>
                    </svg>
                    {{ currentSection.updatedBy }} 于 {{ formatTime(currentSection.updatedTime) }}
                  </span>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="editor-placeholder">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect x="8" y="6" width="32" height="36" rx="4" stroke="#D1D5DB" stroke-width="2"/>
              <path d="M16 18H32M16 24H28M16 30H30" stroke="#D1D5DB" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <p>请从左侧选择章节进行编辑</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios, { exportService } from '@/axios'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Underline from '@tiptap/extension-underline'
import Highlight from '@tiptap/extension-highlight'
import { Table, TableRow, TableCell, TableHeader } from '@tiptap/extension-table'

const route = useRoute()
const projectId = route.params.projectId

const sections = ref([])
const activeSection = ref('')
const loading = ref(true)
const exporting = ref(false)
let skipNextWatch = false

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: { levels: [2, 3] },
    }),
    Placeholder.configure({
      placeholder: '请输入章节内容…\n支持 Markdown 快捷键：# 标题、** 加粗、- 列表等',
    }),
    Underline,
    Highlight,
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader,
  ],
  content: '',
  onUpdate: ({ editor: ed }) => {
    const section = currentSection.value
    if (section) {
      skipNextWatch = true
      section.contentText = ed.getHTML()
    }
  },
})

const currentSection = computed(() =>
  sections.value.find(s => s.sectionKey === activeSection.value) || null
)

const filledCount = computed(() =>
  sections.value.filter(s => s.contentText && s.contentText.replace(/<[^>]*>/g, '').trim().length > 0).length
)

const lastUpdateDisplay = computed(() => {
  const times = sections.value
    .map(s => s.updatedTime)
    .filter(Boolean)
    .map(t => new Date(t).getTime())
  if (times.length === 0) return '-'
  const latest = new Date(Math.max(...times))
  const now = new Date()
  const diffMs = now - latest
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} 小时前`
  return latest.toLocaleDateString('zh-CN')
})

const isFilled = (section) =>
  section.contentText && section.contentText.replace(/<[^>]*>/g, '').trim().length > 0

const selectSection = (key) => {
  activeSection.value = key
}

const insertTable = () => {
  editor.value?.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
}

watch(currentSection, (sec) => {
  if (skipNextWatch) {
    skipNextWatch = false
    return
  }
  if (editor.value && sec) {
    editor.value.commands.setContent(sec.contentText || '')
  } else if (editor.value) {
    editor.value.commands.setContent('')
  }
})

const loadSections = async () => {
  loading.value = true
  try {
    const res = await axios.get('/sdrg/sections', { params: { projectId } })
    if (res.data.success) {
      sections.value = res.data.data.map(s => ({ ...s, _saving: false }))
      if (sections.value.length > 0 && !activeSection.value) {
        activeSection.value = sections.value[0].sectionKey
      }
    }
  } catch (e) {
    ElMessage.error('加载SDRG章节失败')
  } finally {
    loading.value = false
  }
}

const saveSection = async (section) => {
  section._saving = true
  try {
    const res = await axios.post('/sdrg/section', {
      projectId,
      sectionKey: section.sectionKey,
      sectionTitle: section.sectionTitle,
      sectionOrder: section.sectionOrder,
      contentText: section.contentText,
    })
    if (res.data.success) {
      ElMessage.success('章节保存成功')
      Object.assign(section, res.data.data)
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    section._saving = false
  }
}

const exportSdrg = async () => {
  exporting.value = true
  try {
    const res = await exportService.get('/sdrg/export', {
      params: { projectId },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `SDRG_${projectId}.docx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('SDRG导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadSections)

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped lang="less">
.sdrg-page {
  padding: var(--saas-content-padding);
}

.sdrg-container {
  max-width: var(--saas-content-max-width);
  margin: 0 auto;
}

/* ===== 页头 ===== */
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
    flex-shrink: 0;
  }
}

/* ===== 统计卡片 ===== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--saas-space-4);
  margin-bottom: var(--saas-space-5);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--saas-space-3);
  padding: var(--saas-space-4) var(--saas-space-5);
  background: var(--saas-bg-card);
  border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg);
  transition: box-shadow var(--saas-transition);

  &:hover {
    box-shadow: var(--saas-shadow-sm);
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    border-radius: var(--saas-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &.variant-primary {
      background: var(--saas-primary-bg);
      color: var(--saas-primary);
    }
    &.variant-success {
      background: var(--saas-success-bg);
      color: var(--saas-success);
    }
    &.variant-warning {
      background: var(--saas-warning-bg);
      color: var(--saas-warning);
    }
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .stat-value {
    font-size: var(--saas-text-lg);
    font-weight: var(--saas-font-bold);
    color: var(--saas-text-primary);
    line-height: 1.2;
  }

  .stat-label {
    font-size: var(--saas-text-xs);
    color: var(--saas-text-tertiary);
  }
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 80px 20px;

  .empty-icon {
    margin-bottom: var(--saas-space-4);
  }

  h4 {
    font-size: var(--saas-text-lg);
    font-weight: var(--saas-font-semibold);
    color: var(--saas-text-primary);
    margin: 0 0 var(--saas-space-2) 0;
  }

  p {
    font-size: var(--saas-text-sm);
    color: var(--saas-text-tertiary);
    margin: 0 0 var(--saas-space-5) 0;
  }
}

/* ===== 主工作区 ===== */
.sdrg-workspace {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: var(--saas-space-5);
  align-items: flex-start;
}

/* ===== 左侧章节导航 ===== */
.chapter-nav {
  background: var(--saas-bg-card);
  border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg);
  overflow: hidden;
  position: sticky;
  top: var(--saas-space-5);
}

.chapter-nav-title {
  padding: 14px 16px;
  font-size: 13px;
  font-weight: var(--saas-font-semibold);
  color: var(--saas-text-primary);
  background: var(--saas-bg-input);
  border-bottom: 1px solid var(--saas-border-light);
}

.chapter-list {
  padding: var(--saas-space-2);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chapter-item {
  display: flex;
  align-items: center;
  gap: var(--saas-space-2);
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: var(--saas-radius-md);
  cursor: pointer;
  transition: all var(--saas-transition);
  text-align: left;
  width: 100%;
  font-family: inherit;

  .chapter-status {
    width: 18px;
    height: 18px;
    border-radius: var(--saas-radius-full);
    border: 1.5px solid var(--saas-border);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    color: transparent;
    transition: all var(--saas-transition);

    &.filled {
      background: var(--saas-success-bg);
      border-color: var(--saas-success);
      color: var(--saas-success);
    }
  }

  .chapter-label {
    font-size: 13px;
    color: var(--saas-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    transition: color var(--saas-transition);
  }

  &:hover {
    background: var(--saas-bg-hover);

    .chapter-label {
      color: var(--saas-text-primary);
    }
  }

  &.active {
    background: var(--saas-primary-bg);

    .chapter-status {
      border-color: var(--saas-primary-lighter);
    }

    .chapter-label {
      color: var(--saas-primary-dark);
      font-weight: var(--saas-font-semibold);
    }
  }
}

/* ===== 右侧编辑面板 ===== */
.editor-panel {
  min-width: 0;
}

.op-card {
  background: var(--saas-bg-card);
  border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg);
  overflow: hidden;

  .op-card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 14px 20px;
    border-bottom: 1px solid var(--saas-border-light);
    font-size: 14px;
    font-weight: var(--saas-font-semibold);
    color: var(--saas-text-primary);
    background: var(--saas-bg-input);

    .section-order {
      margin-left: auto;
      font-size: var(--saas-text-xs);
      font-weight: var(--saas-font-normal);
      color: var(--saas-text-tertiary);
      background: var(--saas-bg-hover);
      padding: 2px 8px;
      border-radius: var(--saas-radius-full);
    }
  }

  .op-card-body {
    padding: 20px;
  }
}

.editor-field {
  margin-bottom: var(--saas-space-4);

  .field-label {
    display: block;
    font-size: 13px;
    font-weight: var(--saas-font-semibold);
    color: var(--saas-text-primary);
    margin-bottom: var(--saas-space-2);
  }
}

/* ===== 富文本工具栏 ===== */
.rich-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 8px;
  background: var(--saas-bg-input);
  border: 1px solid var(--saas-border-light);
  border-bottom: none;
  border-radius: var(--saas-radius-md) var(--saas-radius-md) 0 0;
  flex-wrap: wrap;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.toolbar-sep {
  width: 1px;
  height: 20px;
  background: var(--saas-border);
  margin: 0 4px;
  flex-shrink: 0;
}

.tb-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: var(--saas-radius-sm);
  cursor: pointer;
  color: var(--saas-text-secondary);
  font-size: 11px;
  font-weight: 700;
  font-family: inherit;
  transition: all var(--saas-transition);

  &:hover {
    background: var(--saas-bg-hover);
    color: var(--saas-text-primary);
  }

  &.active {
    background: var(--saas-primary-bg);
    color: var(--saas-primary);
  }

  &:disabled {
    opacity: 0.35;
    cursor: not-allowed;
  }
}

/* ===== 富文本编辑区 ===== */
.rich-editor-wrap {
  border: 1px solid var(--saas-border-light);
  border-radius: 0 0 var(--saas-radius-md) var(--saas-radius-md);
  background: var(--saas-bg-card);
  overflow: hidden;
}

.rich-editor {
  :deep(.tiptap) {
    padding: 16px 20px;
    min-height: 320px;
    max-height: 600px;
    overflow-y: auto;
    outline: none;
    font-size: 14px;
    line-height: 1.7;
    color: var(--saas-text-primary);

    > * + * {
      margin-top: 0.6em;
    }

    h2 {
      font-size: 1.25em;
      font-weight: 700;
      color: var(--saas-text-primary);
      margin-top: 1.2em;
      margin-bottom: 0.4em;
    }

    h3 {
      font-size: 1.1em;
      font-weight: 600;
      color: var(--saas-text-primary);
      margin-top: 1em;
      margin-bottom: 0.3em;
    }

    p {
      margin: 0;
    }

    strong {
      font-weight: 700;
    }

    ul, ol {
      padding-left: 1.5em;
      margin: 0.4em 0;
    }

    li {
      margin: 0.15em 0;
    }

    blockquote {
      border-left: 3px solid var(--saas-primary-lighter);
      padding-left: 1em;
      margin: 0.6em 0;
      color: var(--saas-text-secondary);
      font-style: italic;
    }

    hr {
      border: none;
      border-top: 1px solid var(--saas-border);
      margin: 1em 0;
    }

    mark {
      background: #fef08a;
      border-radius: 2px;
      padding: 0 2px;
    }

    table {
      border-collapse: collapse;
      width: 100%;
      margin: 0.6em 0;

      td, th {
        border: 1px solid var(--saas-border);
        padding: 8px 12px;
        text-align: left;
        vertical-align: top;
        min-width: 80px;
      }

      th {
        background: var(--saas-bg-input);
        font-weight: 600;
        font-size: 13px;
      }

      td {
        font-size: 13px;
      }
    }

    p.is-editor-empty:first-child::before {
      content: attr(data-placeholder);
      float: left;
      color: var(--saas-text-tertiary);
      pointer-events: none;
      height: 0;
      font-style: italic;
    }
  }
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: var(--saas-space-3);
  padding-top: var(--saas-space-3);
  border-top: 1px solid var(--saas-border-light);
}

.update-meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--saas-text-xs);
  color: var(--saas-text-tertiary);
}

.editor-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  background: var(--saas-bg-card);
  border: 1px dashed var(--saas-border);
  border-radius: var(--saas-radius-lg);

  p {
    margin: var(--saas-space-3) 0 0 0;
    font-size: var(--saas-text-sm);
    color: var(--saas-text-tertiary);
  }
}

.btn-icon {
  margin-right: var(--saas-space-1);
}
</style>
