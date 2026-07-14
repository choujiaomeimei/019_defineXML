<template>
  <el-drawer
    :model-value="visible"
    :title="drawerTitle"
    direction="rtl"
    :size="drawerSize"
    class="processing-rules-drawer"
    modal-class="processing-rules-overlay"
    :append-to-body="true"
    destroy-on-close
    @update:model-value="handleVisibleChange"
  >
    <div class="drawer-body">
      <div class="standard-switcher" role="tablist" aria-label="数据标准">
        <button
          v-for="std in PROCESSING_STANDARDS"
          :key="std.key"
          type="button"
          role="tab"
          :id="`processing-standard-${std.key}`"
          class="standard-tab"
          :class="{ active: activeStandard === std.key, unavailable: !std.available }"
          :aria-selected="activeStandard === std.key"
          aria-controls="processing-rule-panel"
          @click="activeStandard = std.key"
        >
          {{ std.label }}
          <span v-if="!std.available" class="standard-badge">开发中</span>
        </button>
      </div>

      <div
        id="processing-rule-panel"
        class="drawer-content"
        role="tabpanel"
        :aria-labelledby="`processing-standard-${activeStandard}`"
      >
        <ProcessingRuleContent
          v-if="ruleResult.type === 'content'"
          :sections="ruleResult.rule.sections"
        />

        <div v-else class="rule-placeholder">
          <div class="placeholder-icon" aria-hidden="true">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
              <rect x="4" y="8" width="32" height="26" rx="4" stroke="currentColor" stroke-width="1.5"/>
              <path d="M12 16h16M12 22h10M12 28h14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h4 class="placeholder-title">{{ ruleResult.placeholder.title }}</h4>
          <p class="placeholder-desc">{{ ruleResult.placeholder.description }}</p>
          <div v-if="ruleResult.placeholder.plannedSections?.length" class="placeholder-sections">
            <span class="placeholder-label">规划章节</span>
            <ul>
              <li v-for="(section, idx) in ruleResult.placeholder.plannedSections" :key="idx">
                {{ section }}
              </li>
            </ul>
          </div>
          <p class="placeholder-hint">
            当前模块：<strong>{{ ruleResult.placeholder.moduleTitle }}</strong>
          </p>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import ProcessingRuleContent from './ProcessingRuleContent.vue'
import { getProcessingRule, PROCESSING_STANDARDS, RULE_MODULES } from '@/data/processingRules'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  moduleKey: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:visible'])

const drawerSize = 'var(--saas-rules-drawer-width, 50vw)'

const activeStandard = ref('SDTM')

const drawerTitle = computed(() => {
  return RULE_MODULES[props.moduleKey]?.title || '处理规则'
})

const ruleResult = computed(() => {
  if (!props.moduleKey) {
    return { type: 'placeholder', placeholder: { title: '处理规则', description: '' } }
  }
  return getProcessingRule(activeStandard.value, props.moduleKey)
})

watch(
  () => props.visible,
  (open) => {
    if (open) {
      activeStandard.value = 'SDTM'
    }
  },
)

watch(
  () => props.moduleKey,
  () => {
    activeStandard.value = 'SDTM'
  },
)

const handleVisibleChange = (val) => {
  emit('update:visible', val)
}
</script>

<style lang="less">
.processing-rules-overlay {
  right: var(--saas-rules-rail-width, 56px) !important;
  left: 0 !important;
  width: auto !important;
}

