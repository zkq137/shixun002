<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ChatDotRound, Promotion, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useModuleAssistant } from '@/stores/moduleAssistant'

interface TextSegment {
  type: 'text'
  content: string
}

interface TrendPoint {
  year: string
  score: string
  grade: string
  [dimension: string]: string
}

interface TrendData {
  employeeName: string
  empNo: string
  points: TrendPoint[]
  dimensions: string[]
  averageScore: string
  maxScore: string
  minScore: string
  yoyChange: string
  trend: string
}

interface SkillItem {
  id: string
  name: string
  category: string
  level: string
  status: string
  description: string
  childCount: number
  employeeCount: number
  positionCount: number
}

interface SkillGroup {
  name: string
  items: SkillItem[]
}

interface JsonSegment {
  type: 'json'
  raw: string
  message: string
  code?: number
  total?: number
  pageNum?: number
  pageSize?: number
  records: Record<string, unknown>[]
  columns: Array<{ prop: string; label: string }>
  fields: Array<{ label: string; value: string }>
  trend?: TrendData
  skillGroups?: SkillGroup[]
  skillCount?: number
}

type AnswerSegment = TextSegment | JsonSegment

const props = withDefaults(
  defineProps<{
    moduleKey: string
    title?: string
    quickPrompts?: string[]
    statusEndpoint?: string
    chatEndpoint?: string
  }>(),
  {
    title: '模块智能助手',
    quickPrompts: () => [],
    statusEndpoint: '/ai/status',
    chatEndpoint: '/ai/chat',
  },
)

const columnLabels: Record<string, string> = {
  id: 'ID',
  empNo: '工号',
  name: '姓名',
  gender: '性别',
  age: '年龄',
  department: '部门',
  positionName: '岗位',
  levelTier: '层级',
  jobRank: '职级',
  status: '状态',
  perfScore: '绩效',
  potentialLevel: '潜力',
  warningLevel: '风险',
  talentTag: '人才标签',
  tenureYears: '司龄',
  workMode: '办公方式',
  categoryName: '分类',
  skillName: '技能',
  employeeId: '员工ID',
  year: '年份',
  score: '得分',
}

const employeeColumns = [
  'empNo',
  'name',
  'gender',
  'age',
  'department',
  'positionName',
  'levelTier',
  'status',
  'perfScore',
  'potentialLevel',
]

const chat = useModuleAssistant(props.moduleKey, {
  status: props.statusEndpoint,
  chat: props.chatEndpoint,
})
const status = computed(() => chat.state.status)
const messages = computed(() => chat.state.messages)
const sending = computed(() => chat.state.sending)
const input = computed({
  get: () => chat.state.input,
  set: (value: string) => {
    chat.state.input = value
  },
})
const listRef = ref<HTMLElement | null>(null)

const quickPrompts = computed(() => props.quickPrompts)

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function unwrapData(value: unknown): unknown {
  if (isRecord(value) && 'data' in value) return value.data
  return value
}

function findRecordArray(value: unknown, depth = 0): Record<string, unknown>[] | null {
  if (Array.isArray(value)) {
    const records = value.filter(isRecord)
    return records.length ? records : null
  }
  if (!isRecord(value) || depth > 2) return null

  // 只识别列表字段，避免把趋势维度等业务对象误判成记录列表。
  for (const key of ['records', 'list', 'items', 'rows']) {
    const found = findRecordArray(value[key], depth + 1)
    if (found) return found
  }
  return null
}

function numberValue(value: unknown) {
  return typeof value === 'number' ? value : undefined
}

function formatFieldValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  if (Array.isArray(value)) {
    return value.length ? value.map((item) => formatFieldValue(item)).join(' / ') : '-'
  }
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function numericValue(value: unknown) {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function toSkillItem(record: Record<string, unknown>): SkillItem {
  return {
    id: formatFieldValue(record.id),
    name: formatFieldValue(record.skillName),
    category: formatFieldValue(record.skillCategory ?? record.categoryName),
    level: formatFieldValue(record.level),
    status: formatFieldValue(record.status),
    description: formatFieldValue(record.description),
    childCount: numericValue(record.childCount),
    employeeCount: numericValue(record.employeeCount),
    positionCount: numericValue(record.positionCount),
  }
}

function buildSkillGroups(value: unknown): SkillGroup[] | null {
  if (!Array.isArray(value)) return null
  const records = value.filter(
    (item): item is Record<string, unknown> =>
      isRecord(item) && typeof item.skillName === 'string',
  )
  if (!records.length) return null

  const treeGroups = records
    .filter((record) => Array.isArray(record.children))
    .map((record) => ({
      name: formatFieldValue(record.skillName),
      items: (record.children as unknown[])
        .filter(isRecord)
        .map(toSkillItem),
    }))
    .filter((group) => group.items.length)
  if (treeGroups.length) return treeGroups

  const isCategoryList = records.every((record) => numericValue(record.level) === 1)
  if (isCategoryList) {
    return [{ name: '技能分类', items: records.map(toSkillItem) }]
  }

  const groups = new Map<string, SkillItem[]>()
  for (const record of records) {
    const item = toSkillItem(record)
    const groupName = item.category === '-' ? '全部技能' : item.category
    const group = groups.get(groupName) ?? []
    group.push(item)
    groups.set(groupName, group)
  }
  return Array.from(groups, ([name, items]) => ({ name, items }))
}

function buildTrendData(payload: Record<string, unknown>): TrendData | null {
  const years = payload.years
  const scores = payload.scores
  if (!Array.isArray(years) || !Array.isArray(scores)) return null

  const grades = Array.isArray(payload.grades) ? payload.grades : []
  const dimensionSeries = Array.isArray(payload.dimensionSeries)
    ? payload.dimensionSeries.filter(isRecord)
    : []
  const dimensions = dimensionSeries
    .map((series) => String(series.name ?? ''))
    .filter(Boolean)

  const points: TrendPoint[] = years.map((year, index) => {
    const point: TrendPoint = {
      year: String(year ?? '-'),
      score: formatFieldValue(scores[index]),
      grade: formatFieldValue(grades[index]),
    }
    dimensionSeries.forEach((series, seriesIndex) => {
      const name = String(series.name ?? `维度${seriesIndex + 1}`)
      const values = Array.isArray(series.data) ? series.data : []
      point[name] = formatFieldValue(values[index])
    })
    return point
  })

  return {
    employeeName: formatFieldValue(payload.name ?? payload.employeeName),
    empNo: formatFieldValue(payload.empNo),
    points,
    dimensions,
    averageScore: formatFieldValue(payload.averageScore),
    maxScore: formatFieldValue(payload.maxScore),
    minScore: formatFieldValue(payload.minScore),
    yoyChange: formatFieldValue(payload.yoyChange),
    trend: formatFieldValue(payload.trend),
  }
}

function buildJsonSegment(raw: string): JsonSegment | null {
  try {
    const data: unknown = JSON.parse(raw)
    const body = unwrapData(data)
    const wrapper = isRecord(data) ? data : null
    const payload = isRecord(body) ? body : null
    const trend = payload ? buildTrendData(payload) : null
    const skillGroups = trend ? null : buildSkillGroups(body)
    const records = trend || skillGroups ? [] : (findRecordArray(body) ?? [])
    const keys = Array.from(new Set(records.flatMap((record) => Object.keys(record))))
    const preferredKeys = employeeColumns.filter((key) => keys.includes(key))
    const otherKeys = keys.filter((key) => !preferredKeys.includes(key))
    const selectedKeys = preferredKeys.length
      ? preferredKeys
      : otherKeys.slice(0, Math.min(otherKeys.length, 8))

    const rawFields = payload
      ? Object.entries(payload).filter(
          ([key, value]) =>
            !['records', 'list', 'items', 'rows', 'total', 'pageNum', 'pageSize'].includes(key) &&
            value !== null &&
            value !== undefined,
        )
      : []

    return {
      type: 'json',
      raw,
      message: String(wrapper?.message ?? payload?.message ?? '操作成功'),
      code: numberValue(wrapper?.code ?? payload?.code),
      total: numberValue(payload?.total ?? wrapper?.total),
      pageNum: numberValue(payload?.pageNum ?? wrapper?.pageNum),
      pageSize: numberValue(payload?.pageSize ?? wrapper?.pageSize),
      records,
      columns: selectedKeys.map((prop) => ({ prop, label: columnLabels[prop] || prop })),
      fields: rawFields.map(([key, value]) => ({
        label: columnLabels[key] || key,
        value: formatFieldValue(value),
      })),
      trend: trend ?? undefined,
      skillGroups: skillGroups ?? undefined,
      skillCount: skillGroups
        ? skillGroups.reduce((total, group) => total + group.items.length, 0)
        : undefined,
    }
  } catch {
    return null
  }
}

function answerSegments(content: string): AnswerSegment[] {
  const segments: AnswerSegment[] = []
  const fencePattern = /```(?:json)?\s*([\s\S]*?)```/gi
  let cursor = 0

  for (const match of content.matchAll(fencePattern)) {
    const start = match.index ?? 0
    const text = content.slice(cursor, start).trim()
    if (text) segments.push({ type: 'text', content: text })

    const raw = match[1].trim()
    const structured = buildJsonSegment(raw)
    segments.push(structured ?? { type: 'text', content: raw })
    cursor = start + match[0].length
  }

  const tail = content.slice(cursor).trim()
  if (tail) {
    const structured = buildJsonSegment(tail)
    segments.push(structured ?? { type: 'text', content: tail })
  }

  return segments.length ? segments : [{ type: 'text', content }]
}

function tagType(value: unknown) {
  const text = String(value ?? '')
  if (text.includes('在职') || text.includes('低风险') || text.includes('成功')) return 'success'
  if (text.includes('上升') || text.includes('提升') || text.includes('增长')) return 'success'
  if (text.includes('高潜') || text.includes('优秀') || text.includes('高管')) return 'primary'
  if (text.includes('离职') || text.includes('高风险') || text.includes('失败')) return 'danger'
  if (text.includes('下降') || text.includes('降低')) return 'danger'
  if (text.includes('中风险') || text.includes('待')) return 'warning'
  return 'info'
}

function scorePercent(value: string) {
  const score = Number.parseFloat(value)
  if (!Number.isFinite(score)) return '0%'
  return `${Math.max(0, Math.min(100, score * 20))}%`
}

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) {
    listRef.value.scrollTop = listRef.value.scrollHeight
  }
}

