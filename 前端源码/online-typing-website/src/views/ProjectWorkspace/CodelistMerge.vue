<template>
  <div class="codelist-merge-page">
    <div class="table-actions">
      <el-button text :icon="ArrowLeft" size="small" @click="goBack">返回 Codelists</el-button>
      <div class="ws-btn-group ws-btn-group--extract">
          <el-button size="small" :loading="analyzing" @click="analyze">
            分析合并候选
          </el-button>
          <el-button
            v-if="pendingRules.length > 0"
            size="small"
            :loading="reapplying"
            @click="confirmReapplyAll"
          >
            一键合并 ({{ pendingRules.length }} 组)
          </el-button>
          <el-button size="small" @click="showLogDialog">
            合并历史
          </el-button>
      </div>
    </div>

    <div class="merge-content" v-loading="analyzing" element-loading-text="正在分析Codelist...">
      <el-empty v-if="!analyzed && clusters.length === 0" description='点击「分析合并候选」开始扫描可合并的Codelist' />

      <div v-if="analyzed && clusters.length === 0" class="no-merge">
        <el-result icon="success" title="无需合并" sub-title="所有Codelist的Term集合均不重复且无子集关系" />
      </div>

      <div v-if="clusters.length > 0" class="merge-layout">
        <!-- 左侧：合并候选组列表 -->
        <div class="group-sidebar">
          <div class="sidebar-header">
            <span class="sidebar-title">合并候选组</span>
            <el-tag type="info" size="small">{{ clusters.length }} 组</el-tag>
          </div>
          <div class="group-list">
            <div
              v-for="(cluster, idx) in clusters" :key="idx"
              class="group-item"
              :class="{
                active: selectedIdx === idx,
                merged: mergedSet.has(idx),
                skipped: skippedSet.has(idx),
                'has-subset': cluster.type === 'subset_chain',
                'has-ct-align': cluster.type === 'ct_aligned'
              }"
              @click="selectCluster(idx)"
            >
              <div class="group-header">
                <el-tag v-if="mergedSet.has(idx)" type="success" size="small" effect="dark">已合并</el-tag>
                <el-tag v-else-if="skippedSet.has(idx)" type="info" size="small">已跳过</el-tag>
                <el-tag v-else-if="cluster.type === 'ct_aligned'" type="warning" size="small" effect="dark">有交叉</el-tag>
                <el-tag v-else-if="cluster.type === 'subset_chain'" type="danger" size="small">含子集关系</el-tag>
                <el-tag v-else-if="hasPendingRule(cluster)" type="warning" size="small">有历史规则</el-tag>
                <el-tag v-else type="info" size="small">完全相同</el-tag>
                <el-tag v-if="batchSiblingInfo(idx).total > 1" size="small" type="info" effect="plain">
                  批次 {{ batchSiblingInfo(idx).pos }}/{{ batchSiblingInfo(idx).total }}
                </el-tag>
                <el-tag size="small" effect="plain">{{ cluster.totalVcdCount }} 个VCD</el-tag>
              </div>
              <div class="group-terms" :title="cluster.supersetGroup.termList">
                {{ cluster.supersetGroup.termCount }} 个Terms: {{ truncate(cluster.supersetGroup.termList, 50) }}
              </div>
              <div class="group-vcds">
                {{ truncate(cluster.allVcds.join(', '), 60) }}
              </div>
              <div v-if="cluster.type === 'subset_chain'" class="group-subset-hint">
                超集 + {{ cluster.subsetGroups.length }} 组子集
              </div>
              <div v-else-if="cluster.type === 'ct_aligned'" class="group-ct-hint">
                有交叉 · CT建议 {{ cluster.ct_codelist_code }} · {{ cluster.subsetGroups.length }} 组
              </div>
              <div v-if="mergedSet.has(idx)" class="group-merged-info">
                → {{ mergedTargets[idx] }}
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：详情 -->
        <div class="detail-panel" v-if="selectedCluster">
          <!-- 完全相同类型 -->
          <template v-if="selectedCluster.type === 'identity'">
            <div class="compare-section">
              <div class="compare-block before-block">
                <div class="block-title">
                  <el-tag type="warning">合并前</el-tag>
                  <span>{{ activeVcdDetails.length }} 个独立Codelist，Terms完全相同</span>
                  <span v-if="excludedVcds.length > 0" class="excluded-hint">（已移除 {{ excludedVcds.length }} 个）</span>
                </div>
                <el-table :data="activeVcdDetails" border size="small" max-height="280" stripe>
                  <el-table-column prop="vcd" label="Codelist ID" width="160" show-overflow-tooltip />
                  <el-table-column prop="vlabel" label="Name" min-width="130" show-overflow-tooltip />
                  <el-table-column prop="specRefCount" label="Vars引用" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.specRefCount > 0" size="small">{{ row.specRefCount }}</el-tag>
                      <span v-else class="no-ref">0</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="vlmRefCount" label="VLM引用" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.vlmRefCount > 0" size="small">{{ row.vlmRefCount }}</el-tag>
                      <span v-else class="no-ref">0</span>
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!mergedSet.has(selectedIdx) && targets.length > 0" label="目标分配" width="140" align="center">
                    <template #default="{ row }">
                      <el-select
                        :model-value="vcdTargetMap[row.vcd]"
                        @update:model-value="val => vcdTargetMap[row.vcd] = val"
                        size="small" style="width: 120px"
                      >
                        <el-option
                          v-for="opt in targetOptionLabels()"
                          :key="opt.value" :value="opt.value" :label="opt.label"
                        />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="60" align="center" v-if="!mergedSet.has(selectedIdx)">
                    <template #default="{ row }">
                      <el-button
                        v-if="activeVcdDetails.length > 2"
                        type="danger" link size="small"
                        @click="excludeVcd(row.vcd)"
                      >移除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="target-add-row" v-if="!mergedSet.has(selectedIdx)">
                  <el-button size="small" type="primary" plain @click="addTarget">+ 添加目标</el-button>
                  <span class="target-hint">将不同VCD分配到不同目标Codelist</span>
                </div>
                <div v-if="excludedVcds.length > 0" class="excluded-list">
                  <span class="excluded-label">已移除:</span>
                  <el-tag
                    v-for="vcd in excludedVcds" :key="vcd"
                    size="small" closable type="info"
                    @close="restoreVcd(vcd)"
                  >{{ vcd }}</el-tag>
                </div>
                <div class="terms-preview">
                  <span class="terms-label">共有Terms ({{ selectedCluster.supersetGroup.termCount }}):</span>
                  <div class="terms-tags">
                    <el-tag v-for="term in supersetTermsList" :key="term" size="small" class="term-tag">{{ term }}</el-tag>
                  </div>
                </div>
              </div>

              <div class="compare-block after-block">
                <div class="block-title">
                  <el-tag type="success">合并后</el-tag>
                  <span v-if="targets.length <= 1">合并为单个Codelist，所有引用自动更新</span>
                  <span v-else>合并为 {{ targets.length }} 个目标Codelist</span>
                </div>

                <!-- CT匹配按钮 -->
                <div class="ct-match-row" v-if="!mergedSet.has(selectedIdx)">
                  <el-button size="small" @click="suggestFromCt" :loading="ctSuggesting">CT匹配</el-button>
                  <template v-if="ctSuggestion && ctSuggestion.codelist_code">
                    <span class="ct-hint">CT精确匹配: </span>
                    <el-link type="primary" @click="applySuggestion()">{{ ctSuggestion.codelist_code }} - {{ ctSuggestion.codelist_name }}</el-link>
                  </template>
                  <template v-else-if="ctSuggestion && ctSuggestion.suggestions">
                    <div class="ct-hint ct-fuzzy">
                      <div class="ct-reason">{{ ctSuggestion.reason }}</div>
                      <div v-for="s in ctSuggestion.suggestions" :key="s.codelist_code" class="ct-fuzzy-item">
                        <el-link type="primary" @click="applySuggestion(s)">{{ s.codelist_code }} - {{ s.codelist_name }}</el-link>
                        <el-tag size="small" type="warning">匹配 {{ s.matched_count }}/{{ s.total_count }}</el-tag>
                      </div>
                    </div>
                  </template>
                  <span v-else-if="ctSuggestion && ctSuggestion.reason" class="ct-hint ct-no-match">{{ ctSuggestion.reason }}</span>
                </div>

                <!-- 目标配置卡片 -->
                <div v-for="(t, tIdx) in targets" :key="tIdx" class="target-card" :class="{ 'single-vcd-warn': targetVcdCount(t.id) < 2 }">
                  <div class="target-card-header">
                    <el-tag size="small" :type="targetVcdCount(t.id) >= 2 ? 'success' : 'info'">
                      目标 {{ tIdx + 1 }} ({{ targetVcdCount(t.id) }} 个VCD)
                    </el-tag>
                    <el-button
                      v-if="targets.length > 1 && !mergedSet.has(selectedIdx)"
                      type="danger" link size="small"
                      @click="removeTarget(tIdx)"
                    >删除目标</el-button>
                  </div>
                  <el-form label-width="120px" size="small" class="merge-form">
                    <el-form-item label="Codelist ID">
                      <el-input :model-value="t.id" @update:model-value="val => updateTargetId(tIdx, val)" placeholder="目标ID" clearable :disabled="mergedSet.has(selectedIdx)" />
                    </el-form-item>
                    <el-form-item label="Name">
                      <el-input v-model="t.vlabel" placeholder="目标Name" clearable :disabled="mergedSet.has(selectedIdx)" />
                    </el-form-item>
                    <el-form-item label="NCI Code">
                      <el-input v-model="t.nciCode" placeholder="NCI代码（可选）" clearable :disabled="mergedSet.has(selectedIdx)" />
                    </el-form-item>
                  </el-form>
                  <div v-if="targetVcdCount(t.id) < 2 && !mergedSet.has(selectedIdx)" class="target-warn">
                    此目标仅有 {{ targetVcdCount(t.id) }} 个VCD，至少需要2个才能合并
                  </div>
                  <div v-if="targetVcdCount(t.id) >= 1" class="target-vcds-preview">
                    VCDs: {{ (targetGroups[t.id] || []).join(', ') }}
                  </div>
                </div>

                <div v-if="!mergedSet.has(selectedIdx)" class="impact-summary" style="margin-top:12px">
                  <el-tag type="info">总VCD: {{ activeVcdDetails.length }}</el-tag>
                  <el-tag type="warning">Variables引用: {{ totalSpecRefs }} 条</el-tag>
                  <el-tag type="warning">VLM引用: {{ totalVlmRefs }} 条</el-tag>
                </div>

                <div v-if="currentTermDetails.length > 0" class="term-details">
                  <div class="term-details-title">Term 详情 ({{ currentTermDetails.length }})</div>
                  <el-table :data="currentTermDetails" border size="small" max-height="240" stripe>
                    <el-table-column type="index" label="#" width="45" align="center" />
                    <el-table-column prop="code" label="Term (Code)" min-width="140" show-overflow-tooltip />
                    <el-table-column prop="nci_term_code" label="NCI Term Code" width="140" show-overflow-tooltip>
                      <template #default="{ row }">{{ row.nci_term_code || '-' }}</template>
                    </el-table-column>
                    <el-table-column prop="code_des" label="Decoded Value" min-width="200" show-overflow-tooltip>
                      <template #default="{ row }">{{ row.code_des || '-' }}</template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </div>

            <div class="merge-actions">
              <el-button
                v-if="!mergedSet.has(selectedIdx)"
                size="default"
                @click="skipCluster"
              >不合并</el-button>
              <el-button
                v-if="!mergedSet.has(selectedIdx)"
                type="primary" size="default" :loading="merging"
                :disabled="targets.filter(t => targetVcdCount(t.id) >= 2).length === 0"
                @click="confirmBatchMerge"
              >执行合并</el-button>
              <template v-if="mergedSet.has(selectedIdx)">
                <el-tag type="success" size="large" effect="light">此组已合并</el-tag>
                <el-button type="warning" size="default" :loading="undoing" @click="confirmUndo">撤销合并</el-button>
              </template>
            </div>
          </template>

          <!-- 子集关系类型 / CT 对齐类型（共用模板） -->
          <template v-if="selectedCluster.type === 'subset_chain' || selectedCluster.type === 'ct_aligned'">
            <div class="subset-chain-view">
              <!-- 超集 / CT 全集 -->
              <div class="chain-block superset-block" :class="{ 'ct-aligned-block': selectedCluster.type === 'ct_aligned' }">
                <div class="block-title">
                  <template v-if="selectedCluster.type === 'ct_aligned'">
                    <el-tag type="warning" effect="dark">合并后 Term 集（并集）</el-tag>
                    <span>{{ selectedCluster.supersetGroup.termCount }} 个Terms</span>
                    <el-tag type="info" size="small" effect="plain">
                      CT 建议: {{ selectedCluster.ct_codelist_code }} - {{ selectedCluster.ct_codelist_name }}
                    </el-tag>
                  </template>
                  <template v-else>
                    <el-tag type="danger" effect="dark">超集</el-tag>
                    <span>{{ selectedCluster.supersetGroup.termCount }} 个Terms — 包含所有子集的Term</span>
                  </template>
                </div>
                <!-- ct_aligned 的超集没有项目vcd（虚拟全集），跳过表格 -->
                <el-table v-if="selectedCluster.supersetGroup.vcdCount > 0" :data="chainActiveSupVcds" border size="small" max-height="200" stripe>
                  <el-table-column prop="vcd" label="Codelist ID" width="160" show-overflow-tooltip />
                  <el-table-column prop="vlabel" label="Name" min-width="130" show-overflow-tooltip />
                  <el-table-column prop="specRefCount" label="Vars引用" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.specRefCount > 0" size="small">{{ row.specRefCount }}</el-tag>
                      <span v-else class="no-ref">0</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="vlmRefCount" label="VLM引用" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.vlmRefCount > 0" size="small">{{ row.vlmRefCount }}</el-tag>
                      <span v-else class="no-ref">0</span>
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!mergedSet.has(selectedIdx) && targets.length > 0" label="目标分配" width="140" align="center">
                    <template #default="{ row }">
                      <el-select
                        :model-value="vcdTargetMap[row.vcd]"
                        @update:model-value="val => vcdTargetMap[row.vcd] = val"
                        size="small" style="width: 120px"
                      >
                        <el-option
                          v-for="opt in targetOptionLabels()"
                          :key="opt.value" :value="opt.value" :label="opt.label"
                        />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="60" align="center" v-if="!mergedSet.has(selectedIdx)">
                    <template #default="{ row }">
                      <el-button
                        v-if="chainAllActiveVcds.length > 2"
                        type="danger" link size="small"
                        @click="excludeVcd(row.vcd)"
                      >移除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="terms-preview">
                  <span class="terms-label">
                    {{ selectedCluster.type === 'ct_aligned' ? '并集 Terms' : '超集Terms' }}
                    ({{ selectedCluster.supersetGroup.termCount }}):
                  </span>
                  <div class="terms-tags">
                    <el-tag v-for="term in supersetTermsList" :key="term" size="small" class="term-tag">{{ term }}</el-tag>
                  </div>
                </div>
              </div>

              <!-- 各子集组（subset_chain 是真子集；ct_aligned 是项目侧的"局部子集"） -->
              <div v-for="(subGroup, sIdx) in selectedCluster.subsetGroups" :key="sIdx" class="chain-block subset-block">
                <div class="block-title">
                  <el-tag type="warning">
                    {{ selectedCluster.type === 'ct_aligned' ? '项目子集' : '子集' }} {{ sIdx + 1 }}
                  </el-tag>
                  <span>{{ subGroup.termCount }} 个Terms</span>
                  <el-tag v-if="subGroup.missingCount > 0" type="danger" size="small" effect="plain">
                    缺少: {{ subGroup.missingTerms }}
                  </el-tag>
                </div>
                <el-table :data="chainActiveSubVcds(subGroup)" border size="small" max-height="200" stripe>
                  <el-table-column prop="vcd" label="Codelist ID" width="160" show-overflow-tooltip />
                  <el-table-column prop="vlabel" label="Name" min-width="130" show-overflow-tooltip />
                  <el-table-column prop="specRefCount" label="Vars引用" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.specRefCount > 0" size="small">{{ row.specRefCount }}</el-tag>
                      <span v-else class="no-ref">0</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="vlmRefCount" label="VLM引用" width="80" align="center">
                    <template #default="{ row }">
                      <el-tag v-if="row.vlmRefCount > 0" size="small">{{ row.vlmRefCount }}</el-tag>
                      <span v-else class="no-ref">0</span>
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!mergedSet.has(selectedIdx) && targets.length > 0" label="目标分配" width="140" align="center">
                    <template #default="{ row }">
                      <el-select
                        :model-value="vcdTargetMap[row.vcd]"
                        @update:model-value="val => vcdTargetMap[row.vcd] = val"
                        size="small" style="width: 120px"
                      >
                        <el-option
                          v-for="opt in targetOptionLabels()"
                          :key="opt.value" :value="opt.value" :label="opt.label"
                        />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="60" align="center" v-if="!mergedSet.has(selectedIdx)">
                    <template #default="{ row }">
                      <el-button
                        v-if="chainAllActiveVcds.length > 2"
                        type="danger" link size="small"
                        @click="excludeVcd(row.vcd)"
                      >移除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="terms-preview">
                  <span class="terms-label">子集Terms ({{ subGroup.termCount }}):</span>
                  <div class="terms-tags">
                    <el-tag v-for="term in (subGroup.termList || '').split(', ').filter(t => t)" :key="term" size="small" class="term-tag">{{ term }}</el-tag>
                  </div>
                </div>
              </div>

              <div class="target-add-row" v-if="!mergedSet.has(selectedIdx)">
                <el-button size="small" type="primary" plain @click="addTarget">+ 添加目标</el-button>
                <span class="target-hint">将不同VCD分配到不同目标Codelist</span>
              </div>

              <!-- 已移除列表 -->
              <div v-if="excludedVcds.length > 0" class="excluded-list">
                <span class="excluded-label">已移除:</span>
                <el-tag
                  v-for="vcd in excludedVcds" :key="vcd"
                  size="small" closable type="info"
                  @close="restoreVcd(vcd)"
                >{{ vcd }}</el-tag>
              </div>

              <!-- 合并操作区 -->
              <div class="compare-block after-block">
                <div class="block-title">
                  <el-tag type="success">合并后</el-tag>
                  <span v-if="targets.length <= 1">将所有VCD合并为并集，引用自动更新</span>
                  <span v-else>合并为 {{ targets.length }} 个目标Codelist</span>
                </div>

                <!-- CT匹配按钮 -->
                <div class="ct-match-row" v-if="!mergedSet.has(selectedIdx)">
                  <el-button size="small" @click="suggestFromCt" :loading="ctSuggesting">CT匹配</el-button>
                  <template v-if="ctSuggestion && ctSuggestion.codelist_code">
                    <span class="ct-hint">CT精确匹配: </span>
                    <el-link type="primary" @click="applySuggestion()">{{ ctSuggestion.codelist_code }} - {{ ctSuggestion.codelist_name }}</el-link>
                  </template>
                  <template v-else-if="ctSuggestion && ctSuggestion.suggestions">
                    <div class="ct-hint ct-fuzzy">
                      <div class="ct-reason">{{ ctSuggestion.reason }}</div>
                      <div v-for="s in ctSuggestion.suggestions" :key="s.codelist_code" class="ct-fuzzy-item">
                        <el-link type="primary" @click="applySuggestion(s)">{{ s.codelist_code }} - {{ s.codelist_name }}</el-link>
                        <el-tag size="small" type="warning">匹配 {{ s.matched_count }}/{{ s.total_count }}</el-tag>
                      </div>
                    </div>
                  </template>
                  <span v-else-if="ctSuggestion && ctSuggestion.reason" class="ct-hint ct-no-match">{{ ctSuggestion.reason }}</span>
                </div>

                <!-- 目标配置卡片 -->
                <div v-for="(t, tIdx) in targets" :key="tIdx" class="target-card" :class="{ 'single-vcd-warn': targetVcdCount(t.id) < 2 }">
                  <div class="target-card-header">
                    <el-tag size="small" :type="targetVcdCount(t.id) >= 2 ? 'success' : 'info'">
                      目标 {{ tIdx + 1 }} ({{ targetVcdCount(t.id) }} 个VCD)
                    </el-tag>
                    <el-button
                      v-if="targets.length > 1 && !mergedSet.has(selectedIdx)"
                      type="danger" link size="small"
                      @click="removeTarget(tIdx)"
                    >删除目标</el-button>
                  </div>
                  <el-form label-width="120px" size="small" class="merge-form">
                    <el-form-item label="Codelist ID">
                      <el-input :model-value="t.id" @update:model-value="val => updateTargetId(tIdx, val)" placeholder="目标ID" clearable :disabled="mergedSet.has(selectedIdx)" />
                    </el-form-item>
                    <el-form-item label="Name">
                      <el-input v-model="t.vlabel" placeholder="目标Name" clearable :disabled="mergedSet.has(selectedIdx)" />
                    </el-form-item>
                    <el-form-item label="NCI Code">
                      <el-input v-model="t.nciCode" placeholder="NCI代码（可选）" clearable :disabled="mergedSet.has(selectedIdx)" />
                    </el-form-item>
                  </el-form>
                  <div v-if="targetVcdCount(t.id) < 2 && !mergedSet.has(selectedIdx)" class="target-warn">
                    此目标仅有 {{ targetVcdCount(t.id) }} 个VCD，至少需要2个才能合并
                  </div>
                  <div v-if="targetVcdCount(t.id) >= 1" class="target-vcds-preview">
                    VCDs: {{ (targetGroups[t.id] || []).join(', ') }}
                  </div>
                </div>

                <div v-if="!mergedSet.has(selectedIdx)" class="impact-summary" style="margin-top:12px">
                  <el-tag type="info">总VCD: {{ chainAllActiveVcds.length }}</el-tag>
                  <el-tag type="warning">Variables引用: {{ chainTotalSpecRefs }} 条</el-tag>
                  <el-tag type="warning">VLM引用: {{ chainTotalVlmRefs }} 条</el-tag>
                </div>

                <div v-if="currentTermDetails.length > 0" class="term-details">
                  <div class="term-details-title">合并后Term 详情 ({{ currentTermDetails.length }})</div>
                  <el-table :data="currentTermDetails" border size="small" max-height="240" stripe>
                    <el-table-column type="index" label="#" width="45" align="center" />
                    <el-table-column prop="code" label="Term (Code)" min-width="140" show-overflow-tooltip />
                    <el-table-column prop="nci_term_code" label="NCI Term Code" width="140" show-overflow-tooltip>
                      <template #default="{ row }">{{ row.nci_term_code || '-' }}</template>
                    </el-table-column>
                    <el-table-column prop="code_des" label="Decoded Value" min-width="200" show-overflow-tooltip>
                      <template #default="{ row }">{{ row.code_des || '-' }}</template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>

              <div class="merge-actions">
                <el-button
                  v-if="!mergedSet.has(selectedIdx)"
                  size="default"
                  @click="skipCluster"
                >不合并</el-button>
                <el-button
                  v-if="!mergedSet.has(selectedIdx)"
                  type="primary" size="default" :loading="merging"
                  :disabled="targets.filter(t => targetVcdCount(t.id) >= 2).length === 0"
                  @click="confirmBatchMerge"
                >合并 Codelist</el-button>
                <template v-if="mergedSet.has(selectedIdx)">
                  <el-tag type="success" size="large" effect="light">此组已合并</el-tag>
                  <el-button type="warning" size="default" :loading="undoing" @click="confirmUndo">撤销合并</el-button>
                </template>
              </div>
            </div>
          </template>
        </div>

        <div v-else class="detail-placeholder">
          <el-empty description="选择左侧一个候选组查看详情" />
        </div>
      </div>
    </div>

    <!-- 合并历史对话框 -->
    <el-dialog v-model="logDialogVisible" title="合并历史记录" width="700px" destroy-on-close>
      <el-table :data="mergeLogs" border size="small" max-height="400">
        <el-table-column prop="original_vcd" label="原始VCD" width="160" show-overflow-tooltip />
        <el-table-column prop="merged_vcd" label="合并为" width="140" show-overflow-tooltip />
        <el-table-column prop="merged_vlabel" label="合并后Name" min-width="140" show-overflow-tooltip />
        <el-table-column prop="merged_nci_code" label="NCI Code" width="120" show-overflow-tooltip />
        <el-table-column prop="merge_time" label="合并时间" width="160" />
      </el-table>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import service from '@/axios'

