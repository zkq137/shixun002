<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Delete,
  Download,
  Edit,
  Plus,
  Refresh,
  Search,
  Upload,
  UploadFilled,
} from '@element-plus/icons-vue'
import {
  IMPORT_TEMPLATE_URL,
  IMPORT_URL,
  batchDeleteEmployees,
  batchUpdateEmployees,
  createEmployee,
  deleteEmployee,
  employeeExportUrl,
  fetchDepartments,
  fetchEmployeeDetail,
  fetchEmployeePage,
  fetchEmployeeSkills,
  fetchPositionOptions,
  updateEmployee,
  updateEmployeeSkills,
} from '@/api/employee'
import { fetchSkillTree } from '@/api/skill'
import type {
  EmployeeDetail,
  EmployeeForm,
  EmployeeRow,
  ImportResult,
  PositionOption,
  SkillNode,
} from '@/api/types'

const loading = ref(false)
const rows = ref<EmployeeRow[]>([])
const total = ref(0)
const departments = ref<string[]>([])
const positions = ref<PositionOption[]>([])
const selectedRows = ref<EmployeeRow[]>([])

const talentTags = ['核心骨干', '储备人才', '普通员工', '待优化']
const warningLevels = ['低风险', '中风险', '高风险']

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  department: '',
  warningLevel: '',
  talentTag: '',
  status: '',
})

async function load() {
  loading.value = true
  try {
    const page = await fetchEmployeePage(query)
    rows.value = page.records
    total.value = page.total
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  query.keyword = ''
  query.department = ''
  query.warningLevel = ''
  query.talentTag = ''
  query.status = ''
  search()
}

function onSelectionChange(selection: EmployeeRow[]) {
  selectedRows.value = selection
}

/* ---------------- 批量操作 ---------------- */
async function batchRemove() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先勾选员工')
    return
  }
  await ElMessageBox.confirm(
    `确定删除勾选的 ${selectedRows.value.length} 名员工吗？关联的薪酬、绩效、技能、培训记录会一起删除。`,
    '批量删除',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
  const result = await batchDeleteEmployees(selectedRows.value.map((item) => item.id))
  ElMessage.success(`删除完成：成功 ${result.success} 条，失败 ${result.failed} 条`)
  load()
}

const batchVisible = ref(false)
const batchSaving = ref(false)
const batchForm = reactive({
  department: '',
  status: '',
  workMode: '',
  levelTier: '',
  jobRank: '',
})

function openBatch() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先勾选员工')
    return
  }
  Object.assign(batchForm, { department: '', status: '', workMode: '', levelTier: '', jobRank: '' })
  batchVisible.value = true
}

async function submitBatch() {
  const hasField = Object.values(batchForm).some((value) => value !== '')
  if (!hasField) {
    ElMessage.warning('至少要选一个要修改的字段')
    return
  }
  batchSaving.value = true
  try {
    const result = await batchUpdateEmployees({
      ids: selectedRows.value.map((item) => item.id),
      ...batchForm,
    })
    ElMessage.success(`修改完成：成功 ${result.success} 条，失败 ${result.failed} 条`)
    batchVisible.value = false
    load()
  } finally {
    batchSaving.value = false
  }
}

/* ---------------- 导入导出 ---------------- */
function exportExcel() {
  window.open(employeeExportUrl(query), '_blank')
}

function downloadTemplate() {
  window.open(IMPORT_TEMPLATE_URL, '_blank')
}

const importVisible = ref(false)
const importResult = ref<ImportResult | null>(null)
const importing = ref(false)

function openImport() {
  importResult.value = null
  importVisible.value = true
}

interface UploadResponse {
  code: number
  message: string
  data: ImportResult
}

function handleImportSuccess(response: UploadResponse) {
  importing.value = false
  if (response.code === 200) {
    importResult.value = response.data
    ElMessage.success(`导入完成：成功 ${response.data.success} 条，失败 ${response.data.failed} 条`)
    load()
  } else {
    ElMessage.error(response.message || '导入失败')
  }
}

function handleImportError() {
  importing.value = false
  ElMessage.error('上传失败，请确认后端服务已启动')
}

/* ---------------- 详情 ---------------- */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<EmployeeDetail | null>(null)

async function openDetail(row: EmployeeRow) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchEmployeeDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

/* ---------------- 技能配置 ---------------- */
const skillVisible = ref(false)
const skillSaving = ref(false)
const skillTree = ref<SkillNode[]>([])
const skillTreeRef = ref()
const skillEmployeeId = ref<number | null>(null)
const skillEmployeeName = ref('')

