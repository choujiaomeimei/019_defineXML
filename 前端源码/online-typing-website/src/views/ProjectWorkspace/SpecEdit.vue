<template>
  <div class="spec-edit-page">
    <!-- 表格模式 -->
    <div v-if="editMode === 'table'" class="table-mode">
      <div class="table-layout ws-edit-grid">
        <div class="domain-sidebar">
          <template v-if="domainsLoading">
            <div class="sidebar-head-spacer" aria-hidden="true"></div>
            <div class="sidebar-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
          </template>
          <template v-else-if="domains.length === 0">
            <div class="sidebar-head-spacer" aria-hidden="true"></div>
            <div class="sidebar-empty">
            <span>暂无数据</span>
            <p class="sidebar-hint">请先上传并处理项目 Spec</p>
            </div>
          </template>
          <template v-else>
            <div class="sidebar-search">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索变量名或标签..."
                size="small"
                clearable
                prefix-icon="Search"
              />
            </div>
            <div class="sidebar-list">
              <div
                v-for="d in domains" :key="d"
                class="domain-item"
                :class="{ active: selectedDomain === d }"
                @click="selectDomain(d)"
              >
                <span class="domain-name">{{ d }}</span>
                <span class="domain-count">{{ domainStats[d] || 0 }}</span>
              </div>
            </div>
          </template>
        </div>

        <div class="table-content">
          <div class="table-actions ws-toolbar-zones">
            <div class="ws-toolbar-zone ws-toolbar-zone--start">
              <div class="ws-btn-group">
                <el-button type="primary" size="small" @click="showAddDialog" :disabled="!selectedDomain">
                  <el-icon><Plus /></el-icon> 新增
                </el-button>
                <el-button size="small" @click="refreshData">
                  <el-icon><Refresh /></el-icon> 刷新
                </el-button>
              </div>
            </div>
            <div class="ws-toolbar-zone ws-toolbar-zone--center">
              <div class="ws-btn-group ws-btn-group--extract">
                <el-button size="small" @click="generateSupp" :loading="suppGenerating">
                  <el-icon><SetUp /></el-icon> 生成SUPP
                </el-button>
                <el-button size="small" @click="compareP21" :loading="p21Comparing">
                  <el-icon><DataAnalysis /></el-icon> 对比P21
                </el-button>
                <el-button size="small" title="从XPT提取类型、长度、精度和格式" @click="extractXptMetadata" :loading="xptExtracting" :disabled="p21Extracting">
                  <el-icon><DataAnalysis /></el-icon> 提取XPT
                </el-button>
                <el-button size="small" title="从P21空Spec提取Mandatory、Role和Has No Data" @click="extractP21Fields" :loading="p21Extracting" :disabled="xptExtracting">
                  <el-icon><Download /></el-icon> 提取P21
                </el-button>
                <el-button size="small" @click="extractPages" :loading="pagesExtracting">
                  <el-icon><Document /></el-icon> 提取Pages
                </el-button>
                <el-button size="small" @click="extractMethodsComments" :loading="mcExtracting">
                  <el-icon><Connection /></el-icon> Method&Comments
                </el-button>
                <el-button size="small" @click="extractCodelist" :loading="codelistExtracting" :disabled="codelistExtracting">
                  <el-icon><Grid /></el-icon> 提取Codelist
                </el-button>
              </div>
            </div>
            <div class="ws-toolbar-zone ws-toolbar-zone--end">
              <el-radio-group v-model="editMode" size="small" class="ws-mode-switch">
                <el-radio-button value="table">表格模式</el-radio-button>
                <el-radio-button value="excel">Excel 模式</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div class="table-body">
          <el-table
            :data="filteredVariables"
            v-loading="tableLoading"
            border
            stripe
            size="small"
            max-height="calc(100vh - 260px)"
            @cell-dblclick="handleCellDblClick"
          >
            <el-table-column type="index" label="#" width="45" fixed />
            <el-table-column prop="sortOrder" label="Order" width="70" align="center">
              <template #default="{ row }"><el-input v-if="isEditing(row,'sortOrder')" v-model="editForm.sortOrder" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else @dblclick.stop="startEdit(row,'sortOrder')">{{row.sortOrder||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="domain" label="Dataset" width="90" fixed class-name="col-dataset">
              <template #default="{ row }"><span class="ds-tag">{{row.domain||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="variable" label="Variable" width="120" fixed>
              <template #default="{ row }"><el-input v-if="isEditing(row,'variable')" v-model="editForm.variable" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else class="var-name" @dblclick.stop="startEdit(row,'variable')">{{row.variable}}</span></template>
            </el-table-column>
            <el-table-column prop="label" label="Label" min-width="170">
              <template #default="{ row }"><el-input v-if="isEditing(row,'label')" v-model="editForm.label" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else @dblclick.stop="startEdit(row,'label')">{{row.label||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="type" label="Data Type" width="90" align="center">
              <template #default="{ row }">
                <el-select v-if="isEditing(row,'type')" v-model="editForm.type" size="small" style="width:100%">
                  <el-option label="text" value="text" /><el-option label="integer" value="integer" /><el-option label="float" value="float" />
                  <el-option label="date" value="date" /><el-option label="datetime" value="datetime" /><el-option label="time" value="time" />
                </el-select>
                <el-tag v-else-if="row.type" size="small" round @dblclick.stop="startEdit(row,'type')">{{row.type}}</el-tag>
                <span v-else @dblclick.stop="startEdit(row,'type')">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="length" label="Length" width="75" align="center">
              <template #default="{ row }"><el-input v-if="isEditing(row,'length')" v-model="editForm.length" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else @dblclick.stop="startEdit(row,'length')">{{row.length||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="significantDigits" label="Sig. Digits" width="90" align="center">
              <template #default="{ row }"><el-input v-if="isEditing(row,'significantDigits')" v-model="editForm.significantDigits" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else @dblclick.stop="startEdit(row,'significantDigits')">{{row.significantDigits||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="format" label="Format" width="100">
              <template #default="{ row }"><el-input v-if="isEditing(row,'format')" v-model="editForm.format" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.format" @dblclick.stop="startEdit(row,'format')">{{row.format||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="mandatory" label="Mandatory" width="95" align="center">
              <template #default="{ row }">
                <el-select v-if="isEditing(row,'mandatory')" v-model="editForm.mandatory" size="small" style="width:100%"><el-option label="Yes" value="Yes" /><el-option label="No" value="No" /></el-select>
                <el-tag v-else-if="row.mandatory" :type="row.mandatory==='Yes'?'danger':'info'" size="small" effect="plain" round @dblclick.stop="startEdit(row,'mandatory')">{{row.mandatory}}</el-tag>
                <span v-else @dblclick.stop="startEdit(row,'mandatory')">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="assignedValue" label="Assigned Value" width="120">
              <template #default="{ row }"><el-input v-if="isEditing(row,'assignedValue')" v-model="editForm.assignedValue" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.assignedValue" @dblclick.stop="startEdit(row,'assignedValue')">{{row.assignedValue||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="codelist" label="Codelist" width="130">
              <template #default="{ row }"><el-input v-if="isEditing(row,'codelist')" v-model="editForm.codelist" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.codelist" @dblclick.stop="startEdit(row,'codelist')">{{row.codelist||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="cdiscSubmissionValue" label="Submission Value" min-width="130">
              <template #default="{ row }"><el-input v-if="isEditing(row,'cdiscSubmissionValue')" v-model="editForm.cdiscSubmissionValue" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.cdiscSubmissionValue" @dblclick.stop="startEdit(row,'cdiscSubmissionValue')">{{row.cdiscSubmissionValue||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="common" label="Common" width="85" align="center">
              <template #default="{ row }">
                <el-select v-if="isEditing(row,'common')" v-model="editForm.common" size="small" clearable style="width:100%"><el-option label="Yes" value="Yes" /><el-option label="No" value="No" /></el-select>
                <span v-else @dblclick.stop="startEdit(row,'common')">{{row.common||'-'}}</span>
              </template>
            </el-table-column>
            <el-table-column prop="origin" label="Origin" width="100">
              <template #default="{ row }">
                <el-select v-if="isEditing(row,'origin')" v-model="editForm.origin" size="small" allow-create filterable clearable style="width:100%"><el-option v-for="o in originOptions" :key="o" :label="o" :value="o" /></el-select>
                <span v-else @dblclick.stop="startEdit(row,'origin')">{{row.origin||'-'}}</span>
              </template>
            </el-table-column>
            <el-table-column prop="source" label="Source" width="100">
              <template #default="{ row }"><el-input v-if="isEditing(row,'source')" v-model="editForm.source" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.source" @dblclick.stop="startEdit(row,'source')">{{row.source||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="pages" label="Pages" width="90">
              <template #default="{ row }"><el-input v-if="isEditing(row,'pages')" v-model="editForm.pages" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.pages" @dblclick.stop="startEdit(row,'pages')">{{row.pages||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="textContent" label="Text" width="120">
              <template #default="{ row }"><el-input v-if="isEditing(row,'textContent')" v-model="editForm.textContent" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.textContent" @dblclick.stop="startEdit(row,'textContent')">{{row.textContent||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="derivation" label="Predecessor" min-width="120">
              <template #default="{ row }"><el-input v-if="isEditing(row,'derivation')" v-model="editForm.derivation" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.derivation" @dblclick.stop="startEdit(row,'derivation')">{{row.derivation||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="role" label="Role" width="110">
              <template #default="{ row }">
                <el-select v-if="isEditing(row,'role')" v-model="editForm.role" size="small" allow-create filterable clearable style="width:100%"><el-option v-for="r in roleOptions" :key="r" :label="r" :value="r" /></el-select>
                <span v-else @dblclick.stop="startEdit(row,'role')">{{row.role||'-'}}</span>
              </template>
            </el-table-column>
            <el-table-column prop="hasNoData" label="Has No Data" width="100" align="center">
              <template #default="{ row }">
                <el-select v-if="isEditing(row,'hasNoData')" v-model="editForm.hasNoData" size="small" clearable style="width:100%"><el-option label="Yes" value="Yes" /><el-option label="No" value="No" /></el-select>
                <span v-else @dblclick.stop="startEdit(row,'hasNoData')">{{row.hasNoData||'-'}}</span>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="Comment" min-width="140">
              <template #default="{ row }"><el-input v-if="isEditing(row,'comment')" v-model="editForm.comment" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.comment" @dblclick.stop="startEdit(row,'comment')">{{row.comment||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="developerNotes" label="Developer Notes" min-width="130">
              <template #default="{ row }"><el-input v-if="isEditing(row,'developerNotes')" v-model="editForm.developerNotes" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else :title="row.developerNotes" @dblclick.stop="startEdit(row,'developerNotes')">{{row.developerNotes||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="supp" label="SUPP" width="75" align="center">
              <template #default="{ row }"><el-input v-if="isEditing(row,'supp')" v-model="editForm.supp" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else @dblclick.stop="startEdit(row,'supp')">{{row.supp||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="qeval" label="QEVAL" width="80" align="center">
              <template #default="{ row }"><el-input v-if="isEditing(row,'qeval')" v-model="editForm.qeval" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else @dblclick.stop="startEdit(row,'qeval')">{{row.qeval||'-'}}</span></template>
            </el-table-column>
            <el-table-column prop="method" label="Method" width="100" align="center">
              <template #default="{ row }"><el-input v-if="isEditing(row,'method')" v-model="editForm.method" size="small" @keyup.enter="saveRow(row)" @keyup.escape="cancelEdit" /><span v-else @dblclick.stop="startEdit(row,'method')">{{row.method||'-'}}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="{ row }">
                <div class="ops-nowrap">
                  <template v-if="editingRowId === row.id">
                    <el-button size="small" type="primary" text @click="saveRow(row)">保存</el-button>
                    <el-button size="small" text @click="cancelEdit">取消</el-button>
                  </template>
                  <template v-else>
                    <el-button size="small" type="primary" text @click="startEditRow(row)">编辑</el-button>
                    <el-button size="small" type="danger" text @click="deleteRow(row)">删除</el-button>
                  </template>
                </div>
              </template>
            </el-table-column>
          </el-table>
          </div>
        </div>
      </div>
    </div>

    <!-- Excel 模式 -->
    <div v-if="editMode === 'excel'" class="excel-mode">
      <div class="excel-toolbar ws-toolbar-zones">
        <div class="ws-toolbar-zone ws-toolbar-zone--start">
          <div class="ws-btn-group">
            <el-button type="primary" size="small" @click="saveExcel" :loading="excelSaving" :disabled="!excelLoaded">
              <el-icon><DocumentChecked /></el-icon> 保存到数据库
            </el-button>
            <el-button size="small" @click="exportExcel" :disabled="!excelLoaded">
              <el-icon><Download /></el-icon> 导出 xlsx
            </el-button>
          </div>
          <el-tag v-if="excelDirty" type="warning" size="small" effect="plain">未保存</el-tag>
        </div>
        <div class="ws-toolbar-zone ws-toolbar-zone--center">
          <div class="ws-btn-group ws-btn-group--extract">
            <el-button size="small" @click="generateSupp" :loading="suppGenerating">
              <el-icon><SetUp /></el-icon> 生成SUPP
            </el-button>
            <el-button size="small" @click="compareP21" :loading="p21Comparing">
              <el-icon><DataAnalysis /></el-icon> 对比P21
            </el-button>
            <el-button size="small" title="从XPT提取类型、长度、精度和格式" @click="extractXptMetadata" :loading="xptExtracting" :disabled="p21Extracting">
              <el-icon><DataAnalysis /></el-icon> 提取XPT
            </el-button>
            <el-button size="small" title="从P21空Spec提取Mandatory、Role和Has No Data" @click="extractP21Fields" :loading="p21Extracting" :disabled="xptExtracting">
              <el-icon><Download /></el-icon> 提取P21
            </el-button>
            <el-button size="small" @click="extractPages" :loading="pagesExtracting">
              <el-icon><Document /></el-icon> 提取Pages
            </el-button>
            <el-button size="small" @click="extractMethodsComments" :loading="mcExtracting">
              <el-icon><Connection /></el-icon> Method&Comments
            </el-button>
            <el-button size="small" @click="extractCodelist" :loading="codelistExtracting" :disabled="codelistExtracting">
              <el-icon><Grid /></el-icon> 提取Codelist
            </el-button>
          </div>
        </div>
        <div class="ws-toolbar-zone ws-toolbar-zone--end">
          <el-radio-group v-model="editMode" size="small" class="ws-mode-switch">
            <el-radio-button value="table">表格模式</el-radio-button>
            <el-radio-button value="excel">Excel 模式</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div class="excel-container">
        <div v-if="excelLoading" class="excel-loading">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div id="spec-luckysheet" class="luckysheet-host"></div>
      </div>
    </div>

    <!-- 新增变量对话框 -->
    <el-dialog v-model="addDialogVisible" title="新增变量" width="760px" destroy-on-close>
      <el-form :model="addForm" label-width="130px" size="default">
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="Variable" required><el-input v-model="addForm.variable" placeholder="如 STUDYID" /></el-form-item></el-col>
          <el-col :span="16"><el-form-item label="Label" required><el-input v-model="addForm.label" placeholder="变量描述" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="Data Type">
              <el-select v-model="addForm.type" style="width:100%">
                <el-option label="text" value="text" /><el-option label="integer" value="integer" /><el-option label="float" value="float" />
                <el-option label="date" value="date" /><el-option label="datetime" value="datetime" /><el-option label="time" value="time" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="Length"><el-input v-model="addForm.length" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Sig. Digits"><el-input v-model="addForm.significantDigits" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="Format"><el-input v-model="addForm.format" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="Mandatory">
              <el-select v-model="addForm.mandatory" style="width:100%">
                <el-option label="Yes" value="Yes" /><el-option label="No" value="No" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="Assigned Value"><el-input v-model="addForm.assignedValue" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="Codelist"><el-input v-model="addForm.codelist" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="Common">
              <el-select v-model="addForm.common" clearable style="width:100%">
                <el-option label="Yes" value="Yes" /><el-option label="No" value="No" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Origin">
              <el-select v-model="addForm.origin" allow-create filterable clearable style="width:100%">
                <el-option v-for="o in originOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="Source"><el-input v-model="addForm.source" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Pages"><el-input v-model="addForm.pages" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="Text"><el-input v-model="addForm.textContent" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="Predecessor"><el-input v-model="addForm.derivation" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="Role">
              <el-select v-model="addForm.role" allow-create filterable clearable style="width:100%">
                <el-option v-for="r in roleOptions" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Has No Data">
              <el-select v-model="addForm.hasNoData" clearable style="width:100%">
                <el-option label="Yes" value="Yes" /><el-option label="No" value="No" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="Core">
              <el-select v-model="addForm.core" style="width:100%" @change="onAddCoreChange">
                <el-option label="Req" value="Req" /><el-option label="Exp" value="Exp" /><el-option label="Perm" value="Perm" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="SUPP"><el-input v-model="addForm.supp" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="QEVAL"><el-input v-model="addForm.qeval" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="Comment"><el-input v-model="addForm.comment" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="Developer Notes"><el-input v-model="addForm.developerNotes" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :disabled="!addForm.variable || !addForm.label">确定新增</el-button>
      </template>
    </el-dialog>

    <!-- P21 变量对比对话框 -->
    <el-dialog v-model="p21DiffVisible" title="Variables 与 P21 变量对比" width="720px" destroy-on-close>
      <div class="p21-diff-result">
        <div class="diff-summary">
          <div class="diff-stat matched">
            <span class="stat-num">{{ p21DiffData.matched || 0 }}</span>
            <span class="stat-label">匹配成功</span>
          </div>
          <div class="diff-stat spec-only">
            <span class="stat-num">{{ (p21DiffData.specOnly || []).length }}</span>
            <span class="stat-label">Spec 独有</span>
          </div>
          <div class="diff-stat p21-only">
            <span class="stat-num">{{ (p21DiffData.p21Only || []).length }}</span>
            <span class="stat-label">P21 独有</span>
          </div>
        </div>

        <div v-if="(p21DiffData.specOnly || []).length > 0" class="diff-section">
          <div class="diff-section-header warning">
            <el-icon><WarningFilled /></el-icon>
            <span>Spec 有 / P21 无（{{ (p21DiffData.specOnly || []).length }} 个变量不会出现在最终 Define 中）</span>
          </div>
          <el-table :data="p21DiffData.specOnly" border size="small" max-height="200" class="diff-table">
            <el-table-column prop="dataset" label="Dataset" width="120" />
            <el-table-column prop="variable" label="Variable" />
          </el-table>
        </div>

        <div v-if="(p21DiffData.p21Only || []).length > 0" class="diff-section">
          <div class="diff-section-header info">
            <el-icon><InfoFilled /></el-icon>
            <span>P21 有 / Spec 无（{{ (p21DiffData.p21Only || []).length }} 个变量在 Define 中 Spec 字段将为空）</span>
          </div>
          <el-table :data="p21DiffData.p21Only" border size="small" max-height="200" class="diff-table">
            <el-table-column prop="dataset" label="Dataset" width="120" />
            <el-table-column prop="variable" label="Variable" />
          </el-table>
        </div>

        <div v-if="(p21DiffData.specOnly || []).length === 0 && (p21DiffData.p21Only || []).length === 0" class="diff-perfect">
          Spec 与 P21 变量完全匹配
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="p21DiffVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Plus, Refresh, Search, Download, DocumentChecked, WarningFilled, InfoFilled, SetUp, DataAnalysis, Document, Connection, Grid } from '@element-plus/icons-vue'
import LuckyExcel from 'luckyexcel'
import service from '@/axios'
import {
  CODELIST_SCOPES,
  extractCodelists,
  getCodelistExtractionError,
  showCodelistExtractionResult,
} from '@/utils/codelistExtraction'

