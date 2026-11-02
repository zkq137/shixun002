<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  fetchModelArtifacts,
  fetchModelEffect,
  fetchModelLogDetail,
  fetchModelLogs,
  predictAttrition,
  predictHighPotential,
  recommendPositions,
  trainAllModels,
  trainModel,
} from '@/api/analysis'
import type {
  ModelArtifact,
  ModelEffectData,
  ModelLogItem,
  ModelPrediction,
  PositionRecommendation,
} from '@/api/types'
import MiniChart from '@/components/MiniChart.vue'

const loading = ref(false)
const effect = ref<ModelEffectData | null>(null)

const query = reactive({ modelName: '', status: '', version: '', pageNum: 1, pageSize: 10 })
const logs = ref<ModelLogItem[]>([])
const total = ref(0)
const logLoading = ref(false)

const detailVisible = ref(false)
const detail = ref<ModelLogItem | null>(null)

const artifacts = ref<ModelArtifact[]>([])
const training = ref(false)
const predicting = ref(false)
const employeeId = ref<string | number>('龙秀英')
const topN = ref(5)
const predictionVisible = ref(false)
const prediction = ref<ModelPrediction | null>(null)
const recommendations = ref<PositionRecommendation | null>(null)
const predictionTitle = ref('模型推理结果')

async function loadEffect() {
  loading.value = true
  try {
    effect.value = await fetchModelEffect()
  } finally {
    loading.value = false
  }
}

async function loadArtifacts() {
  artifacts.value = await fetchModelArtifacts()
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
const contributionRows = computed(() =>
  Object.entries(prediction.value?.topFeatures ?? {}).map(([name, value]) => ({ name, value })),
)

async function runTraining(modelCode?: string) {
  if (training.value) return
  training.value = true
  try {
    const results = modelCode ? [await trainModel(modelCode)] : await trainAllModels()
    ElMessage.success(`训练完成：${results.length} 个模型已部署`)
    await Promise.all([loadEffect(), loadLogs(), loadArtifacts()])
  } finally {
    training.value = false
  }
}

async function runPrediction(kind: 'attrition' | 'potential' | 'position') {
  if (!String(employeeId.value).trim() || predicting.value) return
  predicting.value = true
  prediction.value = null
  recommendations.value = null
  try {
    if (kind === 'attrition') {
      predictionTitle.value = '员工流失风险预测'
      prediction.value = await predictAttrition(employeeId.value)
    } else if (kind === 'potential') {
      predictionTitle.value = '高潜人才识别'
      prediction.value = await predictHighPotential(employeeId.value)
    } else {
      predictionTitle.value = '岗位匹配推荐'
      recommendations.value = await recommendPositions(employeeId.value, topN.value)
    }
    predictionVisible.value = true
  } finally {
    predicting.value = false
  }
}

function statusType(status?: string | null): 'success' | 'danger' | 'warning' | 'info' {
  if (status === '已完成') return 'success'
  if (status === '失败') return 'danger'
  if (status === '训练中') return 'warning'
  return 'info'
}

onMounted(() => {
  loadEffect()
  loadLogs()
  loadArtifacts()
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
      <template #header>
        <div class="search-bar">
          <span>模型训练与部署</span>
          <el-button type="primary" :loading="training" @click="runTraining()">
            训练全部模型
          </el-button>
          <el-button :loading="training" @click="runTraining('attrition-risk')">
            训练流失模型
          </el-button>
          <el-button :loading="training" @click="runTraining('high-potential')">
            训练高潜模型
          </el-button>
          <el-button :loading="training" @click="runTraining('position-fit')">
            训练岗位匹配
          </el-button>
        </div>
      </template>

      <el-empty
        v-if="artifacts.length === 0"
        description="尚未训练模型，请先点击“训练全部模型”"
        :image-size="60"
      />
      <el-table v-else :data="artifacts" stripe border>
        <el-table-column prop="modelName" label="模型" width="180" />
        <el-table-column prop="modelType" label="类型" width="90" />
        <el-table-column prop="version" label="部署版本" width="90" />
        <el-table-column prop="sampleCount" label="样本量" width="80" />
        <el-table-column prop="positiveCount" label="正样本" width="80" />
        <el-table-column label="AUC" width="70">
          <template #default="{ row }">{{ row.metrics.auc ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="Precision" width="85">
          <template #default="{ row }">{{ row.metrics.precision ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="Recall" width="75">
          <template #default="{ row }">{{ row.metrics.recall ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="F1" width="70">
          <template #default="{ row }">{{ row.metrics.f1 ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="datasetVersion" label="数据版本" width="110" />
        <el-table-column prop="trainTime" label="训练时间" min-width="135" />
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>模型在线推理</template>
      <div class="search-bar">
        <span>员工 ID、工号或姓名</span>
        <el-input
          v-model="employeeId"
          placeholder="如 1、E70014679、龙秀英"
          clearable
          style="width: 220px"
        />
        <el-button type="danger" plain :loading="predicting" @click="runPrediction('attrition')">
          流失风险预测
        </el-button>
        <el-button type="success" plain :loading="predicting" @click="runPrediction('potential')">
          高潜人才识别
        </el-button>
        <el-input-number v-model="topN" :min="1" :max="20" style="width: 110px" />
        <el-button type="primary" plain :loading="predicting" @click="runPrediction('position')">
          推荐匹配岗位
        </el-button>
      </div>
    </el-card>

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

    <el-dialog v-model="predictionVisible" :title="predictionTitle" width="620px">
      <template v-if="prediction">
        <el-result
          :icon="prediction.resultLabel.includes('高潜') || prediction.resultLabel === '低风险' ? 'success' : 'warning'"
          :title="prediction.resultLabel"
          :sub-title="`${prediction.employeeName}（员工ID ${prediction.employeeId}）`"
        >
          <template #extra>
            <div class="prediction-score">{{ prediction.scorePercent }} 分</div>
            <div class="stat-label">
              {{ prediction.modelName }} {{ prediction.modelVersion }}
            </div>
          </template>
        </el-result>
        <el-alert type="info" :closable="false" :title="prediction.explanation" />
        <el-table :data="contributionRows" size="small" border style="margin-top: 14px">
          <el-table-column prop="name" label="主要特征" />
          <el-table-column label="贡献方向" width="140">
            <template #default="{ row }">
              <el-tag :type="row.value >= 0 ? 'danger' : 'success'" effect="plain">
                {{ row.value >= 0 ? '提高概率' : '降低概率' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="value" label="标准化贡献" width="130" />
        </el-table>
      </template>

      <template v-else-if="recommendations">
        <p class="page-tip" style="margin-top: 0">
          {{ recommendations.employeeName }} 的岗位匹配结果，模型版本
          {{ recommendations.modelVersion }}
        </p>
        <el-table :data="recommendations.recommendations" stripe border>
          <el-table-column prop="positionName" label="推荐岗位" min-width="160" />
          <el-table-column prop="department" label="部门" width="130" />
          <el-table-column prop="score" label="匹配分" width="100">
            <template #default="{ row }">
              <el-tag type="success">{{ row.score }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
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

.prediction-score {
  color: #2f5eea;
  font-size: 28px;
  font-weight: 700;
}
</style>
