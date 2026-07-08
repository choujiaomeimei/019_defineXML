<template>
  <div class="workspace">
    <!-- 左侧二级导航 -->
    <aside class="ws-sidebar">
      <!-- 项目标识区 -->
      <div class="ws-sidebar-header">
        <button class="back-btn" @click="goBack" title="返回项目列表">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M10 3L5 8L10 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <div class="project-badge" :style="{ background: avatarGradient }">
          {{ projectInfo.projectName ? projectInfo.projectName.charAt(0) : 'P' }}
        </div>
        <div class="project-brief">
          <span class="project-name" :title="projectInfo.projectName || projectId">{{ projectInfo.projectName || projectId }}</span>
          <span class="project-id">{{ projectId }}</span>
        </div>
      </div>

      <!-- 导航菜单 -->
      <nav class="ws-nav">
        <div class="nav-group-label">工作流程</div>
        <a
          v-for="item in navItems"
          :key="item.key"
          class="ws-nav-item"
          :class="{ active: activeNav === item.key }"
          @click="switchNav(item.key)"
        >
          <div class="nav-icon" v-html="item.icon"></div>
          <div class="nav-content">
            <span class="nav-label">{{ item.label }}</span>
            <span class="nav-desc">{{ item.desc }}</span>
          </div>
          <div v-if="item.badge" class="nav-badge">{{ item.badge }}</div>
        </a>
      </nav>

      
    </aside>

    <!-- 右侧内容区 -->
    <main class="ws-content">
      <router-view :project-id="projectId" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiGetProjects } from '@/api'

const route = useRoute()
const router = useRouter()
const activeNav = ref('info')
const projectInfo = ref({ projectName: '', protocolName: '', sponsor: '' })
const projectId = computed(() => route.params.projectId as string)