const props = defineProps({ projectId: String })
const route = useRoute()
const baseUrl = import.meta.env.VITE_APP_BASE_URL || ''
const currentProjectId = computed(() => props.projectId || route.params.projectId)

const editMode = ref('table')
const originOptions = ['CRF', 'Derived', 'Assigned', 'Protocol', 'eDT']
const roleOptions = ['Identifier', 'Topic', 'Timing', 'Qualifier', 'Record Qualifier', 'Variable Qualifier', 'Grouping Qualifier', 'Rule']

// --- Table mode state ---
const domainsLoading = ref(false)
const tableLoading = ref(false)
const domains = ref([])
const domainStats = ref({})
const selectedDomain = ref('')
const domainVariables = ref([])
const searchKeyword = ref('')
const editingRowId = ref(null)
const editingField = ref(null)
const editForm = ref({})
const addDialogVisible = ref(false)
const addForm = ref({})

// --- P21 extract / compare / SUPP / Pages state ---
const xptExtracting = ref(false)
const p21Extracting = ref(false)
const p21Comparing = ref(false)
const p21DiffVisible = ref(false)
const p21DiffData = ref({})
const suppGenerating = ref(false)
const pagesExtracting = ref(false)
const mcExtracting = ref(false)
const codelistExtracting = ref(false)

