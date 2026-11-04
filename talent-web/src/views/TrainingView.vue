<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Download, Edit, Folder, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import TrainingAlgorithmPanel from '@/components/TrainingAlgorithmPanel.vue'
import TrainingReportPanel from '@/components/TrainingReportPanel.vue'
import ModuleAssistant from '@/components/ModuleAssistant.vue'
import { useUrlTab } from '@/composables/useUrlTab'
import { fetchEmployeePage } from '@/api/employee'
import {
  createCourse,
  createCourseCategory,
  createLearningPath,
  createTrainingRecord,
  courseExportUrl,
  deleteCourse,
  deleteCourseCategory,
  deleteLearningPath,
  fetchCourseCategories,
  fetchCourses,
  fetchLearningPaths,
  fetchTrainingEffects,
  fetchTrainingPlan,
  importCourses,
  importLearningPaths,
  updateCourse,
  updateCourseCategory,
  updateLearningPath,
} from '@/api/training'
import type {
  CourseCategory,
  CourseForm,
  CourseItem,
  EmployeeRow,
  LearningPathForm,
  LearningPathTemplate,
  TrainingEffect,
  TrainingPlan,
  TrainingRecordForm,
} from '@/api/types'

const activeTab = useUrlTab(['recommend', 'records', 'courses', 'paths', 'effects', 'algorithm', 'assistant'] as const, 'recommend')
const loading = ref(false)
const plan = ref<TrainingPlan | null>(null)
const employees = ref<EmployeeRow[]>([])
const employeeId = ref<number | null>(null)
const developmentGoal = ref('')
const reportPanel = ref<InstanceType<typeof TrainingReportPanel>>()

async function searchEmployees(keyword: string) {
  const page = await fetchEmployeePage({ keyword, pageNum: 1, pageSize: 20 })
  employees.value = page.records
}

let planRequest = 0
async function loadPlan(selectedEmployeeId: number) {
  const request = ++planRequest
  loading.value = true
  plan.value = null
  try {
    const result = await fetchTrainingPlan(selectedEmployeeId, developmentGoal.value || undefined)
    if (request === planRequest) plan.value = result
  } finally {
    if (request === planRequest) loading.value = false
  }
}

function handleEmployeeChange(id: number | null) {
  if (id !== null) loadPlan(id)
  else { planRequest++; plan.value = null; loading.value = false }
}

function exportCourseFile() {
  window.open(courseExportUrl({
    keyword: courseFilters.keyword || undefined,
    courseType: courseFilters.courseType || undefined,
    categoryId: courseFilters.categoryId,
  }), '_blank')
}

function refreshTraining() {
  return Promise.allSettled([loadCategories(), loadCourses(), loadPaths(), loadEffects(), loadCourseOptions(), reportPanel.value?.refresh(), employeeId.value === null ? Promise.resolve() : loadPlan(employeeId.value)])
}

const categories = ref<CourseCategory[]>([])
const courses = ref<CourseItem[]>([])
const courseLoading = ref(false)
const courseFilters = reactive({ keyword: '', courseType: '', categoryId: undefined as number | undefined })
const coursePage = ref(1)
const coursePageSize = ref(10)
const courseTotal = ref(0)
const courseDialog = ref(false)
const courseSaving = ref(false)
const editingCourseId = ref<number | null>(null)
const courseFormRef = ref<FormInstance>()
const courseForm = reactive<CourseForm>({ courseName: '', courseType: '', categoryId: null, forPosition: '', difficulty: '', duration: 0 })
const courseRules: FormRules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
}
const courseFileInput = ref<HTMLInputElement>()

async function loadCategories() {
  categories.value = await fetchCourseCategories()
}

async function loadCourses() {
  courseLoading.value = true
  try {
    const page = await fetchCourses({
      pageNum: coursePage.value,
      pageSize: coursePageSize.value,
      keyword: courseFilters.keyword || undefined,
      courseType: courseFilters.courseType || undefined,
      categoryId: courseFilters.categoryId,
    })
    courses.value = page.records
    courseTotal.value = page.total
  } finally {
    courseLoading.value = false
  }
}

function applyCourseFilters() {
  coursePage.value = 1
  loadCourses()
}

function openCourseCreate() {
  editingCourseId.value = null
  Object.assign(courseForm, { courseName: '', courseType: '', categoryId: null, forPosition: '', difficulty: '', duration: 0 })
  courseDialog.value = true
}

function openCourseEdit(row: CourseItem) {
  editingCourseId.value = row.id
  Object.assign(courseForm, {
    courseName: row.courseName,
    courseType: row.courseType ?? '',
    categoryId: row.categoryId ?? null,
    forPosition: row.forPosition ?? '',
    difficulty: row.difficulty ?? '',
    duration: row.duration ?? 0,
  })
  courseDialog.value = true
}

async function saveCourse() {
  const valid = await courseFormRef.value?.validate().catch(() => false)
  if (!valid) return
  courseSaving.value = true
  try {
    if (editingCourseId.value === null) {
      await createCourse(courseForm)
      ElMessage.success('课程已新增')
    } else {
      await updateCourse(editingCourseId.value, courseForm)
      ElMessage.success('课程已更新')
    }
    courseDialog.value = false
    await Promise.all([loadCourses(), loadCategories(), loadCourseOptions()])
  } finally {
    courseSaving.value = false
  }
}