const props = defineProps({ projectId: String })
const route = useRoute()
const router = useRouter()
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const currentProjectId = computed(() => props.projectId || route.params.projectId)

const analyzing = ref(false)
const analyzed = ref(false)
const clusters = ref([])
const selectedIdx = ref(null)
const mergedSet = ref(new Set())
const mergedTargets = ref({})
const merging = ref(false)
const undoing = ref(false)
const reapplying = ref(false)
const ctSuggesting = ref(false)
const ctSuggestion = ref(null)
const excludedVcds = ref([])
const pendingRules = ref([])

const logDialogVisible = ref(false)
const mergeLogs = ref([])

const mergeForm = ref({
  targetVcd: '',
  targetVlabel: '',
  targetNciCode: ''
})

const targets = ref([])
const vcdTargetMap = ref({})
// Per-cluster snapshot of what was actually merged (used to restore the right panel
// correctly when user re-selects a merged cluster). Map<clusterIdx, [{id, vlabel, nciCode, sourceVcds}]>
const mergedTargetsDetail = ref({})

const selectedCluster = computed(() => {
  if (selectedIdx.value === null) return null
  return clusters.value[selectedIdx.value]
})

const allVcdDetails = computed(() => {
  if (!selectedCluster.value) return []
  return selectedCluster.value.supersetGroup?.vcds || []
})

