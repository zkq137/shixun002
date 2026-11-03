<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import ModuleAssistant from '@/components/ModuleAssistant.vue'
import { useUrlTab } from '@/composables/useUrlTab'
import { fetchEmployeePage } from '@/api/employee'
import {
  fetchCourses,
  fetchLearningPathDetail,
  fetchLearningPaths,
  fetchTrainingEffects,
  fetchTrainingPlan,
  fetchTrainingRecords,
} from '@/api/training'
import type {
  CourseItem,
  EmployeeRow,
  LearningPath,
  PageResult,
  TrainingEffect,
  TrainingPlan,
} from '@/api/types'

const activeTab = useUrlTab(
  ['recommend', 'records', 'courses', 'paths', 'effects', 'assistant'] as const,
  'recommend',
)
const employeeOptions = ref<EmployeeRow[]>([])
const employeeSearching = ref(false)
const employeeId = ref<number | null>(null)
const planLoading = ref(false)
const plan = ref<TrainingPlan | null>(null)
const recordsLoading = ref(false)
const records = ref<CourseItem[]>([])

const courseLoading = ref(false)
const coursePage = ref<PageResult<CourseItem> | null>(null)
const courseQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '', courseType: '' })

const pathLoading = ref(false)
const paths = ref<LearningPath[]>([])
const selectedPath = ref<LearningPath | null>(null)

const effectLoading = ref(false)
const effects = ref<TrainingEffect[]>([])

const trainingAssistantPrompts = [
  '查看当前员工的培训推荐和技能缺口',
  '市场经理岗位应该优先补哪些课程',
  '为研发中心员工制定分阶段学习路径',
  '分析培训参与率和通过率最高的课程',
]

async function searchEmployees(keyword: string) {
  employeeSearching.value = true
  try {
    const page = await fetchEmployeePage({ keyword, pageNum: 1, pageSize: 20 })
    employeeOptions.value = page.records
  } finally {
    employeeSearching.value = false
  }
}

async function loadPlan() {
  if (employeeId.value === null) {
    plan.value = null
    return
  }
  planLoading.value = true
  try {
    plan.value = await fetchTrainingPlan(employeeId.value)
  } finally {
    planLoading.value = false
  }
}

async function loadRecords() {
  if (employeeId.value === null) {
    records.value = []
    return
  }
  recordsLoading.value = true
  try {
    records.value = await fetchTrainingRecords(employeeId.value)
  } finally {
    recordsLoading.value = false
  }
}

function onEmployeeChange() {
  loadPlan()
  loadRecords()
}

async function loadCourses() {
  courseLoading.value = true
  try {
    coursePage.value = await fetchCourses(courseQuery)
  } finally {
    courseLoading.value = false
  }
}

function searchCourses() {
  courseQuery.pageNum = 1
  loadCourses()
}

async function loadPaths() {
  pathLoading.value = true
  try {
    paths.value = await fetchLearningPaths()
  } finally {
    pathLoading.value = false
  }
}

async function openPath(row: LearningPath) {
  selectedPath.value = await fetchLearningPathDetail(row.id)
}

async function loadEffects() {
  effectLoading.value = true
  try {
    effects.value = await fetchTrainingEffects()
  } finally {
    effectLoading.value = false
  }
}

function rate(value?: number | null) {
  return value === null || value === undefined ? '-' : `${value}%`
}

function rateTone(value?: number | null) {
  if (value === null || value === undefined) return ''
  if (value >= 90) return 'value-positive'
  if (value >= 75) return 'text-info'
  if (value >= 60) return 'value-warning'
  return 'value-negative'
}

function rateColor(value?: number | null) {
  if (value === null || value === undefined) return '#94a3b8'
  if (value >= 90) return '#16a34a'
  if (value >= 75) return '#2563eb'
  if (value >= 60) return '#d97706'
  return '#dc2626'
}

function improvementTone(value?: number | null) {
  if (value === null || value === undefined) return ''
  if (value > 0) return 'value-positive'
  if (value < 0) return 'value-negative'
  return 'value-warning'
}

onMounted(async () => {
  await Promise.allSettled([searchEmployees(''), loadCourses(), loadPaths(), loadEffects()])
  const first = employeeOptions.value[0]
  if (first) {
    employeeId.value = first.id
    await Promise.allSettled([loadPlan(), loadRecords()])
  }
})
</script>

