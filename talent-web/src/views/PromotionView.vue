<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import MiniChart from '@/components/MiniChart.vue'
import ModuleAssistant from '@/components/ModuleAssistant.vue'
import { useUrlTab } from '@/composables/useUrlTab'
import { fetchPositionOptions } from '@/api/employee'
import { fetchPromotionCandidates, fetchPromotionMatrixCandidates } from '@/api/promotion'
import type { PositionOption, PromotionCandidate } from '@/api/types'

const activeTab = useUrlTab(
  ['ranking', 'matrix', 'distribution', 'risk', 'suggestions', 'assistant'] as const,
  'ranking',
)
const positions = ref<PositionOption[]>([])
const positionId = ref<number | null>(null)
const limit = ref(20)
const loading = ref(false)
const candidates = ref<PromotionCandidate[]>([])
const matrixCandidates = ref<PromotionCandidate[]>([])
const matrixDisplayLimit = 10
const assistantPositionContext = computed(() => {
  const current = positions.value.find((item) => item.id === positionId.value)
  return current ? `${current.department} / ${current.positionName}（岗位ID：${current.id}）` : ''
})
const promotionAssistantPrompts = [
  '评估当前岗位的晋升候选人',
  '比较前 5 名候选人的晋升准备度',
  '分析候选人集中度与部门分布',
  '给出高风险候选人的保留建议',
]

const departmentStats = computed(() => {
  const map = new Map<string, { name: string; count: number; totalScore: number }>()
  candidates.value.forEach((item) => {
    const name = item.department || '未填写'
    const current = map.get(name) ?? { name, count: 0, totalScore: 0 }
    current.count += 1
    current.totalScore += item.totalScore ?? 0
    map.set(name, current)
  })
  return Array.from(map.values())
    .map((item) => ({ ...item, averageScore: item.count ? Number((item.totalScore / item.count).toFixed(1)) : 0 }))
    .sort((a, b) => b.count - a.count)
})

const levelStats = computed(() => {
  const map = new Map<string, number>()
  candidates.value.forEach((item) => {
    const name = item.levelTier || '未填写'
    map.set(name, (map.get(name) ?? 0) + 1)
  })
  return Array.from(map, ([name, value]) => ({ name, value })).sort((a, b) => b.value - a.value)
})

function matrixGroup(item: PromotionCandidate) {
  const highPerformance = (item.perfScore ?? 0) >= 4.5
  const highPotential = (item.potentialScore ?? 0) >= 80
  if (highPerformance && highPotential) return '高绩效 · 高潜力'
  if (highPerformance) return '高绩效 · 潜力待观察'
  if (highPotential) return '绩效待验证 · 高潜力'
  return '持续培养'
}

const matrixGroups = computed(() => {
  const groups = [
    { title: '高绩效 · 高潜力', description: '优先进入晋升与关键岗位储备', items: [] as PromotionCandidate[] },
    { title: '高绩效 · 潜力待观察', description: '建议先扩大职责范围再评估', items: [] as PromotionCandidate[] },
    { title: '绩效待验证 · 高潜力', description: '适合安排挑战性任务和轮岗', items: [] as PromotionCandidate[] },
    { title: '持续培养', description: '保持跟踪，补齐绩效或潜力短板', items: [] as PromotionCandidate[] },
  ]
  const byTitle = new Map(groups.map((group) => [group.title, group]))
  matrixCandidates.value.forEach((item) => byTitle.get(matrixGroup(item))?.items.push(item))
  return groups
})

const riskCandidates = computed(() =>
  matrixCandidates.value.filter((item) => item.warningLevel === '高风险' || item.warningLevel === '中风险'),
)

const suggestions = computed(() =>
  candidates.value.slice(0, 10).map((item, index) => {
    const score = item.totalScore ?? 0
    const readiness = score >= 85 ? '优先推荐' : score >= 70 ? '重点培养' : '持续观察'
    const action = item.warningLevel === '高风险'
      ? '先处理流失风险，再启动晋升沟通'
      : score >= 85
        ? '进入晋升评审并安排岗位见习'
        : score >= 70
          ? '补充关键能力后进入下一轮评估'
          : '制定 3-6 个月成长计划'
    return { ...item, rank: index + 1, readiness, action }
  }),
)

async function load() {
  if (positionId.value === null) return
  loading.value = true
  try {
    const [ranking, matrix] = await Promise.all([
      fetchPromotionCandidates(positionId.value, limit.value),
      fetchPromotionMatrixCandidates(positionId.value),
    ])
    candidates.value = ranking
    matrixCandidates.value = matrix
  } finally {
    loading.value = false
  }
}