const activeVcdDetails = computed(() => {
  return allVcdDetails.value.filter(d => !excludedVcds.value.includes(d.vcd))
})

const supersetTermsList = computed(() => {
  if (!selectedCluster.value) return []
  const tl = selectedCluster.value.supersetGroup?.termList || ''
  return tl.split(', ').filter(t => t.trim())
})

const currentTermDetails = computed(() => {
  if (!selectedCluster.value) return []
  return selectedCluster.value.mergedTermDetails || []
})

const totalSpecRefs = computed(() => {
  return activeVcdDetails.value.reduce((sum, d) => sum + (d.specRefCount || 0), 0)
})

const totalVlmRefs = computed(() => {
  return activeVcdDetails.value.reduce((sum, d) => sum + (d.vlmRefCount || 0), 0)
})

const chainActiveSupVcds = computed(() => {
  if (!selectedCluster.value) return []
  const vcds = selectedCluster.value.supersetGroup?.vcds || []
  return vcds.filter(d => !excludedVcds.value.includes(d.vcd))
})

function chainActiveSubVcds(subGroup) {
  if (!subGroup?.vcds) return []
  return subGroup.vcds.filter(d => !excludedVcds.value.includes(d.vcd))
}

const chainAllActiveVcds = computed(() => {
  if (!selectedCluster.value) return []
  const result = []
  const seen = new Set()
  const sup = selectedCluster.value.supersetGroup
  if (sup?.vcds) {
    sup.vcds.forEach(v => {
      if (!excludedVcds.value.includes(v.vcd) && !seen.has(v.vcd)) { seen.add(v.vcd); result.push(v) }
    })
  }
  const subs = selectedCluster.value.subsetGroups || []
  subs.forEach(sg => {
    if (sg.vcds) {
      sg.vcds.forEach(v => {
        if (!excludedVcds.value.includes(v.vcd) && !seen.has(v.vcd)) { seen.add(v.vcd); result.push(v) }
      })
    }
  })
  return result
})