// --- Excel mode state ---
const excelLoading = ref(false)
const excelLoaded = ref(false)
const excelSaving = ref(false)
const excelDirty = ref(false)

const filteredVariables = computed(() => {
  if (!searchKeyword.value) return domainVariables.value
  const kw = searchKeyword.value.toLowerCase()
  return domainVariables.value.filter(v =>
    (v.variable && v.variable.toLowerCase().includes(kw)) ||
    (v.label && v.label.toLowerCase().includes(kw))
  )
})

// --- Table mode methods ---
const loadDomains = async () => {
  const pid = currentProjectId.value
  if (!pid) return
  domainsLoading.value = true
  try {
    const [domainsRes, statsRes] = await Promise.all([
      service.get(`${baseUrl}/project-spec-data/domains`, { params: { projectId: pid } }),
      service.get(`${baseUrl}/project-spec-data/domain-stats`, { params: { projectId: pid } })
    ])
    if (domainsRes.data.success && Array.isArray(domainsRes.data.data)) {
      domains.value = domainsRes.data.data
    }
    if (statsRes.data.success && Array.isArray(statsRes.data.data)) {
      const map = {}
      statsRes.data.data.forEach(s => { map[s.domain] = s.variable_count || s.variableCount || s.count || 0 })
      domainStats.value = map
    }
    if (domains.value.length > 0 && !selectedDomain.value) {
      selectDomain(domains.value[0])
    }
  } catch (e) {
    console.error('加载域列表失败:', e)
    ElMessage.error('加载 Spec 数据失败')
  } finally {
    domainsLoading.value = false
  }
}

