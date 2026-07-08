<template>
  <template v-if="!isAuthPage">
    <div class="saas-layout">
      <!-- 一级侧边栏（图标模式） -->
      <aside class="saas-sidebar">
        <div class="sidebar-brand" title="Define.XML">
          <div class="brand-icon">
            <svg width="24" height="24" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="8" fill="white" fill-opacity="0.15"/>
              <path d="M8 10h12M8 14h8M8 18h10" stroke="white" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
        </div>

        <nav class="sidebar-nav">
          <div
            class="nav-item"
            :class="{ active: isProjectListActive }"
            @click="navigateTo('/ProjectList')"
            title="项目列表"
          >
            <div class="nav-icon">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <rect x="2" y="2" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <rect x="11" y="2" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <rect x="2" y="11" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/>
                <rect x="11" y="11" width="7" height="7" rx="2" stroke="currentColor" stroke-width="1.5"/>
              </svg>
            </div>
          </div>

          <div class="nav-divider"></div>
          <div class="nav-section-label">处理规则</div>

          <div class="nav-item" :class="{ active: activeRuleTab === 'datasets' }" @click="showRule('datasets')" title="Datasets 处理规则">
            <div class="nav-icon"><span class="nav-letter">D</span></div>
          </div>
          <div class="nav-item" :class="{ active: activeRuleTab === 'variables' }" @click="showRule('variables')" title="Variables 映射规则">
            <div class="nav-icon"><span class="nav-letter">S</span></div>
          </div>
          <div class="nav-item" :class="{ active: activeRuleTab === 'vlm' }" @click="showRule('vlm')" title="ValueLevel 生成规则">
            <div class="nav-icon"><span class="nav-letter">V</span></div>
          </div>
          <div class="nav-item" :class="{ active: activeRuleTab === 'codelist' }" @click="showRule('codelist')" title="Codelists 处理规则">
            <div class="nav-icon"><span class="nav-letter">C</span></div>
          </div>
          <div class="nav-item" :class="{ active: activeRuleTab === 'pages' }" @click="showRule('pages')" title="Pages 处理规则">
            <div class="nav-icon"><span class="nav-letter">P</span></div>
          </div>
        </nav>
      </aside>

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

    <!-- 处理规则对话框 -->
    <el-dialog v-model="showRuleDialog" :title="ruleDialogTitle" width="720px" class="saas-dialog rule-dialog" top="5vh" destroy-on-close>
      <div class="rule-content" v-html="currentRuleHtml"></div>
    </el-dialog>
  </template>

  <template v-else>
    <router-view></router-view>
  </template>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { ref, watch, computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'

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

const isProjectListActive = computed(() => {
  return route.path === '/' || route.path === '/ProjectList'
})

const navigateTo = (path) => {
  router.push({ path })
}

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
const showRuleDialog = ref(false)
const activeRuleTab = ref('')

const ruleDialogTitle = computed(() => {
  const titles = { datasets: 'Datasets 处理规则', variables: 'Variables 映射规则', vlm: 'ValueLevel 生成规则', codelist: 'Codelists 处理规则', pages: 'Pages 处理规则' }
  return titles[activeRuleTab.value] || '处理规则'
})

const datasetsRuleHtml = `
<h4>1. 数据来源</h4>
<p>Datasets 数据提取自上传的原始 Spec 的 <code>TOC</code> 工作表。TOC 表中的每一行代表一个数据集。</p>

<h4>2. 字段映射规则</h4>
<table class="rule-table">
  <thead><tr><th>P21 字段</th><th>TOC 列来源</th><th>说明</th></tr></thead>
  <tbody>
    <tr><td><b>Dataset</b></td><td>Dataset / Domain</td><td>数据集名称（如 DM, AE, LB），必填</td></tr>
    <tr><td><b>Label</b></td><td>Description / Label</td><td>数据集描述</td></tr>
    <tr><td><b>Class</b></td><td>Class / Observation Class</td><td>如 Events, Findings, Interventions, Special Purpose 等</td></tr>
    <tr><td><b>SubClass</b></td><td>SubClass</td><td>子分类，可选</td></tr>
    <tr><td><b>Structure</b></td><td>Structure</td><td>数据结构，如 "One record per subject" 等</td></tr>
    <tr><td><b>Key Variables</b></td><td>Key Variables / Keys</td><td>关键变量列表，逗号分隔</td></tr>
    <tr><td><b>Standard</b></td><td>Standard</td><td>标准名称，如 SDTM-IG 3.4</td></tr>
    <tr><td><b>Has No Data</b></td><td>Has No Data</td><td>是否无数据（Yes/No），默认空</td></tr>
    <tr><td><b>Repeating</b></td><td>Repeating</td><td>是否可重复（Yes/No），默认空</td></tr>
    <tr><td><b>Reference Data</b></td><td>Reference Data</td><td>是否为参考数据（Yes/No），默认空</td></tr>
    <tr><td><b>Comment</b></td><td>Comment / Comments</td><td>备注说明</td></tr>
    <tr><td><b>Developer Notes</b></td><td>Developer Notes</td><td>开发备注，可选</td></tr>
  </tbody>
</table>

<h4>3. 数据存储</h4>
<p>结果存入 <code>sas_datasets_data</code> 表（按用户隔离），用户可在 Datasets 页面的表格模式或 Excel 模式中编辑。</p>
`

const variablesRuleHtml = `
<h4>1. 上传与解析</h4>
<p>上传项目 Spec（Excel），系统自动识别工作表名为 2-4 个大写字母的 Domain Sheet（如 DM、AE、LB），跳过 TOC 等非域名 Sheet。匹配到 3 个及以上标准字段的 Sheet 才会被解析。</p>

<h4>2. Spec → P21 Variables 映射</h4>
<p>原始 Spec 字段经过处理后映射到 P21 标准的 Variables 字段：</p>
<table class="rule-table">
  <thead><tr><th>P21 Variables</th><th>Spec 来源</th><th>映射规则</th></tr></thead>
  <tbody>
    <tr><td><b>Order</b></td><td>Seq</td><td>自动编号</td></tr>
    <tr><td><b>Dataset</b></td><td>Domain</td><td>直接映射</td></tr>
    <tr><td><b>Variable</b></td><td>Variable Name</td><td>直接映射</td></tr>
    <tr><td><b>Label</b></td><td>Variable Label</td><td>直接映射</td></tr>
    <tr><td><b>Data Type</b></td><td>Type</td><td>Char → text, Num → integer/float</td></tr>
    <tr><td><b>Length</b></td><td>Length</td><td>直接映射（自动去 .0 后缀）</td></tr>
    <tr><td><b>Significant Digits</b></td><td>—</td><td>默认空，用户填写</td></tr>
    <tr><td><b>Format</b></td><td>Controlled Terms or Format</td><td>直接映射</td></tr>
    <tr><td><b>Mandatory</b></td><td>Core</td><td>Req → Yes, Exp/Perm → No</td></tr>
    <tr><td><b>Assigned Value</b></td><td>—</td><td>置空，用户填写</td></tr>
    <tr><td><b>Codelist</b></td><td>Controlled Terms or Format</td><td>直接映射</td></tr>
    <tr><td><b>Submission Value</b></td><td>CDISC Submission Value</td><td>直接映射</td></tr>
    <tr><td><b>Common</b></td><td>—</td><td>置空，用户填写</td></tr>
    <tr><td><b>Origin</b></td><td>Origin</td><td>直接映射</td></tr>
    <tr><td><b>Source</b></td><td>Source</td><td>直接映射</td></tr>
    <tr><td><b>Pages</b></td><td>CRF Page</td><td>直接映射</td></tr>
    <tr><td><b>Text</b></td><td>Text</td><td>Spec "Text" 列（原 Derived Method）</td></tr>
    <tr><td><b>Predecessor</b></td><td>—</td><td>置空，用户填写</td></tr>
    <tr><td><b>Role</b></td><td>Role</td><td>直接映射</td></tr>
    <tr><td><b>Has No Data</b></td><td>—</td><td>置空，用户填写</td></tr>
    <tr><td><b>Comment</b></td><td>Comment</td><td>直接映射</td></tr>
    <tr><td><b>Developer Notes</b></td><td>—</td><td>置空，用户填写</td></tr>
    <tr><td><b>SUPP</b></td><td>SUPP</td><td>直接映射</td></tr>
    <tr><td><b>QEVAL</b></td><td>QEVAL</td><td>直接映射</td></tr>
  </tbody>
</table>

<h4>3. 附加映射说明</h4>
<p><b>Method</b>：Spec "Method" 列值映射到 Method 字段（标记变量是否有方法描述）。<b>Codelist</b> 和 <b>Format</b> 均来自 Spec "Controlled Terms or Format" 列。</p>

<h4>4. 数据流向</h4>
<p>解析结果存入 <code>sas_project_spec</code> 表（按用户隔离） → 用户在 Variables 页面可修改 → 同步生成 <code>spec_synced_{projectId}.xlsx</code> → 下游 VLM / Codelists / Pages / Define 提取均读取此同步文件。</p>
`

const vlmRuleHtml = `
<h4>1. 前置依赖</h4>
<p>需要已上传 <b>XPT 数据集</b>和<b>项目 Spec</b>（已处理）。系统扫描 XPT 目录下可用的域名来确定哪些域参与提取。</p>

<h4>2. 提取配置</h4>
<p>系统按预置的 <code>vlm_configs</code> 逐项处理，每项包含：<code>(域名, 排序变量, 分组变量列表, 码表参数)</code>。典型配置如下：</p>
<table class="rule-table">
  <thead><tr><th>域</th><th>排序变量</th><th>分组变量</th><th>识别模式</th></tr></thead>
  <tbody>
    <tr><td>TS</td><td>TSSEQ</td><td>TSPARMCD, TSPARM</td><td>TSPARMCD 模式</td></tr>
    <tr><td>TX</td><td>TXSEQ</td><td>TXPARMCD, TXPARM</td><td>PARMCD 模式</td></tr>
    <tr><td>LB</td><td>LBSEQ</td><td>LBCAT, LBTESTCD, LBTEST</td><td>TESTCD 模式</td></tr>
    <tr><td>EG / PC / PP / VS / IE 等</td><td>各自 SEQ</td><td>各自 TESTCD + TEST</td><td>TESTCD 模式</td></tr>
  </tbody>
</table>

<h4>3. 字段生成规则</h4>
<table class="rule-table">
  <thead><tr><th>字段</th><th>生成规则</th></tr></thead>
  <tbody>
    <tr><td><b>Dataset</b></td><td>配置中的域名转大写（如 <code>lb → LB</code>）</td></tr>
    <tr><td><b>Variable</b></td><td>
      <b>TSPARMCD 模式</b>：<code>{DATASET}VAL</code>（如 TSVAL）<br/>
      <b>PARMCD 模式</b>：<code>{DATASET}VAL</code>（如 TXVAL）<br/>
      <b>TESTCD 模式</b>：<code>{DATASET}ORRES</code>（如 LBORRES）
    </td></tr>
    <tr><td><b>Where Clause</b></td><td>
      <b>TSPARMCD</b>：<code>TSPARMCD EQ "{该行 TSPARMCD 值}"</code><br/>
      <b>PARMCD</b>：<code>{域}PARMCD EQ "{该行 PARMCD 值}"</code><br/>
      <b>TESTCD</b>：<code>{域}TESTCD EQ "{该行 TESTCD 值}"</code>
    </td></tr>
    <tr><td><b>Label</b></td><td>取对应参数/测试名称列的值（如 TSPARM、LBTEST），即 <code>*CD</code> 列去掉 CD 的同名列</td></tr>
    <tr><td><b>Controlled Terms or Format</b></td><td>不自动填充（留空，用户可手动编辑）</td></tr>
    <tr><td><b>Origin</b></td><td><b>TSPARMCD / PARMCD</b>：<code>Assigned</code>；<b>TESTCD</b>：<code>CRF</code></td></tr>
    <tr><td><b>Pages</b></td><td>不自动填充（后续由 Pages 流程补充）</td></tr>
    <tr><td><b>Derivation/Comment</b></td><td>仅 TSPARMCD 模式取 <code>TSVAL</code> 列的值作为注释</td></tr>
    <tr><td><b>Method</b></td><td>仅 TSPARMCD 模式设为 <code>"Y"</code></td></tr>
    <tr><td><b>Comment / 类别</b></td><td>不自动填充</td></tr>
  </tbody>
</table>

<h4>4. 去重与排序</h4>
<p>对每组 XPT 数据按分组变量 <code>drop_duplicates</code> 后逐行生成，最终按 Dataset、Variable、Where Clause 排序。结果存入 <code>sas_vlm_data</code> 表供用户审查编辑。</p>
`

const codelistRuleHtml = `
<h4>1. 前置依赖</h4>
<p>需要已上传 <b>XPT 数据集</b>和<b>项目 Spec</b>（已处理）。</p>

<h4>2. 提取流程</h4>
<ol>
  <li><b>从 Spec 识别需要码表的变量</b>：遍历每个域的 Spec，读取 <code>CDISC Submission Value</code> / <code>Controlled Terms or Format</code> / <code>Codelist</code> 列（按优先级），有值的变量需要生成 CodeList</li>
  <li><b>从 XPT 提取实际值</b>：读取对应 XPT 文件，收集该变量的所有唯一值（<code>#</code> 分隔拼接）</li>
  <li><b>拆分为逐行码表</b>：将拼接的值按 <code>#</code> 拆开，每个值为一条 code 行</li>
  <li><b>合并 VLM 阶段产生的附加码表</b>（如 ARM/ARMCD 等域配置生成的码表行）</li>
  <li><b>后处理</b>：按 <code>vcd → cdnum</code> 重排序号；对 <code>type=Num</code> 且有 <code>codeDes</code> 的，把 code 设为序号</li>
</ol>

<h4>3. 字段说明</h4>
<table class="rule-table">
  <thead><tr><th>字段</th><th>来源与规则</th></tr></thead>
  <tbody>
    <tr><td><b>vcd</b></td><td>码表标识符，取自 Spec 的 <code>CDISC Submission Value</code> 或 <code>Controlled Terms or Format</code> 列值</td></tr>
    <tr><td><b>vlabel</b></td><td>变量标签，来自 Spec 对应变量的 Label</td></tr>
    <tr><td><b>type</b></td><td>数据类型，来自 Spec（Char / Num），默认 Char</td></tr>
    <tr><td><b>cdnum</b></td><td>代码序号，在每个 vcd 组内从 1 开始自动编号</td></tr>
    <tr><td><b>code</b></td><td>代码值，从 XPT 中该变量的实际唯一值提取；<code>type=Num</code> 且有 codeDes 时设为序号</td></tr>
    <tr><td><b>code_des</b></td><td>代码描述（Decoded Value），目前留空由用户填写</td></tr>
    <tr><td><b>code_ver</b></td><td>代码版本（如 CDISC CT 版本号），留空由用户填写</td></tr>
    <tr><td><b>flag</b></td><td>固定域标记：<code>ARM, ARMCD, VISIT, VISITNUM, IETEST, IETESTCD</code> 等预设域的 flag 为 <code>"Y"</code>，其余为空</td></tr>
  </tbody>
</table>

<h4>4. 过滤规则</h4>
<p>Spec 中变量名包含 <code>CATN</code> 或 <code>TESTN</code> 的行会被自动跳过（非标准编码变量）。Spec 中受控术语列为空的行也会被跳过。</p>

<h4>5. 数据存储</h4>
<p>结果存入 <code>sas_codelist_data</code> 表，按用户隔离，用户可在 CodeList 编辑页面审查修改。</p>
`

const pagesRuleHtml = `
<h4>1. 前置依赖</h4>
<p>需要已上传 <b>aCRF</b>（处理后生成 <code>Annots2.xlsx</code>）和<b>项目 Spec</b>（已处理）。</p>

<h4>2. 提取流程</h4>
<ol>
  <li>读取 <code>Annots2.xlsx</code>，解析每条 PDF 注释的文本内容和所在页码</li>
  <li>读取 Spec 中所有域的变量列表</li>
  <li>将注释文本与变量名进行匹配（支持 <code>DOMAIN.VARIABLE</code> 和 <code>VARIABLE</code> 两种格式）</li>
  <li>汇总每个变量对应的所有页码，去重、排序后拼接为字符串（如 "12, 15, 23"）</li>
</ol>

<h4>3. 字段说明</h4>
<table class="rule-table">
  <thead><tr><th>字段</th><th>来源与规则</th></tr></thead>
  <tbody>
    <tr><td><b>dataset</b></td><td>域名（如 DM、AE），从 Spec 的 Domain Sheet 名获取</td></tr>
    <tr><td><b>variable</b></td><td>变量名，来自 Spec</td></tr>
    <tr><td><b>pages</b></td><td>注释文本匹配到的 PDF 页码，去重排序后逗号拼接</td></tr>
    <tr><td><b>origin</b></td><td>来自 Spec 的 Origin 字段</td></tr>
  </tbody>
</table>

<h4>4. 数据存储</h4>
<p>结果存入 <code>sas_pages_data</code> 表，按用户隔离，用户可在 Pages 编辑页面审查修改。</p>
`

const ruleContents = { datasets: datasetsRuleHtml, variables: variablesRuleHtml, vlm: vlmRuleHtml, codelist: codelistRuleHtml, pages: pagesRuleHtml }
const currentRuleHtml = computed(() => ruleContents[activeRuleTab.value] || '')

const showRule = (tab) => {
  activeRuleTab.value = tab
  showRuleDialog.value = true
}

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

.saas-sidebar {
  width: var(--saas-sidebar-width);
  background: var(--saas-bg-sidebar);
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  z-index: 100;
  overflow: hidden;

  .sidebar-brand {
    height: var(--saas-topbar-height);
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    flex-shrink: 0;
    width: 100%;
    cursor: default;

    .brand-icon {
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .sidebar-nav {
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
    transition: all var(--saas-transition);

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

  .nav-divider {
    width: 28px;
    height: 1px;
    background: rgba(255, 255, 255, 0.12);
    margin: 8px auto;
  }

  .nav-section-label {
    font-size: 8px;
    color: rgba(255, 255, 255, 0.3);
    text-align: center;
    letter-spacing: 0.05em;
    margin-bottom: 4px;
    line-height: 1;
  }

  .nav-letter {
    font-size: 14px;
    font-weight: 700;
    font-family: 'SF Mono', 'Consolas', monospace;
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

// 处理规则对话框
:deep(.rule-dialog) {
  .rule-content {
    font-size: 14px;
    line-height: 1.7;
    color: var(--saas-text-primary, #1f2937);
    max-height: 70vh;
    overflow-y: auto;

    h4 {
      margin: 18px 0 8px;
      font-size: 15px;
      color: var(--saas-text-primary, #1f2937);
      &:first-child { margin-top: 0; }
    }
    p { margin: 6px 0; }
    code {
      background: var(--saas-bg-input, #f3f4f6);
      padding: 1px 6px;
      border-radius: 4px;
      font-size: 13px;
      color: var(--saas-primary-dark, #2563eb);
    }
    ol, ul { padding-left: 20px; margin: 6px 0; }
    li { margin: 4px 0; }

    .rule-table {
      width: 100%;
      border-collapse: collapse;
      margin: 10px 0;
      font-size: 13px;

      th, td {
        border: 1px solid var(--saas-border-light, #e5e7eb);
        padding: 8px 12px;
        text-align: left;
      }
      th {
        background: var(--saas-bg-input, #f3f4f6);
        font-weight: 600;
        font-size: 12px;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
      td:first-child {
        width: 160px;
        white-space: nowrap;
        font-family: 'SF Mono', 'Consolas', monospace;
      }
    }
  }
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