const chainTotalSpecRefs = computed(() => {
  return chainAllActiveVcds.value.reduce((sum, v) => sum + (v.specRefCount || 0), 0)
})

const chainTotalVlmRefs = computed(() => {
  return chainAllActiveVcds.value.reduce((sum, v) => sum + (v.vlmRefCount || 0), 0)
})

const skippedSet = ref(new Set())

function truncate(str, max) {
  if (!str) return ''
  return str.length > max ? str.substring(0, max) + '...' : str
}

function batchSiblingInfo(idx) {
  const c = clusters.value[idx]
  if (!c || !c.merge_batch_id) return { pos: 0, total: 0 }
  const bid = c.merge_batch_id
  const siblings = clusters.value
    .map((cc, i) => ({ i, match: cc.merge_batch_id === bid }))
    .filter(x => x.match)
  const pos = siblings.findIndex(x => x.i === idx) + 1
  return { pos, total: siblings.length }
}

function makeBatchId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    try { return crypto.randomUUID() } catch { /* fallthrough */ }
  }
  return 'batch-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10)
}

// After a multi-target merge, replace the original (single) cluster entry with N
// virtual sub-cluster entries — one per target — so the sidebar shows them
// immediately (without forcing a refresh).
function splitClusterIntoSubClusters(srcIdx, succeededDetails, batchId) {
  const src = clusters.value[srcIdx]
  if (!src || !succeededDetails || succeededDetails.length <= 1) return false

  const vcdDetailMap = new Map()
  if (src.supersetGroup?.vcds) {
    src.supersetGroup.vcds.forEach(v => { if (v && v.vcd) vcdDetailMap.set(v.vcd, v) })
  }
  if (Array.isArray(src.subsetGroups)) {
    src.subsetGroups.forEach(sg => {
      if (sg && Array.isArray(sg.vcds)) {
        sg.vcds.forEach(v => { if (v && v.vcd) vcdDetailMap.set(v.vcd, v) })
      }
    })
  }

  const orderedDetails = [...succeededDetails].sort((a, b) => (a.id || '').localeCompare(b.id || ''))

  const subClusters = orderedDetails.map((d) => {
    const filteredVcds = (d.sourceVcds || []).map(v => vcdDetailMap.get(v) || {
      vcd: v, vlabel: '', nciCode: '', specRefCount: 0, vlmRefCount: 0
    })
    const actualTerms = (Array.isArray(d.mergedTermDetails) && d.mergedTermDetails.length > 0)
      ? d.mergedTermDetails
      : (src.mergedTermDetails || [])
    const termCodes = actualTerms.map(t => (t.code || '').toString().trim()).filter(Boolean)
    const supersetGroup = {
      termList: termCodes.join(', '),
      termCount: termCodes.length,
      vcds: filteredVcds,
      vcdCount: filteredVcds.length
    }
    return {
      ...src,
      type: 'identity',
      already_merged: true,
      merged_target_vcd: d.id,
      merged_target_vlabel: d.vlabel,
      merged_target_nci_code: d.nciCode,
      merge_batch_id: batchId,
      allVcds: [...(d.sourceVcds || [])],
      totalVcdCount: (d.sourceVcds || []).length,
      supersetGroup,
      subsetGroups: [],
      mergedTermDetails: actualTerms
    }
  })

  clusters.value.splice(srcIdx, 1, ...subClusters)

  // All indices > srcIdx shift by (N - 1) because we removed 1 and added N.
  const shift = subClusters.length - 1
  const remap = (i) => (i < srcIdx ? i : (i === srcIdx ? -1 : i + shift))

  const newMergedSet = new Set()
  for (const i of mergedSet.value) {
    const ni = remap(i)
    if (ni >= 0) newMergedSet.add(ni)
  }
  const newMergedTargets = {}
  for (const [k, v] of Object.entries(mergedTargets.value)) {
    const ni = remap(parseInt(k, 10))
    if (ni >= 0) newMergedTargets[ni] = v
  }
  const newMergedDetail = {}
  for (const [k, v] of Object.entries(mergedTargetsDetail.value)) {
    const ni = remap(parseInt(k, 10))
    if (ni >= 0) newMergedDetail[ni] = v
  }
  const newSkipped = new Set()
  for (const i of skippedSet.value) {
    const ni = remap(i)
    if (ni >= 0) newSkipped.add(ni)
  }

  // Each new sub-cluster is "already merged" and has exactly one target detail.
  succeededDetails.forEach((d, i) => {
    const ni = srcIdx + i
    newMergedSet.add(ni)
    newMergedTargets[ni] = d.id
    newMergedDetail[ni] = [d]
  })

  mergedSet.value = newMergedSet
  mergedTargets.value = newMergedTargets
  mergedTargetsDetail.value = newMergedDetail
  skippedSet.value = newSkipped

  selectCluster(srcIdx)
  return true
}

