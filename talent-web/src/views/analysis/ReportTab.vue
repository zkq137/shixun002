<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, MagicStick, Refresh, Star } from '@element-plus/icons-vue'
import {
  deleteReport,
  deleteReportTemplate,
  fetchReportDetail,
  fetchReportHistory,
  fetchReportOptions,
  fetchReportTemplates,
  generateReport,
  generateReportFromTemplate,
  saveReportTemplate,
} from '@/api/analysis'
import { fetchDepartments } from '@/api/employee'
import type { ReportChart, ReportOption, ReportResult, SavedReport } from '@/api/types'
import MiniChart from '@/components/MiniChart.vue'

const loading = ref(false)
const generating = ref(false)
const options = ref<ReportOption[]>([])
const departments = ref<string[]>([])

const form = reactive({
  reportName: '自定义人才报表',
  dimensions: ['department'] as string[],
  metrics: ['count', 'avgSalary', 'highRiskCount'] as string[],
  filters: {} as Record<string, string>,
})

const result = ref<ReportResult | null>(null)
const templates = ref<SavedReport[]>([])
const history = ref<SavedReport[]>([])

const dimensionOptions = computed(() => options.value.filter((item) => item.type === 'dimension'))
const metricOptions = computed(() => options.value.filter((item) => item.type === 'metric'))

const riskLevels = ['低风险', '中风险', '高风险']
const talentTags = ['核心骨干', '储备人才', '普通员工', '待优化']
const potentialLevels = ['S级（高潜）', 'A级（优秀）', 'B级（合格）', 'C级（待提升）']
const positionTypes = ['高管', '总监', '经理岗', '管理岗', '技术岗', '设计岗', '支撑岗']

function labelOf(key: string) {
  return options.value.find((item) => item.key === key)?.label ?? key
}