async function removeCourse(row: CourseItem) {
  try { await ElMessageBox.confirm(`确定删除课程“${row.courseName}”吗？`, '删除课程', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }) } catch { return }
  await deleteCourse(row.id)
  ElMessage.success('课程已删除')
  await Promise.all([loadCourses(), loadCategories(), loadCourseOptions()])
}

function csvCell(value: unknown) {
  const text = value === null || value === undefined ? '' : String(value)
  return `"${text.replaceAll('"', '""')}"`
}

async function onCourseFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const result = await importCourses(file)
    ElMessage.success(`成功导入 ${result.imported} 门课程`)
    await Promise.all([loadCourses(), loadCategories(), loadCourseOptions()])
  } finally {
    input.value = ''
  }
}

function downloadCourseTemplate() {
  const sample = ['示例课程', '专业培训', '专业技能', '数据分析师', '初级', '8']
  const csv = `\uFEFFcourseName,courseType,categoryName,forPosition,difficulty,duration\r\n${sample.map(csvCell).join(',')}\r\n`
  downloadBlob(csv, 'training-course-template.csv', 'text/csv;charset=utf-8')
}

const categoryDialog = ref(false)
const categorySaving = ref(false)
const editingCategoryId = ref<number | null>(null)
const categoryForm = reactive({ categoryName: '', description: '' })
const categoryFormRef = ref<FormInstance>()
const categoryRules: FormRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

function openCategoryDialog() {
  categoryDialog.value = true
}

function openCategoryForm(category?: CourseCategory) {
  editingCategoryId.value = category?.id ?? null
  Object.assign(categoryForm, { categoryName: category?.categoryName ?? '', description: category?.description ?? '' })
}

async function saveCategory() {
  const valid = await categoryFormRef.value?.validate().catch(() => false)
  if (!valid) return
  categorySaving.value = true
  try {
    if (editingCategoryId.value === null) await createCourseCategory(categoryForm)
    else await updateCourseCategory(editingCategoryId.value, categoryForm)
    ElMessage.success(editingCategoryId.value === null ? '分类已新增' : '分类已更新')
    editingCategoryId.value = null
    Object.assign(categoryForm, { categoryName: '', description: '' })
    await Promise.all([loadCategories(), loadCourses()])
  } finally {
    categorySaving.value = false
  }
}

async function removeCategory(category: CourseCategory) {
  try { await ElMessageBox.confirm(`确定删除分类“${category.categoryName}”吗？`, '删除分类', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }) } catch { return }
  await deleteCourseCategory(category.id)
  ElMessage.success('分类已删除')
  if (courseFilters.categoryId === category.id) courseFilters.categoryId = undefined
  await Promise.all([loadCategories(), loadCourses()])
}

const paths = ref<LearningPathTemplate[]>([])
const pathLoading = ref(false)
const pathDialog = ref(false)
const pathSaving = ref(false)
const editingPathId = ref<number | null>(null)
const pathFormRef = ref<FormInstance>()
const pathForm = reactive<LearningPathForm>({ pathName: '', targetSkill: '', forPosition: '', developmentGoal: '', tasks: [] })
const pathRules: FormRules = {
  pathName: [{ required: true, message: '请输入路径名称', trigger: 'blur' }],
}
const courseOptions = ref<CourseItem[]>([])
const pathFileInput = ref<HTMLInputElement>()

async function loadPaths() {
  pathLoading.value = true
  try {
    paths.value = await fetchLearningPaths()
  } finally {
    pathLoading.value = false
  }
}

async function loadCourseOptions() {
  const all: CourseItem[] = []
  let pageNum = 1
  while (true) {
    const page = await fetchCourses({ pageNum, pageSize: 100 })
    all.push(...page.records)
    if (all.length >= page.total || page.records.length === 0) break
    pageNum++
  }
  courseOptions.value = all
}

function openPathCreate() {
  editingPathId.value = null
  Object.assign(pathForm, { pathName: '', targetSkill: '', forPosition: '', developmentGoal: '', tasks: [{ taskName: '', stage: 1, courseId: null }] })
  pathDialog.value = true
}

function openPathEdit(path: LearningPathTemplate) {
  editingPathId.value = path.id
  Object.assign(pathForm, {
    pathName: path.pathName,
    targetSkill: path.targetSkill ?? '',
    forPosition: path.forPosition ?? '',
    developmentGoal: path.developmentGoal ?? '',
    tasks: (path.tasks ?? []).map((task) => ({ taskName: task.taskName, stage: task.stage, courseId: task.courseId ?? null })),
  })
  if (!pathForm.tasks.length) pathForm.tasks.push({ taskName: '', stage: 1, courseId: null })
  pathDialog.value = true
}

function addPathTask() {
  const nextStage = Math.max(0, ...pathForm.tasks.map((task) => task.stage)) + 1
  pathForm.tasks.push({ taskName: '', stage: nextStage, courseId: null })
}

function removePathTask(index: number) {
  if (pathForm.tasks.length <= 1) {
    ElMessage.warning('路径至少保留一个阶段任务')
    return
  }
  pathForm.tasks.splice(index, 1)
}

