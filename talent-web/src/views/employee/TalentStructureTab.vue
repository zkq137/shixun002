<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchEmployeePage } from '@/api/employee'
import type { EmployeeRow, NameValue } from '@/api/types'

const loading = ref(false)
const rows = ref<EmployeeRow[]>([])

function distribution(field: keyof EmployeeRow) {
  const counts = new Map<string, number>()
  rows.value.forEach((row) => {
    const value = String(row[field] ?? '未填写') || '未填写'
    counts.set(value, (counts.get(value) ?? 0) + 1)
  })
  return Array.from(counts, ([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value)
}

const talentTags = computed(() => distribution('talentTag'))
const warningLevels = computed(() => distribution('warningLevel'))
const potentialLevels = computed(() => distribution('potentialLevel'))
const perfLevels = computed<NameValue[]>(() => {
  const counts = new Map<string, number>([['高绩效（4.5+）', 0], ['中绩效（3.5-4.5）', 0], ['待提升（<3.5）', 0]])
  rows.value.forEach((row) => {
    const score = row.perfScore ?? 0
    const level = score >= 4.5 ? '高绩效（4.5+）' : score >= 3.5 ? '中绩效（3.5-4.5）' : '待提升（<3.5）'
    counts.set(level, (counts.get(level) ?? 0) + 1)
  })
  return Array.from(counts, ([name, value]) => ({ name, value }))
})

const total = computed(() => rows.value.length)
const coreCount = computed(() => rows.value.filter((row) => row.talentTag === '核心骨干').length)
const highRiskCount = computed(() => rows.value.filter((row) => row.warningLevel === '高风险').length)
const highPerfCount = computed(() => rows.value.filter((row) => (row.perfScore ?? 0) >= 4.5).length)

function percent(value: number) {
  return total.value ? Math.round((value / total.value) * 100) : 0
}

onMounted(async () => {
  loading.value = true
  try {
    const page = await fetchEmployeePage({ pageNum: 1, pageSize: 1000 })
    rows.value = page.records
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="14">
      <el-col :span="8"><el-card shadow="never"><div class="stat-label">核心骨干占比</div><div class="stat-value text-info">{{ percent(coreCount) }}%</div><div class="page-tip" style="margin: 6px 0 0">共 {{ coreCount }} 人</div></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><div class="stat-label">高绩效占比</div><div class="stat-value text-success">{{ percent(highPerfCount) }}%</div><div class="page-tip" style="margin: 6px 0 0">共 {{ highPerfCount }} 人</div></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><div class="stat-label">高风险占比</div><div class="stat-value text-danger">{{ percent(highRiskCount) }}%</div><div class="page-tip" style="margin: 6px 0 0">共 {{ highRiskCount }} 人</div></el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col v-for="panel in [
        { title: '人才标签', items: talentTags },
        { title: '流失风险', items: warningLevels },
        { title: '潜力等级', items: potentialLevels },
        { title: '绩效分层', items: perfLevels },
      ]" :key="panel.title" :span="12" style="margin-bottom: 16px">
        <el-card shadow="never">
          <template #header>{{ panel.title }}</template>
          <div v-for="item in panel.items" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress :percentage="percent(item.value)" :show-text="false" style="flex: 1" />
            <span class="dist-count">{{ item.value }} 人 · {{ percent(item.value) }}%</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