async function send() {
  const result = await chat.send()
  if (result === 'unconfigured') {
    ElMessage.warning('AI 助手还没配置 Dify，请先设置 DIFY_BASE_URL 和 DIFY_API_KEY')
  }
}

function usePrompt(prompt: string) {
  input.value = prompt
  send()
}

function newChat() {
  chat.newChat()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
  }
}

watch(
  () => [messages.value.length, sending.value],
  async () => {
    await scrollToBottom()
  },
  { flush: 'post' },
)

onMounted(async () => {
  await chat.loadStatus()
  await scrollToBottom()
})
</script>

<template>
  <div :id="`assistant-${moduleKey}`" class="module-assistant">
    <el-alert
      v-if="status && !status.configured"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
      title="AI 助手尚未连接 Dify"
    >
      <div style="line-height: 1.8">
        在员工服务启动前设置环境变量：
        <code>DIFY_BASE_URL=http://你的Dify地址</code> 和
        <code>DIFY_API_KEY=app-xxxx</code>，然后重启员工服务。
      </div>
    </el-alert>

    <el-card shadow="never" class="chat-card">
      <template #header>
        <div class="chat-header">
          <div class="chat-title">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ title }}</span>
            <el-tag v-if="status?.configured" type="success" size="small" effect="plain">
              已连接 Dify
            </el-tag>
            <el-tag v-else type="info" size="small" effect="plain">未配置</el-tag>
            <el-tag v-if="sending" type="warning" size="small" effect="plain">
              后台处理中
            </el-tag>
          </div>
          <el-button :icon="RefreshLeft" size="small" :disabled="sending" @click="newChat">
            新对话
          </el-button>
        </div>
      </template>

      <div ref="listRef" class="chat-list">
        <div v-if="messages.length === 0" class="empty-chat">
          <el-empty description="问点什么吧，例如：" :image-size="80" />
          <div class="prompt-list">
            <el-button
              v-for="prompt in quickPrompts"
              :key="prompt"
              round
              size="small"
              @click="usePrompt(prompt)"
            >
              {{ prompt }}
            </el-button>
          </div>
        </div>

        <div
          v-for="(message, index) in messages"
          :key="index"
          class="message-row"
          :class="message.role"
        >
          <div v-if="message.role !== 'system'" class="avatar">
            {{ message.role === 'user' ? '我' : 'AI' }}
          </div>
          <div class="bubble">
            <template v-if="message.role === 'assistant'">
              <template v-for="(segment, segmentIndex) in answerSegments(message.content)" :key="segmentIndex">
                <div v-if="segment.type === 'text'" class="bubble-text">{{ segment.content }}</div>
                <div v-else class="result-card">
                  <div class="result-header">
                    <div>
                      <span class="result-title">接口执行结果</span>
                      <el-tag :type="tagType(segment.message)" size="small" effect="light">
                        {{ segment.message }}
                      </el-tag>
                    </div>
                    <span
                      v-if="segment.total !== undefined || segment.skillCount !== undefined"
                      class="result-count"
                    >
                      共 {{ segment.total ?? segment.skillCount }} 项
                    </span>
                  </div>

                  <div
                    v-if="segment.pageNum !== undefined || segment.pageSize !== undefined"
                    class="result-pagination"
                  >
                    第 {{ segment.pageNum ?? 1 }} 页
                    <span v-if="segment.pageSize">，每页 {{ segment.pageSize }} 条</span>
                  </div>

                  <div v-if="segment.trend" class="trend-panel">
                    <div class="trend-summary">
                      <div class="trend-person">
                        <span class="trend-label">员工</span>
                        <strong>{{ segment.trend.employeeName }}</strong>
                        <span class="trend-number">{{ segment.trend.empNo }}</span>
                        <el-tag
                          :type="tagType(segment.trend.trend)"
                          size="small"
                          effect="light"
                        >
                          {{ segment.trend.trend }}
                        </el-tag>
                      </div>
                      <div class="trend-metrics">
                        <span>平均分 <b>{{ segment.trend.averageScore }}</b></span>
                        <span>最高分 <b>{{ segment.trend.maxScore }}</b></span>
                        <span>最低分 <b>{{ segment.trend.minScore }}</b></span>
                        <span>
                          同比
                          <b :class="{ negative: segment.trend.yoyChange.startsWith('-') }">
                            {{ segment.trend.yoyChange }}
                          </b>
                        </span>
                      </div>
                    </div>

                    <div class="trend-bars">
                      <div
                        v-for="point in segment.trend.points"
                        :key="point.year"
                        class="trend-bar-row"
                      >
                        <span class="trend-year">{{ point.year }}</span>
                        <div class="trend-track">
                          <i :style="{ width: scorePercent(point.score) }"></i>
                        </div>
                        <b>{{ point.score }}</b>
                        <el-tag size="small" effect="plain">{{ point.grade }}</el-tag>
                      </div>
                    </div>

                    <el-table :data="segment.trend.points" size="small" border stripe class="result-table">
                      <el-table-column prop="year" label="年份" width="90" />
                      <el-table-column prop="score" label="综合绩效" width="100" />
                      <el-table-column prop="grade" label="等级" width="80" />
                      <el-table-column
                        v-for="dimension in segment.trend.dimensions"
                        :key="dimension"
                        :prop="dimension"
                        :label="dimension"
                        min-width="100"
                      />
                    </el-table>
                  </div>

                  <div v-else-if="segment.skillGroups" class="skill-panel">
                    <section
                      v-for="group in segment.skillGroups"
                      :key="group.name"
                      class="skill-group"
                    >
                      <div class="skill-group-title">
                        <span>{{ group.name }}</span>
                        <small>{{ group.items.length }} 项</small>
                      </div>
                      <div class="skill-grid">
                        <article v-for="item in group.items" :key="item.id" class="skill-item">
                          <div class="skill-item-head">
                            <strong>{{ item.name }}</strong>
                            <el-tag
                              v-if="item.status !== '-'"
                              :type="tagType(item.status)"
                              size="small"
                              effect="plain"
                            >
                              {{ item.status }}
                            </el-tag>
                          </div>
                          <p v-if="item.description !== '-'">{{ item.description }}</p>
                          <div class="skill-item-meta">
                            <span>ID {{ item.id }}</span>
                            <span v-if="item.childCount">含 {{ item.childCount }} 个子技能</span>
                            <span v-if="item.employeeCount">员工 {{ item.employeeCount }} 人</span>
                            <span v-if="item.positionCount">岗位 {{ item.positionCount }} 个</span>
                          </div>
                        </article>
                      </div>
                    </section>
                  </div>

                  <el-table
                    v-else-if="segment.records.length"
                    :data="segment.records"
                    size="small"
                    stripe
                    border
                    class="result-table"
                  >
                    <el-table-column
                      v-for="column in segment.columns"
                      :key="column.prop"
                      :prop="column.prop"
                      :label="column.label"
                      min-width="90"
                    >
                      <template #default="{ row }">
                        <el-tag
                          v-if="['status', 'levelTier', 'potentialLevel', 'warningLevel'].includes(column.prop)"
                          :type="tagType(row[column.prop])"
                          size="small"
                          effect="plain"
                        >
                          {{ formatFieldValue(row[column.prop]) }}
                        </el-tag>
                        <span v-else>{{ formatFieldValue(row[column.prop]) }}</span>
                      </template>
                    </el-table-column>
                  </el-table>

                  <el-descriptions
                    v-else-if="segment.fields.length"
                    :column="2"
                    size="small"
                    border
                    class="result-details"
                  >
                    <el-descriptions-item
                      v-for="field in segment.fields"
                      :key="field.label"
                      :label="field.label"
                    >
                      {{ field.value }}
                    </el-descriptions-item>
                  </el-descriptions>

                  <div v-else class="result-empty">
                    <el-empty :image-size="56" description="没有找到同时满足这些筛选条件的记录" />
                    <p>可以尝试减少筛选条件，例如先查询“高风险”或“核心骨干”。</p>
                  </div>

                  <el-collapse class="raw-json">
                    <el-collapse-item title="查看原始 JSON" name="raw">
                      <pre>{{ segment.raw }}</pre>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </template>
            </template>
            <div v-else class="bubble-text">{{ message.content }}</div>
            <div class="bubble-time">{{ message.time }}</div>
          </div>
        </div>

        <div v-if="sending" class="message-row assistant">
          <div class="avatar">AI</div>
          <div class="bubble typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <div v-if="sending" class="background-tip">
        AI 正在后台处理。你可以切换到其他页面，处理完成后对话记录会自动保留。
      </div>

      <div class="composer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="2000"
          show-word-limit
          placeholder="例如：查看龙秀英的绩效趋势；或按 Enter 发送，Shift+Enter 换行"
          @keydown="onKeydown"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          :loading="sending"
          :disabled="!input.trim()"
          @click="send"
        >
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.chat-card {
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.chat-list {
  height: min(520px, 60vh);
  min-height: 320px;
  overflow-y: auto;
  padding: 4px 4px 12px;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 24px;
}

.prompt-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 640px;
}