function defaultIdForCluster(cluster, targetIdx = 0) {
  const base = nextComId()
  return targetIdx === 0 ? base : `${base}_${targetIdx + 1}`
}

// Fallback: pure auto-increment used only when no cluster context is available.
function nextComId() {
  const existing = new Set()
  clusters.value.forEach(c => {
    (c.allVcds || []).forEach(v => existing.add(v.toUpperCase()))
  })
  for (const [, target] of Object.entries(mergedTargets.value)) {
    if (!target) continue
    String(target).split(',').map(s => s.trim()).filter(Boolean).forEach(t => existing.add(t.toUpperCase()))
  }
  targets.value.forEach(t => { if (t.id) existing.add(t.id.toUpperCase()) })
  for (let i = 1; i <= 999; i++) {
    const id = 'COM' + String(i).padStart(2, '0')
    if (!existing.has(id)) return id
  }
  return 'COM999'
}

function skipCluster() {
  if (selectedIdx.value === null) return
  skippedSet.value = new Set([...skippedSet.value, selectedIdx.value])
  const nextIdx = clusters.value.findIndex((_, i) =>
    i > selectedIdx.value && !mergedSet.value.has(i) && !skippedSet.value.has(i))
  if (nextIdx >= 0) {
    selectCluster(nextIdx)
  }
}

function goBack() {
  router.push(`/project/${currentProjectId.value}/codelists`)
}

async function analyze() {
  analyzing.value = true
  analyzed.value = false
  clusters.value = []
  selectedIdx.value = null
  mergedSet.value = new Set()
  mergedTargets.value = {}
  mergedTargetsDetail.value = {}
  skippedSet.value = new Set()
  pendingRules.value = []
  try {
    const [unifiedRes, rulesRes] = await Promise.all([
      service.get(`${baseUrl}/api/codelist/merge/analyze-unified/${currentProjectId.value}`),
      service.get(`${baseUrl}/api/codelist/merge/pending-rules/${currentProjectId.value}`)
    ])
    if (unifiedRes.data?.success) {
      clusters.value = unifiedRes.data.data || []
      analyzed.value = true
      const newMerged = new Set()
      const newTargets = {}
      const newDetails = {}
      clusters.value.forEach((c, idx) => {
        if (c.already_merged) {
          newMerged.add(idx)
          newTargets[idx] = c.merged_target_vcd
          newDetails[idx] = [{
            id: c.merged_target_vcd || '',
            vlabel: c.merged_target_vlabel || '',
            nciCode: c.merged_target_nci_code || '',
            sourceVcds: c.allVcds || []
          }]
        }
      })
      mergedSet.value = newMerged
      mergedTargets.value = newTargets
      mergedTargetsDetail.value = newDetails
      if (clusters.value.length > 0) {
        selectCluster(0)
      }
    } else {
      ElMessage.error(unifiedRes.data?.message || '分析失败')
    }
    if (rulesRes.data?.success) {
      pendingRules.value = rulesRes.data.data || []
    }
  } catch (e) {
    ElMessage.error('分析请求失败: ' + (e.message || ''))
  } finally {
    analyzing.value = false
  }
}

