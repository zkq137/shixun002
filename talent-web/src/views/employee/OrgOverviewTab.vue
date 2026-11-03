<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MiniChart from '@/components/MiniChart.vue'
import { fetchDepartmentStats, fetchEmployeePage } from '@/api/employee'
import type { EmployeeRow, NameValue } from '@/api/types'

const loading = ref(false)
const total = ref(0)
const rows = ref<EmployeeRow[]>([])
const departmentStats = ref<NameValue[]>([])

function distribution(field: keyof EmployeeRow) {
  const counts = new Map<string, number>()
  rows.value.forEach((row) => {
    const value = String(row[field] ?? '未填写') || '未填写'
    counts.set(value, (counts.get(value) ?? 0) + 1)
  })
  return Array.from(counts, ([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value)
}

function average(field: 'age' | 'tenureYears') {
  const values = rows.value
    .map((row) => row[field])
    .filter((value): value is number => typeof value === 'number')
  return values.length ? values.reduce((sum, value) => sum + value, 0) / values.length : null
}

const levelDistribution = computed(() => distribution('levelTier'))
const workModeDistribution = computed(() => distribution('workMode'))
const statusDistribution = computed(() => distribution('status'))

onMounted(async () => {
  loading.value = true
  try {
    const [stats, page] = await Promise.all([
      fetchDepartmentStats(),
      fetchEmployeePage({ pageNum: 1, pageSize: 1000 }),
    ])
    departmentStats.value = stats
    rows.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="14">
      <el-col :span="6"><el-card shadow="never"><div class="stat-label">员工总数</div><div class="stat-value text-info">{{ total }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-label">部门数量</div><div class="stat-value text-success">{{ departmentStats.length }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-label">平均年龄</div><div class="stat-value text-warning">{{ average('age')?.toFixed(1) ?? '-' }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="stat-label">平均司龄</div><div class="stat-value text-info">{{ average('tenureYears')?.toFixed(1) ?? '-' }} 年</div></el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="15">
        <el-card shadow="never">
          <template #header>部门人数分布</template>
          <MiniChart :labels="departmentStats.map((item) => item.name)" :values="departmentStats.map((item) => item.value)" type="bar" />
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>职级层级</template>
          <div v-for="item in levelDistribution" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress :percentage="total ? Math.round((item.value / total) * 100) : 0" :show-text="false" style="flex: 1" />
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>办公方式</template>
          <el-table :data="workModeDistribution" stripe>
            <el-table-column prop="name" label="方式" />
            <el-table-column prop="value" label="人数" width="100" />
            <el-table-column label="占比" width="100"><template #default="{ row }">{{ total ? Math.round((row.value / total) * 100) : 0 }}%</template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>在职状态</template>
          <el-table :data="statusDistribution" stripe>
            <el-table-column prop="name" label="状态" />
            <el-table-column prop="value" label="人数" width="100" />
            <el-table-column label="占比" width="100"><template #default="{ row }">{{ total ? Math.round((row.value / total) * 100) : 0 }}%</template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dist-row:last-child { margin-bottom: 0; }
</style>
