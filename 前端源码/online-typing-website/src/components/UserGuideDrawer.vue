<template>
  <el-drawer
    :model-value="visible"
    title="使用说明"
    direction="rtl"
    :size="drawerSize"
    class="user-guide-drawer"
    modal-class="user-guide-overlay"
    :append-to-body="true"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="guide-content">
      <header class="guide-intro">
        <p class="guide-lead">工作区提取与校验按钮说明</p>
        <p>建议按 Variables → ValueLevel → Codelists 的顺序处理。提取类操作会更新项目数据，运行前请先保存当前表格或 Excel 编辑内容。</p>
      </header>

      <section class="guide-checklist" aria-labelledby="guide-checklist-title">
        <h3 id="guide-checklist-title">执行前确认</h3>
        <ul>
          <li>已在“文件上传”中准备 Spec、XPT、P21、aCRF、CT 和 EDC 等对应输入文件。</li>
          <li>项目配置中的标准类型、CT 版本与当前项目一致。</li>
          <li>长任务执行期间不要重复点击；按钮恢复可用后再进行下一步。</li>
        </ul>
      </section>

      <nav class="guide-jump" aria-label="说明章节">
        <a
          v-for="section in sections"
          :key="section.key"
          :href="`#guide-${section.key}`"
          :class="{ current: section.key === currentSection }"
        >
          {{ section.shortTitle }}
        </a>
      </nav>

      <section
        v-for="section in sections"
        :id="`guide-${section.key}`"
        :key="section.key"
        class="guide-section"
        :class="{ 'is-current': section.key === currentSection }"
      >
        <div class="section-heading">
          <div>
            <h3>{{ section.title }}</h3>
            <p>{{ section.description }}</p>
          </div>
          <span v-if="section.key === currentSection" class="current-badge">当前页面</span>
        </div>

        <article v-for="action in section.actions" :key="action.name" class="action-row">
          <div class="action-name">{{ action.name }}</div>
          <div class="action-detail">
            <p>{{ action.purpose }}</p>
            <dl>
              <div>
                <dt>需要</dt>
                <dd>{{ action.requires }}</dd>
              </div>
              <div>
                <dt>结果</dt>
                <dd>{{ action.result }}</dd>
              </div>
              <div v-if="action.notice">
                <dt>注意</dt>
                <dd>{{ action.notice }}</dd>
              </div>
            </dl>
          </div>
        </article>
      </section>

      <section class="guide-workflow" aria-labelledby="guide-workflow-title">
        <h3 id="guide-workflow-title">推荐执行顺序</h3>
        <ol>
          <li><strong>Variables：</strong>生成 SUPP → 对比 P21 → 提取 XPT → 提取 P21 → 提取 Pages → Method&Comments → 提取 Codelist。</li>
          <li><strong>ValueLevel：</strong>提取 Where Clause → 提取 XPT 字段 → 提取 Codelist → 提取 Pages。</li>
          <li><strong>Codelists：</strong>匹配 NCI 码 → 比对 EDC → 根据差异补充 → 必要时进入合并工具。</li>
        </ol>
      </section>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  routeName: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:visible'])
const drawerSize = 'var(--saas-rules-drawer-width, 50vw)'

const currentSection = computed(() => {
  const routeMap = {
    VariablesView: 'variables',
    ValueLevelView: 'valuelevel',
    CodelistsView: 'codelists',
    CodelistMerge: 'codelists',
  }
  return routeMap[props.routeName] || ''
})

