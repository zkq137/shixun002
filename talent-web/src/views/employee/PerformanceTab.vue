<script setup lang="ts">
import { computed, ref } from 'vue'
import MiniChart from '@/components/MiniChart.vue'
import { fetchEmployeePage, fetchPerformanceHistory, fetchPerformanceTrend } from '@/api/employee'
import type { EmployeeRow, PerformanceRecord, PerformanceTrend } from '@/api/types'

const loading = ref(false)
const searching = ref(false)
const options = ref<EmployeeRow[]>([])
const selectedEmployee = ref<EmployeeRow | null>(null)
const employeeId = ref<number | null>(null)
const history = ref<PerformanceRecord[]>([])
const trend = ref<PerformanceTrend | null>(null)

const chartLabels = computed(() => (trend.value?.years ?? []).map(String))
const chartValues = computed(() => trend.value?.scores ?? [])
const latestRecord = computed(() => history.value[history.value.length - 1] ?? null)
const latestDimensions = computed(() => Object.entries(latestRecord.value?.dimensions ?? {}))
const trendTone = computed(() => {
  const value = trend.value?.trend ?? ''
  if (/上升|提升|增长/.test(value)) return 'value-positive'
  if (/下降|降低|下滑/.test(value)) return 'value-negative'
  return 'value-warning'
})
const chartColor = computed(() => {
  if (trendTone.value === 'value-positive') return '#16a34a'
  if (trendTone.value === 'value-negative') return '#dc2626'
  return '#2563eb'
})

function gradeType(grade?: string | null): 'success' | 'warning' | 'danger' | 'info' {
  const value = String(grade ?? '')
  if (/^(S|A|优秀)/.test(value)) return 'success'
  if (/^(B|良好)/.test(value)) return 'info'
  if (/^(C|合格|中等)/.test(value)) return 'warning'
  if (/^(D|待改进|差)/.test(value)) return 'danger'
  return 'info'
}

async function searchEmployees(keyword: string) {
  const text = keyword.trim()
  if (!text) {
    options.value = []
    return
  }
  searching.value = true
  try {
    const page = await fetchEmployeePage({ pageNum: 1, pageSize: 30, keyword: text })
    options.value = page.records
  } catch {
    options.value = []
  } finally {
    searching.value = false
  }
}

function handleEmployeeChange(id: number | null | undefined) {
  if (id === null || id === undefined) {
    employeeId.value = null
    selectedEmployee.value = null
    history.value = []
    trend.value = null
    return
  }
  selectedEmployee.value = options.value.find((item) => item.id === id) ?? null
  loadData()
}

async function loadData() {
  if (employeeId.value === null || employeeId.value === undefined) return
  loading.value = true
  try {
    const [records, trendData] = await Promise.all([
      fetchPerformanceHistory(employeeId.value),
      fetchPerformanceTrend(employeeId.value),
    ])
    history.value = records ?? []
    trend.value = trendData
  } catch {
    history.value = []
    trend.value = null
  } finally {
    loading.value = false
  }
}

</script>

<template>
  <div>
    <el-card shadow="never">
      <div class="search-bar">
        <el-select
          v-model="employeeId"
          filterable
          remote
          reserve-keyword
          clearable
          :remote-method="searchEmployees"
          :loading="searching"
          placeholder="输入员工姓名搜索"
          style="width: 340px"
          @change="handleEmployeeChange"
        >
          <el-option v-for="item in options" :key="item.id" :label="`${item.name}（${item.empNo}）- ${item.department}`" :value="item.id" />
        </el-select>
        <el-button type="primary" :disabled="employeeId === null" :loading="loading" @click="loadData">加载档案</el-button>
      </div>
    </el-card>

    <el-row :gutter="14" style="margin-top: 16px">
      <el-col :span="6"><el-card shadow="never"><div class="stat-label">当前员工</div><div class="stat-value compact text-info">{{ selectedEmployee?.name ?? '-' }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-label">平均绩效</div><div class="stat-value text-info">{{ trend?.averageScore ?? '-' }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-label">最高绩效</div><div class="stat-value text-success">{{ trend?.maxScore ?? '-' }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-label">趋势</div><div class="stat-value compact" :class="trendTone">{{ trend?.trend ?? '-' }}</div><div class="page-tip" style="margin: 6px 0 0">同比变化 <span :class="trendTone">{{ trend?.yoyChange ?? '-' }}</span></div></el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="15">
        <el-card v-loading="loading" shadow="never">
          <template #header>历年绩效趋势</template>
          <MiniChart v-if="chartValues.length" :labels="chartLabels" :values="chartValues" type="line" :color="chartColor" />
          <el-empty v-else description="暂无绩效趋势数据" :image-size="72" />
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>最新维度得分</template>
          <div v-if="latestDimensions.length" class="dimension-list">
            <div v-for="[name, score] in latestDimensions" :key="name" class="dimension-item">
              <span>{{ name }}</span><strong>{{ score }}</strong>
            </div>
          </div>
          <el-empty v-else description="暂无维度得分" :image-size="64" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>年度绩效记录</template>
      <el-table v-loading="loading" :data="history" stripe>
        <el-table-column prop="perfYear" label="年度" width="90" />
        <el-table-column prop="score" label="绩效分" width="100" />
        <el-table-column label="等级" width="100"><template #default="{ row }"><el-tag :type="gradeType(row.grade)" effect="plain">{{ row.grade }}</el-tag></template></el-table-column>
        <el-table-column label="维度明细" min-width="360">
          <template #default="{ row }">
            <el-tag v-for="[name, score] in Object.entries(row.dimensions ?? {})" :key="name" effect="plain" style="margin: 2px 6px 2px 0">{{ name }} {{ score }}</el-tag>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无绩效记录" /></template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.stat-value.compact { font-size: 21px; }
.dimension-list { display: grid; gap: 10px; }
.dimension-item { display: flex; align-items: center; justify-content: space-between; padding-bottom: 8px; border-bottom: 1px solid var(--app-line); font-size: 13px; }
.dimension-item:last-child { padding-bottom: 0; border-bottom: none; }
</style>
