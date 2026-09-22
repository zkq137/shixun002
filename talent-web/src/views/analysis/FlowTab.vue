<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchFlowTrend } from '@/api/analysis'
import type { FlowTrendData } from '@/api/types'
import MiniChart from '@/components/MiniChart.vue'

const loading = ref(false)
const data = ref<FlowTrendData | null>(null)

async function load() {
  loading.value = true
  try {
    data.value = await fetchFlowTrend()
  } finally {
    loading.value = false
  }
}

onMounted(load)

const yearlyLabels = computed(() => (data.value?.joinYearly ?? []).map((item) => item.name))
const yearlyValues = computed(() => (data.value?.joinYearly ?? []).map((item) => item.value))

const monthlyLabels = computed(() => (data.value?.joinMonthly ?? []).map((item) => item.name))
const monthlyValues = computed(() => (data.value?.joinMonthly ?? []).map((item) => item.value))

const riskLabels = computed(() => (data.value?.riskByTenure ?? []).map((item) => item.name))
const riskValues = computed(() => (data.value?.riskByTenure ?? []).map((item) => item.value))
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="5">
        <el-card shadow="hover">
          <div class="stat-value">{{ data?.totalJoin ?? '-' }}</div>
          <div class="stat-label">累计入职记录</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover">
          <div class="stat-value">{{ data?.recentJoinCount ?? '-' }}</div>
          <div class="stat-label">近 12 个月入职</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover">
          <div class="stat-value">{{ data?.avgAnnualJoin ?? '-' }}</div>
          <div class="stat-label">年均入职人数</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="hover">
          <div class="stat-value">{{ data?.leaveCount ?? 0 }}</div>
          <div class="stat-label">离职记录</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-value">{{ data?.promoteCount ?? 0 }}</div>
          <div class="stat-label">晋升记录</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      type="info"
      :closable="false"
      style="margin-top: 16px"
      title="说明：入职时间由员工司龄推算得到，是现有数据里唯一能真实还原的流动事件；晋升/调岗/离职会在业务发生时写入 emp_movement 表，之后这里会自动出现。"
    />

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>历年入职人数（人才流入趋势）</template>
          <MiniChart :labels="yearlyLabels" :values="yearlyValues" type="line" color="#409eff" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>近 24 个月入职人数</template>
          <MiniChart :labels="monthlyLabels" :values="monthlyValues" type="bar" color="#67c23a" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>各司龄段的高风险人数（哪个阶段最容易流失）</template>
          <MiniChart :labels="riskLabels" :values="riskValues" type="bar" color="#f56c6c" />
          <div class="stat-label" style="margin-top: 8px">
            柱越高说明这个司龄段的高风险人员越多，留任措施应该优先投在这里
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>流动事件构成</template>
          <div v-for="item in data?.movementByType ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress
              :percentage="
                Math.round((item.value / Math.max(1, (data?.movementByType ?? []).reduce((s, i) => s + i.value, 0))) * 100)
              "
              :stroke-width="14"
              style="flex: 1"
            />
            <span class="dist-count">{{ item.value }} 条</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>按入职年份看每一批人（现在过得怎么样）</template>
      <el-table :data="data?.cohorts ?? []" stripe border max-height="360">
        <el-table-column prop="hireYear" label="入职年份" width="110" />
        <el-table-column prop="employeeCount" label="现有人数" width="110" />
        <el-table-column prop="avgTenure" label="平均司龄(年)" width="130" />
        <el-table-column prop="avgPerf" label="平均绩效" width="110" />
        <el-table-column prop="coreTalentCount" label="核心骨干" width="110" />
        <el-table-column prop="highRiskCount" label="高风险人数" width="120" />
        <el-table-column label="高风险占比" min-width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min(100, Number(row.highRiskRate ?? 0) * 5)"
              :format="() => `${row.highRiskRate ?? 0}%`"
              :stroke-width="14"
              :color="(row.highRiskRate ?? 0) > 1 ? '#f56c6c' : '#67c23a'"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