async function openSkills(row: EmployeeRow) {
  skillEmployeeId.value = row.id
  skillEmployeeName.value = row.name
  skillVisible.value = true
  const [treeData, mine] = await Promise.all([
    fetchSkillTree(),
    fetchEmployeeSkills(row.id),
  ])
  skillTree.value = treeData
  await nextTick()
  skillTreeRef.value?.setCheckedKeys(mine.map((item) => item.id))
}

async function saveSkills() {
  if (skillEmployeeId.value === null) return
  const checkedLeaves = skillTreeRef.value?.getCheckedNodes(true) ?? []
  skillSaving.value = true
  try {
    await updateEmployeeSkills(
      skillEmployeeId.value,
      checkedLeaves.map((node: SkillNode) => node.id),
    )
    ElMessage.success('技能已更新')
    skillVisible.value = false
    load()
  } finally {
    skillSaving.value = false
  }
}

/* ---------------- 新增 / 修改 ---------------- */
const dialogVisible = ref(false)
const dialogTitle = ref('新增员工')
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const emptyForm = (): EmployeeForm => ({
  empNo: '',
  name: '',
  gender: '男',
  age: undefined,
  department: '',
  positionId: null,
  levelTier: '',
  jobRank: '',
  managerEmpNo: '',
  workMode: '',
  tenureYears: undefined,
  education: '',
  phone: '',
  status: '在职',
})

const form = reactive<EmployeeForm>(emptyForm())

