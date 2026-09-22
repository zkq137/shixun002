<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchModelEffect, fetchModelLogDetail, fetchModelLogs } from '@/api/analysis'
import type { ModelEffectData, ModelLogItem } from '@/api/types'
import MiniChart from '@/components/MiniChart.vue'

const loading = ref(false)
const effect = ref<ModelEffectData | null>(null)

const query = reactive({ modelName: '', status: '', version: '', pageNum: 1, pageSize: 10 })
const logs = ref<ModelLogItem[]>([])
const total = ref(0)
const logLoading = ref(false)

const detailVisible = ref(false)
const detail = ref<ModelLogItem | null>(null)

async function loadEffect() {
  loading.value = true
  try {
    effect.value = await fetchModelEffect()
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const page = await fetchModelLogs(query)
    logs.value = page.records
    total.value = page.total
  } finally {
    logLoading.value = false
  }
}

function search() {
  query.pageNum = 1
  loadLogs()
}

async function openDetail(row: ModelLogItem) {
  detail.value = await fetchModelLogDetail(row.id)
  detailVisible.value = true
}

/** 把 JSON 字符串格式化后展示，方便看训练参数 */
function pretty(json?: string | null) {
  if (!json) return '-'
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    return json
  }
}

const aucLabels = computed(() => (effect.value?.aucTrend ?? []).map((item) => item.name))
const aucValues = computed(() => (effect.value?.aucTrend ?? []).map((item) => item.value / 1000))
const durationLabels = computed(() => (effect.value?.durationTrend ?? []).map((item) => item.name))
const durationValues = computed(() => (effect.value?.durationTrend ?? []).map((item) => item.value))

function statusType(status?: string | null): 'success' | 'danger' | 'warning' | 'info' {
  if (status === '已完成') return 'success'
  if (status === '失败') return 'danger'
  if (status === '训练中') return 'warning'
  return 'info'
}

onMounted(() => {
  loadEffect()
  loadLogs()
})
</script>

<template>
  <div>
    <el-row v-loading="loading" :gutter="16">
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-value">{{ effect?.totalRuns ?? '-' }}</div>
          <div class="stat-label">训练总次数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-value" style="color: #67c23a">{{ effect?.successRuns ?? '-' }}</div>
          <div class="stat-label">成功</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-value" style="color: #f56c6c">{{ effect?.failedRuns ?? '-' }}</div>
          <div class="stat-label">失败</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-value">{{ effect?.successRate ?? '-' }}%</div>
          <div class="stat-label">成功率</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-value">{{ effect?.avgAuc ?? '-' }}</div>
          <div class="stat-label">平均 AUC</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-value">{{ effect?.bestAuc ?? '-' }}</div>
          <div class="stat-label">
            最好效果（{{ effect?.bestModel ?? '-' }} {{ effect?.bestVersion ?? '' }}）
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>分模型效果汇总</template>
      <el-table :data="effect?.modelSummary ?? []" stripe border>
        <el-table-column prop="modelName" label="模型" width="180" />
        <el-table-column prop="modelType" label="类型" width="100" />
        <el-table-column prop="runCount" label="训练次数" width="100" />
        <el-table-column prop="successCount" label="成功次数" width="100" />
        <el-table-column prop="latestVersion" label="最新版本" width="100" />
        <el-table-column label="最新状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.latestStatus)" size="small">{{ row.latestStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="latestTrainTime" label="最近训练时间" width="160" />
        <el-table-column prop="latestAuc" label="最新 AUC" width="100" />
        <el-table-column prop="bestAuc" label="历史最好" width="100" />
        <el-table-column prop="latestDatasetVersion" label="数据版本" min-width="110" />
      </el-table>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>AUC 走势（版本迭代效果）</template>
          <MiniChart :labels="aucLabels" :values="aucValues" type="line" color="#67c23a" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>训练耗时走势（秒）</template>
          <MiniChart :labels="durationLabels" :values="durationValues" type="bar" color="#e6a23c" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="search-bar">
          <span>模型训练日志（溯源）</span>
          <el-input v-model="query.modelName" placeholder="按模型名搜" clearable style="width: 180px" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="已完成" value="已完成" />
            <el-option label="失败" value="失败" />
            <el-option label="训练中" value="训练中" />
          </el-select>
          <el-input v-model="query.version" placeholder="版本号" clearable style="width: 120px" />
          <el-button type="primary" @click="search">查询</el-button>
        </div>
      </template>

      <el-table v-loading="logLoading" :data="logs" stripe border>
        <el-table-column prop="trainTime" label="训练时间" width="160" />
        <el-table-column prop="modelName" label="模型" width="180" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sampleCount" label="样本量" width="90" />
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ row.trainDurationMs ? (row.trainDurationMs / 1000).toFixed(1) + ' 秒' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="datasetVersion" label="数据版本" width="120" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">溯源</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="search"
        @current-change="loadLogs"
      />
    </el-card>

    <el-drawer v-model="detailVisible" size="620px" title="训练日志溯源">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="模型/版本">
          {{ detail.modelName }}（{{ detail.modelType }}） {{ detail.version }}
        </el-descriptions-item>
        <el-descriptions-item label="训练时间">{{ detail.trainTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail.status)" size="small">{{ detail.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="样本量 / 耗时">
          {{ detail.sampleCount }} 条 /
          {{ detail.trainDurationMs ? (detail.trainDurationMs / 1000).toFixed(1) + ' 秒' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="数据版本">{{ detail.datasetVersion }}</el-descriptions-item>
        <el-descriptions-item label="评估指标">
          <pre class="json-block">{{ pretty(detail.metrics) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="训练参数">
          <pre class="json-block">{{ pretty(detail.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<style scoped>
.json-block {
  margin: 0;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