.message-row {
  display: flex;
  gap: 10px;
  margin: 14px 0;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-row.assistant .bubble {
  flex: 1;
  max-width: calc(100% - 44px);
}

.message-row.user .bubble {
  max-width: 72%;
}

.message-row.system {
  justify-content: center;
}

.avatar {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 9px;
  background: #eef3ff;
  color: #3b6ef6;
  font-size: 12px;
  font-weight: 600;
  line-height: 30px;
  text-align: center;
}

.message-row.user .avatar {
  background: #3b6ef6;
  color: #fff;
}

.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f8fafc;
  color: #1f2a44;
}

.message-row.user .bubble {
  background: #3b6ef6;
  color: #fff;
}

.message-row.system .bubble {
  max-width: 80%;
  background: #fef0f0;
  color: #b42318;
  font-size: 12.5px;
}

.bubble-text {
  font-size: 13.5px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-text + .bubble-text,
.bubble-text + .result-card,
.result-card + .bubble-text,
.result-card + .result-card {
  margin-top: 12px;
}

.result-card {
  overflow: hidden;
  border: 1px solid #e3e9f4;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 22px rgb(31 42 68 / 6%);
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f8;
  background: linear-gradient(135deg, #f8faff 0%, #f2f7ff 100%);
}

.result-header > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-title {
  color: #21304d;
  font-size: 13.5px;
  font-weight: 650;
}

.result-count {
  color: #667085;
  font-size: 12px;
  white-space: nowrap;
}

.result-pagination {
  padding: 8px 14px;
  color: #7b8496;
  font-size: 12px;
}

.trend-panel {
  padding: 0 14px 14px;
}

.trend-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
}

.trend-person,
.trend-metrics {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
}

.trend-person strong {
  color: #1f2a44;
  font-size: 14px;
}

.trend-label {
  color: #98a2b3;
  font-size: 12px;
}

.trend-number {
  color: #7b8496;
  font-size: 12px;
}

.trend-metrics {
  gap: 14px;
  color: #7b8496;
  font-size: 12px;
}

.trend-metrics b {
  color: #21304d;
  font-weight: 650;
}

.trend-metrics b.negative {
  color: #d92d20;
}

.trend-bars {
  display: grid;
  gap: 9px;
  margin: 2px 0 14px;
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
}

.trend-bar-row {
  display: grid;
  grid-template-columns: 48px minmax(120px, 1fr) 44px 42px;
  align-items: center;
  gap: 10px;
  color: #5f6b7c;
  font-size: 12px;
}

.trend-year {
  font-variant-numeric: tabular-nums;
}

.trend-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #e8edf5;
}

