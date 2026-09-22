<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { fetchPositionOptions } from '@/api/employee'
import { fetchPromotionCandidates } from '@/api/promotion'
import type { PositionOption, PromotionCandidate } from '@/api/types'

const positions = ref<PositionOption[]>([])
const positionId = ref<number | null>(null)
const limit = ref(10)
const loading = ref(false)
const candidates = ref<PromotionCandidate[]>([])

async function load() {
  if (positionId.value === null) return
  loading.value = true
  try {
    candidates.value = await fetchPromotionCandidates(positionId.value, limit.value)
  } finally {
    loading.value = false
  }
}

function warningType(level?: string | null): 'success' | 'warning' | 'danger' | 'info' {
  if (level === '高风险') return 'danger'
  if (level === '中风险') return 'warning'
  return 'success'
}

onMounted(async () => {
  positions.value = await fetchPositionOptions()
})
</script>

<template>
  <div>
    <h2 class="page-title">晋升决策支持</h2>
    <p class="page-tip">
      筛选规则：职级层级要低于目标岗位（员工层 &lt; 主管层 &lt; 经理层 &lt; 总监层 &lt; 高管层 &lt; 决策层）、
      绩效 4 分以上；综合分 = 绩效×15 + 潜力评分×0.5 + 司龄×2。
    </p>

    <el-card shadow="never">
      <div class="search-bar">
        <el-select
          v-model="positionId"
          filterable
          placeholder="选择要晋升去的目标岗位"
          style="width: 320px"
          @change="load"
        >
          <el-option
            v-for="item in positions"
            :key="item.id"
            :label="`${item.department} / ${item.positionName}`"
            :value="item.id"
          />
        </el-select>
        <el-input-number v-model="limit" :min="3" :max="50" />
        <el-button type="primary" :icon="Search" :disabled="positionId === null" @click="load">
          查询候选人
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>晋升候选人（按综合分排序）</template>
      <el-table v-loading="loading" :data="candidates" stripe border>
        <el-table-column type="index" label="排名" width="70" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="empNo" label="工号" width="120" />
        <el-table-column prop="department" label="部门" width="110" />
        <el-table-column prop="levelTier" label="层级" width="90" />
        <el-table-column prop="jobRank" label="职级" width="90" />
        <el-table-column prop="tenureYears" label="司龄" width="80" />
        <el-table-column prop="perfScore" label="绩效" width="80" />
        <el-table-column prop="potentialLevel" label="潜力" width="130" />
        <el-table-column prop="potentialScore" label="潜力分" width="90" />
        <el-table-column label="综合分" width="120">
          <template #default="{ row }">
            <el-tag type="danger" effect="dark">{{ row.totalScore }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="流失风险" width="100">
          <template #default="{ row }">
            <el-tag :type="warningType(row.warningLevel)" effect="plain">
              {{ row.warningLevel ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>选择岗位后点「查询候选人」</template>
      </el-table>
    </el-card>
  </div>
</template>