const sections = [
  {
    key: 'variables',
    shortTitle: 'Variables',
    title: 'Variables 页面',
    description: '补齐变量级元数据，并生成后续模块所需的数据。',
    actions: [
      {
        name: '生成 SUPP',
        purpose: '将 SUPP=Y 的变量移出原域，并为各域生成标准 SUPPxx 变量模板。',
        requires: 'Variables 中已准确标记 SUPP=Y。',
        result: '删除原 SUPP 标记变量，重建对应 SUPPxx 数据集。',
        notice: '这是结构性变更，执行前应确认 SUPP 标记；重复执行会先删除已有 SUPPxx。',
      },
      {
        name: '对比 P21',
        purpose: '按 Dataset + Variable 对比当前 Variables 与 P21，显示匹配、Spec 独有和 P21 独有项。',
        requires: '已上传 P21 Spec，当前 Variables 已保存。',
        result: '只显示差异，不修改数据。',
      },
      {
        name: '提取 XPT',
        purpose: '从 XPT 补齐 Data Type、Length、Significant Digits、Format。',
        requires: '已上传项目 XPT。',
        result: '更新 Variables 对应技术字段并刷新域列表。',
        notice: '会覆盖上述自动提取字段。',
      },
      {
        name: '提取 P21',
        purpose: '从 P21 匹配 Mandatory、Role、Has No Data。',
        requires: '已上传 P21 Spec。',
        result: '更新 Variables 对应字段；如有差异可弹出对比摘要。',
        notice: '不会新增 P21 中独有的变量。',
      },
      {
        name: '提取 Pages',
        purpose: '根据 Pages 数据，把 aCRF 页码回填到 Source=Investigator 的变量。',
        requires: '已生成 Pages 数据；首次运行还需 Annots2.xlsx 和可用的 Pages 提取脚本。',
        result: '先清空 Investigator 变量原页码，再写入最新匹配页码；SUPPxx 不处理。',
        notice: '非 Investigator 变量即使存在匹配页码也会跳过。',
      },
      {
        name: 'Method&Comments',
        purpose: '把标记为 Y 的 Method/Comment 按 Text 内容去重，生成 MTxxx、CTxxx，并回写变量引用。',
        requires: 'Variables 中 Method 或 Comment 为 Y，且 Text 字段已填写。',
        result: '重建 Methods、Comments 数据，并把变量中的 Y 替换为生成的 ID。',
        notice: '该操作会重建现有 Methods/Comments；手工维护内容应先确认是否需要保留。',
      },
      {
        name: '提取 Codelist',
        purpose: '按 Variables 的 CDISC Submission Value 从 XPT 提取术语，并根据项目 CT 版本匹配 NCI 编码；MEDDRA/WHODRUG 写入 Dictionaries。',
        requires: 'Variables、XPT 和项目 CT 配置已准备。',
        result: '重建变量级 Codelists，并更新 Variables 的 Codelist 引用。',
        notice: '会删除并重建现有变量级 Codelist；VLM 生成的 Codelist 会保留。',
      },
    ],
  },
  {
    key: 'valuelevel',
    shortTitle: 'ValueLevel',
    title: 'ValueLevel 页面',
    description: '从数据和注释中生成值级元数据，并按步骤补齐字段。',
    actions: [
      {
        name: '提取 Where Clause',
        purpose: '运行 VLM 提取脚本生成值级记录，回填 Variables 中的 Origin/Source，并按 Dataset 重排顺序。',
        requires: '已上传并处理 Spec/XPT，服务器 Python 与 VLM 脚本可用。',
        result: '生成或更新 VLM 数据，Mandatory 默认设为 No。',
        notice: '耗时可能较长，应作为 ValueLevel 的第一步执行。',
      },
      {
        name: '提取 XPT 字段',
        purpose: '从 XPT 补齐 VLM 的 Data Type、Length、Significant Digits 和 Format。',
        requires: '已完成 Where Clause 提取，项目 XPT 文件齐全。',
        result: '更新当前 VLM 技术字段。',
      },
      {
        name: '提取 Codelist',
        purpose: '先生成 VLM Codelist 引用，再从 XPT 提取值级术语并匹配 CT 编码。',
        requires: 'VLM、XPT 及项目 CT 配置已准备。',
        result: '写入 VLM 级 Codelist 和 Term 数据。',
        notice: '会重建由 VLM 提取产生的 Codelist Term。',
      },
      {
        name: '提取 Pages',
        purpose: '按 Dataset + Variable + Where Clause 匹配 VLM_WhereClause 页码。',
        requires: 'Pages 中已有 VLM_WhereClause 数据，Variables Source=Investigator。',
        result: '更新匹配成功的非 SUPP VLM Pages；未匹配记录保留原页码。',
        notice: '非 Investigator 来源不会自动更新页码。',
      },
    ],
  },
  {
    key: 'codelists',
    shortTitle: 'Codelists',
    title: 'Codelists 页面',
    description: '补齐受控术语编码、核对 EDC 差异并处理重复 Codelist。',
    actions: [
      {
        name: '匹配 NCI 码',
        purpose: '按项目标准与 CT 版本匹配 NCI Codelist Code、NCI Term Code、Terminology 和名称。',
        requires: '项目 CT 版本已配置，CT 数据包已导入，Codelist 已提取。',
        result: '补齐可匹配的 NCI 编码；XPT 解码值不会被覆盖。',
        notice: '自定义术语或 CT 中不存在的值会保持未匹配。',
      },
      {
        name: '比对 EDC',
        purpose: '将当前 Codelist 与最新上传的 EDC CODELIST Sheet 按 Codelist 和 Term 对比。',
        requires: '已上传类别为 EDC_CODELIST 的建库说明，且存在 CODELIST Sheet。',
        result: '展示 EDC 有/系统无和系统有/EDC 无的 Term；可将缺失项补入系统。',
        notice: '补入前请核对编码名、编码值和标签，避免把同值但不同语义的 Term 合并。',
      },
      {
        name: '合并 Codelist',
        purpose: '进入独立合并页面，对相同或相近 Codelist 分组、选择目标并执行合并。',
        requires: '当前 Codelist 已提取并完成必要校验。',
        result: '跳转到合并工具；点击入口本身不会立即修改数据。',
        notice: '实际合并会改变 Variables/VLM 引用，执行前应检查合并预览。',
      },
    ],
  },
]
</script>

