<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Download, MagicStick, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  FLOW_FORECAST_EXPORT_URL,
  FLOW_FORECAST_IMPORT_URL,
  fetchFlowTrend,
  fetchLatestFlowForecast,
  generateFlowForecast,
} from '@/api/analysis'
import type { FlowForecastData, FlowForecastSeries, FlowTrendData } from '@/api/types'
import MiniChart from '@/components/MiniChart.vue'

const loading = ref(false)
const data = ref<FlowTrendData | null>(null)
const forecast = ref<FlowForecastData | null>(null)
const forecastLoading = ref(false)
const forecastImporting = ref(false)
const horizonMonths = ref(6)

async function load() {
  loading.value = true
  try {
    data.value = await fetchFlowTrend()
  } finally {
    loading.value = false
  }
}

async function loadForecast() {
  forecast.value = await fetchLatestFlowForecast()
}

async function generateForecast() {
  forecastLoading.value = true
  try {
    forecast.value = await generateFlowForecast(horizonMonths.value)
    ElMessage.success(`已生成未来 ${horizonMonths.value} 个月预测`)
  } finally {
    forecastLoading.value = false
  }
}

function exportForecast() {
  if (!forecast.value) {
    ElMessage.warning('请先生成预测')
    return
  }
  window.open(FLOW_FORECAST_EXPORT_URL, '_blank')
}

interface ForecastUploadResponse {
  code: number
  message: string
  data: FlowForecastData
}

function onForecastImportSuccess(response: ForecastUploadResponse) {
  forecastImporting.value = false
  if (response.code === 200) {
    forecast.value = response.data
    ElMessage.success('预测数据导入成功')
  } else {
    ElMessage.error(response.message || '预测数据导入失败')
  }
}

function onForecastImportError() {
  forecastImporting.value = false
  ElMessage.error('预测文件上传失败')
}

function forecastLabels(series: FlowForecastSeries) {
  return series.points.map((point) => point.month)
}

function forecastValues(series: FlowForecastSeries) {
  return series.points.map((point) => point.predictedValue)
}

onMounted(async () => {
  await Promise.all([load(), loadForecast()])
})

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

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="forecast-header">
          <span>智能流动趋势预测</span>
          <div class="forecast-actions">
            <el-select v-model="horizonMonths" style="width: 120px">
              <el-option label="未来 3 个月" :value="3" />
              <el-option label="未来 6 个月" :value="6" />
              <el-option label="未来 12 个月" :value="12" />
            </el-select>
            <el-button
              type="primary"
              :icon="MagicStick"
              :loading="forecastLoading"
              @click="generateForecast"
            >
              生成预测
            </el-button>
            <el-button :icon="Download" @click="exportForecast">导出预测</el-button>
            <el-upload
              :action="FLOW_FORECAST_IMPORT_URL"
              name="file"
              accept=".csv"
              :show-file-list="false"
              :on-progress="() => (forecastImporting = true)"
              :on-success="onForecastImportSuccess"
              :on-error="onForecastImportError"
            >
              <el-button :icon="Upload" :loading="forecastImporting">导入预测</el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <el-empty
        v-if="!forecast"
        description="尚未生成预测，请选择预测周期后点击“生成预测”"
        :image-size="64"
      />
      <template v-else>
        <div class="forecast-meta">
          <span>预测批次：{{ forecast.runCode }}</span>
          <span>生成时间：{{ forecast.generatedAt }}</span>
          <span>模型：{{ forecast.modelVersion }}</span>
        </div>
        <el-row :gutter="14" style="margin-top: 12px">
          <el-col v-for="series in forecast.series" :key="series.type" :span="8">
            <div class="forecast-series">
              <div class="forecast-series-title">
                <strong>{{ series.name }}</strong>
                <el-tag
                  size="small"
                  :type="series.confidence === '低' ? 'warning' : 'success'"
                  effect="plain"
                >
                  {{ series.confidence }}置信度
                </el-tag>
              </div>
              <div class="forecast-total">{{ series.totalPredicted }}</div>
              <div class="stat-label">
                {{ forecast.horizonMonths }} 个月预计合计
              </div>
              <MiniChart
                :labels="forecastLabels(series)"
                :values="forecastValues(series)"
                type="line"
                :color="series.type === 'inflow' ? '#409eff' : series.type === 'outflow' ? '#f56c6c' : '#67c23a'"
              />
              <div class="stat-label">方法：{{ series.method }}</div>
            </div>
          </el-col>
        </el-row>
        <el-alert
          type="info"
          :closable="false"
          style="margin-top: 14px"
          title="预测用于战略情景规划；低置信度指标受历史事件不足影响，不能用于个人决策。"
        />
        <ul class="forecast-notes">
          <li v-for="note in forecast.notes" :key="note">{{ note }}</li>
        </ul>
      </template>
    </el-card>

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

<style scoped>
.forecast-header,
.forecast-actions,
.forecast-meta,
.forecast-series-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.forecast-header {
  justify-content: space-between;
}

.forecast-actions {
  flex-wrap: wrap;
}

.forecast-meta {
  flex-wrap: wrap;
  color: #7b8496;
  font-size: 12px;
}

.forecast-series {
  height: 100%;
  padding: 14px;
  border: 1px solid #e7ecf5;
  border-radius: 10px;
  background: #fbfcff;
}

.forecast-series-title {
  justify-content: space-between;
}

.forecast-total {
  margin-top: 10px;
  color: #21304d;
  font-size: 26px;
  font-weight: 700;
}

.forecast-notes {
  margin: 12px 0 0;
  padding-left: 20px;
  color: #667085;
  font-size: 12px;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .forecast-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
