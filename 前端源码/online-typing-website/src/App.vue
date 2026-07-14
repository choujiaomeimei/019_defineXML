<template>
  <template v-if="!isAuthPage">
    <div class="saas-layout">
      <!-- 主区域 -->
      <div class="saas-main">
        <!-- 顶栏 -->
        <header class="saas-topbar">
          <div class="topbar-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">项目列表</el-breadcrumb-item>
              <el-breadcrumb-item v-if="route.params.projectId">{{ route.params.projectId }}</el-breadcrumb-item>
              <el-breadcrumb-item v-if="route.params.projectId && route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
              <template v-if="!route.params.projectId && route.meta.title && route.meta.title !== '项目列表'">
                <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
              </template>
            </el-breadcrumb>
          </div>
          <div class="topbar-right">
            <div class="topbar-user" @click="showUserInfoDialog = true">
              <div class="user-avatar">
                {{ currentUsername.charAt(0).toUpperCase() }}
              </div>
              <span class="user-name">{{ currentUsername }}</span>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M4 6L8 10L12 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="topbar-divider"></div>
            <button class="topbar-btn logout-btn" @click="logout">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                <path d="M6.75 15.75H3.75C3.35218 15.75 2.97064 15.592 2.68934 15.3107C2.40804 15.0294 2.25 14.6478 2.25 14.25V3.75C2.25 3.35218 2.40804 2.97064 2.68934 2.68934C2.97064 2.40804 3.35218 2.25 3.75 2.25H6.75" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M12 12.75L15.75 9L12 5.25" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M15.75 9H6.75" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              退出
            </button>
          </div>
        </header>

        <!-- 内容区 -->
        <main class="saas-content">
          <router-view></router-view>
        </main>
      </div>

      <!-- 右侧处理规则快捷栏 -->
      <aside class="saas-rules-rail" aria-label="处理规则">
        <div class="rules-rail-header" title="字段来源参考">规则</div>
        <nav class="rules-rail-nav">
          <button
            v-for="key in ruleModuleKeys"
            :key="key"
            type="button"
            class="nav-item"
            :class="{ active: activeRuleTab === key }"
            @click="showRule(key)"
            :title="ruleModules[key].tooltip"
            :aria-label="ruleModules[key].tooltip"
            :aria-pressed="activeRuleTab === key && showRuleDrawer"
          >
            <div class="nav-icon">
              <span class="nav-letter">{{ ruleModules[key].letter }}</span>
            </div>
          </button>
          <button
            type="button"
            class="nav-item nav-guide"
            :class="{ active: showGuideDrawer }"
            title="使用说明"
            aria-label="打开使用说明"
            :aria-pressed="showGuideDrawer"
            @click="showGuide"
          >
            <div class="nav-icon">
              <span class="nav-letter" aria-hidden="true">?</span>
            </div>
          </button>
        </nav>
      </aside>
    </div>

    <!-- 用户信息对话框 -->
    <el-dialog v-model="showUserInfoDialog" title="用户信息" width="420px" class="saas-dialog">
      <div class="user-info-dialog">
        <div class="user-info-avatar-lg">
          {{ currentUsername.charAt(0).toUpperCase() }}
        </div>
        <div class="user-info-rows">
          <div class="info-row">
            <span class="info-key">用户名</span>
            <span class="info-val">{{ userInfo.username || '未知' }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">用户ID</span>
            <span class="info-val">{{ userInfo.id || '未知' }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">创建时间</span>
            <span class="info-val">{{ userInfo.createTime || '未知' }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showUserInfoDialog = false" round>关闭</el-button>
      </template>
    </el-dialog>

    <!-- 处理规则侧滑面板 -->
    <ProcessingRulesDrawer
      :visible="showRuleDrawer"
      :module-key="activeRuleTab"
      @update:visible="handleRuleDrawerClose"
    />
    <UserGuideDrawer
      v-model:visible="showGuideDrawer"
      :route-name="String(route.name || '')"
    />
  </template>

  <template v-else>
    <router-view></router-view>
  </template>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { ref, computed, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import ProcessingRulesDrawer from '@/components/ProcessingRulesDrawer.vue'
import UserGuideDrawer from '@/components/UserGuideDrawer.vue'
import { RULE_MODULES, RULE_MODULE_KEYS } from '@/data/processingRules'

const ruleModules = RULE_MODULES
const ruleModuleKeys = RULE_MODULE_KEYS

const router = useRouter()
const route = useRoute()
const currentUsername = computed(() => {
  try {
    const user = localStorage.getItem('user')
    if (user) {
      const userInfo = JSON.parse(user)
      return userInfo.username || '未知用户'
    }
    return '未登录'
  } catch (error) {
    return '未知用户'
  }
})

const isAuthPage = computed(() => {
  return route.path === '/login' || route.path === '/register'
})

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    localStorage.removeItem('user')
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    // cancelled
  }
}

const showUserInfoDialog = ref(false)
const showRuleDrawer = ref(false)
const showGuideDrawer = ref(false)
const activeRuleTab = ref('')

const showRule = (tab) => {
  showGuideDrawer.value = false
  activeRuleTab.value = tab
  showRuleDrawer.value = true
}

const showGuide = () => {
  showRuleDrawer.value = false
  activeRuleTab.value = ''
  showGuideDrawer.value = true
}

const handleRuleDrawerClose = (visible) => {
  showRuleDrawer.value = visible
  if (!visible) {
    activeRuleTab.value = ''
  }
}

watch(
  () => route.fullPath,
  () => {
    showRuleDrawer.value = false
    showGuideDrawer.value = false
    activeRuleTab.value = ''
  },
)

const userInfo = computed(() => {
  try {
    const user = localStorage.getItem('user')
    if (user) return JSON.parse(user)
    return {}
  } catch (error) {
    return {}
  }
})
</script>

<style lang="less" scoped>
.saas-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.saas-rules-rail {
  width: var(--saas-rules-rail-width, 56px);
  background: var(--saas-bg-sidebar);
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  z-index: 2100;
  overflow: hidden;
  border-left: 1px solid rgba(255, 255, 255, 0.08);

  .rules-rail-header {
    height: var(--saas-topbar-height);
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    flex-shrink: 0;
    width: 100%;
    font-size: 11px;
    font-weight: 600;
    font-family: var(--saas-font-rules);
    color: rgba(255, 255, 255, 0.45);
    letter-spacing: 0.08em;
    writing-mode: vertical-rl;
    text-orientation: mixed;
    cursor: default;
    user-select: none;
  }

  .rules-rail-nav {
    flex: 1;
    padding: 12px 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    width: 100%;
  }

  .nav-item {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--saas-radius-md);
    cursor: pointer;
    color: rgba(255, 255, 255, 0.5);
    border: none;
    padding: 0;
    background: transparent;
    font-family: inherit;
    transition: all var(--saas-transition);

    &:focus-visible {
      outline: 2px solid var(--saas-primary-lighter);
      outline-offset: 1px;
    }

    &:hover {
      background: var(--saas-bg-sidebar-hover);
      color: rgba(255, 255, 255, 0.9);
    }

    &.active {
      background: var(--saas-bg-sidebar-active);
      color: var(--saas-text-inverse);
    }

    .nav-icon {
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .nav-guide {
    position: relative;
    margin-top: 9px;

    &::before {
      content: '';
      position: absolute;
      top: -7px;
      left: 8px;
      right: 8px;
      height: 1px;
      background: rgba(255, 255, 255, 0.12);
    }
  }

  .nav-letter {
    font-size: 14px;
    font-weight: 700;
    font-family: var(--saas-font-rules);
  }
}

.saas-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  transition: margin-left var(--saas-transition-slow);
}

.saas-topbar {
  height: var(--saas-topbar-height);
  background: var(--saas-bg-card);
  border-bottom: 1px solid var(--saas-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  z-index: 50;

  .topbar-left {
    :deep(.el-breadcrumb) {
      font-size: 14px;

      .el-breadcrumb__item {
        .el-breadcrumb__inner {
          font-weight: 500;
          color: var(--saas-text-secondary);
          transition: color var(--saas-transition);

          &.is-link:hover {
            color: var(--saas-primary);
          }
        }

        &:last-child .el-breadcrumb__inner {
          font-weight: 600;
          color: var(--saas-text-primary);
        }
      }

      .el-breadcrumb__separator {
        color: var(--saas-text-tertiary);
      }
    }
  }

  .topbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .topbar-user {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 6px 12px;
    border-radius: var(--saas-radius-md);
    cursor: pointer;
    transition: background var(--saas-transition);

    &:hover {
      background: var(--saas-bg-hover);
    }

    .user-avatar {
      width: 32px;
      height: 32px;
      border-radius: var(--saas-radius-full);
      background: linear-gradient(135deg, var(--saas-primary), var(--saas-primary-light));
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--saas-text-inverse);
      font-weight: 600;
      font-size: 13px;
    }

    .user-name {
      font-size: 14px;
      font-weight: 500;
      color: var(--saas-text-primary);
    }

    svg {
      color: var(--saas-text-tertiary);
    }
  }

  .topbar-divider {
    width: 1px;
    height: 24px;
    background: var(--saas-border);
    margin: 0 4px;
  }

  .topbar-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    border: none;
    background: transparent;
    border-radius: var(--saas-radius-md);
    cursor: pointer;
    font-size: 13px;
    font-family: inherit;
    color: var(--saas-text-secondary);
    transition: all var(--saas-transition);

    &:hover {
      background: var(--saas-bg-hover);
      color: var(--saas-text-primary);
    }

    &.logout-btn:hover {
      color: var(--saas-danger);
      background: var(--saas-danger-bg);
    }
  }
}

.saas-content {
  flex: 1;
  overflow: auto;
  background: var(--saas-bg-page);
}

// 用户信息对话框
.user-info-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;

  .user-info-avatar-lg {
    width: 64px;
    height: 64px;
    border-radius: var(--saas-radius-full);
    background: linear-gradient(135deg, var(--saas-primary), var(--saas-primary-light));
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--saas-text-inverse);
    font-weight: 700;
    font-size: 24px;
    margin-bottom: 20px;
  }

  .user-info-rows {
    width: 100%;

    .info-row {
      display: flex;
      justify-content: space-between;
      padding: 12px 16px;
      border-bottom: 1px solid var(--saas-border-light);

      &:last-child {
        border-bottom: none;
      }

      .info-key {
        color: var(--saas-text-secondary);
        font-size: 14px;
      }

      .info-val {
        color: var(--saas-text-primary);
        font-weight: 500;
        font-size: 14px;
      }
    }
  }
}
</style>