function selectCluster(idx) {
  selectedIdx.value = idx
  ctSuggestion.value = null
  excludedVcds.value = []
  const cluster = clusters.value[idx]
  if (!cluster) return

  // Case A: cluster is merged (either backend `already_merged` or merged in this session)
  // — restore the right panel from the saved detail snapshot, NOT from defaults.
  if (mergedSet.value.has(idx) && mergedTargetsDetail.value[idx]?.length > 0) {
    const details = mergedTargetsDetail.value[idx]
    targets.value = details.map(d => ({ id: d.id, vlabel: d.vlabel, nciCode: d.nciCode }))
    const map = {}
    details.forEach(d => {
      (d.sourceVcds || []).forEach(vcd => { map[vcd] = d.id })
    })
    vcdTargetMap.value = map
    mergeForm.value.targetVcd = details[0].id
    mergeForm.value.targetVlabel = details[0].vlabel
    mergeForm.value.targetNciCode = details[0].nciCode
    return
  }

  // Case B: backend says already_merged but no details snapshot (legacy) — fall back to cluster fields
  if (cluster.already_merged) {
    mergeForm.value.targetVcd = cluster.merged_target_vcd || ''
    mergeForm.value.targetVlabel = cluster.merged_target_vlabel || ''
    mergeForm.value.targetNciCode = cluster.merged_target_nci_code || ''
    targets.value = [{
      id: cluster.merged_target_vcd || '',
      vlabel: cluster.merged_target_vlabel || '',
      nciCode: cluster.merged_target_nci_code || ''
    }]
    const map = {}
    const av = cluster.allVcds || []
    av.forEach(vcd => { map[vcd] = cluster.merged_target_vcd || '' })
    vcdTargetMap.value = map
    return
  }

  // Case C: not merged yet — generate sensible defaults.
  // CT match / pending rules only affect Name and NCI Code, never the ID.
  let defaultId, defaultVlabel, defaultNciCode
  defaultId = defaultIdForCluster(cluster, 0)
  const matchedRule = findMatchingRule(cluster)
  if (matchedRule) {
    defaultVlabel = matchedRule.mergedVlabel || ''
    defaultNciCode = matchedRule.mergedNciCode || ''
  } else if (cluster.ct_codelist_code) {
    defaultVlabel = cluster.ct_codelist_name || ''
    defaultNciCode = cluster.ct_codelist_code
  } else if (cluster.suggested_nci_code) {
    defaultVlabel = ''
    defaultNciCode = cluster.suggested_nci_code
  } else {
    const vcds = cluster.supersetGroup?.vcds || []
    defaultVlabel = vcds.length > 0 ? (vcds[0].vlabel || '') : ''
    defaultNciCode = ''
  }

  mergeForm.value.targetVcd = defaultId
  mergeForm.value.targetVlabel = defaultVlabel
  mergeForm.value.targetNciCode = defaultNciCode

  targets.value = [{ id: defaultId, vlabel: defaultVlabel, nciCode: defaultNciCode }]
  const map = {}
  const allVcds = cluster.allVcds || []
  allVcds.forEach(vcd => { map[vcd] = defaultId })
  vcdTargetMap.value = map
}

function findMatchingRule(cluster) {
  if (!cluster || pendingRules.value.length === 0) return null
  const clusterVcds = new Set(cluster.allVcds || [])
  for (const rule of pendingRules.value) {
    const ruleSourceVcds = rule.sourceVcds || []
    const overlap = ruleSourceVcds.filter(v => clusterVcds.has(v))
    if (overlap.length >= 2) return rule
  }
  return null
}

function hasPendingRule(cluster) {
  return findMatchingRule(cluster) !== null
}

async function suggestFromCt() {
  ctSuggesting.value = true
  ctSuggestion.value = null
  try {
    const terms = supersetTermsList.value
    const res = await service.post(`${baseUrl}/api/codelist/merge/ct-suggest`, {
      projectId: currentProjectId.value,
      terms
    })
    if (res.data?.success && res.data.data) {
      const data = res.data.data
      if (data.codelist_code) {
        ctSuggestion.value = data
        ElMessage.success('找到CT精确匹配')
      } else if (data.suggestions && data.suggestions.length > 0) {
        ctSuggestion.value = data
        ElMessage.warning(data.reason || '未找到精确匹配，已返回模糊建议')
      } else {
        ctSuggestion.value = data
        ElMessage.info(data.reason || '未找到匹配的CT Codelist')
      }
    } else {
      ElMessage.error(res.data?.message || 'CT查询失败')
    }
  } catch (e) {
    ElMessage.warning('CT查询失败')
  } finally {
    ctSuggesting.value = false
  }
}

function applySuggestion(suggestion, targetIdx) {
  const s = suggestion || ctSuggestion.value
  if (!s) return
  const code = s.codelist_code
  const name = s.codelist_name || ''
  if (!code) return

  // Per spec: Codelist ID is ALWAYS COMxx auto-generated. CT match only fills
  // Name and NCI Code — never overwrites the ID.
  const tIdx = targetIdx != null ? targetIdx : 0
  if (targets.value[tIdx]) {
    targets.value[tIdx].vlabel = name
    targets.value[tIdx].nciCode = code
  }
  mergeForm.value.targetVlabel = name
  mergeForm.value.targetNciCode = code
}

function excludeVcd(vcd) {
  if (!excludedVcds.value.includes(vcd)) {
    excludedVcds.value.push(vcd)
  }
  delete vcdTargetMap.value[vcd]
}

function restoreVcd(vcd) {
  excludedVcds.value = excludedVcds.value.filter(v => v !== vcd)
  if (!vcdTargetMap.value[vcd] && targets.value.length > 0) {
    vcdTargetMap.value[vcd] = targets.value[0].id
  }
}

function addTarget() {
  // Falls back to plain COMxx auto-increment if it would clash with an existing target.
  let id = defaultIdForCluster(selectedCluster.value, targets.value.length)
  const taken = new Set(targets.value.map(t => (t.id || '').toUpperCase()))
  if (taken.has(id.toUpperCase())) id = nextComId()
  targets.value.push({ id, vlabel: '', nciCode: '' })
}

function removeTarget(tIdx) {
  if (targets.value.length <= 1) return
  const removedId = targets.value[tIdx].id
  targets.value.splice(tIdx, 1)
  const fallbackId = targets.value[0].id
  for (const vcd of Object.keys(vcdTargetMap.value)) {
    if (vcdTargetMap.value[vcd] === removedId) {
      vcdTargetMap.value[vcd] = fallbackId
    }
  }
}

function updateTargetId(tIdx, newId) {
  const oldId = targets.value[tIdx].id
  targets.value[tIdx].id = newId
  for (const vcd of Object.keys(vcdTargetMap.value)) {
    if (vcdTargetMap.value[vcd] === oldId) {
      vcdTargetMap.value[vcd] = newId
    }
  }
}

function targetOptionLabels() {
  return targets.value.map(t => ({ value: t.id, label: t.id + (t.vlabel ? ` (${t.vlabel})` : '') }))
}

const targetGroups = computed(() => {
  const groups = {}
  targets.value.forEach(t => { groups[t.id] = [] })
  for (const [vcd, tid] of Object.entries(vcdTargetMap.value)) {
    if (excludedVcds.value.includes(vcd)) continue
    if (groups[tid]) groups[tid].push(vcd)
    else if (targets.value.length > 0) {
      const fallback = targets.value[0].id
      groups[fallback] = groups[fallback] || []
      groups[fallback].push(vcd)
    }
  }
  return groups
})

