<template>
  <div :class="['empty-state', { 'empty-state--compact': compact }]">
    <div v-if="$slots.icon" class="empty-icon">
      <slot name="icon" />
    </div>
    <h3 v-if="title">{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' },
  description: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})
</script>

<style scoped lang="less">
.empty-state {
  text-align: center;
  padding: var(--saas-space-12) var(--saas-space-5);
  background: var(--saas-bg-card);
  border-radius: var(--saas-radius-lg);
  border: 1px solid var(--saas-border-light);

  .empty-icon {
    margin-bottom: var(--saas-space-4);
  }

  h3 {
    font-size: var(--saas-text-lg);
    font-weight: var(--saas-font-semibold);
    color: var(--saas-text-primary);
    margin: 0 0 var(--saas-space-2);
  }

  p {
    font-size: var(--saas-text-base);
    color: var(--saas-text-secondary);
    margin: 0 0 var(--saas-space-5);
  }

  .empty-action {
    margin-top: var(--saas-space-5);
  }

  &--compact {
    padding: var(--saas-space-6) var(--saas-space-5);

    .empty-icon { margin-bottom: var(--saas-space-2); }
    h3 { font-size: var(--saas-text-base); }
    p { font-size: var(--saas-text-sm); margin-bottom: var(--saas-space-2); }
  }
}
</style>