async function savePath() {
  const valid = await pathFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (pathForm.tasks.some((task) => !task.taskName.trim() || task.stage < 1)) {
    ElMessage.warning('请填写所有阶段任务，阶段序号须大于 0')
    return
  }
  pathSaving.value = true
  try {
    if (editingPathId.value === null) {
      await createLearningPath(pathForm)
      ElMessage.success('学习路径模板已新增')
    } else {
      await updateLearningPath(editingPathId.value, pathForm)
      ElMessage.success('学习路径模板已更新')
    }
    pathDialog.value = false
    await Promise.all([loadPaths(), employeeId.value === null ? Promise.resolve() : loadPlan(employeeId.value)])
  } finally {
    pathSaving.value = false
  }
}

async function removePath(path: LearningPathTemplate) {
  try { await ElMessageBox.confirm(`确定删除路径模板“${path.pathName}”吗？`, '删除路径模板', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }) } catch { return }
  await deleteLearningPath(path.id)
  ElMessage.success('路径模板已删除')
  await loadPaths()
}

function exportPaths() {
  window.open('/api/training/path-templates/export', '_blank')
}

async function onPathFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    if (file.size > 2 * 1024 * 1024) throw new Error('模板文件不能超过 2 MB')
    const parsed: unknown = JSON.parse((await file.text()).replace(/^\uFEFF/, ''))
    if (!Array.isArray(parsed)) throw new Error('文件内容必须是路径模板数组')
    const templates = parsed as LearningPathForm[]
    if (templates.some((path) => !path.pathName || !Array.isArray(path.tasks))) throw new Error('路径模板字段不完整')
    await importLearningPaths(templates)
    ElMessage.success(`成功导入 ${templates.length} 个路径模板`)
    await loadPaths()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '路径模板导入失败')
  } finally {
    input.value = ''
  }
}

const effects = ref<TrainingEffect[]>([])
const effectLoading = ref(false)
const recordDialog = ref(false)
const recordSaving = ref(false)
const recordFormRef = ref<FormInstance>()
const recordForm = reactive({ employeeId: null as number | null, courseId: null as number | null, attendRate: null as number | null, passRate: null as number | null, improveScore: null as number | null })
const recordRules: FormRules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
}

function openRecordDialog() {
  Object.assign(recordForm, { employeeId: null, courseId: null, attendRate: null, passRate: null, improveScore: null })
  recordDialog.value = true
}

async function saveTrainingRecord() {
  const valid = await recordFormRef.value?.validate().catch(() => false)
  if (!valid || recordForm.employeeId === null || recordForm.courseId === null) return
  recordSaving.value = true
  try {
    await createTrainingRecord(recordForm as TrainingRecordForm)
    ElMessage.success('培训结果已登记')
    recordDialog.value = false
    await Promise.all([loadEffects(), reportPanel.value?.refresh(), employeeId.value === null ? Promise.resolve() : loadPlan(employeeId.value)])
  } finally {
    recordSaving.value = false
  }
}
const simulation = reactive({ learners: 50, attendRate: 85, passRate: 80, improveScore: 8 })
const estimatedAttendees = computed(() => Math.round(simulation.learners * simulation.attendRate / 100))
const estimatedPassed = computed(() => Math.round(estimatedAttendees.value * simulation.passRate / 100))
const totalImprovement = computed(() => Number((estimatedPassed.value * simulation.improveScore).toFixed(1)))
const totalParticipants = computed(() => effects.value.reduce((sum, item) => sum + (item.participantCount || 0), 0))
const weightedAverage = (key: 'averageAttendRate' | 'averagePassRate' | 'averageImproveScore') => {
  const selected = effects.value.filter((row) => row.participantCount > 0 && row[key] !== null && row[key] !== undefined)
  const countKey = { averageAttendRate: 'attendSampleCount', averagePassRate: 'passSampleCount', averageImproveScore: 'improveSampleCount' } as const
  const weight = selected.reduce((sum, row) => sum + (row[countKey[key]] ?? row.participantCount), 0)
  if (!weight) return null
  return selected.reduce((sum, row) => sum + Number(row[key]) * (row[countKey[key]] ?? row.participantCount), 0) / weight
}
const averageAttendRate = computed(() => weightedAverage('averageAttendRate'))
const averagePassRate = computed(() => weightedAverage('averagePassRate'))
const averageImproveScore = computed(() => weightedAverage('averageImproveScore'))

async function loadEffects() {
  effectLoading.value = true
  try {
    effects.value = await fetchTrainingEffects()
  } finally {
    effectLoading.value = false
  }
}

function exportEffects() {
  const rows = [
    ['课程 ID', '课程名称', '参与人数', '平均参与率', '平均通过率', '平均能力提升'],
    ...effects.value.map((row) => [row.courseId, row.courseName, row.participantCount, row.averageAttendRate, row.averagePassRate, row.averageImproveScore]),
  ]
  downloadBlob(`\uFEFF${rows.map((row) => row.map(csvCell).join(',')).join('\r\n')}`, 'training-effects.csv', 'text/csv;charset=utf-8')
}