const rules: FormRules = {
  empNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  positionId: [{ required: true, message: '请选择岗位', trigger: 'change' }],
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增员工'
  Object.assign(form, emptyForm())
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row: EmployeeRow) {
  editingId.value = row.id
  dialogTitle.value = `修改员工 - ${row.name}`
  Object.assign(form, emptyForm(), {
    empNo: row.empNo,
    name: row.name,
    gender: row.gender ?? '男',
    age: row.age ?? undefined,
    department: row.department ?? '',
    positionId: row.positionId ?? null,
    levelTier: row.levelTier ?? '',
    jobRank: row.jobRank ?? '',
    managerEmpNo: row.managerEmpNo ?? '',
    workMode: row.workMode ?? '',
    tenureYears: row.tenureYears ?? undefined,
    status: row.status ?? '在职',
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function onPositionChange(id: number) {
  const position = positions.value.find((item) => item.id === id)
  if (position) {
    form.department = position.department
    form.levelTier = position.levelTier ?? ''
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value === null) {
      await createEmployee(form)
      ElMessage.success('新增成功')
    } else {
      await updateEmployee(editingId.value, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: EmployeeRow) {
  await ElMessageBox.confirm(
    `确定删除 ${row.name}（${row.empNo}）吗？关联的薪酬、绩效、技能、培训记录会一起删除。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
  await deleteEmployee(row.id)
  ElMessage.success('删除成功')
  if (rows.value.length === 1 && query.pageNum > 1) {
    query.pageNum -= 1
  }
  load()
}

function warningType(level?: string | null): 'success' | 'warning' | 'danger' | 'info' {
  if (level === '高风险') return 'danger'
  if (level === '中风险') return 'warning'
  return 'success'
}

function talentType(tag?: string | null): 'success' | 'warning' | 'danger' | 'info' {
  if (tag === '核心骨干') return 'danger'
  if (tag === '储备人才') return 'warning'
  if (tag === '待优化') return 'info'
  return 'success'
}

onMounted(async () => {
  const [deptList, positionList] = await Promise.all([
    fetchDepartments(),
    fetchPositionOptions(),
  ])
  departments.value = deptList
  positions.value = positionList
  load()
})
</script>

<template>
  <div>
    <h2 class="page-title">员工档案</h2>
    <p class="page-tip">
      数据来自 emp_employee 等表。支持单条增删改查、勾选后批量修改/删除、Excel 批量导入导出，
      点「技能」可以给员工配技能树里的技能。
    </p>

    <el-card shadow="never">
      <div class="search-bar">
        <el-input
          v-model="query.keyword"
          placeholder="姓名或工号"
          clearable
          style="width: 180px"
          @keyup.enter="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.department" placeholder="部门" clearable style="width: 130px">
          <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.talentTag" placeholder="人才标签" clearable style="width: 130px">
          <el-option v-for="item in talentTags" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.warningLevel" placeholder="流失风险" clearable style="width: 130px">
          <el-option v-for="item in warningLevels" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>

      <el-divider style="margin: 16px 0" />

      <div class="search-bar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增员工</el-button>
        <el-button :icon="Edit" :disabled="selectedRows.length === 0" @click="openBatch">
          批量修改{{ selectedRows.length ? `（${selectedRows.length}）` : '' }}
        </el-button>
        <el-button
          type="danger"
          :icon="Delete"
          :disabled="selectedRows.length === 0"
          @click="batchRemove"
        >
          批量删除{{ selectedRows.length ? `（${selectedRows.length}）` : '' }}
        </el-button>
        <el-button :icon="Upload" @click="openImport">导入 Excel</el-button>
        <el-button :icon="Download" @click="exportExcel">导出 Excel</el-button>
        <el-button link type="primary" @click="downloadTemplate">下载导入模板</el-button>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        border
        height="520"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column prop="empNo" label="工号" width="120" />
        <el-table-column prop="name" label="姓名" width="90" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="age" label="年龄" width="70" />
        <el-table-column prop="department" label="部门" width="110" />
        <el-table-column prop="positionName" label="岗位" min-width="140" />
        <el-table-column prop="jobRank" label="职级" width="90" />
        <el-table-column prop="tenureYears" label="司龄" width="80" />
        <el-table-column prop="perfScore" label="绩效" width="80" />
        <el-table-column prop="potentialLevel" label="潜力" width="130" />
        <el-table-column label="流失风险" width="100">
          <template #default="{ row }">
            <el-tag :type="warningType(row.warningLevel)" effect="plain">
              {{ row.warningLevel ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="人才标签" width="110">
          <template #default="{ row }">
            <el-tag :type="talentType(row.talentTag)" effect="light">
              {{ row.talentTag ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openSkills(row)">技能</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="search"
        @current-change="load"
      />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="480px" :title="detail ? detail.name : '员工详情'">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border>
          <el-descriptions-item label="工号">{{ detail.empNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">
            {{ detail.name }}（{{ detail.gender }}，{{ detail.age }} 岁）
          </el-descriptions-item>
          <el-descriptions-item label="部门岗位">
            {{ detail.department }} / {{ detail.positionName }}
          </el-descriptions-item>
          <el-descriptions-item label="职级">
            {{ detail.levelTier }} / {{ detail.jobRank }}
          </el-descriptions-item>
          <el-descriptions-item label="上级">
            {{ detail.managerName ?? '无（最高层级）' }}
          </el-descriptions-item>
          <el-descriptions-item label="司龄 / 办公方式">
            {{ detail.tenureYears }} 年 / {{ detail.workMode }}
          </el-descriptions-item>
          <el-descriptions-item label="基本年薪">
            {{ detail.baseSalary ? detail.baseSalary.toLocaleString() + ' 元' : '-' }}
            （系数 {{ detail.salaryCoefficient ?? '-' }}）
          </el-descriptions-item>
          <el-descriptions-item label="绩效 / 潜力">
            {{ detail.perfScore ?? '-' }} 分 / {{ detail.potentialLevel ?? '-' }}（{{ detail.potentialScore ?? '-' }}）
          </el-descriptions-item>
          <el-descriptions-item label="流失风险">
            <el-tag :type="warningType(detail.warningLevel)" effect="plain">
              {{ detail.warningLevel }}（{{ detail.riskScore ?? '-' }} 分）
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="人才标签">
            <el-tag :type="talentType(detail.talentTag)">{{ detail.talentTag }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="技能标签">
            <el-tag
              v-for="skill in detail.skills"
              :key="skill"
              size="small"
              effect="plain"
              style="margin: 0 6px 6px 0"
            >
              {{ skill }}
            </el-tag>
            <span v-if="detail.skills.length === 0">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="已完成培训">
            <el-tag
              v-for="course in detail.courses"
              :key="course"
              size="small"
              type="success"
              effect="plain"
              style="margin: 0 6px 6px 0"
            >
              {{ course }}
            </el-tag>
            <span v-if="detail.courses.length === 0">-</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <!-- 员工技能 -->
    <el-dialog v-model="skillVisible" :title="`配置技能 - ${skillEmployeeName}`" width="560px">
      <p class="page-tip" style="margin-top: 0">勾选这个员工会的技能（分类只是分组，勾选具体的技能）：</p>
      <el-tree
        ref="skillTreeRef"
        :data="skillTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'skillName', children: 'children' }"
        style="max-height: 420px; overflow: auto; border: 1px solid #ebeef5; border-radius: 4px; padding: 8px"
      />
      <template #footer>
        <el-button @click="skillVisible = false">取消</el-button>
        <el-button type="primary" :loading="skillSaving" @click="saveSkills">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量修改 -->
    <el-dialog v-model="batchVisible" title="批量修改" width="480px">
      <p class="page-tip" style="margin-top: 0">
        已选 {{ selectedRows.length }} 名员工，只填要改的字段，没填的不动。
      </p>
      <el-form label-width="90px">
        <el-form-item label="部门">
          <el-select v-model="batchForm.department" clearable placeholder="不修改" style="width: 100%">
            <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="batchForm.status" clearable placeholder="不修改" style="width: 100%">
            <el-option label="在职" value="在职" />
            <el-option label="试用" value="试用" />
            <el-option label="离职" value="离职" />
          </el-select>
        </el-form-item>
        <el-form-item label="办公方式">
          <el-select v-model="batchForm.workMode" clearable placeholder="不修改" style="width: 100%">
            <el-option label="现场办公" value="现场办公" />
            <el-option label="远程办公" value="远程办公" />
            <el-option label="混合办公" value="混合办公" />
          </el-select>
        </el-form-item>
        <el-form-item label="职级层级">
          <el-input v-model="batchForm.levelTier" placeholder="不修改" />
        </el-form-item>
        <el-form-item label="职级">
          <el-input v-model="batchForm.jobRank" placeholder="不修改" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="submitBatch">确定修改</el-button>
      </template>
    </el-dialog>

    <!-- 导入 Excel -->
    <el-dialog v-model="importVisible" title="导入员工 Excel" width="640px">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        先「下载导入模板」，按模板填写。工号、姓名、岗位是必填；技能标签用顿号分隔，只认技能库里已有的技能。
      </el-alert>

      <el-upload
        drag
        :action="IMPORT_URL"
        name="file"
        accept=".xlsx,.xls"
        :show-file-list="false"
        :on-progress="() => (importing = true)"
        :on-success="handleImportSuccess"
        :on-error="handleImportError"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">把 Excel 拖到这里，或<em>点击选择文件</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .xlsx / .xls，单次建议不超过 5000 行</div>
        </template>
      </el-upload>

      <div v-if="importing" style="margin-top: 12px">
        <el-progress :indeterminate="true" :duration="2" />
      </div>

      <div v-if="importResult" style="margin-top: 16px">
        <el-alert
          :type="importResult.failed === 0 ? 'success' : 'warning'"
          :closable="false"
          :title="`共 ${importResult.total} 行：成功 ${importResult.success} 条，失败 ${importResult.failed} 条`"
        />

        <div v-if="importResult.unknownSkills.length > 0" style="margin-top: 12px">
          <p class="page-tip" style="margin: 0 0 6px">
            下面这些技能在技能库里没有，已跳过（员工本身导入成功）：
          </p>
          <el-tag
            v-for="item in importResult.unknownSkills"
            :key="item"
            type="warning"
            effect="plain"
            style="margin: 0 6px 6px 0"
          >
            {{ item }}
          </el-tag>
        </div>

        <el-table
          v-if="importResult.errors.length > 0"
          :data="importResult.errors"
          stripe
          border
          max-height="260"
          style="margin-top: 12px"
        >
          <el-table-column prop="row" label="行号" width="80" />
          <el-table-column prop="empNo" label="工号" width="130" />
          <el-table-column prop="message" label="失败原因" min-width="240" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/修改 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="工号" prop="empNo">
              <el-input v-model="form.empNo" placeholder="如 E12345678" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="18" :max="70" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="岗位" prop="positionId">
              <el-select
                v-model="form.positionId"
                filterable
                placeholder="按部门或岗位名搜索"
                style="width: 100%"
                @change="onPositionChange"
              >
                <el-option
                  v-for="item in positions"
                  :key="item.id"
                  :label="`${item.department} / ${item.positionName}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-select v-model="form.department" style="width: 100%">
                <el-option v-for="item in departments" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职级层级">
              <el-input v-model="form.levelTier" placeholder="选岗位后自动带出" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职级">
              <el-input v-model="form.jobRank" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="办公方式">
              <el-select v-model="form.workMode" clearable style="width: 100%">
                <el-option label="现场办公" value="现场办公" />
                <el-option label="远程办公" value="远程办公" />
                <el-option label="混合办公" value="混合办公" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="司龄(年)">
              <el-input-number v-model="form.tenureYears" :min="0" :max="40" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上级工号">
              <el-input v-model="form.managerEmpNo" placeholder="可以不填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="在职" value="在职" />
                <el-option label="试用" value="试用" />
                <el-option label="离职" value="离职" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历">
              <el-input v-model="form.education" placeholder="数据集没有，可后补" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系方式">
              <el-input v-model="form.phone" placeholder="数据集没有，可后补" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
