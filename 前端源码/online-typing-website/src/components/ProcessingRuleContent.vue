<template>
  <div class="rule-content">
    <section v-for="(section, idx) in sections" :key="idx" class="rule-section">
      <h4 class="rule-section-heading">{{ section.heading }}</h4>
      <template v-for="(block, bIdx) in section.blocks" :key="bIdx">
        <p
          v-if="block.type === 'paragraph'"
          class="rule-paragraph"
          v-html="block.html"
        />
        <ol v-else-if="block.type === 'ordered-list'" class="rule-list">
          <li v-for="(item, iIdx) in block.items" :key="iIdx" v-html="item" />
        </ol>
        <div v-else-if="block.type === 'table'" class="rule-table-wrap">
          <table class="rule-table">
            <thead>
              <tr>
                <th v-for="(col, cIdx) in block.columns" :key="cIdx" scope="col">{{ col }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rIdx) in block.rows" :key="rIdx">
                <td
                  v-for="(cell, cellIdx) in row"
                  :key="cellIdx"
                  :class="{ 'rule-table-field': cellIdx === 0 }"
                  v-html="cell"
                />
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
defineProps({
  sections: {
    type: Array,
    required: true,
  },
})
</script>

<style lang="less" scoped>
.rule-content {
  font-size: var(--saas-text-base);
  line-height: 1.7;
  color: var(--saas-text-primary);
  font-family: var(--saas-font-rules);
}

.rule-section {
  & + & {
    margin-top: var(--saas-space-6);
    padding-top: var(--saas-space-6);
    border-top: 1px solid var(--saas-border-light);
  }
}

.rule-section-heading {
  margin: 0 0 var(--saas-space-3);
  font-size: var(--saas-text-lg);
  font-weight: var(--saas-font-semibold);
  font-family: var(--saas-font-rules);
  color: var(--saas-text-primary);
  letter-spacing: 0;
}

.rule-paragraph {
  margin: var(--saas-space-2) 0;

  :deep(code) {
    background: var(--saas-bg-input);
    padding: 1px 6px;
    border-radius: var(--saas-radius-sm);
    font-size: var(--saas-text-sm);
    color: var(--saas-primary-dark);
    font-family: var(--saas-font-rules);
  }

  :deep(b) {
    font-weight: var(--saas-font-semibold);
  }
}

.rule-list {
  padding-left: 20px;
  margin: var(--saas-space-2) 0;

  li {
    margin: var(--saas-space-2) 0;

    :deep(code) {
      background: var(--saas-bg-input);
      padding: 1px 6px;
      border-radius: var(--saas-radius-sm);
      font-size: var(--saas-text-sm);
      color: var(--saas-primary-dark);
      font-family: var(--saas-font-rules);
    }

    :deep(b) {
      font-weight: var(--saas-font-semibold);
    }
  }
}

.rule-table-wrap {
  margin: var(--saas-space-3) 0;
  border: 1px solid var(--saas-border);
  border-radius: var(--saas-radius-md);
  overflow: hidden;
}

.rule-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--saas-text-sm);
  font-family: var(--saas-font-rules);

  th,
  td {
    padding: 10px 14px;
    text-align: left;
    vertical-align: top;
    border-bottom: 1px solid var(--saas-border-light);
    font-family: var(--saas-font-rules);
  }

  tr:last-child td {
    border-bottom: none;
  }

  th {
    background: var(--saas-bg-input);
    font-weight: var(--saas-font-semibold);
    font-size: var(--saas-text-xs);
    font-family: var(--saas-font-rules);
    letter-spacing: 0;
    color: var(--saas-text-secondary);
  }

  td {
    color: var(--saas-text-primary);

    :deep(code) {
      background: var(--saas-primary-bg);
      padding: 1px 5px;
      border-radius: 4px;
      font-size: var(--saas-text-xs);
      color: var(--saas-primary-dark);
      font-family: var(--saas-font-rules);
    }

    :deep(b) {
      font-weight: var(--saas-font-semibold);
    }
  }

  .rule-table-field {
    white-space: nowrap;
    font-family: var(--saas-font-rules);
    font-size: var(--saas-text-sm);
    color: var(--saas-text-primary);
    background: var(--saas-bg-page);
    width: 160px;
  }
}
</style>