const selectDomain = async (domain) => {
  cancelEdit()
  selectedDomain.value = domain
  searchKeyword.value = ''
  const pid = currentProjectId.value
  if (!pid) return
  tableLoading.value = true
  try {
    const res = await service.get(`${baseUrl}/project-spec-data/list-by-domain`, {
      params: { projectId: pid, domain }
    })
    if (res.data.success && Array.isArray(res.data.data)) {
      domainVariables.value = res.data.data
    } else {
      domainVariables.value = []
    }
  } catch (e) {
    console.error('加载域变量失败:', e)
    domainVariables.value = []
  } finally {
    tableLoading.value = false
  }
}

const refreshData = () => {
  cancelEdit()
  loadDomains()
}

const confirmRebuild = async (message) => {
  try {
    await ElMessageBox.confirm(message, '确认重建数据', {
      confirmButtonText: '继续执行',
      cancelButtonText: '取消',
      type: 'warning',
    })
    return true
  } catch {
    return false
  }
}

const extractXptMetadata = async () => {
  xptExtracting.value = true
  try {
    const res = await service.post(`${baseUrl}/project-spec-data/extract-xpt-metadata/${currentProjectId.value}`)
    if (res.data?.success) {
      const detail = res.data.data || {}
      const message = detail.message || 'XPT元数据提取完成'
      if (detail.partial) {
        ElMessage.warning(message)
        const failedDatasets = (detail.datasets || [])
          .filter(item => item.status !== 'ok')
          .map(item => `${item.dataset}: ${item.error || (item.status === 'missing' ? '未找到XPT文件' : item.status)}`)
        if (failedDatasets.length > 0) {
          ElMessageBox.alert(failedDatasets.join('\n'), 'XPT部分提取详情', {
            confirmButtonText: '确定',
            type: 'warning'
          })
        }
      } else {
        ElMessage.success(message)
      }
      refreshData()
    } else {
      ElMessage.warning(res.data?.message || 'XPT元数据提取失败')
    }
  } catch (e) {
    ElMessage.error('提取XPT元数据失败: ' + (e.response?.data?.message || e.message))
  } finally {
    xptExtracting.value = false
  }
}