<template>
  <div>
    <h2 id="training-recommendation" class="page-title">智能培训</h2>
    <p class="page-tip">
      推荐逻辑：员工所在岗位的「必备培训」减去他「已完成的培训」，剩下的就是要补的课。
      同时提供学习记录、课程库、路径模板和培训效果分析。
    </p>

    <el-card shadow="never">
      <div class="search-bar">
        <el-select
          v-model="employeeId"
          filterable
          remote
          :remote-method="searchEmployees"
          :loading="employeeSearching"
          placeholder="输入姓名或工号搜员工"
          style="width: 320px"
          @change="onEmployeeChange"
        >
          <el-option
            v-for="item in employeeOptions"
            :key="item.id"
            :label="`${item.name}（${item.empNo}）- ${item.positionName}`"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" :disabled="employeeId === null" @click="onEmployeeChange">刷新员工数据</el-button>
        <span class="page-tip" style="margin: 0">推荐计划与学习记录共用当前员工</span>
      </div>
    </el-card>

    <el-tabs v-model="activeTab" class="module-tabs">
      <el-tab-pane label="推荐计划" name="recommend">
        <el-card v-if="plan" v-loading="planLoading" shadow="never">
          <template #header>{{ plan.employeeName }} · {{ plan.positionName }}</template>
          <el-alert v-if="plan.recommendationReason" type="info" :closable="false" style="margin-bottom: 16px" :title="plan.recommendationReason" />
          <el-row :gutter="24">
            <el-col :span="10">
              <h3 class="sub-title">已完成的培训（{{ (plan.completedCourses ?? []).length }} 门）</h3>
              <el-tag v-for="item in plan.completedCourses ?? []" :key="item.id" type="success" effect="plain" style="margin: 0 8px 8px 0">{{ item.courseName }}</el-tag>
              <el-empty v-if="(plan.completedCourses ?? []).length === 0" description="还没有已完成记录" :image-size="60" />
              <h3 class="sub-title" style="margin-top: 20px">技能缺口（{{ (plan.skillGaps ?? []).length }} 项）</h3>
              <el-tag v-for="skill in plan.skillGaps ?? []" :key="skill" type="warning" effect="plain" style="margin: 0 8px 8px 0">{{ skill }}</el-tag>
              <p v-if="(plan.skillGaps ?? []).length === 0" class="page-tip">岗位要求的技能都已经具备</p>
            </el-col>
            <el-col :span="14">
              <h3 class="sub-title">需要补的培训（{{ (plan.recommendedCourses ?? []).length }} 门）</h3>
              <el-table :data="plan.recommendedCourses ?? []" stripe border max-height="420">
                <el-table-column type="index" label="#" width="60" />
                <el-table-column prop="courseName" label="课程" min-width="180" />
                <el-table-column prop="courseType" label="类型" width="100" />
                <el-table-column prop="difficulty" label="难度" width="90" />
                <el-table-column prop="duration" label="时长(小时)" width="110" />
                <template #empty>岗位必修都学完了，没有缺口</template>
              </el-table>
            </el-col>
          </el-row>
        </el-card>
        <el-card v-else shadow="never"><el-empty description="选择一名员工查看培训推荐" :image-size="96" /></el-card>
      </el-tab-pane>

      <el-tab-pane label="学习记录" name="records">
        <el-card shadow="never">
          <template #header>已完成课程与培训效果</template>
          <el-table v-loading="recordsLoading" :data="records" stripe>
            <el-table-column prop="courseName" label="课程" min-width="220" />
            <el-table-column prop="courseType" label="类型" width="110" />
            <el-table-column prop="difficulty" label="难度" width="90" />
            <el-table-column prop="duration" label="时长" width="90" />
            <el-table-column label="参与率" width="100"><template #default="{ row }"><span class="number-emphasis" :class="rateTone(row.attendRate)">{{ rate(row.attendRate) }}</span></template></el-table-column>
            <el-table-column label="通过率" width="100"><template #default="{ row }"><span class="number-emphasis" :class="rateTone(row.passRate)">{{ rate(row.passRate) }}</span></template></el-table-column>
            <el-table-column label="能力提升" width="100"><template #default="{ row }"><span class="number-emphasis" :class="improvementTone(row.improveScore)">{{ row.improveScore ?? '-' }}</span></template></el-table-column>
            <el-table-column label="完成时间" min-width="155"><template #default="{ row }">{{ row.completedAt?.replace('T', ' ').slice(0, 16) ?? '-' }}</template></el-table-column>
            <template #empty><el-empty description="当前员工暂无学习记录" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="课程库" name="courses">
        <el-card shadow="never">
          <div class="search-bar">
            <el-input v-model="courseQuery.keyword" clearable placeholder="课程名称" style="width: 240px" @keyup.enter="searchCourses">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-input v-model="courseQuery.courseType" clearable placeholder="课程类型" style="width: 160px" @keyup.enter="searchCourses" />
            <el-button type="primary" :icon="Search" @click="searchCourses">查询课程</el-button>
          </div>
        </el-card>
        <el-card shadow="never" style="margin-top: 16px">
          <template #header>课程目录（共 {{ coursePage?.total ?? 0 }} 门）</template>
          <el-table v-loading="courseLoading" :data="coursePage?.records ?? []" stripe>
            <el-table-column prop="courseName" label="课程" min-width="220" />
            <el-table-column prop="courseType" label="类型" width="120" />
            <el-table-column prop="forPosition" label="适用岗位" min-width="170" />
            <el-table-column prop="difficulty" label="难度" width="90" />
            <el-table-column prop="duration" label="时长(小时)" width="110" />
            <template #empty><el-empty description="暂无课程" /></template>
          </el-table>
          <el-pagination v-if="(coursePage?.total ?? 0) > 0" v-model:current-page="courseQuery.pageNum" v-model:page-size="courseQuery.pageSize" :total="coursePage?.total ?? 0" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" style="margin-top: 16px; justify-content: flex-end" @current-change="loadCourses" @size-change="searchCourses" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="学习路径" name="paths">
        <el-card shadow="never">
          <template #header>可复用学习路径模板</template>
          <el-table v-loading="pathLoading" :data="paths" stripe>
            <el-table-column prop="pathName" label="路径名称" min-width="220" />
            <el-table-column prop="targetSkill" label="目标技能" min-width="180" />
            <el-table-column label="更新时间" width="170"><template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0, 16) ?? '-' }}</template></el-table-column>
            <el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="openPath(row)">查看阶段</el-button></template></el-table-column>
            <template #empty><el-empty description="暂无学习路径模板" /></template>
          </el-table>
        </el-card>
        <el-card v-if="selectedPath" shadow="never" style="margin-top: 16px">
          <template #header>{{ selectedPath.pathName }} · 阶段任务</template>
          <el-timeline>
            <el-timeline-item v-for="task in selectedPath.tasks ?? []" :key="task.id" :timestamp="`阶段 ${task.stage}`" placement="top">
              <strong>{{ task.taskName }}</strong>
              <span v-if="task.courseName" class="page-tip" style="margin-left: 10px">{{ task.courseName }}</span>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="培训效果" name="effects">
        <el-card shadow="never">
          <template #header>课程参与率、通过率与能力提升</template>
          <el-table v-loading="effectLoading" :data="effects" stripe>
            <el-table-column prop="courseName" label="课程" min-width="220" />
            <el-table-column prop="participantCount" label="参与人数" width="110" />
            <el-table-column label="平均参与率" min-width="160"><template #default="{ row }"><el-progress :percentage="Math.min(100, Number(row.averageAttendRate ?? 0))" :color="rateColor(row.averageAttendRate)" :show-text="false" style="display: inline-block; width: 90px; margin-right: 8px" /><span class="number-emphasis" :class="rateTone(row.averageAttendRate)">{{ rate(row.averageAttendRate) }}</span></template></el-table-column>
            <el-table-column label="平均通过率" min-width="160"><template #default="{ row }"><el-progress :percentage="Math.min(100, Number(row.averagePassRate ?? 0))" :color="rateColor(row.averagePassRate)" :show-text="false" style="display: inline-block; width: 90px; margin-right: 8px" /><span class="number-emphasis" :class="rateTone(row.averagePassRate)">{{ rate(row.averagePassRate) }}</span></template></el-table-column>
            <el-table-column label="平均能力提升" width="130"><template #default="{ row }"><span class="number-emphasis" :class="improvementTone(row.averageImproveScore)">{{ row.averageImproveScore ?? '-' }}</span></template></el-table-column>
            <template #empty><el-empty description="暂无培训效果数据" /></template>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="推荐助手" name="assistant">
        <ModuleAssistant
          id="assistant-training"
          module-key="training"
          title="智能培训推荐助手"
          :quick-prompts="trainingAssistantPrompts"
          status-endpoint="/training/ai/status"
          chat-endpoint="/training/ai/chat"
          service-name="培训服务"
          config-env-prefix="TRAINING_DIFY"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.sub-title {
  margin: 0 0 12px;
  font-size: 15px;
}
</style>
