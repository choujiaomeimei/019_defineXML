<template>
  <div class="section-header">
    <div class="section-title-group">
      <h3><slot /></h3>
      <span v-if="$slots.desc" class="section-desc"><slot name="desc" /></span>
    </div>
    <div v-if="$slots.actions" class="header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped lang="less">
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
</style>
