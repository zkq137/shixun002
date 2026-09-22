<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Calendar, Clock, Money, OfficeBuilding, User, Warning } from '@element-plus/icons-vue'
import { fetchDashboard } from '@/api/analysis'
import type { Dashboard, NameValue } from '@/api/types'

const loading = ref(false)
const data = ref<Dashboard | null>(null)

async function load() {
  loading.value = true
  try {
    data.value = await fetchDashboard()
  } finally {
    loading.value = false
  }
}

onMounted(load)

function percent(value: number, items?: NameValue[]) {
  const total = (items ?? []).reduce((sum, item) => sum + item.value, 0)
  return total === 0 ? 0 : Math.round((value / total) * 100)
}

function money(value?: number | null) {
  if (value === null || value === undefined) return '-'
  return `${(value / 10000).toFixed(1)} 万`
}

function num(value?: number | null, unit = '') {
  if (value === null || value === undefined) return '-'
  return `${value}${unit}`
}
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="4">
        <el-card shadow="hover">
          <el-icon color="#409eff" :size="20"><User /></el-icon>
          <div class="stat-value">{{ data?.totalEmployees ?? '-' }}</div>
          <div class="stat-label">在职员工</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-icon color="#67c23a" :size="20"><OfficeBuilding /></el-icon>
          <div class="stat-value">{{ data?.positionCount ?? '-' }}</div>
          <div class="stat-label">岗位数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-icon color="#f56c6c" :size="20"><Warning /></el-icon>
          <div class="stat-value">{{ data?.highRiskCount ?? '-' }}</div>
          <div class="stat-label">高风险人员</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-icon color="#e6a23c" :size="20"><Money /></el-icon>
          <div class="stat-value">{{ money(data?.avgSalary) }}</div>
          <div class="stat-label">平均年薪</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-icon color="#909399" :size="20"><Clock /></el-icon>
          <div class="stat-value">{{ num(data?.avgTenure, ' 年') }}</div>
          <div class="stat-label">平均司龄</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-icon color="#909399" :size="20"><Calendar /></el-icon>
          <div class="stat-value">{{ num(data?.avgAge, ' 岁') }}</div>
          <div class="stat-label">平均年龄</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>部门人数分布</template>
          <div v-for="item in data?.departmentDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress
              :percentage="percent(item.value, data?.departmentDistribution)"
              :stroke-width="14"
              style="flex: 1"
            />
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>人数最多的 10 个岗位</template>
          <div v-for="item in data?.positionTop ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <el-progress
              :percentage="percent(item.value, data?.positionTop)"
              :stroke-width="14"
              color="#67c23a"
              style="flex: 1"
            />
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>人才标签</template>
          <div v-for="item in data?.talentTagDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>潜力等级</template>
          <div v-for="item in data?.potentialDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>流失风险</template>
          <div v-for="item in data?.warningDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>绩效分布</template>
          <div v-for="item in data?.perfDistribution ?? []" :key="item.name" class="dist-row">
            <span class="dist-name">{{ item.name }}</span>
            <span class="dist-count">{{ item.value }} 人</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