<style lang="less">
.user-guide-overlay {
  right: var(--saas-rules-rail-width, 56px) !important;
  left: 0 !important;
  width: auto !important;
}

.user-guide-drawer {
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
  }

  .el-drawer__title {
    font-size: var(--saas-text-xl);
    font-weight: var(--saas-font-semibold);
    color: var(--saas-text-primary);
  }

  .el-drawer__body {
    padding: 0;
    overflow-y: auto;
  }
}
</style>

<style lang="less" scoped>
.guide-content {
  padding: var(--saas-space-5) var(--saas-space-6) var(--saas-space-8);
  color: var(--saas-text-primary);
  font-size: var(--saas-text-base);
  line-height: 1.65;
}

.guide-intro {
  max-width: 72ch;

  .guide-lead {
    margin-bottom: var(--saas-space-1);
    font-size: var(--saas-text-lg);
    font-weight: var(--saas-font-semibold);
  }

  p:last-child {
    color: var(--saas-text-secondary);
  }
}

.guide-checklist,
.guide-workflow {
  margin-top: var(--saas-space-5);
  padding: var(--saas-space-4);
  border: 1px solid var(--saas-border);
  border-radius: var(--saas-radius-md);
  background: var(--saas-bg-page);

  h3 {
    margin-bottom: var(--saas-space-2);
    font-size: var(--saas-text-base);
  }

  ul,
  ol {
    padding-left: 20px;
    color: var(--saas-text-secondary);
  }

  li + li {
    margin-top: var(--saas-space-1);
  }
}

.guide-jump {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  gap: var(--saas-space-1);
  margin: var(--saas-space-5) calc(-1 * var(--saas-space-6)) 0;
  padding: var(--saas-space-3) var(--saas-space-6);
  border-top: 1px solid var(--saas-border-light);
  border-bottom: 1px solid var(--saas-border-light);
  background: var(--saas-bg-card);

  a {
    padding: 6px 10px;
    border-radius: var(--saas-radius-sm);
    color: var(--saas-text-secondary);
    text-decoration: none;
    font-weight: var(--saas-font-medium);

    &:hover,
    &:focus-visible {
      background: var(--saas-bg-hover);
      color: var(--saas-text-primary);
      outline: none;
    }

    &.current {
      background: var(--saas-primary-bg);
      color: var(--saas-primary-dark);
    }
  }
}

.guide-section {
  scroll-margin-top: 60px;
  padding-top: var(--saas-space-6);

  & + & {
    margin-top: var(--saas-space-6);
    border-top: 1px solid var(--saas-border-light);
  }
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--saas-space-4);
  margin-bottom: var(--saas-space-3);

  h3 {
    font-size: var(--saas-text-lg);
    margin-bottom: 2px;
  }

  p {
    color: var(--saas-text-secondary);
  }
}

.current-badge {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: var(--saas-radius-full);
  background: var(--saas-primary-bg);
  color: var(--saas-primary-dark);
  font-size: var(--saas-text-xs);
  font-weight: var(--saas-font-semibold);
}

.action-row {
  display: grid;
  grid-template-columns: minmax(112px, 0.28fr) minmax(0, 1fr);
  gap: var(--saas-space-4);
  padding: var(--saas-space-4) 0;
  border-top: 1px solid var(--saas-border-light);
}

.action-name {
  font-weight: var(--saas-font-semibold);
  color: var(--saas-primary-dark);
}

.action-detail {
  p {
    margin-bottom: var(--saas-space-2);
  }

  dl {
    display: grid;
    gap: var(--saas-space-1);
  }

  dl > div {
    display: grid;
    grid-template-columns: 42px minmax(0, 1fr);
    gap: var(--saas-space-2);
  }

  dt {
    color: var(--saas-text-tertiary);
    font-size: var(--saas-text-sm);
  }

  dd {
    margin: 0;
    color: var(--saas-text-secondary);
    font-size: var(--saas-text-sm);
  }
}

@media (max-width: 900px) {
  .action-row {
    grid-template-columns: 1fr;
    gap: var(--saas-space-2);
  }
}
</style>