const navItems = [
  {
    key: 'info',
    label: '项目信息',
    desc: '基本信息与配置',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M10 9V14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="10" cy="6.75" r="0.75" fill="currentColor"/></svg>'
  },
  {
    key: 'file-upload',
    label: '文件上传',
    desc: '上传与管理文件',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M10 13V4M10 4L6 8M10 4L14 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M3 13V15C3 16.1 3.9 17 5 17H15C16.1 17 17 16.1 17 15V13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  },
  {
    key: 'study',
    label: 'Study',
    desc: '研究级别元数据',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M3 5H17M3 10H17M3 15H12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
  },
  {
    key: 'datasets',
    label: 'Datasets',
    desc: '数据集定义',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><ellipse cx="10" cy="6" rx="7" ry="3" stroke="currentColor" stroke-width="1.5"/><path d="M3 6V10C3 11.7 6.1 13 10 13C13.9 13 17 11.7 17 10V6" stroke="currentColor" stroke-width="1.5"/><path d="M3 10V14C3 15.7 6.1 17 10 17C13.9 17 17 15.7 17 14V10" stroke="currentColor" stroke-width="1.5"/></svg>'
  },
  {
    key: 'variables',
    label: 'Variables',
    desc: 'P21 变量定义',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M11.5 2.5H5.5C4.4 2.5 3.5 3.4 3.5 4.5V15.5C3.5 16.6 4.4 17.5 5.5 17.5H14.5C15.6 17.5 16.5 16.6 16.5 15.5V7.5L11.5 2.5Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M11.5 2.5V7.5H16.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M7 10.5H13M7 13.5H10.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
  },
  {
    key: 'valuelevel',
    label: 'ValueLevel',
    desc: '变量级元数据',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M3.5 4.5H16.5M3.5 8H16.5M3.5 11.5H16.5M3.5 15H16.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="6" cy="4.5" r="1" fill="currentColor"/><circle cx="6" cy="8" r="1" fill="currentColor"/><circle cx="6" cy="11.5" r="1" fill="currentColor"/><circle cx="6" cy="15" r="1" fill="currentColor"/></svg>'
  },
  {
    key: 'codelists',
    label: 'Codelists',
    desc: '代码列表管理',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M5 3.5H15C15.6 3.5 16.5 4.4 16.5 5V15C16.5 15.6 15.6 16.5 15 16.5H5C4.4 16.5 3.5 15.6 3.5 15V5C3.5 4.4 4.4 3.5 5 3.5Z" stroke="currentColor" stroke-width="1.5"/><path d="M3.5 7.5H16.5M3.5 11.5H16.5M10 3.5V16.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
  },
  {
    key: 'dictionaries',
    label: 'Dictionaries',
    desc: '外部字典引用',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M4 3h12v14H4z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M4 3c0 0-1 0-1 1.5S4 6 4 6" stroke="currentColor" stroke-width="1.5"/><path d="M4 17c0 0-1 0-1-1.5S4 14 4 14" stroke="currentColor" stroke-width="1.5"/><path d="M7 7h6M7 10h4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>'
  },
  {
    key: 'methods',
    label: 'Methods',
    desc: '方法定义',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M10 3V17M3 10H17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M5 5L15 15M15 5L5 15" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" opacity="0.4"/><circle cx="10" cy="10" r="7" stroke="currentColor" stroke-width="1.5"/></svg>'
  },
  {
    key: 'comments',
    label: 'Comments',
    desc: '注释定义',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M4 4H16V13H8L4 17V4Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M7 7.5H13M7 10H11" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>'
  },
  {
    key: 'documents',
    label: 'Documents',
    desc: '文档引用',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M11 2H5.5C4.4 2 3.5 2.9 3.5 4v12c0 1.1.9 2 2 2h9c1.1 0 2-.9 2-2V7.5L11 2Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M11 2v5.5h5.5" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>'
  },
  {
    key: 'pages',
    label: 'Pages',
    desc: '变量页码映射',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><rect x="3.5" y="2.5" width="13" height="15" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M7 6H13M7 9H13M7 12H10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="14" cy="14" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M13 14H15M14 13V15" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>'
  },
  {
    key: 'define',
    label: 'Define 制作',
    desc: '生成 Define.xlsx',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><rect x="3.5" y="2.5" width="13" height="15" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M7 7H13M7 10H11M7 13H12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
  },
  {
    key: 'sdrg',
    label: 'SDRG 撰写',
    desc: '审阅指南文档',
    icon: '<svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M11.5 2.5H5.5C4.4 2.5 3.5 3.4 3.5 4.5V15.5C3.5 16.6 4.4 17.5 5.5 17.5H14.5C15.6 17.5 16.5 16.6 16.5 15.5V7.5L11.5 2.5Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M11.5 2.5V7.5H16.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M9 11L7.5 15L10 13.5L12.5 15L11 11" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  }
]

const routeNameMap: Record<string, string> = {
  'info': 'ProjectInfo',
  'file-upload': 'FileUpload',
  'study': 'StudyView',
  'datasets': 'DatasetsView',
  'variables': 'VariablesView',
  'valuelevel': 'ValueLevelView',
  'codelists': 'CodelistsView',
  'dictionaries': 'DictionariesView',
  'methods': 'MethodsView',
  'comments': 'CommentsView',
  'documents': 'DocumentsView',
  'pages': 'PagesView',
  'define': 'DefineXlsx',
  'sdrg': 'SdrgEditor'
}

const reverseRouteMap: Record<string, string> = {
  'ProjectInfo': 'info',
  'FileUpload': 'file-upload',
  'StudyView': 'study',
  'DatasetsView': 'datasets',
  'VariablesView': 'variables',
  'ValueLevelView': 'valuelevel',
  'CodelistsView': 'codelists',
  'DictionariesView': 'dictionaries',
  'MethodsView': 'methods',
  'CommentsView': 'comments',
  'DocumentsView': 'documents',
  'PagesView': 'pages',
  'DefineXlsx': 'define',
  'DefineEditor': 'define',
  'SdrgEditor': 'sdrg'
}

const avatarGradient = computed(() => {
  const colors = [
    'linear-gradient(135deg, #6366f1, #818cf8)',
    'linear-gradient(135deg, #10b981, #34d399)',
    'linear-gradient(135deg, #f59e0b, #fbbf24)',
    'linear-gradient(135deg, #8b5cf6, #a78bfa)',
  ]
  let hash = 0
  for (let i = 0; i < (projectId.value || '').length; i++) {
    hash = projectId.value.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
})

watch(() => route.name, (newName) => {
  const key = reverseRouteMap[newName as string]
  if (key) activeNav.value = key
}, { immediate: true })

const switchNav = (key: string) => {
  const routeName = routeNameMap[key]
  if (routeName) {
    router.push({ name: routeName, params: { projectId: projectId.value } })
  }
}

const goBack = () => router.push({ name: 'Home' })

const loadProjectInfo = async () => {
  if (!projectId.value) return
  try {
    const user = localStorage.getItem('user')
    const username = user ? JSON.parse(user).username : ''
    const res = await ApiGetProjects(username)
    if (res.data.success && res.data.data) {
      const projects = Array.isArray(res.data.data) ? res.data.data : [res.data.data]
      const cur = projects.find((p: any) => p.projectId === projectId.value)
      if (cur) {
        projectInfo.value = {
          projectName: cur.projectName || '',
          protocolName: cur.protocolName || '',
          sponsor: cur.sponsor || ''
        }
      }
    }
  } catch (error) {
    console.error('加载项目信息失败:', error)
  }
}

onMounted(() => {
  loadProjectInfo()
  if (route.name === 'ProjectWorkspace') {
    router.replace({ name: 'ProjectInfo', params: { projectId: projectId.value } })
  }
})
</script>

<style scoped lang="less">
.workspace {
  display: flex;
  height: calc(100vh - var(--saas-topbar-height));
  background: var(--saas-bg-page);
}

/* ===== 左侧二级导航 ===== */
.ws-sidebar {
  width: var(--saas-sidebar-secondary-width);
  min-width: var(--saas-sidebar-secondary-width);
  background: var(--saas-bg-card);
  border-right: 1px solid var(--saas-border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ws-sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--saas-border-light);
  display: flex;
  align-items: center;
  gap: 10px;

  .back-btn {
    width: 30px;
    height: 30px;
    border: 1px solid var(--saas-border);
    background: var(--saas-bg-card);
    border-radius: var(--saas-radius-sm);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--saas-text-tertiary);
    transition: all var(--saas-transition);
    flex-shrink: 0;

    &:hover {
      border-color: var(--saas-primary-lighter);
      color: var(--saas-primary);
      background: var(--saas-primary-bg);
    }
  }

  .project-badge {
    width: 32px;
    height: 32px;
    border-radius: var(--saas-radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--saas-text-inverse);
    font-weight: 700;
    font-size: 14px;
    flex-shrink: 0;
  }

  .project-brief {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 1px;

    .project-name {
      font-size: 13px;
      font-weight: 600;
      color: var(--saas-text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .project-id {
      font-size: 11px;
      color: var(--saas-text-tertiary);
      font-family: 'SF Mono', 'Consolas', monospace;
    }
  }
}

/* ===== 导航菜单 ===== */
.ws-nav {
  flex: 1;
  padding: 12px 10px;
  overflow-y: auto;

  .nav-group-label {
    font-size: 10px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--saas-text-tertiary);
    padding: 0 10px 8px;
  }
}

.ws-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--saas-radius-md);
  cursor: pointer;
  transition: all var(--saas-transition);
  margin-bottom: 2px;
  text-decoration: none;
  position: relative;

  .nav-icon {
    width: 32px;
    height: 32px;
    border-radius: var(--saas-radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--saas-text-tertiary);
    background: var(--saas-bg-input);
    transition: all var(--saas-transition);
    flex-shrink: 0;
  }

  .nav-content {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 1px;

    .nav-label {
      font-size: 13px;
      font-weight: 500;
      color: var(--saas-text-primary);
      transition: color var(--saas-transition);
    }

    .nav-desc {
      font-size: 11px;
      color: var(--saas-text-tertiary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .nav-badge {
    font-size: 10px;
    font-weight: 600;
    color: var(--saas-text-inverse);
    background: var(--saas-primary);
    padding: 2px 6px;
    border-radius: var(--saas-radius-full);
    line-height: 1;
  }

  &:hover {
    background: var(--saas-bg-hover);

    .nav-icon {
      color: var(--saas-primary);
      background: var(--saas-primary-bg);
    }
  }

  &.active {
    background: var(--saas-primary-bg);

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 8px;
      bottom: 8px;
      width: 3px;
      border-radius: 0 3px 3px 0;
      background: var(--saas-primary);
    }

    .nav-icon {
      color: var(--saas-text-inverse);
      background: var(--saas-primary);
    }

    .nav-content .nav-label {
      color: var(--saas-primary-dark);
      font-weight: 600;
    }
  }
}

/* ===== 底部工作流提示 ===== */
.ws-sidebar-footer {
  padding: 14px 16px 16px;
  border-top: 1px solid var(--saas-border-light);

  .workflow-hint {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }

  .workflow-steps {
    display: flex;
    align-items: center;

    .step {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--saas-border);
      color: var(--saas-text-tertiary);
      font-size: 11px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;
      flex-shrink: 0;

      &.done {
        background: var(--saas-primary);
        color: var(--saas-text-inverse);
      }

      &.active {
        background: var(--saas-primary);
        color: var(--saas-text-inverse);
        box-shadow: 0 0 0 3px var(--saas-primary-bg);
      }
    }

    .step-line {
      width: 8px;
      height: 2px;
      background: var(--saas-border);
      transition: background 0.3s ease;

      &.filled {
        background: var(--saas-primary);
      }
    }
  }

  .workflow-labels {
    display: flex;
    gap: 2px;
    justify-content: center;

    .workflow-label {
      font-size: 9px;
      color: var(--saas-text-tertiary);
      width: 26px;
      text-align: center;

      &.active {
        color: var(--saas-primary);
        font-weight: var(--saas-font-semibold);
      }
    }
  }
}

/* ===== 右侧内容区 ===== */
.ws-content {
  flex: 1;
  overflow: auto;
  background: var(--saas-bg-page);
}
</style>
