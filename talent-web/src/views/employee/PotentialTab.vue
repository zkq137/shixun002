<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchEmployeePage, fetchPotential, fetchPotentialModel } from '@/api/employee'
import type { EmployeeRow, PotentialModel, PotentialRecord } from '@/api/types'

const loading = ref(false)
const options = ref<EmployeeRow[]>([])
const employeeId = ref<number | null>(null)
const model = ref<PotentialModel | null>(null)
const record = ref<PotentialRecord | null>(null)

const indicatorRows = computed(() => (model.value?.indicators ?? []).map((indicator) => ({
  ...indicator,
  score: record.value?.indicators?.[indicator.key] ?? 0,
})))

function scoreTone(score?: number | null) {
  if (score === null || score === undefined) return ''
  if (score >= 85) return 'value-positive'
  if (score >= 70) return 'text-info'
  if (score >= 60) return 'value-warning'
  return 'value-negative'
}

async function loadData() {
  if (employeeId.value === null) return
  loading.value = true
  try {
    record.value = await fetchPotential(employeeId.value)
  } catch {
    record.value = null
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const [page, modelData] = await Promise.all([
    fetchEmployeePage({ pageNum: 1, pageSize: 300 }),
    fetchPotentialModel(),
  ])
  options.value = page.records
  model.value = modelData
  const first = options.value[0]
  if (first) {
    employeeId.value = first.id
    await loadData()
  }
})
</script>

<template>
  <div>
    <el-card shadow="never">
      <div class="search-bar">
        <el-select v-model="employeeId" filterable placeholder="选择员工查看潜力评估" style="width: 340px" @change="loadData">
          <el-option v-for="item in options" :key="item.id" :label="`${item.name}（${item.empNo}）- ${item.department}`" :value="item.id" />
        </el-select>
        <el-button type="primary" :disabled="employeeId === null" :loading="loading" @click="loadData">加载评估</el-button>
      </div>
    </el-card>

    <el-alert v-if="model" :title="model.formula" type="info" :closable="false" style="margin-top: 16px" />

    <el-row :gutter="14" style="margin-top: 16px">
      <el-col :span="8"><el-card shadow="never"><div class="stat-label">潜力等级</div><div class="stat-value compact text-info">{{ record?.potentialLevel ?? '-' }}</div></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><div class="stat-label">量化总分</div><div class="stat-value" :class="scoreTone(record?.totalScore)">{{ record?.totalScore ?? '-' }}</div></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><div class="stat-label">评估更新时间</div><div class="stat-value date">{{ record?.updatedAt?.replace('T', ' ').slice(0, 16) ?? '-' }}</div></el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="15">
        <el-card v-loading="loading" shadow="never">
          <template #header>潜力指标得分</template>
          <el-table :data="indicatorRows" stripe>
            <el-table-column prop="label" label="指标" min-width="150" />
            <el-table-column prop="weight" label="权重" width="90" />
            <el-table-column label="得分" width="90"><template #default="{ row }"><span class="number-emphasis" :class="scoreTone(row.score)">{{ row.score }}</span></template></el-table-column>
            <el-table-column prop="description" label="说明" min-width="240" />
            <template #empty><el-empty description="暂无指标数据" /></template>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="9">
        <el-card shadow="never">
          <template #header>等级规则</template>
          <el-table :data="model?.levels ?? []" stripe>
            <el-table-column prop="level" label="等级" />
            <el-table-column prop="minScore" label="最低分" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-value.compact { font-size: 21px; }
.stat-value.date { font-size: 17px; }
</style>