.trend-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #5b8cff 0%, #3b6ef6 100%);
}

.trend-bar-row > b {
  color: #21304d;
  font-size: 12.5px;
  text-align: right;
}

.skill-panel {
  display: grid;
  gap: 16px;
  padding: 14px;
}

.skill-group {
  display: grid;
  gap: 10px;
}

.skill-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #344054;
  font-size: 13px;
  font-weight: 650;
}

.skill-group-title::before {
  width: 3px;
  height: 14px;
  border-radius: 999px;
  background: #3b6ef6;
  content: '';
}

.skill-group-title small {
  color: #98a2b3;
  font-size: 11.5px;
  font-weight: 400;
}

.skill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 9px;
}

.skill-item {
  min-width: 0;
  padding: 11px 12px;
  border: 1px solid #e7ecf5;
  border-radius: 10px;
  background: linear-gradient(145deg, #fff 0%, #fbfcff 100%);
}

.skill-item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.skill-item-head strong {
  overflow: hidden;
  color: #21304d;
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.skill-item p {
  display: -webkit-box;
  overflow: hidden;
  margin: 6px 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.skill-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  margin-top: 8px;
  color: #98a2b3;
  font-size: 10.5px;
}

.result-table {
  width: 100%;
}

.result-table :deep(.el-table__header th) {
  background: #f7f9fc;
  color: #5f6b7c;
  font-weight: 600;
}

.result-table :deep(.el-table__cell) {
  padding: 8px 0;
  font-size: 12.5px;
}

.result-details {
  padding: 12px 14px;
}

.result-details :deep(.el-descriptions__label) {
  width: 110px;
  color: #697386;
  font-weight: 500;
}

.result-empty {
  padding: 16px 0 18px;
  text-align: center;
}

.result-empty :deep(.el-empty__description) {
  color: #7b8496;
  font-size: 12.5px;
}

.result-empty p {
  margin: -4px 0 0;
  color: #98a2b3;
  font-size: 12px;
}

.raw-json {
  border-top: 1px solid #eef2f8;
}

.raw-json :deep(.el-collapse-item__header) {
  height: 38px;
  padding: 0 14px;
  color: #7b8496;
  font-size: 12px;
}

.raw-json :deep(.el-collapse-item__content) {
  padding: 0 14px 12px;
}

.raw-json pre {
  max-height: 320px;
  margin: 0;
  overflow: auto;
  padding: 12px;
  border-radius: 8px;
  background: #111827;
  color: #dbeafe;
  font: 12px/1.65 Consolas, 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-time {
  margin-top: 4px;
  font-size: 10.5px;
  opacity: 0.55;
  text-align: right;
}

.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 14px 16px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9aa4b2;
  animation: pulse 1.2s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes pulse {
  0%,
  80%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.composer {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding-top: 12px;
  border-top: 1px solid var(--app-line);
}

.background-tip {
  margin-bottom: 10px;
  padding: 8px 12px;
  border: 1px solid #ffe1a8;
  border-radius: 8px;
  background: #fff9eb;
  color: #8a5a00;
  font-size: 12px;
}

.composer .el-textarea {
  flex: 1;
}

@media (max-width: 900px) {
  .chat-list {
    height: min(480px, 62vh);
  }

  .message-row.assistant .bubble {
    max-width: calc(100% - 40px);
  }

  .result-header {
    align-items: flex-start;
  }

  .trend-summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .result-details {
    padding: 8px;
  }
}
</style>