const extractP21Fields = async () => {
  p21Extracting.value = true
  try {
    const res = await service.post(`${baseUrl}/project-spec-data/extract-p21-fields/${currentProjectId.value}`)
    if (res.data?.success) {
      const detail = res.data.data || {}
      ElMessage.success(detail.message || 'P21字段提取完成')
      if ((detail.specOnlyCount || 0) > 0 || (detail.p21OnlyCount || 0) > 0) {
        const toDiffRow = key => {
          const [dataset, variable = ''] = String(key).split('|', 2)
          return { dataset, variable }
        }
        p21DiffData.value = {
          matched: detail.matched || 0,
          specOnly: (detail.specOnly || []).map(toDiffRow),
          p21Only: (detail.p21Only || []).map(toDiffRow)
        }
        p21DiffVisible.value = true
      }
      refreshData()
    } else {
      ElMessage.warning(res.data?.message || '提取失败')
    }
  } catch (e) {
    ElMessage.error('提取P21字段失败: ' + (e.response?.data?.message || e.message))
  } finally {
    p21Extracting.value = false
  }
}

const compareP21 = async () => {
  p21Comparing.value = true
  try {
    const res = await service.get(`${baseUrl}/project-spec-data/compare-p21/${currentProjectId.value}`)
    if (res.data?.success) {
      p21DiffData.value = res.data.data
      p21DiffVisible.value = true
    } else {
      ElMessage.warning(res.data?.message || '对比失败')
    }
  } catch (e) {
    ElMessage.error('对比P21失败: ' + (e.response?.data?.message || e.message))
  } finally {
    p21Comparing.value = false
  }
}