function targetVcdCount(targetId) {
  return (targetGroups.value[targetId] || []).length
}

async function confirmMerge() {
  return confirmBatchMerge()
}

async function confirmChainMerge() {
  return confirmBatchMerge()
}

async function confirmBatchMerge() {
  if (!selectedCluster.value) return

  // Duplicate Codelist ID guard: warn if user added two targets with the same ID.
  const trimmedIds = targets.value.map(t => (t.id || '').trim()).filter(Boolean)
  const seenIds = new Set()
  const dupIds = new Set()
  trimmedIds.forEach(id => {
    const key = id.toUpperCase()
    if (seenIds.has(key)) dupIds.add(id)
    else seenIds.add(key)
  })
  if (dupIds.size > 0) {
    ElMessage.warning(`目标 Codelist ID 重复: ${[...dupIds].join('、')}，请修改为不同的 ID 后再合并`)
    return
  }
  const emptyIdTargets = targets.value.filter(t => !(t.id || '').trim())
  if (emptyIdTargets.length > 0) {
    ElMessage.warning('存在未填写 Codelist ID 的目标，请补充后再合并')
    return
  }

  const validGroups = targets.value
    .map(t => ({ target: t, vcds: targetGroups.value[t.id] || [] }))
    .filter(g => g.vcds.length >= 2)

  if (validGroups.length === 0) {
    ElMessage.warning('每个目标至少需要分配 2 个Codelist才能合并')
    return
  }

  const singleVcdTargets = targets.value.filter(t => (targetGroups.value[t.id] || []).length === 1)
  const summaryLines = validGroups
    .map(g => `[${g.target.id}] ${g.target.vlabel || ''}: ${g.vcds.length} 个VCD`)
    .join('\n')
  const warnLine = singleVcdTargets.length > 0
    ? `\n\n注意: ${singleVcdTargets.map(t => t.id).join('、')} 只有1个VCD，将跳过。`
    : ''

  try {
    await ElMessageBox.confirm(
      `将执行 ${validGroups.length} 组合并：\n${summaryLines}${warnLine}\n\n此操作将更新 Variables 和 ValueLevel 中的引用。`,
      '确认合并',
      { type: 'warning', confirmButtonText: '执行合并', cancelButtonText: '取消' }
    )
  } catch { return }

  merging.value = true
  const allVcdDetails = getAllActiveVcds()
  const vcdTermCountMap = {}
  allVcdDetails.forEach(v => {
    vcdTermCountMap[v.vcd] = v.termCount || 0
  })

  // Generate one batch id; share across all target merges so the backend can group
  // them into the same original candidate group when re-analyzed (#N-1, #N-2 ...).
  const batchId = makeBatchId()
  // Source cluster_key — the backend will associate this batch_id to the same
  const sourceClusterKey = selectedCluster.value?.cluster_key || ''

  try {
    const succeededTargets = []
    const failedTargets = []
    // Map<targetId, mergedTermDetails[]> — captured from backend response so we can
    // show the TRUE post-merge terms (esp. when source was a subset_chain).
    const targetTermsMap = new Map()
    for (const g of validGroups) {
      const sorted = [...g.vcds].sort((a, b) => (vcdTermCountMap[b] || 0) - (vcdTermCountMap[a] || 0))
      const res = await service.post(`${baseUrl}/api/codelist/merge/execute/${currentProjectId.value}`, {
        targetVcd: g.target.id,
        targetVlabel: g.target.vlabel,
        targetNciCode: g.target.nciCode,
        sourceVcds: sorted,
        batchId,
        sourceClusterKey
      })
      if (res.data?.success) {
        succeededTargets.push(g.target.id)
        const data = res.data?.data
        if (data && Array.isArray(data.mergedTermDetails)) {
          targetTermsMap.set(g.target.id, data.mergedTermDetails)
        }
      } else {
        failedTargets.push(g.target.id)
        ElMessage.error(`合并 [${g.target.id}] 失败: ${res.data?.message || ''}`)
      }
    }
    if (succeededTargets.length > 0) {
      const summary = `合并完成: ${succeededTargets.length} 组` +
        (failedTargets.length > 0 ? `（失败 ${failedTargets.length} 组: ${failedTargets.join(', ')}）` : '')
      ElMessage.success(summary)

      const succeededDetails = validGroups
        .filter(g => succeededTargets.includes(g.target.id))
        .map(g => ({
          id: g.target.id,
          vlabel: g.target.vlabel || '',
          nciCode: g.target.nciCode || '',
          sourceVcds: [...g.vcds],
          mergedTermDetails: targetTermsMap.get(g.target.id) || []
        }))

      const srcIdx = selectedIdx.value

      if (succeededDetails.length > 1) {
        splitClusterIntoSubClusters(srcIdx, succeededDetails, batchId)
      } else {
        const c = clusters.value[srcIdx]
        const d0 = succeededDetails[0]
        if (c) {
          c.already_merged = true
          c.merged_target_vcd = d0.id
          c.merged_target_vlabel = d0.vlabel
          c.merged_target_nci_code = d0.nciCode
          c.merge_batch_id = batchId
          if (Array.isArray(d0.mergedTermDetails) && d0.mergedTermDetails.length > 0) {
            c.mergedTermDetails = d0.mergedTermDetails
            const codes = d0.mergedTermDetails.map(t => (t.code || '').toString().trim()).filter(Boolean)
            if (c.supersetGroup) {
              c.supersetGroup = {
                ...c.supersetGroup,
                termList: codes.join(', '),
                termCount: codes.length
              }
            }
          }
        }
        mergedSet.value = new Set([...mergedSet.value, srcIdx])
        mergedTargets.value[srcIdx] = d0.id
        mergedTargetsDetail.value = {
          ...mergedTargetsDetail.value,
          [srcIdx]: succeededDetails
        }
        targets.value = succeededDetails.map(d => ({ id: d.id, vlabel: d.vlabel, nciCode: d.nciCode }))
        const newMap = {}
        succeededDetails.forEach(d => {
          (d.sourceVcds || []).forEach(vcd => { newMap[vcd] = d.id })
        })
        vcdTargetMap.value = newMap
        mergeForm.value.targetVcd = d0.id
        mergeForm.value.targetVlabel = d0.vlabel
        mergeForm.value.targetNciCode = d0.nciCode
      }
    }
  } catch (e) {
    ElMessage.error('合并请求失败: ' + (e.message || ''))
  } finally {
    merging.value = false
  }
}

function getAllActiveVcds() {
  if (!selectedCluster.value) return []
  if (selectedCluster.value.type === 'identity') {
    return activeVcdDetails.value
  }
  return chainAllActiveVcds.value
}