function downloadBlob(content: string, filename: string, type: string) {
  const url = URL.createObjectURL(new Blob([content], { type }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function formattedDate(value?: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  await Promise.allSettled([searchEmployees(''), loadCategories(), loadCourses(), loadPaths(), loadCourseOptions(), loadEffects()])
  if (employees.value[0]) { employeeId.value = employees.value[0].id; await loadPlan(employeeId.value) }
})
</script>

<template>
  <main class="training-page">
    <header class="training-heading">
      <div>
        <h1 class="page-title">智能培训</h1>
        <p class="page-tip">围绕岗位要求管理课程、设计分阶段学习路径，并追踪培训效果。</p>
      </div>
      <el-button :icon="Refresh" @click="refreshTraining">刷新数据</el-button>
    </header>

    <el-tabs v-model="activeTab" class="training-tabs">
      <el-tab-pane label="学习计划" name="recommend">
        <div class="filter-row"><el-input v-model="developmentGoal" style="max-width:420px" maxlength="255" clearable placeholder="培养目标（可选）：如骨干人才、团队管理" @change="employeeId !== null && loadPlan(employeeId)" /><el-tag v-if="plan?.algorithmName">{{ plan.algorithmName }} · 阈值 {{ plan.matchThreshold }}</el-tag></div>
        <section class="workspace-toolbar">
          <div>
            <h2>员工学习计划</h2>
            <p>根据岗位必修课程、培训记录和技能要求查看当前学习缺口。</p>
          </div>
          <el-select
            v-model="employeeId"
            filterable
            remote
            clearable
            :remote-method="searchEmployees"
            placeholder="搜索员工姓名或工号"
            class="employee-picker"
            @change="handleEmployeeChange"
          >
            <el-option v-for="item in employees" :key="item.id" :label="`${item.name}（${item.empNo}）· ${item.positionName ?? '未分配岗位'}`" :value="item.id" />
          </el-select>
        </section>

        <section v-if="plan" v-loading="loading" class="plan-content">
          <div class="plan-summary">
            <div><span>员工</span><strong>{{ plan.employeeName }}</strong></div>
            <div><span>当前岗位</span><strong>{{ plan.positionName || '未分配' }}</strong></div>
            <div><span>岗位待补课程</span><strong>{{ plan.recommendedCourses?.length ?? 0 }} 门</strong></div>
            <div><span>技能缺口</span><strong>{{ plan.skillGaps?.length ?? 0 }} 项</strong></div>
            <small>生成于 {{ formattedDate(plan.generatedAt) }}</small>
          </div>
          <el-alert :title="plan.recommendationReason || '按当前岗位要求生成学习建议'" type="info" :closable="false" />
          <section class="path-recommendations">
            <div class="section-heading"><h3>匹配的学习路径</h3><span>按技能关键词覆盖率排序</span></div>
            <div v-if="plan.recommendedPaths?.length" class="path-match-list">
              <div v-for="path in plan.recommendedPaths" :key="path.id" class="path-match-row">
                  <div class="path-match-main"><strong>{{ path.pathName }}</strong><span>适配技能：{{ path.targetSkill }}</span><span>{{ path.matchReason }}</span><span v-for="task in path.tasks" :key="task.id">阶段 {{ task.stage }}：{{ task.taskName }}{{ task.courseName ? ' · ' + task.courseName : '' }}</span></div>
                <el-progress :percentage="Math.round(path.matchScore ?? 0)" :stroke-width="8" />
                <span class="path-task-count">{{ path.tasks?.length ?? 0 }} 个阶段任务</span>
              </div>
            </div>
            <el-empty v-else description="暂无路径模板匹配；可在路径模板中补充适配技能" :image-size="56" />
          </section>
          <div class="plan-columns">
            <section class="plan-section">
              <div class="section-heading"><h3>待补课程</h3><span>{{ plan.recommendedCourses?.length ?? 0 }}</span></div>
              <el-table :data="plan.recommendedCourses ?? []" stripe>
                <el-table-column prop="courseName" label="课程" min-width="180" />
                <el-table-column prop="matchScore" label="匹配分" width="90" />
                <el-table-column prop="matchReason" label="推荐理由" min-width="260" />
                <el-table-column prop="courseType" label="类型" width="110" />
                <el-table-column prop="difficulty" label="难度" width="90" />
                <el-table-column prop="duration" label="时长" width="90"><template #default="{ row }">{{ row.duration ?? 0 }} 小时</template></el-table-column>
                <template #empty><el-empty description="岗位必修课程已完成" :image-size="70" /></template>
              </el-table>
            </section>
            <aside class="plan-aside">
              <section class="plan-section">
                <div class="section-heading"><h3>已完成课程</h3><span>{{ plan.completedCourses?.length ?? 0 }}</span></div>
                <div v-if="plan.completedCourses?.length" class="completed-list">
                  <div v-for="item in plan.completedCourses" :key="item.id" class="completed-item">
                    <span>{{ item.courseName }}</span><el-tag type="success" effect="plain">已完成</el-tag>
                  </div>
                </div>
                <el-empty v-else description="暂无完成记录" :image-size="54" />
              </section>
              <section class="plan-section">
                <div class="section-heading"><h3>技能缺口</h3><span>{{ plan.skillGaps?.length ?? 0 }}</span></div>
                <div v-if="plan.skillGaps?.length" class="skill-tags">
                  <el-tag v-for="skill in plan.skillGaps" :key="skill" type="warning" effect="plain">{{ skill }}</el-tag>
                </div>
                <el-empty v-else description="岗位技能要求已满足" :image-size="54" />
              </section>
            </aside>
          </div>
        </section>
        <el-empty v-else description="搜索并选择一名员工，查看岗位培训缺口" :image-size="88" />
      </el-tab-pane>

      <el-tab-pane label="课程库" name="courses">
        <section class="workspace-toolbar">
          <div><h2>课程库</h2><p>维护课程类别、适用岗位、难度和培训时长。</p></div>
          <div class="toolbar-actions">
            <el-button :icon="Folder" @click="openCategoryDialog">管理分类</el-button>
            <el-button :icon="Download" @click="downloadCourseTemplate">下载导入模板</el-button>
            <el-button :icon="Download" @click="exportCourseFile">导出 CSV</el-button>
            <input ref="courseFileInput" class="file-input" type="file" accept=".csv,text/csv" @change="onCourseFileChange">
            <el-button :icon="Upload" @click="courseFileInput?.click()">批量导入</el-button>
            <el-button type="primary" :icon="Plus" @click="openCourseCreate">新增课程</el-button>
          </div>
        </section>
        <section class="data-workspace">
          <div class="filter-row">
            <el-input v-model="courseFilters.keyword" clearable placeholder="按课程名称搜索" class="filter-keyword" @keyup.enter="applyCourseFilters">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="courseFilters.categoryId" clearable placeholder="全部分类" class="filter-select" @change="applyCourseFilters">
              <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
            </el-select>
            <el-input v-model="courseFilters.courseType" clearable placeholder="课程类型" class="filter-select" @keyup.enter="applyCourseFilters" />
            <el-button type="primary" :icon="Search" @click="applyCourseFilters">查询</el-button>
          </div>
          <el-table v-loading="courseLoading" :data="courses" stripe>
            <el-table-column prop="id" label="课程 ID" width="88" />
            <el-table-column prop="courseName" label="课程名称" min-width="190" />
            <el-table-column prop="categoryName" label="分类" width="130"><template #default="{ row }">{{ row.categoryName || '未分类' }}</template></el-table-column>
            <el-table-column prop="courseType" label="课程类型" width="120" />
            <el-table-column prop="forPosition" label="适用岗位" min-width="150" />
            <el-table-column prop="difficulty" label="难度" width="100" />
            <el-table-column prop="duration" label="时长" width="100"><template #default="{ row }">{{ row.duration ?? 0 }} 小时</template></el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" aria-label="编辑课程" @click="openCourseEdit(row)" />
                <el-button link type="danger" @click="removeCourse(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="没有匹配的课程" /></template>
          </el-table>
          <div class="pagination-row">
            <span>共 {{ courseTotal }} 门课程</span>
            <el-pagination v-model:current-page="coursePage" v-model:page-size="coursePageSize" :page-sizes="[10, 20, 50]" :total="courseTotal" layout="sizes, prev, pager, next" @current-change="loadCourses" @size-change="applyCourseFilters" />
          </div>
          <p class="import-note">CSV 列顺序：courseName、courseType、categoryName、forPosition、difficulty、duration。分类按名称自动匹配或创建，首行表头可省略。</p>
        </section>
      </el-tab-pane>

      <el-tab-pane label="路径模板" name="paths">
        <section class="workspace-toolbar">
          <div><h2>学习路径模板</h2><p>把适配技能和课程编排成分阶段任务，可导出为 JSON 复用。</p></div>
          <div class="toolbar-actions">
            <el-button :icon="Upload" @click="pathFileInput?.click()">导入 JSON</el-button>
            <input ref="pathFileInput" class="file-input" type="file" accept=".json,application/json" @change="onPathFileChange">
            <el-button :icon="Download" @click="exportPaths">导出 JSON</el-button>
            <el-button type="primary" :icon="Plus" @click="openPathCreate">新建路径</el-button>
          </div>
        </section>
        <section class="data-workspace">
          <el-table v-loading="pathLoading" :data="paths" stripe>
            <el-table-column prop="id" label="路径 ID" width="90" />
            <el-table-column prop="pathName" label="路径名称" min-width="200" /><el-table-column prop="forPosition" label="适用岗位" min-width="140" /><el-table-column prop="developmentGoal" label="培养目标" min-width="160" />
            <el-table-column prop="targetSkill" label="适配技能" min-width="180"><template #default="{ row }">{{ row.targetSkill || '未指定' }}</template></el-table-column>
            <el-table-column label="阶段任务" width="120"><template #default="{ row }">{{ row.tasks?.length ?? 0 }} 项</template></el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="170"><template #default="{ row }">{{ formattedDate(row.createdAt) }}</template></el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" aria-label="编辑路径模板" @click="openPathEdit(row)" />
                <el-button link type="danger" @click="removePath(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="还没有路径模板，先新建一条" /></template>
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="效果分析" name="effects">
        <section class="workspace-toolbar">
          <div><h2>培训效果</h2><p>历史指标来自已登记记录；模拟区按你填写的假设计算，不作为预测结论。</p></div>
          <div class="toolbar-actions">
            <el-button :icon="Plus" @click="openRecordDialog">登记培训结果</el-button>
            <el-button :icon="Download" @click="exportEffects">导出 CSV</el-button>
          </div>
        </section>
        <section v-loading="effectLoading" class="effects-content">
          <TrainingReportPanel ref="reportPanel" :simulation="simulation" />
          <div class="effect-summary">
            <div><span>培训参与记录</span><strong>{{ totalParticipants }}</strong></div>
            <div><span>加权平均参与率</span><strong>{{ averageAttendRate === null ? '暂无' : `${averageAttendRate.toFixed(1)}%` }}</strong></div>
            <div><span>加权平均通过率</span><strong>{{ averagePassRate === null ? '暂无' : `${averagePassRate.toFixed(1)}%` }}</strong></div>
            <div><span>平均能力提升</span><strong>{{ averageImproveScore === null ? '暂无' : averageImproveScore.toFixed(1) }}</strong></div>
          </div>
          <section class="data-workspace">
            <div class="section-heading"><h3>按课程汇总</h3><span>根据培训记录计算</span></div>
            <el-table :data="effects" stripe>
              <el-table-column prop="courseId" label="课程 ID" width="90" />
              <el-table-column prop="courseName" label="课程名称" min-width="190" />
              <el-table-column prop="participantCount" label="培训记录数" width="110" />
              <el-table-column label="参与率" width="110"><template #default="{ row }">{{ row.averageAttendRate == null ? '—' : `${row.averageAttendRate}%` }}</template></el-table-column>
              <el-table-column label="通过率" width="110"><template #default="{ row }">{{ row.averagePassRate == null ? '—' : `${row.averagePassRate}%` }}</template></el-table-column>
              <el-table-column label="能力提升" width="120"><template #default="{ row }">{{ row.averageImproveScore ?? '—' }}</template></el-table-column>
              <template #empty><el-empty description="暂无培训记录，完成登记后会生成课程汇总" /></template>
            </el-table>
          </section>
          <section class="simulation-workspace">
            <div class="simulation-header"><div><h3>培训落地测算</h3><p>输入计划人数和预估指标，查看简单情景测算。</p></div><el-tag type="warning" effect="plain">假设测算</el-tag></div>
            <div class="simulation-inputs">
              <el-form-item label="计划人数"><el-input-number v-model="simulation.learners" :min="0" :max="100000" /></el-form-item>
              <el-form-item label="预估参与率"><el-input-number v-model="simulation.attendRate" :min="0" :max="100" :step="5" /><span>%</span></el-form-item>
              <el-form-item label="预估通过率"><el-input-number v-model="simulation.passRate" :min="0" :max="100" :step="5" /><span>%</span></el-form-item>
              <el-form-item label="通过者提升分"><el-input-number v-model="simulation.improveScore" :min="0" :max="100" :step="1" /></el-form-item>
            </div>
            <div class="simulation-results">
              <div><span>预计参与人数</span><strong>{{ estimatedAttendees }}</strong></div>
              <div><span>预计通过人数</span><strong>{{ estimatedPassed }}</strong></div>
              <div><span>提升分值合计估算</span><strong>{{ totalImprovement }}</strong></div>
            </div>
            <p class="simulation-formula">计算方式：计划人数 × 参与率；参与人数 × 通过率；通过人数 × 单人提升分。结果仅用于方案讨论。</p>
          </section>
        </section>
      </el-tab-pane>
<el-tab-pane label="推荐助手" name="assistant">
        <ModuleAssistant
          id="assistant-training"
          module-key="training"
          title="智能培训推荐助手"
          :quick-prompts="['查看当前员工的培训推荐和技能缺口', '制定分阶段学习路径']"
          status-endpoint="/training/ai/status"
          chat-endpoint="/training/ai/chat"
          service-name="培训服务"
          config-env-prefix="TRAINING_DIFY"
        />
      </el-tab-pane>
      <el-tab-pane label="学习记录" name="records">
        <el-select v-model="employeeId" filterable remote :remote-method="searchEmployees" placeholder="搜索员工" @change="handleEmployeeChange"><el-option v-for="item in employees" :key="item.id" :value="item.id" :label="item.name" /></el-select>
        <el-table :data="plan?.completedCourses ?? []"><el-table-column prop="courseName" label="课程" /><el-table-column prop="attendRate" label="参与率（%）" /><el-table-column prop="passRate" label="通过率（%）" /><el-table-column prop="improveScore" label="能力提升" /><el-table-column prop="completedAt" label="完成时间" /></el-table>
      </el-tab-pane>
      <el-tab-pane label="匹配算法" name="algorithm"><TrainingAlgorithmPanel @changed="employeeId !== null && loadPlan(employeeId)" /></el-tab-pane>
    </el-tabs>

    <el-dialog v-model="courseDialog" :title="editingCourseId === null ? '新增课程' : '编辑课程'" width="540px" destroy-on-close>
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="100px">
        <el-form-item label="课程名称" prop="courseName"><el-input v-model="courseForm.courseName" maxlength="128" /></el-form-item>
        <el-form-item label="课程分类"><el-select v-model="courseForm.categoryId" clearable placeholder="选择分类"><el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="课程类型"><el-input v-model="courseForm.courseType" maxlength="32" /></el-form-item>
        <el-form-item label="适用岗位"><el-input v-model="courseForm.forPosition" maxlength="64" /></el-form-item>
        <el-form-item label="难度等级"><el-select v-model="courseForm.difficulty" clearable><el-option label="入门" value="入门" /><el-option label="初级" value="初级" /><el-option label="中级" value="中级" /><el-option label="高级" value="高级" /></el-select></el-form-item>
        <el-form-item label="培训时长"><el-input-number v-model="courseForm.duration" :min="0" :max="10000" /><span class="unit-label">小时</span></el-form-item>
      </el-form>
      <template #footer><el-button @click="courseDialog = false">取消</el-button><el-button type="primary" :loading="courseSaving" @click="saveCourse">保存课程</el-button></template>
    </el-dialog>

    <el-dialog v-model="categoryDialog" title="课程分类" width="700px" destroy-on-close>
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" inline class="category-form">
        <el-form-item prop="categoryName"><el-input v-model="categoryForm.categoryName" placeholder="分类名称" maxlength="64" /></el-form-item>
        <el-form-item><el-input v-model="categoryForm.description" placeholder="用途说明（可选）" maxlength="255" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="categorySaving" @click="saveCategory">{{ editingCategoryId === null ? '新增分类' : '保存修改' }}</el-button><el-button v-if="editingCategoryId !== null" @click="openCategoryForm()">取消编辑</el-button></el-form-item>
      </el-form>
      <el-table :data="categories" stripe>
        <el-table-column prop="categoryName" label="分类" min-width="150" />
        <el-table-column prop="description" label="说明" min-width="210" />
        <el-table-column prop="courseCount" label="课程数" width="90" />
        <el-table-column label="操作" width="130"><template #default="{ row }"><el-button link type="primary" @click="openCategoryForm(row)">编辑</el-button><el-button link type="danger" @click="removeCategory(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="pathDialog" :title="editingPathId === null ? '新建学习路径' : '编辑学习路径'" width="760px" destroy-on-close>
      <el-form ref="pathFormRef" :model="pathForm" :rules="pathRules" label-width="100px">
        <el-form-item label="路径名称" prop="pathName"><el-input v-model="pathForm.pathName" maxlength="64" /></el-form-item>
        <el-form-item label="适用岗位"><el-input v-model="pathForm.forPosition" maxlength="128" placeholder="多个岗位用逗号分隔" /></el-form-item>
        <el-form-item label="培养目标"><el-input v-model="pathForm.developmentGoal" maxlength="255" placeholder="例如：骨干人才、团队管理；用于匹配员工培养目标和梯队标签" /></el-form-item>
        <el-form-item label="适配技能"><el-input v-model="pathForm.targetSkill" maxlength="64" placeholder="例如：数据分析、团队管理" /></el-form-item>
        <div class="task-editor-heading"><span>阶段学习任务</span><el-button size="small" :icon="Plus" @click="addPathTask">添加任务</el-button></div>
        <div v-for="(task, index) in pathForm.tasks" :key="index" class="task-row">
          <el-form-item label="阶段"><el-input-number v-model="task.stage" :min="1" :max="99" /></el-form-item>
          <el-form-item label="任务名称"><el-input v-model="task.taskName" maxlength="128" placeholder="填写阶段目标或任务" /></el-form-item>
          <el-form-item label="关联课程"><el-select v-model="task.courseId" clearable placeholder="可选课程"><el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" /></el-select></el-form-item>
          <el-button link type="danger" @click="removePathTask(index)">移除</el-button>
        </div>
      </el-form>
      <template #footer><el-button @click="pathDialog = false">取消</el-button><el-button type="primary" :loading="pathSaving" @click="savePath">保存路径</el-button></template>
    </el-dialog>

    <el-dialog v-model="recordDialog" title="登记培训结果" width="560px" destroy-on-close>
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="110px">
        <el-form-item label="员工" prop="employeeId">
          <el-select v-model="recordForm.employeeId" filterable remote clearable :remote-method="searchEmployees" placeholder="搜索姓名或工号">
            <el-option v-for="item in employees" :key="item.id" :value="item.id" :label="`${item.name}（${item.empNo}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="培训课程" prop="courseId"><el-select v-model="recordForm.courseId" filterable placeholder="选择课程"><el-option v-for="item in courseOptions" :key="item.id" :value="item.id" :label="item.courseName" /></el-select></el-form-item>
        <el-form-item label="参与率"><el-input-number v-model="recordForm.attendRate" :min="0" :max="100" :precision="2" /><span class="unit-label">%</span></el-form-item>
        <el-form-item label="考核通过率"><el-input-number v-model="recordForm.passRate" :min="0" :max="100" :precision="2" /><span class="unit-label">%</span></el-form-item>
        <el-form-item label="能力提升分值"><el-input-number v-model="recordForm.improveScore" :min="0" :max="100" :precision="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="recordDialog = false">取消</el-button><el-button type="primary" :loading="recordSaving" @click="saveTrainingRecord">保存记录</el-button></template>
    </el-dialog>
  </main>
</template>

<style scoped>
.training-page { color: #24313d; }
.training-heading, .workspace-toolbar, .toolbar-actions, .filter-row, .pagination-row, .section-heading, .simulation-header, .task-editor-heading { display: flex; align-items: center; }
.training-heading { justify-content: space-between; gap: 20px; margin-bottom: 14px; }
.page-title { margin-bottom: 4px; color: #203c57; }
.page-tip { margin: 0; }
.training-tabs :deep(.el-tabs__header) { margin-bottom: 18px; }
.training-tabs :deep(.el-tabs__item) { padding: 0 22px; }
.workspace-toolbar { justify-content: space-between; flex-wrap: wrap; gap: 16px; margin: 6px 0 18px; }
.workspace-toolbar h2, .simulation-header h3 { margin: 0; font-size: 17px; font-weight: 620; color: #203c57; }
.workspace-toolbar p, .simulation-header p { margin: 5px 0 0; color: #72808a; font-size: 13px; }
.toolbar-actions { flex-wrap: wrap; gap: 8px; }
.data-workspace { padding: 16px 18px 12px; border: 1px solid #dce5e8; border-radius: 6px; background: #fff; }
.filter-row { flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.filter-keyword { width: min(320px, 100%); }
.filter-select { width: 170px; }
.pagination-row { justify-content: space-between; flex-wrap: wrap; gap: 12px; padding-top: 14px; color: #71808a; font-size: 13px; }
.import-note, .simulation-formula { margin: 10px 0 0; color: #7a8790; font-size: 12px; line-height: 1.6; }
.file-input { display: none; }
.employee-picker { width: min(360px, 100%); }
.plan-content { display: flex; flex-direction: column; gap: 14px; }
.plan-summary, .effect-summary, .simulation-results { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border: 1px solid #dce5e8; border-radius: 6px; background: #fff; }
.plan-summary > div, .effect-summary > div, .simulation-results > div { display: flex; flex-direction: column; gap: 5px; padding: 16px 18px; border-right: 1px solid #e8edef; }
.plan-summary > div:last-of-type, .effect-summary > div:last-child, .simulation-results > div:last-child { border-right: 0; }
.plan-summary span, .effect-summary span, .simulation-results span { color: #72808a; font-size: 12px; }
.plan-summary strong, .effect-summary strong, .simulation-results strong { color: #203c57; font-size: 19px; font-weight: 650; }
.plan-summary small { grid-column: 1 / -1; padding: 0 18px 12px; color: #87939b; }
.plan-columns { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.9fr); gap: 14px; align-items: start; }
.plan-section { min-width: 0; padding: 16px 18px; border: 1px solid #dce5e8; border-radius: 6px; background: #fff; }
.path-recommendations { padding: 16px 18px; border: 1px solid #dce5e8; border-radius: 6px; background: #fff; }
.path-match-list { display: grid; gap: 10px; }
.path-match-row { display: grid; grid-template-columns: minmax(180px, 1fr) minmax(160px, 0.7fr) 120px; gap: 16px; align-items: center; padding: 10px 0; border-top: 1px solid #edf1f2; }
.path-match-main { display: grid; gap: 4px; }
.path-match-main strong { font-weight: 600; }
.path-match-main span, .path-task-count { color: #72808a; font-size: 12px; }
.plan-aside { display: grid; gap: 14px; }
.section-heading { justify-content: space-between; gap: 10px; margin-bottom: 12px; }
.section-heading h3 { margin: 0; font-size: 15px; font-weight: 620; }
.section-heading > span { color: #72808a; font-size: 12px; }
.completed-list { display: grid; gap: 6px; }
.completed-item { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 8px 0; border-bottom: 1px solid #edf1f2; }
.skill-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.effects-content { display: grid; gap: 14px; }
.effect-summary { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.simulation-workspace { padding: 18px; border: 1px solid #dce5e8; border-radius: 6px; background: #fff; }
.simulation-header { justify-content: space-between; gap: 12px; }
.simulation-inputs { display: grid; grid-template-columns: repeat(4, minmax(160px, 1fr)); gap: 12px; margin-top: 18px; }
.simulation-inputs :deep(.el-form-item) { margin: 0; display: flex; flex-direction: column; align-items: flex-start; }
.simulation-inputs :deep(.el-form-item__label) { margin-bottom: 6px; color: #72808a; }
.simulation-inputs :deep(.el-input-number) { width: 100%; }
.simulation-inputs > .el-form-item > span { color: #72808a; }
.simulation-results { grid-template-columns: repeat(3, minmax(0, 1fr)); margin-top: 18px; background: #f7faf9; border-color: #d9e8e1; }
.simulation-results strong { color: #28734e; }
.category-form { display: flex; flex-wrap: wrap; gap: 6px; }
.category-form :deep(.el-form-item) { margin-right: 0; }
.task-editor-heading { justify-content: space-between; margin: 4px 0 10px 100px; }
.task-row { display: grid; grid-template-columns: 140px minmax(200px, 1fr) minmax(200px, 1fr) 56px; align-items: start; gap: 10px; padding: 12px 0; border-top: 1px solid #edf1f2; }
.task-row :deep(.el-form-item) { display: flex; flex-direction: column; margin: 0; }
.task-row :deep(.el-form-item__label) { margin-bottom: 5px; }
.unit-label { margin-left: 8px; color: #72808a; }
@media (max-width: 900px) {
  .plan-columns { grid-template-columns: 1fr; }
  .simulation-inputs { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .plan-summary, .effect-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .plan-summary > div:nth-child(2), .effect-summary > div:nth-child(2) { border-right: 0; }
  .task-row { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 600px) {
  .training-heading, .workspace-toolbar { align-items: flex-start; }
  .workspace-toolbar { flex-direction: column; }
  .toolbar-actions { width: 100%; }
  .toolbar-actions .el-button { flex: 1 1 auto; }
  .filter-keyword, .filter-select { width: 100%; }
  .plan-summary, .effect-summary, .simulation-results { grid-template-columns: 1fr 1fr; }
  .plan-summary > div:nth-child(odd), .effect-summary > div:nth-child(odd), .simulation-results > div:nth-child(odd) { border-right: 1px solid #e8edef; }
  .plan-summary > div:nth-child(even), .effect-summary > div:nth-child(even), .simulation-results > div:nth-child(even) { border-right: 0; }
  .simulation-inputs { grid-template-columns: 1fr 1fr; }
  .path-match-row { grid-template-columns: 1fr; gap: 8px; }
  .task-row { grid-template-columns: 1fr; }
  .task-editor-heading { margin-left: 0; }
}
</style>