const generateSupp = async () => {
  const confirmed = await confirmRebuild(
    '该操作会删除 SUPP=Y 的原变量，并重建对应 SUPPxx 数据集。请确认 SUPP 标记准确且当前编辑已保存。',
  )
  if (!confirmed) return
  suppGenerating.value = true
  try {
    const res = await service.post(`${baseUrl}/project-spec-data/generate-supp/${currentProjectId.value}`)
    if (res.data?.success) {
      const data = res.data.data
      ElMessage.success(data?.message || '生成完成')
      refreshData()
    } else {
      ElMessage.warning(res.data?.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error('生成SUPP失败: ' + (e.response?.data?.message || e.message))
  } finally {
    suppGenerating.value = false
  }
}

const extractPages = async () => {
  pagesExtracting.value = true
  const loadingMsg = ElMessage({ type: 'info', message: '正在提取 Pages（如首次运行可能需要较长时间）...', duration: 0, showClose: true })
  try {
    const res = await service.post(`${baseUrl}/project-spec-data/extract-pages/${currentProjectId.value}`, null, { timeout: 360000 })
    loadingMsg.close()
    if (res.data?.success) {
      ElMessage.success(res.data.data || '提取完成')
      refreshData()
    } else {
      ElMessage.warning(res.data?.message || '提取失败')
    }
  } catch (e) {
    loadingMsg.close()
    ElMessage.error('提取Pages失败: ' + (e.response?.data?.message || e.message))
  } finally {
    pagesExtracting.value = false
  }
}

const extractMethodsComments = async () => {
  const confirmed = await confirmRebuild(
    '该操作会清空并重建当前项目的 Methods 与 Comments，人工编辑内容可能被覆盖。是否继续？',
  )
  if (!confirmed) return
  mcExtracting.value = true
  try {
    const res = await service.post(`${baseUrl}/project-spec-data/extract-methods-comments/${currentProjectId.value}`)
    if (res.data?.success) {
      const d = res.data.data
      ElMessage.success(d?.message || '提取完成')
      refreshData()
    } else {
      ElMessage.warning(res.data?.message || '提取失败')
    }
  } catch (e) {
    ElMessage.error('提取Method&Comments失败: ' + (e.response?.data?.message || e.message))
  } finally {
    mcExtracting.value = false
  }
}

const extractCodelist = async () => {
  if (codelistExtracting.value) return
  const confirmed = await confirmRebuild(
    '该操作会重建 Variables 级 Codelist，并重建 Dictionaries 数据。人工修改内容可能被覆盖。是否继续？',
  )
  if (!confirmed) return
  codelistExtracting.value = true
  try {
    ElMessage.info('正在提取 Variables 级 Codelist，请稍候...')
    const result = await extractCodelists({
      baseUrl,
      projectId: currentProjectId.value,
      scope: CODELIST_SCOPES.VARIABLES,
    })
    await refreshData()
    await showCodelistExtractionResult(CODELIST_SCOPES.VARIABLES, result)
  } catch (e) {
    ElMessage.error(getCodelistExtractionError(e, CODELIST_SCOPES.VARIABLES))
  } finally {
    codelistExtracting.value = false
  }
}

const isEditing = (row, field) => editingRowId.value === row.id && (editingField.value === field || editingField.value === '__row__')

const startEdit = (row, field) => {
  editingRowId.value = row.id
  editingField.value = field
  editForm.value = { ...row }
}

const startEditRow = (row) => {
  editingRowId.value = row.id
  editingField.value = '__row__'
  editForm.value = { ...row }
}

const handleCellDblClick = (row, column) => {
  const field = column.property
  if (field && field !== 'index') startEdit(row, field)
}

const cancelEdit = () => {
  editingRowId.value = null
  editingField.value = null
  editForm.value = {}
}

const saveRow = async (row) => {
  try {
    const payload = { ...editForm.value, id: row.id }
    const res = await service.put(`${baseUrl}/project-spec-data/update`, payload)
    if (res.data.success) {
      Object.assign(row, res.data.data)
      ElMessage.success('保存成功')
      cancelEdit()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

const deleteRow = async (row) => {
  try {
    const res = await service.delete(`${baseUrl}/project-spec-data/delete`, { params: { id: row.id } })
    if (res.data.success) {
      domainVariables.value = domainVariables.value.filter(v => v.id !== row.id)
      if (domainStats.value[selectedDomain.value]) domainStats.value[selectedDomain.value]--
      ElMessage.success('删除成功')
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const showAddDialog = () => {
  addForm.value = {
    projectId: currentProjectId.value,
    domain: selectedDomain.value,
    variable: '', label: '', type: 'text', length: '',
    controlledTermsOrFormat: '', cdiscSubmissionValue: '',
    origin: '', role: '', cdiscNotes: '', core: 'Exp', mandatory: 'No',
    codelist: '', format: '', comment: '', pages: '', method: '', derivation: '',
    significantDigits: '', assignedValue: '', common: '', hasNoData: '',
    developerNotes: '', textContent: '', source: '', supp: '', qeval: ''
  }
  addDialogVisible.value = true
}

const onAddCoreChange = (val) => {
  if (val === 'Req') {
    addForm.value.mandatory = 'Yes'
  } else {
    addForm.value.mandatory = 'No'
  }
}

const submitAdd = async () => {
  if (!addForm.value.variable || !addForm.value.label) {
    ElMessage.warning('变量名和标签为必填项')
    return
  }
  try {
    const res = await service.post(`${baseUrl}/project-spec-data/add`, addForm.value)
    if (res.data.success) {
      domainVariables.value.push(res.data.data)
      if (domainStats.value[selectedDomain.value] !== undefined) domainStats.value[selectedDomain.value]++
      addDialogVisible.value = false
      ElMessage.success('新增成功')
    } else {
      ElMessage.error(res.data.message || '新增失败')
    }
  } catch (e) {
    ElMessage.error('新增失败: ' + (e.response?.data?.message || e.message))
  }
}

// --- Excel mode methods ---
const initExcelMode = async () => {
  excelLoading.value = true
  excelLoaded.value = false
  excelDirty.value = false
  try {
    const res = await service.get(`${baseUrl}/project-spec-data/export-xlsx/${currentProjectId.value}`, {
      responseType: 'blob'
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const file = new File([blob], 'Spec.xlsx', { type: blob.type })

    LuckyExcel.transformExcelToLucky(file, (exportJson) => {
      if (!exportJson || !exportJson.sheets || exportJson.sheets.length === 0) {
        ElMessage.warning('无Spec数据可编辑，请先上传Spec文件')
        excelLoading.value = false
        return
      }
      nextTick(() => {
        try {
          window.luckysheet.create({
            container: 'spec-luckysheet',
            data: exportJson.sheets,
            title: 'Spec',
            showinfobar: false,
            showsheetbar: true,
            showstatisticBar: true,
            sheetFormulaBar: true,
            allowEdit: true,
            enableAddRow: true,
            enableAddBackTop: false,
            lang: 'zh',
            hook: { cellUpdated: () => { excelDirty.value = true } }
          })
          excelLoaded.value = true
          excelLoading.value = false
        } catch (e) {
          console.error('Luckysheet初始化失败:', e)
          ElMessage.error('编辑器初始化失败')
          excelLoading.value = false
        }
      })
    })
  } catch (e) {
    console.error('加载Excel失败:', e)
    ElMessage.error('加载数据失败')
    excelLoading.value = false
  }
}

const destroyLuckysheet = () => {
  if (window.luckysheet) {
    try { window.luckysheet.destroy() } catch (e) { /* ignore */ }
  }
}

const saveExcel = async () => {
  if (!window.luckysheet) return
  excelSaving.value = true
  try {
    const allSheets = window.luckysheet.getAllSheets()
    const res = await service.post(`${baseUrl}/project-spec-data/import-xlsx/${currentProjectId.value}`, { sheets: allSheets })
    if (res.data?.success) {
      ElMessage.success(res.data.data || '保存成功')
      excelDirty.value = false
    } else {
      ElMessage.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.message || e.message))
  } finally {
    excelSaving.value = false
  }
}

const exportExcel = async () => {
  try {
    const res = await service.get(`${baseUrl}/project-spec-data/export-xlsx/${currentProjectId.value}`, { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `Spec_${currentProjectId.value}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// --- Lifecycle ---
watch(editMode, (newMode, oldMode) => {
  if (oldMode === 'excel') destroyLuckysheet()
  if (newMode === 'excel') nextTick(() => initExcelMode())
  if (newMode === 'table') loadDomains()
})

onMounted(() => {
  if (currentProjectId.value) loadDomains()
})

onBeforeUnmount(() => {
  destroyLuckysheet()
})
</script>

<style scoped lang="less">
.spec-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--saas-topbar-height, 56px));
  background: var(--saas-bg-page, #f5f6fa);
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: var(--saas-bg-card, #fff);
  border-bottom: 1px solid var(--saas-border-light, #e5e7eb);

  .toolbar-left {
    display: flex;
    align-items: baseline;
    gap: 12px;
  }
  .page-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
    color: var(--saas-text-primary, #1f2937);
  }
  .page-desc {
    font-size: 13px;
    color: var(--saas-text-tertiary, #9ca3af);
  }
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

/* ---- table mode ---- */
.domain-sidebar {
  .sidebar-loading, .sidebar-empty {
    padding: 24px 16px;
    text-align: center;
    color: var(--saas-text-tertiary, #9ca3af);
    font-size: 13px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: center;
  }
  .sidebar-hint {
    font-size: 12px;
    margin-top: 6px;
    color: var(--saas-text-quaternary, #bbb);
  }
}

.var-name {
  font-family: inherit;
  font-weight: 600;
  color: var(--saas-primary-dark, #2563eb);
  font-size: 12px;
}
.ds-tag {
  font-family: inherit;
  font-weight: 500;
  font-size: 11px;
  color: var(--saas-text-secondary, #6b7280);
}

.ops-nowrap {
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  gap: 0;
}

/* ---- excel mode ---- */
.excel-mode {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.excel-container {
  flex: 1;
  position: relative;
}
.excel-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: rgba(255,255,255,0.85);
  z-index: 10;
  color: var(--saas-text-tertiary, #9ca3af);
}
.luckysheet-host {
  width: 100%;
  height: 100%;
}

/* ---- P21 diff dialog ---- */
.p21-diff-result {
  .diff-summary {
    display: flex;
    gap: 16px;
    margin-bottom: 20px;
  }
  .diff-stat {
    flex: 1;
    text-align: center;
    padding: 14px 8px;
    border-radius: 8px;
    .stat-num {
      display: block;
      font-size: 28px;
      font-weight: 700;
      line-height: 1.2;
    }
    .stat-label {
      font-size: 12px;
      margin-top: 4px;
      display: block;
    }
    &.matched {
      background: #f0fdf4;
      color: #16a34a;
    }
    &.spec-only {
      background: #fffbeb;
      color: #d97706;
    }
    &.p21-only {
      background: #eff6ff;
      color: #2563eb;
    }
  }
  .diff-section {
    margin-bottom: 16px;
  }
  .diff-section-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 8px;
    &.warning { color: #d97706; }
    &.info { color: #2563eb; }
  }
  .diff-table {
    font-size: 12px;
  }
  .diff-perfect {
    text-align: center;
    padding: 24px;
    color: #16a34a;
    font-weight: 600;
    font-size: 15px;
  }
}
</style>