function warningType(level?: string | null): 'success' | 'warning' | 'danger' | 'info' {
  if (level === '高风险') return 'danger'
  if (level === '中风险') return 'warning'
  return 'success'
}

function scoreTag(score?: number | null): 'success' | 'warning' | 'danger' | 'info' {
  if (score === null || score === undefined) return 'info'
  if (score >= 85) return 'success'
  if (score >= 70) return 'info'
  if (score >= 60) return 'warning'
  return 'danger'
}

function readinessType(readiness: string): 'success' | 'warning' | 'danger' | 'info' {
  if (readiness === '优先推荐') return 'success'
  if (readiness === '重点培养') return 'info'
  if (readiness === '持续观察') return 'warning'
  return 'danger'
}

onMounted(async () => {
  positions.value = await fetchPositionOptions()
  const first = positions.value[0]
  if (first) {
    positionId.value = first.id
    await load()
  }
})
</script>

<template>
  <div>
    <h2 id="promotion-candidates" class="page-title">晋升决策支持</h2>
    <p class="page-tip">
      筛选规则：职级层级要低于目标岗位（员工层 &lt; 主管层 &lt; 经理层 &lt; 总监层 &lt; 高管层 &lt; 决策层）、绩效 4 分以上。
      人才基础分 =（绩效×15 + 潜力评分×0.5 + 司龄×2）/145×100；岗位匹配 = 核心技能×80% + 加分技能×20%；
      综合分 = 人才基础分×60% + 岗位匹配×40%。以下分析均基于目标岗位技能要求实时计算。
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
        <span class="page-tip" style="margin: 0">排行显示上限（矩阵使用完整候选池）</span>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="module-tabs">
      <el-tab-pane label="候选人排行" name="ranking">
        <el-card shadow="never">
          <template #header>晋升候选人（按综合分排序）</template>
          <el-table v-loading="loading" :data="candidates" stripe>
            <el-table-column type="index" label="排名" width="65" />
            <el-table-column prop="name" label="姓名" width="90" />
            <el-table-column prop="empNo" label="工号" width="120" />
            <el-table-column prop="department" label="部门" width="110" />
            <el-table-column prop="levelTier" label="层级" width="90" />
            <el-table-column prop="jobRank" label="职级" width="85" />
            <el-table-column prop="perfScore" label="绩效" width="75" />
            <el-table-column prop="potentialLevel" label="潜力" width="125" />
            <el-table-column prop="potentialScore" label="潜力分" width="85" />
            <el-table-column label="岗位匹配" width="145">
              <template #default="{ row }">
                <div class="match-cell">
                  <el-tag :type="scoreTag(row.skillMatchScore)" effect="plain">{{ row.skillMatchScore ?? 0 }}%</el-tag>
                  <span class="page-tip">核心 {{ row.matchedCoreSkillCount ?? 0 }}/{{ row.requiredCoreSkillCount ?? 0 }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="综合分" width="110"><template #default="{ row }"><el-tag :type="scoreTag(row.totalScore)" effect="plain">{{ row.totalScore }}</el-tag></template></el-table-column>
            <el-table-column label="流失风险" width="100"><template #default="{ row }"><el-tag :type="warningType(row.warningLevel)" effect="plain">{{ row.warningLevel ?? '-' }}</el-tag></template></el-table-column>
            <template #empty><el-empty description="当前岗位暂无晋升候选人" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="绩效潜力矩阵" name="matrix">
        <el-row :gutter="16" class="matrix-grid">
          <el-col v-for="group in matrixGroups" :key="group.title" :span="12" style="margin-bottom: 16px">
            <el-card shadow="never">
              <template #header><div><strong>{{ group.title }}（{{ group.items.length }} 人）</strong><span class="page-tip" style="margin: 4px 0 0">{{ group.description }}</span></div></template>
              <div v-if="group.items.length" class="candidate-list">
                <div v-for="item in group.items.slice(0, matrixDisplayLimit)" :key="item.employeeId" class="candidate-item">
                  <div><strong>{{ item.name }}</strong><span class="muted">{{ item.department }} · {{ item.levelTier }}</span></div>
                  <div class="candidate-score">绩效 {{ item.perfScore ?? '-' }} / 潜力 {{ item.potentialScore ?? '-' }} / 匹配 {{ item.skillMatchScore ?? 0 }}% / 综合 {{ item.totalScore ?? '-' }}</div>
                </div>
                <div v-if="group.items.length > matrixDisplayLimit" class="page-tip" style="margin: 0">仅展示前 {{ matrixDisplayLimit }} 人，共 {{ group.items.length }} 人</div>
              </div>
              <el-empty v-else description="暂无候选人" :image-size="54" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="部门分布" name="distribution">
        <el-row :gutter="16">
          <el-col :span="15">
            <el-card shadow="never">
              <template #header>候选人部门分布</template>
              <MiniChart :labels="departmentStats.map((item) => item.name)" :values="departmentStats.map((item) => item.count)" type="bar" />
            </el-card>
          </el-col>
          <el-col :span="9">
            <el-card shadow="never">
              <template #header>层级分布</template>
              <div v-for="item in levelStats" :key="item.name" class="dist-row">
                <span class="dist-name">{{ item.name }}</span>
                <el-progress :percentage="candidates.length ? Math.round((item.value / candidates.length) * 100) : 0" :show-text="false" style="flex: 1" />
                <span class="dist-count">{{ item.value }} 人</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-card shadow="never" style="margin-top: 16px">
          <template #header>部门候选质量</template>
          <el-table :data="departmentStats" stripe>
            <el-table-column prop="name" label="部门" min-width="160" />
            <el-table-column prop="count" label="候选人数" width="110" />
            <el-table-column prop="averageScore" label="平均综合分" width="130" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="风险关注" name="risk">
        <el-card shadow="never">
          <template #header>中高风险晋升候选人（共 {{ riskCandidates.length }} 人）</template>
          <el-table :data="riskCandidates" stripe>
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="department" label="部门" width="130" />
            <el-table-column prop="jobRank" label="职级" width="100" />
            <el-table-column prop="perfScore" label="绩效" width="90" />
            <el-table-column prop="totalScore" label="综合分" width="100" />
            <el-table-column label="风险" width="100"><template #default="{ row }"><el-tag :type="warningType(row.warningLevel)">{{ row.warningLevel }}</el-tag></template></el-table-column>
            <el-table-column label="建议" min-width="240"><template #default="{ row }">先完成保留面谈，再评估是否进入晋升流程{{ row.warningLevel === '高风险' ? '；建议准备替代人选' : '' }}</template></el-table-column>
            <template #empty><el-empty description="当前岗位暂无中高风险候选人" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="决策建议" name="suggestions">
        <el-card shadow="never">
          <template #header>综合分与行动建议</template>
          <el-table :data="suggestions" stripe>
            <el-table-column prop="rank" label="序号" width="70" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="department" label="部门" width="120" />
            <el-table-column label="岗位匹配" width="100"><template #default="{ row }">{{ row.skillMatchScore ?? 0 }}%</template></el-table-column>
            <el-table-column prop="totalScore" label="综合分" width="100" />
            <el-table-column label="决策标签" width="120"><template #default="{ row }"><el-tag :type="readinessType(row.readiness)" effect="plain">{{ row.readiness }}</el-tag></template></el-table-column>
            <el-table-column prop="action" label="建议行动" min-width="300" />
            <template #empty><el-empty description="当前岗位暂无决策建议" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="晋升助手" name="assistant">
        <ModuleAssistant
          id="assistant-promotion"
          module-key="promotion"
          title="晋升决策智能体"
          :quick-prompts="promotionAssistantPrompts"
          :context-label="assistantPositionContext"
          status-endpoint="/promotion/ai/status"
          chat-endpoint="/promotion/ai/chat"
          service-name="晋升服务"
          config-env-prefix="PROMOTION_DIFY"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.candidate-list {
  display: grid;
  gap: 10px;
}

.candidate-item {
  padding: 10px 12px;
  border: 1px solid var(--app-line);
  border-radius: 10px;
  background: var(--app-bg);
}

.candidate-score,
.muted {
  display: block;
  margin-top: 4px;
  color: var(--app-muted);
  font-size: 12px;
}

.matrix-grid > .el-col:nth-child(1) :deep(.el-card__header strong) { color: #15803d; }
.matrix-grid > .el-col:nth-child(2) :deep(.el-card__header strong) { color: #1d4ed8; }
.matrix-grid > .el-col:nth-child(3) :deep(.el-card__header strong) { color: #b45309; }
.matrix-grid > .el-col:nth-child(4) :deep(.el-card__header strong) { color: #b91c1c; }

.match-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.match-cell .page-tip {
  margin: 0;
  white-space: nowrap;
}
</style>
