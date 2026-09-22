<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Star, TrendCharts, User, Warning } from '@element-plus/icons-vue'
import { fetchPipeline } from '@/api/analysis'
import type { NameValue, PipelineData } from '@/api/types'

const loading = ref(false)
const data = ref<PipelineData | null>(null)

async function load() {
  loading.value = true
  try {
    data.value = await fetchPipeline()
  } finally {
    loading.value = false
  }
}

onMounted(load)

function percent(value: number, items?: NameValue[]) {
  const total = (items ?? []).reduce((sum, item) => sum + item.value, 0)
  return total === 0 ? 0 : Math.round((value / total) * 100)
}

/** 健康度颜色 */
const healthColor = computed(() => {
  const score = data.value?.healthScore ?? 0
  if (score >= 85) return '#67c23a'
  if (score >= 70) return '#409eff'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
})

/**
 * 九宫格：后端给的是 3×3，行是绩效（0 低 / 1 中 / 2 高），列是潜力（0 低 / 1 中 / 2 高）。
 * 展示时把高绩效放上面，所以倒着遍历。
 */
const nineCells = computed(() => {
  const grid = data.value?.nineBox ?? []
  const perfLabels = ['绩效低', '绩效中', '绩效高']
  const potentialLabels = ['潜力低', '潜力中', '潜力高']
  const cells: { count: number; title: string; tone: string }[] = []
  for (let perf = 2; perf >= 0; perf--) {
    for (let potential = 0; potential < 3; potential++) {
      const count = grid[perf]?.[potential] ?? 0
      const sum = perf + potential
      const tone = sum >= 3 ? 'good' : sum >= 2 ? 'ok' : 'warn'
      cells.push({ count, title: `${perfLabels[perf]} · ${potentialLabels[potential]}`, tone })
    }
  }
  return cells
})
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-icon color="#409eff" :size="20"><User /></el-icon>
          <div class="stat-value">{{ data?.totalEmployees ?? '-' }}</div>
          <div class="stat-label">在职员工</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-icon color="#f56c6c" :size="20"><Star /></el-icon>
          <div class="stat-value">{{ data?.coreTalentCount ?? '-' }}</div>
          <div class="stat-label">核心骨干（储备人才 {{ data?.reserveTalentCount ?? 0 }}）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-icon color="#67c23a" :size="20"><TrendCharts /></el-icon>
          <div class="stat-value">{{ data?.highPotentialCount ?? '-' }}</div>
          <div class="stat-label">高潜人才（S/A 级）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-icon color="#e6a23c" :size="20"><Warning /></el-icon>
          <div class="stat-value">{{ data?.highRiskCount ?? '-' }}</div>
          <div class="stat-label">
            高风险（中风险 {{ data?.middleRiskCount ?? 0 }}，占比 {{ data?.highRiskRate ?? 0 }}%）
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>梯队健康度</template>
          <div style="text-align: center">
            <el-progress
              type="dashboard"
              :percentage="Math.round(data?.healthScore ?? 0)"
              :color="healthColor"
              :width="150"
            >
              <template #default>
                <div style="font-size: 26px; font-weight: 600">{{ data?.healthScore ?? '-' }}</div>
                <div class="stat-label">{{ data?.healthLevel ?? '' }}</div>
              </template>
            </el-progress>
          </div>
          <el-divider style="margin: 12px 0" />
          <div class="stat-label" style="margin-bottom: 8px">管理建议</div>
          <ul class="suggestion-list">
            <li v-for="(item, index) in data?.healthSuggestions ?? []" :key="index">{{ item }}</li>
          </ul>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            绩效 × 潜力 九宫格
            <span class="stat-label" style="margin-left: 8px">
              右上角是「高绩效高潜力」的明星员工，左下角需要重点关注
            </span>
          </template>
          <div class="nine-box">
            <div v-for="(cell, index) in nineCells" :key="index" class="nine-cell" :class="cell.tone">
              <div class="nine-count">{{ cell.count }}</div>
              <div class="nine-title">{{ cell.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>职级层级分布</template>
          <div v-for="item in data?.levelTierDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress
              :percentage="percent(item.value, data?.levelTierDistribution)"
              :stroke-width="14"
              style="flex: 1"
            />
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>岗位类型分布</template>
          <div v-for="item in data?.positionTypeDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress
              :percentage="percent(item.value, data?.positionTypeDistribution)"
              :stroke-width="14"
              color="#67c23a"
              style="flex: 1"
            />
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>年龄结构</template>
          <div v-for="item in data?.ageRangeDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress
              :percentage="percent(item.value, data?.ageRangeDistribution)"
              :stroke-width="14"
              color="#e6a23c"
              style="flex: 1"
            />
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
          <el-divider style="margin: 12px 0" />
          <div class="stat-label">司龄结构</div>
          <div v-for="item in data?.tenureRangeDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress
              :percentage="percent(item.value, data?.tenureRangeDistribution)"
              :stroke-width="14"
              color="#909399"
              style="flex: 1"
            />
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.suggestion-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #606266;
  line-height: 1.9;
}

.nine-box {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.nine-cell {
  border-radius: 6px;
  padding: 18px 8px;
  text-align: center;
  border: 1px solid transparent;
}

.nine-cell.good {
  background: #f0f9eb;
  border-color: #e1f3d8;
}

.nine-cell.ok {
  background: #fdf6ec;
  border-color: #faecd8;
}

.nine-cell.warn {
  background: #fef0f0;
  border-color: #fde2e2;
}

.nine-count {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.nine-title {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
