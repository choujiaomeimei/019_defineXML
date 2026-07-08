<template>
  <div class="stat-card">
    <div class="stat-icon" :class="'variant-' + variant">
      <slot name="icon" />
    </div>
    <div class="stat-info">
      <span class="stat-value">{{ value }}</span>
      <span class="stat-label">{{ label }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  value: { type: [String, Number], required: true },
  label: { type: String, required: true },
  variant: { type: String, default: 'primary', validator: v => ['primary', 'success', 'warning'].includes(v) }
})
</script>

<style scoped lang="less">
.stat-card {
  background: var(--saas-bg-card);
  border: 1px solid var(--saas-border-light);
  border-radius: var(--saas-radius-lg);
  padding: var(--saas-space-5);
  display: flex;
  align-items: center;
  gap: var(--saas-space-4);
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

    .stat-value {
      font-size: var(--saas-text-3xl);
      font-weight: var(--saas-font-bold);
      color: var(--saas-text-primary);
      line-height: 1.2;
    }

    .stat-label {
      font-size: var(--saas-text-sm);
      color: var(--saas-text-secondary);
      margin-top: 2px;
    }
  }
}
</style>