.processing-rules-drawer {
  font-family: var(--saas-font-rules);

  &.el-drawer.rtl {
    right: var(--saas-rules-rail-width, 56px) !important;
    left: auto !important;
    width: var(--saas-rules-drawer-width, 50vw) !important;
    max-width: calc(100vw - var(--saas-rules-rail-width, 56px)) !important;
  }

  .el-drawer__header {
    margin-bottom: 0;
    padding: var(--saas-space-5) var(--saas-space-6);
    border-bottom: 1px solid var(--saas-border-light);
    font-family: var(--saas-font-rules);

    .el-drawer__title {
      font-size: var(--saas-text-xl);
      font-weight: var(--saas-font-semibold);
      font-family: var(--saas-font-rules);
      color: var(--saas-text-primary);
      letter-spacing: 0;
    }

    .el-drawer__close-btn {
      font-family: var(--saas-font-rules);
    }
  }

  .el-drawer__body {
    padding: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    width: 100%;
    font-family: var(--saas-font-rules);
  }
}
</style>

<style lang="less" scoped>
.drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
  font-family: var(--saas-font-rules);
}

.standard-switcher {
  display: flex;
  gap: var(--saas-space-1);
  padding: var(--saas-space-4) var(--saas-space-6);
  background: var(--saas-bg-page);
  border-bottom: 1px solid var(--saas-border-light);
  flex-shrink: 0;
}

.standard-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid var(--saas-border);
  border-radius: var(--saas-radius-md);
  background: var(--saas-bg-card);
  font-size: var(--saas-text-sm);
  font-weight: var(--saas-font-medium);
  font-family: var(--saas-font-rules);
  color: var(--saas-text-secondary);
  cursor: pointer;
  transition: all var(--saas-transition);

  &:hover:not(.active) {
    background: var(--saas-bg-hover);
    color: var(--saas-text-primary);
  }

  &.active {
    background: var(--saas-primary-bg);
    border-color: var(--saas-primary-lighter);
    color: var(--saas-primary-dark);
    font-weight: var(--saas-font-semibold);
  }

  &.unavailable:not(.active) {
    opacity: 0.85;
  }
}

.standard-badge {
  font-size: 10px;
  font-weight: var(--saas-font-medium);
  padding: 1px 5px;
  border-radius: var(--saas-radius-sm);
  background: var(--saas-warning-bg);
  color: var(--saas-warning);
  line-height: 1.4;
}

.drawer-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--saas-space-5) var(--saas-space-6) var(--saas-space-8);
}

.rule-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--saas-space-10) var(--saas-space-4);
  min-height: 280px;
  font-family: var(--saas-font-rules);

  .placeholder-icon {
    width: 72px;
    height: 72px;
    border-radius: var(--saas-radius-lg);
    background: var(--saas-bg-input);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--saas-text-tertiary);
    margin-bottom: var(--saas-space-5);
  }

  .placeholder-title {
    margin: 0 0 var(--saas-space-2);
    font-size: var(--saas-text-lg);
    font-weight: var(--saas-font-semibold);
    color: var(--saas-text-primary);
  }

  .placeholder-desc {
    margin: 0 0 var(--saas-space-5);
    font-size: var(--saas-text-base);
    line-height: 1.6;
    color: var(--saas-text-secondary);
    max-width: 36ch;
  }

  .placeholder-sections {
    width: 100%;
    max-width: 280px;
    text-align: left;
    background: var(--saas-bg-page);
    border: 1px solid var(--saas-border-light);
    border-radius: var(--saas-radius-md);
    padding: var(--saas-space-4);
    margin-bottom: var(--saas-space-4);

    .placeholder-label {
      display: block;
      font-size: var(--saas-text-xs);
      font-weight: var(--saas-font-semibold);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--saas-text-tertiary);
      margin-bottom: var(--saas-space-2);
    }

    ul {
      list-style: none;
      padding: 0;
      margin: 0;

      li {
        position: relative;
        padding: 6px 0 6px 16px;
        font-size: var(--saas-text-sm);
        color: var(--saas-text-secondary);

        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 13px;
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background: var(--saas-primary-lighter);
        }
      }
    }
  }

  .placeholder-hint {
    margin: 0;
    font-size: var(--saas-text-sm);
    color: var(--saas-text-tertiary);

    strong {
      color: var(--saas-text-secondary);
      font-weight: var(--saas-font-medium);
    }
  }
}
</style>