async function loadBase() {
  loading.value = true
  try {
    const [optionList, deptList] = await Promise.all([fetchReportOptions(), fetchDepartments()])
    options.value = optionList
    departments.value = deptList
    await Promise.all([loadTemplates(), loadHistory()])
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  templates.value = await fetchReportTemplates()
}

async function loadHistory() {
  history.value = await fetchReportHistory()
}

async function generate(save = false) {
  if (form.dimensions.length === 0 && form.metrics.length === 0) {
    ElMessage.warning('至少要选一个维度或指标')
    return
  }
  generating.value = true
  try {
    result.value = await generateReport({
      reportName: form.reportName,
      dimensions: form.dimensions,
      metrics: form.metrics,
      filters: cleanFilters(),
      save,
    })
    ElMessage.success(save ? '报表已生成并存档' : '报表已生成')
    if (save) {
      loadHistory()
    }
  } finally {
    generating.value = false
  }
}

function cleanFilters() {
  const filters: Record<string, string> = {}
  Object.entries(form.filters).forEach(([key, value]) => {
    if (value) {
      filters[key] = value
    }
  })
  return filters
}

async function saveAsTemplate() {
  await saveReportTemplate({
    reportName: form.reportName,
    dimensions: form.dimensions,
    metrics: form.metrics,
    filters: cleanFilters(),
  })
  ElMessage.success('已保存为模板')
  loadTemplates()
}

async function useTemplate(row: SavedReport) {
  generating.value = true
  try {
    result.value = await generateReportFromTemplate(row.id)
    ElMessage.success(`已按模板「${row.reportName}」生成`)
    loadHistory()
  } finally {
    generating.value = false
  }
}

async function removeTemplate(row: SavedReport) {
  await ElMessageBox.confirm(`确定删除模板「${row.reportName}」吗？`, '删除确认', { type: 'warning' })
  await deleteReportTemplate(row.id)
  ElMessage.success('模板已删除')
  loadTemplates()
}

/** 从历史报表回看：把当时用的维度和指标还原到表单，数据直接用存下来的 */
async function viewHistory(row: SavedReport) {
  const detail = await fetchReportDetail(row.id)
  form.reportName = detail.reportName
  form.dimensions = (detail.dimensions ?? '').split(',').filter(Boolean)
  form.metrics = (detail.metrics ?? '').split(',').filter(Boolean)
  form.filters = detail.filters ? JSON.parse(detail.filters) : {}

  const rows = detail.reportData ? JSON.parse(detail.reportData) : []
  const chart: ReportChart = detail.chartData ? JSON.parse(detail.chartData) : { categories: [], series: [] }
  result.value = {
    reportName: detail.reportName,
    columns: [
      ...form.dimensions.map((key) => ({ key, label: labelOf(key), type: 'dimension' })),
      ...form.metrics.map((key) => ({ key, label: labelOf(key), type: 'number' })),
    ],
    rows,
    chart,
    reportId: detail.id,
    generatedAt: detail.createdAt,
    dataVersion: detail.dataVersion,
    modelVersion: detail.modelVersion,
  }
  ElMessage.info('已载入历史报表，数据是当时存档的')
}

async function removeHistory(row: SavedReport) {
  await ElMessageBox.confirm(`确定删除报表「${row.reportName}」吗？`, '删除确认', { type: 'warning' })
  await deleteReport(row.id)
  ElMessage.success('报表已删除')
  loadHistory()
}

function resetForm() {
  form.reportName = '自定义人才报表'
  form.dimensions = ['department']
  form.metrics = ['count']
  form.filters = {}
  result.value = null
}

onMounted(loadBase)
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="search-bar">
          <span>自定义报表</span>
          <div style="flex: 1"></div>
          <el-button :icon="Refresh" @click="resetForm">重置</el-button>
          <el-button :icon="Star" @click="saveAsTemplate">保存为模板</el-button>
          <el-button type="primary" :icon="MagicStick" :loading="generating" @click="generate(false)">
            生成报表
          </el-button>
        </div>
      </template>

      <el-form label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="报表名称">
              <el-input v-model="form.reportName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="维度">
              <el-select v-model="form.dimensions" multiple collapse-tags placeholder="按什么分组" style="width: 100%">
                <el-option v-for="item in dimensionOptions" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="指标">
              <el-select v-model="form.metrics" multiple collapse-tags placeholder="看哪些数字" style="width: 100%">
                <el-option v-for="item in metricOptions" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="部门">
              <el-select v-model="form.filters.department" clearable placeholder="全部" style="width: 100%">
                <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="流失风险">
              <el-select v-model="form.filters.warningLevel" clearable placeholder="全部" style="width: 100%">
                <el-option v-for="item in riskLevels" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="人才标签">
              <el-select v-model="form.filters.talentTag" clearable placeholder="全部" style="width: 100%">
                <el-option v-for="item in talentTags" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="潜力等级">
              <el-select v-model="form.filters.potentialLevel" clearable placeholder="全部" style="width: 100%">
                <el-option v-for="item in potentialLevels" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="岗位类型">
              <el-select v-model="form.filters.positionType" clearable placeholder="全部" style="width: 100%">
                <el-option v-for="item in positionTypes" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card v-if="result" shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="search-bar">
          <span>{{ result.reportName }}（{{ result.rows.length }} 行）</span>
          <el-tag v-if="result.reportId" type="success" size="small">已存档 #{{ result.reportId }}</el-tag>
          <div style="flex: 1"></div>
          <span class="stat-label">
            生成于 {{ result.generatedAt }} ｜ 数据版本 {{ result.dataVersion }} ｜ 模型版本 {{ result.modelVersion }}
          </span>
          <el-button v-if="!result.reportId" type="primary" @click="generate(true)">存档这份报表</el-button>
        </div>
      </template>

      <MiniChart
        v-if="result.chart.categories.length > 0"
        :labels="result.chart.categories"
        :values="result.chart.series[0]?.data.map((v) => Number(v) || 0) ?? []"
        type="bar"
        :color="'#409eff'"
      />
      <div v-if="result.chart.series.length > 1" class="stat-label" style="margin-bottom: 12px">
        图中画的是第一个指标「{{ result.chart.series[0]?.name }}」；多维度的其余组合见下表。
      </div>

      <el-table :data="result.rows" stripe border max-height="420" style="margin-top: 12px">
        <el-table-column
          v-for="column in result.columns"
          :key="column.key"
          :prop="column.key"
          :label="column.label"
          :min-width="column.type === 'dimension' ? 130 : 110"
        />
      </el-table>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>报表模板（一键生成）</template>
          <el-table :data="templates" stripe border>
            <el-table-column prop="reportName" label="模板名称" min-width="140" />
            <el-table-column prop="dimensions" label="维度" min-width="120" />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button link type="primary" @click="useTemplate(row)">生成</el-button>
                <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>历史报表（可回看、可溯源）</template>
          <el-table :data="history" stripe border max-height="320">
            <el-table-column prop="reportName" label="报表" min-width="140" />
            <el-table-column prop="rowCount" label="行数" width="80" />
            <el-table-column prop="dataVersion" label="数据版本" width="110" />
            <el-table-column prop="modelVersion" label="模型版本" width="100" />
            <el-table-column prop="createdAt" label="生成时间" width="150" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewHistory(row)">回看</el-button>
                <el-button link type="danger" @click="removeHistory(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