async function confirmUndo() {
  const idx = selectedIdx.value
  const cluster = clusters.value[idx]
  const mergedVcdStr = mergedTargets.value[idx]
  if (!cluster || !mergedVcdStr) {
    ElMessage.warning('未找到该组的合并记录')
    return
  }

  const batchId = cluster.merge_batch_id || ''
  const allBatchTargets = batchId
    ? clusters.value.filter(c => c.merge_batch_id === batchId).map(c => c.merged_target_vcd).filter(Boolean)
    : [mergedVcdStr]
  const targetsLabel = allBatchTargets.join(', ')

  try {
    await ElMessageBox.confirm(
      allBatchTargets.length > 1
        ? `确认撤销整批合并 [${targetsLabel}]？将一起恢复 ${allBatchTargets.length} 个Codelist及其引用。`
        : `确认撤销 [${targetsLabel}] 的合并？将恢复原始Codelist及其引用。`,
      '确认撤销',
      { type: 'warning', confirmButtonText: '撤销合并', cancelButtonText: '取消' }
    )
  } catch { return }

  undoing.value = true
  try {
    let success = false
    if (batchId) {
      const res = await service.post(`${baseUrl}/api/codelist/merge/undo-batch/${currentProjectId.value}`, { batchId })
      if (res.data?.success) {
        success = true
      } else {
        ElMessage.error('撤销失败: ' + (res.data?.message || ''))
      }
    } else {
      const res = await service.post(`${baseUrl}/api/codelist/merge/undo/${currentProjectId.value}`, { mergedVcd: mergedVcdStr })
      if (res.data?.success) {
        success = true
      } else {
        ElMessage.error('撤销失败: ' + (res.data?.message || ''))
      }
    }
    if (success) {
      ElMessage.success('撤销成功')
      analyze()
    }
  } catch (e) {
    ElMessage.error('撤销请求失败: ' + (e.message || ''))
  } finally {
    undoing.value = false
  }
}

async function confirmReapplyAll() {
  if (pendingRules.value.length === 0) {
    ElMessage.info('没有可重新合并的历史规则')
    return
  }
  const ruleNames = pendingRules.value.map(r => `[${r.mergedVcd}] ${r.sourceCount}个VCD`).join('、')
  try {
    await ElMessageBox.confirm(
      `将根据历史合并规则重新合并 ${pendingRules.value.length} 组Codelist：\n${ruleNames}\n\n此操作将更新 Variables 和 ValueLevel 中的引用。`,
      '确认一键合并',
      { type: 'warning', confirmButtonText: '一键合并', cancelButtonText: '取消' }
    )
  } catch { return }

  reapplying.value = true
  try {
    const res = await service.post(`${baseUrl}/api/codelist/merge/reapply-all/${currentProjectId.value}`)
    if (res.data?.success) {
      ElMessage.success(res.data.data || '一键合并完成')
      analyze()
    } else {
      ElMessage.error(res.data?.message || '一键合并失败')
    }
  } catch (e) {
    ElMessage.error('一键合并请求失败: ' + (e.message || ''))
  } finally {
    reapplying.value = false
  }
}

async function showLogDialog() {
  logDialogVisible.value = true
  try {
    const res = await service.get(`${baseUrl}/api/codelist/merge/log/${currentProjectId.value}`)
    if (res.data?.success) {
      mergeLogs.value = res.data.data || []
    }
  } catch (e) {
    mergeLogs.value = []
  }
}

onMounted(() => {
  analyze()
})
</script>

<style scoped lang="less">
.codelist-merge-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--saas-bg-page);
}

.merge-content {
  flex: 1;
  overflow: hidden;
  padding: var(--saas-space-4);
}

.no-merge {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

.merge-layout {
  display: flex;
  gap: var(--saas-space-4);
  height: 100%;
}

.group-sidebar {
  width: 340px;
  flex-shrink: 0;
  background: var(--saas-bg-card);
  border-radius: var(--saas-radius-md);
  border: 1px solid var(--saas-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--saas-border-light);
  .sidebar-title { font-weight: 600; font-size: 14px; color: var(--saas-text-primary); }
}

.group-list {
  flex: 1;
  overflow-y: auto;
}

.group-merged-info {
  font-size: 11px;
  color: var(--saas-success);
  font-weight: 600;
  margin-top: 2px;
}
.group-subset-hint {
  font-size: 11px;
  color: var(--saas-danger);
  margin-top: 2px;
}
.group-ct-hint {
  font-size: 11px;
  color: var(--saas-warning);
  font-weight: 600;
  margin-top: 2px;
}
.group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.group-terms {
  font-size: 12px;
  color: var(--saas-text-tertiary);
  margin-bottom: 2px;
  line-height: 1.4;
  word-break: break-all;
}
.group-vcds {
  font-size: 12px;
  color: var(--saas-primary);
  word-break: break-all;
}

.detail-panel {
  flex: 1;
  background: var(--saas-bg-card);
  border-radius: var(--saas-radius-md);
  border: 1px solid var(--saas-border);
  padding: var(--saas-space-4);
  overflow-y: auto;
}

.detail-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--saas-bg-card);
  border-radius: var(--saas-radius-md);
  border: 1px solid var(--saas-border);
}

.compare-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.compare-block, .chain-block {
  border: 1px solid var(--saas-border);
  border-radius: var(--saas-radius-sm);
  padding: var(--saas-space-4);
}
.before-block { background: var(--saas-warning-bg); border-color: var(--saas-warning-light); }
.after-block { background: var(--saas-success-bg); border-color: var(--saas-success-light); }
.superset-block { background: var(--saas-danger-bg); border-color: var(--saas-danger-light); }
.superset-block.ct-aligned-block { background: var(--saas-warning-bg); border-color: var(--saas-warning); }
.subset-block { background: var(--saas-warning-bg); border-color: var(--saas-warning-light); }

.subset-chain-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--saas-text-secondary);
  flex-wrap: wrap;
}

.terms-preview {
  margin-top: 12px;
  .terms-label { font-size: 12px; color: var(--saas-text-tertiary); margin-bottom: 6px; display: block; }
  .terms-tags { display: flex; flex-wrap: wrap; gap: 4px; }
  .term-tag { font-size: 11px; }
}

.merge-form {
  margin-top: 4px;
}

.term-details {
  margin-top: 16px;
  border-top: 1px solid var(--saas-border-light);
  padding-top: 12px;
}
.term-details-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--saas-text-secondary);
  margin-bottom: 8px;
}

.ct-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--saas-text-tertiary);
}
.ct-fuzzy .ct-reason {
  color: var(--saas-warning);
  margin-bottom: 4px;
}
.ct-fuzzy .ct-fuzzy-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}
.ct-no-match {
  color: var(--saas-danger);
}

.impact-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.no-ref { color: var(--saas-text-tertiary); font-size: 12px; }

.excluded-hint {
  color: var(--saas-danger);
  font-size: 12px;
  margin-left: 4px;
}

.excluded-list {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  .excluded-label { font-size: 12px; color: var(--saas-text-tertiary); }
}

.target-add-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  .target-hint { font-size: 11px; color: var(--saas-text-tertiary); }
}

.ct-match-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.target-card {
  border: 1px solid var(--saas-border);
  border-radius: var(--saas-radius-sm);
  padding: 12px;
  margin-top: 10px;
  background: var(--saas-bg-input);
  &.single-vcd-warn { border-color: var(--saas-warning); background: var(--saas-warning-bg); }
}
.target-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.target-warn {
  font-size: 12px;
  color: var(--saas-warning);
  margin-top: 4px;
}
.target-vcds-preview {
  font-size: 12px;
  color: var(--saas-text-secondary);
  margin-top: 4px;
  word-break: break-all;
}

.merge-actions {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  .warn-hint { color: var(--saas-warning); font-size: 13px; }
}
</style>
