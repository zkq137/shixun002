<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { fetchPositionOptions } from '@/api/employee'
import { fetchRiskEmployees, fetchSkillCoverage, fetchSuccession } from '@/api/planning'
import type { NameValue, PositionOption, RiskEmployee, SuccessionCandidate } from '@/api/types'

const positions = ref<PositionOption[]>([])
const positionId = ref<number | null>(null)
const limit = ref(10)

const loading = ref(false)
const candidates = ref<SuccessionCandidate[]>([])
const coverage = ref<NameValue[]>([])

const riskLoading = ref(false)
const riskList = ref<RiskEmployee[]>([])

async function loadByPosition() {
  if (positionId.value === null) return
  loading.value = true
  try {
    const [list, cover] = await Promise.all([
      fetchSuccession(positionId.value, limit.value),
      fetchSkillCoverage(positionId.value),
    ])
    candidates.value = list
    coverage.value = cover
  } finally {
    loading.value = false
  }
}

async function loadRisk() {
  riskLoading.value = true
  try {
    riskList.value = await fetchRiskEmployees(limit.value)
  } finally {
    riskLoading.value = false
  }
}

function warningType(level?: string | null): 'success' | 'warning' | 'danger' | 'info' {
  if (level === '高风险') return 'danger'
  if (level === '中风险') return 'warning'
  return 'success'
}

function matchColor(score?: number | null) {
  if (!score) return '#909399'
  if (score >= 60) return '#67c23a'
  if (score >= 30) return '#e6a23c'
  return '#f56c6c'
}

onMounted(async () => {
  positions.value = await fetchPositionOptions()
  loadRisk()
})
</script>

<template>
  <div>
    <h2 class="page-title">梯队规划与预测</h2>
    <p class="page-tip">
      继任候选 = 拿目标岗位要求的核心技能，去比对全公司员工的实际技能，算出匹配度。数据来自 position /
      pos_skill_require / emp_skill / resign_warning_record。
    </p>

    <el-card shadow="never">
      <div class="search-bar">
        <el-select
          v-model="positionId"
          filterable
          placeholder="选择要接班的目标岗位"
          style="width: 320px"
          @change="loadByPosition"
        >
          <el-option
            v-for="item in positions"
            :key="item.id"
            :label="`${item.department} / ${item.positionName}`"
            :value="item.id"
          />
        </el-select>
        <el-input-number v-model="limit" :min="3" :max="50" />
        <el-button type="primary" :icon="Search" :disabled="positionId === null" @click="loadByPosition">
          查询候选人
        </el-button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>继任候选人（按技能匹配度排序）</template>
      <el-table v-loading="loading" :data="candidates" stripe border>
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="empNo" label="工号" width="120" />
        <el-table-column prop="department" label="部门" width="110" />
        <el-table-column prop="jobRank" label="当前职级" width="90" />
        <el-table-column prop="levelTier" label="层级" width="90" />
        <el-table-column prop="tenureYears" label="司龄" width="80" />
        <el-table-column prop="perfScore" label="绩效" width="80" />
        <el-table-column prop="potentialLevel" label="潜力" width="130" />
        <el-table-column label="技能匹配度" min-width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="row.matchScore ?? 0"
              :color="matchColor(row.matchScore)"
              :stroke-width="14"
            >
              <span style="font-size: 12px">
                {{ row.matched }}/{{ row.coreRequire }} 项（{{ row.matchScore ?? 0 }}%）
              </span>
            </el-progress>
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

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>该岗位核心技能的全公司覆盖人数（越少越是共性缺口）</template>
          <div v-for="item in coverage" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress
              :percentage="Math.min(item.value, 100)"
              :show-text="false"
              :stroke-width="14"
              color="#e6a23c"
              style="flex: 1"
            />
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
          <el-empty v-if="coverage.length === 0" description="选择岗位后显示" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>流失风险名单（按风险分倒序）</template>
          <el-table v-loading="riskLoading" :data="riskList" stripe height="320">
            <el-table-column prop="name" label="姓名" width="90" />
            <el-table-column prop="department" label="部门" width="110" />
            <el-table-column prop="jobRank" label="职级" width="90" />
            <el-table-column prop="riskScore" label="风险分" width="90" />
            <el-table-column label="等级" width="100">
              <template #default="{ row }">
                <el-tag :type="warningType(row.warningLevel)">{{ row.warningLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="potentialLevel" label="潜力" min-width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
