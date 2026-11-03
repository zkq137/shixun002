<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import ModuleAssistant from '@/components/ModuleAssistant.vue'
import { useUrlTab } from '@/composables/useUrlTab'
import {
  createSkill,
  deleteSkill,
  fetchSkillTree,
  searchSkills,
  updateSkill,
} from '@/api/skill'
import type { SkillForm, SkillItem, SkillNode } from '@/api/types'

const activeTab = useUrlTab(
  ['overview', 'categories', 'frequent', 'demand', 'gaps', 'assistant'] as const,
  'overview',
)
const skillAssistantPrompts = [
  '当前最稀缺的技能有哪些',
  '哪些技能在岗位要求中覆盖最广',
  '分析研发技术分类的技能结构',
  '为技能体系提出优化建议',
]

const loading = ref(false)
const tree = ref<SkillNode[]>([])
const treeRef = ref()
const filterText = ref('')
const selected = ref<SkillNode | null>(null)
const expandedKeys = ref<number[]>([])
const children = ref<SkillItem[]>([])
const childrenLoading = ref(false)
const searchResult = ref<SkillItem[]>([])
const searching = ref(false)

const categoryOptions = computed(() =>
  tree.value.map((item) => ({ id: item.id, name: item.skillName })),
)

const totalSkills = computed(() =>
  tree.value.reduce((sum, item) => sum + item.children.length, 0),
)

const allSkills = computed<SkillNode[]>(() =>
  tree.value.flatMap((category) =>
    (category.children ?? []).map((skill) => ({
      ...skill,
      skillCategory: skill.skillCategory ?? category.skillName,
    })),
  ),
)

const categoryStats = computed(() =>
  tree.value.map((category) => {
    const skills = category.children ?? []
    return {
      ...category,
      skillCount: skills.length,
      employeeCoverage: skills.reduce((sum, skill) => sum + (skill.employeeCount ?? 0), 0),
      positionCoverage: skills.reduce((sum, skill) => sum + (skill.positionCount ?? 0), 0),
    }
  }),
)

const frequentSkills = computed(() =>
  [...allSkills.value]
    .sort((a, b) => (b.employeeCount ?? 0) - (a.employeeCount ?? 0))
    .slice(0, 20),
)

const demandedSkills = computed(() =>
  [...allSkills.value]
    .sort((a, b) => (b.positionCount ?? 0) - (a.positionCount ?? 0))
    .slice(0, 20),
)

const gapSkills = computed(() =>
  allSkills.value
    .filter((skill) => (skill.positionCount ?? 0) > 0)
    .map((skill) => {
      const positions = skill.positionCount ?? 0
      const employees = skill.employeeCount ?? 0
      return {
        ...skill,
        gap: Math.max(0, positions - employees),
        coverageRate: Math.min(100, Math.round((employees / positions) * 100)),
      }
    })
    .sort((a, b) => b.gap - a.gap || a.coverageRate - b.coverageRate)
    .slice(0, 30),
)

async function viewCategory(row: SkillNode) {
  const node = findNode(tree.value, row.id)
  if (!node) return
  selected.value = node
  expandedKeys.value = [node.id]
  await loadChildren(node.id)
  activeTab.value = 'overview'
  await nextTick()
  treeRef.value?.setCurrentKey(node.id)
}

async function loadTree(keepSelected = true) {
  loading.value = true
  try {
    tree.value = await fetchSkillTree()
    if (keepSelected && selected.value) {
      const found = findNode(tree.value, selected.value.id)
      selected.value = found
      if (found && found.level === 1) {
        await loadChildren(found.id)
      }
    }
  } finally {
    loading.value = false
  }
}

function findNode(nodes: SkillNode[], id: number): SkillNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    const hit = findNode(node.children ?? [], id)
    if (hit) return hit
  }
  return null
}

async function loadChildren(parentId: number) {
  childrenLoading.value = true
  try {
    const node = findNode(tree.value, parentId)
    children.value = (node?.children ?? []) as unknown as SkillItem[]
  } finally {
    childrenLoading.value = false
  }
}

function onNodeClick(data: SkillNode) {
  selected.value = data
  searchResult.value = []
  if (data.level === 1) {
    loadChildren(data.id)
  } else {
    children.value = []
  }
}

/* ---------------- 搜索 ---------------- */
async function doSearch() {
  const keyword = filterText.value.trim()
  if (!keyword) {
    searchResult.value = []
    return
  }
  searching.value = true
  try {
    searchResult.value = await searchSkills(keyword)
  } finally {
    searching.value = false
  }
}

watch(filterText, (value) => {
  treeRef.value?.filter(value)
  if (!value) {
    searchResult.value = []
  }
})

function filterNode(value: string, data: SkillNode) {
  if (!value) return true
  return data.skillName.includes(value)
}

/* ---------------- 新增 / 修改 ---------------- */
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<SkillForm>({ skillName: '', parentId: 0, description: '', status: '启用', sortOrder: null })

const rules: FormRules = {
  skillName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

function openCreateCategory() {
  editingId.value = null
  dialogTitle.value = '新增技能分类'
  Object.assign(form, { skillName: '', parentId: 0, description: '', status: '启用', sortOrder: null })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openCreateSkill(parentId?: number) {
  editingId.value = null
  dialogTitle.value = '新增技能'
  Object.assign(form, {
    skillName: '',
    parentId: parentId ?? (selected.value?.level === 1 ? selected.value.id : 0),
    description: '',
    status: '启用',
    sortOrder: null,
  })
  if (!form.parentId) {
    ElMessage.warning('请先选一个分类，或者在弹窗里选择所属分类')
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(node: SkillItem | SkillNode) {
  editingId.value = node.id
  dialogTitle.value = node.level === 1 ? '修改分类' : '修改技能'
  Object.assign(form, {
    skillName: node.skillName,
    parentId: node.level === 1 ? 0 : (node.parentId ?? 0),
    description: node.description ?? '',
    status: node.status ?? '启用',
    sortOrder: node.sortOrder ?? null,
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value === null) {
      await createSkill(form)
      ElMessage.success('新增成功')
    } else {
      await updateSkill(editingId.value, form)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    await loadTree()
    if (selected.value?.level === 1) {
      await loadChildren(selected.value.id)
    }
  } finally {
    saving.value = false
  }
}

async function remove(node: SkillItem | SkillNode) {
  const isCategory = node.level === 1
  await ElMessageBox.confirm(
    isCategory
      ? `确定删除分类「${node.skillName}」吗？分类下面还有技能时会删不掉。`
      : `确定删除技能「${node.skillName}」吗？已经有员工或岗位在用时删不掉。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  )
  await deleteSkill(node.id)
  ElMessage.success('删除成功')
  if (selected.value?.id === node.id) {
    selected.value = null
    children.value = []
  }
  await loadTree()
}

onMounted(async () => {
  await loadTree(false)
  if (tree.value.length > 0) {
    const first = tree.value[0]
    expandedKeys.value = [first.id]
    selected.value = first
    await loadChildren(first.id)
    await nextTick()
    treeRef.value?.setCurrentKey(first.id)
  }
})
</script>

<template>
  <div>
    <h2 class="page-title">技能体系</h2>
    <p class="page-tip">
      企业标准化技能树：分类 → 子技能，共 {{ categoryOptions.length }} 个分类、{{ totalSkills }} 个技能。
      员工会什么、岗位要求什么，都从这棵树里选，保证全公司叫法统一。
    </p>

    <el-tabs v-model="activeTab" class="module-tabs">
      <el-tab-pane label="技能全景" name="overview">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card id="skill-tree" v-loading="loading" shadow="never">
          <template #header>
            <div style="display: flex; align-items: center; gap: 8px">
              <span>技能分类</span>
              <div style="flex: 1"></div>
              <el-button size="small" :icon="Refresh" circle @click="loadTree()" />
              <el-button size="small" type="primary" :icon="Plus" @click="openCreateCategory">
                新增分类
              </el-button>
            </div>
          </template>

          <el-input
            v-model="filterText"
            placeholder="过滤分类/技能，回车可全局搜索"
            clearable
            style="margin-bottom: 12px"
            @keyup.enter="doSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <el-tree
            ref="treeRef"
            :data="tree"
            node-key="id"
            :props="{ label: 'skillName', children: 'children' }"
            :filter-node-method="filterNode"
            :expand-on-click-node="false"
            :default-expanded-keys="expandedKeys"
            highlight-current
            style="max-height: 620px; overflow: auto"
            @node-click="onNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span class="tree-label">{{ data.skillName }}</span>
                <span v-if="data.level === 1" class="tree-count">
                  {{ data.children.length }} 项
                </span>
                <span v-else class="tree-count">
                  {{ data.employeeCount ?? 0 }} 人 · {{ data.positionCount ?? 0 }} 岗
                </span>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <el-col id="skill-detail" :span="16">
        <!-- 搜索结果 -->
        <el-card v-if="searchResult.length > 0 || searching" shadow="never">
          <template #header>搜索结果（{{ searchResult.length }} 条）</template>
          <el-table v-loading="searching" :data="searchResult" stripe border>
            <el-table-column prop="skillName" label="技能" min-width="140" />
            <el-table-column prop="skillCategory" label="所属分类" width="130" />
            <el-table-column prop="employeeCount" label="员工数" width="90" />
            <el-table-column prop="positionCount" label="岗位数" width="90" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 选中分类：看子技能 -->
        <el-card v-else-if="selected && selected.level === 1" shadow="never">
          <template #header>
            <div style="display: flex; align-items: center; gap: 8px">
              <span>分类：{{ selected.skillName }}</span>
              <el-tag size="small" type="info">{{ children.length }} 个技能</el-tag>
              <div style="flex: 1"></div>
              <el-button size="small" :icon="Edit" @click="openEdit(selected)">改分类名</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="remove(selected)">
                删分类
              </el-button>
              <el-button size="small" type="primary" :icon="Plus" @click="openCreateSkill(selected.id)">
                新增技能
              </el-button>
            </div>
          </template>

          <el-table v-loading="childrenLoading" :data="children" stripe height="560">
            <el-table-column prop="skillName" label="技能名称" min-width="160" />
            <el-table-column prop="employeeCount" label="多少员工会" width="120" />
            <el-table-column prop="positionCount" label="多少岗位要求" width="130" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '停用' ? 'info' : 'success'" size="small">
                  {{ row.status ?? '启用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 选中技能：看详情 -->
        <el-card v-else-if="selected" shadow="never">
          <template #header>
            <div style="display: flex; align-items: center; gap: 8px">
              <span>技能：{{ selected.skillName }}</span>
              <div style="flex: 1"></div>
              <el-button size="small" :icon="Edit" @click="openEdit(selected)">修改</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="remove(selected)">删除</el-button>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="所属分类">{{ selected.skillCategory ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="说明">{{ selected.description ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ selected.status ?? '启用' }}</el-descriptions-item>
            <el-descriptions-item label="多少员工会">{{ selected.employeeCount ?? 0 }} 人</el-descriptions-item>
            <el-descriptions-item label="多少岗位要求">{{ selected.positionCount ?? 0 }} 个</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-empty v-else description="从左边选一个分类或技能" />
      </el-col>
    </el-row>
      </el-tab-pane>

      <el-tab-pane label="分类概览" name="categories">
        <el-row :gutter="14">
          <el-col :span="8"><el-card shadow="never"><div class="stat-label">技能分类</div><div class="stat-value text-info">{{ categoryStats.length }}</div></el-card></el-col>
          <el-col :span="8"><el-card shadow="never"><div class="stat-label">技能总数</div><div class="stat-value text-warning">{{ totalSkills }}</div></el-card></el-col>
          <el-col :span="8"><el-card shadow="never"><div class="stat-label">员工技能引用</div><div class="stat-value text-success">{{ categoryStats.reduce((sum, item) => sum + item.employeeCoverage, 0) }}</div></el-card></el-col>
        </el-row>
        <el-card shadow="never" style="margin-top: 16px">
          <template #header>分类规模与引用情况</template>
          <el-table :data="categoryStats" stripe>
            <el-table-column prop="skillName" label="分类" min-width="150" />
            <el-table-column prop="skillCount" label="技能数" width="100" />
            <el-table-column prop="employeeCoverage" label="员工引用合计" width="130" />
            <el-table-column prop="positionCoverage" label="岗位要求合计" width="130" />
            <el-table-column prop="description" label="说明" min-width="240" />
            <el-table-column label="操作" width="110"><template #default="{ row }"><el-button link type="primary" @click="viewCategory(row)">查看技能</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="高频技能" name="frequent">
        <el-card shadow="never">
          <template #header>员工掌握人数最多的 20 项技能</template>
          <el-table :data="frequentSkills" stripe>
            <el-table-column type="index" label="#" width="55" />
            <el-table-column prop="skillName" label="技能" min-width="170" />
            <el-table-column prop="skillCategory" label="分类" width="140" />
            <el-table-column label="掌握人数" min-width="220"><template #default="{ row }"><el-progress :percentage="Math.min(100, (row.employeeCount ?? 0) * 2)" :show-text="false" style="display: inline-block; width: 130px; margin-right: 10px" />{{ row.employeeCount ?? 0 }} 人</template></el-table-column>
            <el-table-column prop="positionCount" label="岗位要求数" width="120" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="岗位需求" name="demand">
        <el-card shadow="never">
          <template #header>岗位要求出现最多的 20 项技能</template>
          <el-table :data="demandedSkills" stripe>
            <el-table-column type="index" label="#" width="55" />
            <el-table-column prop="skillName" label="技能" min-width="170" />
            <el-table-column prop="skillCategory" label="分类" width="140" />
            <el-table-column prop="positionCount" label="要求岗位数" width="120" />
            <el-table-column prop="employeeCount" label="具备人数" width="110" />
            <el-table-column prop="description" label="技能说明" min-width="240" />
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="缺口分析" name="gaps">
        <el-card shadow="never">
          <template #header>普及度相对不足的技能</template>
          <p class="page-tip" style="margin: 0 0 14px">用具备该技能的员工数对比要求该技能的岗位数，用于发现普及度不足的技能；该指标不等同于岗位编制缺口。</p>
          <el-table :data="gapSkills" stripe>
            <el-table-column type="index" label="#" width="55" />
            <el-table-column prop="skillName" label="技能" min-width="170" />
            <el-table-column prop="skillCategory" label="分类" width="140" />
            <el-table-column prop="employeeCount" label="具备人数" width="110" />
            <el-table-column prop="positionCount" label="要求岗位" width="110" />
            <el-table-column prop="gap" label="供需差" width="100" />
            <el-table-column label="覆盖比" min-width="180"><template #default="{ row }"><el-progress :percentage="row.coverageRate" :show-text="false" style="display: inline-block; width: 110px; margin-right: 10px" />{{ row.coverageRate }}%</template></el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="智能助手" name="assistant">
        <ModuleAssistant
          module-key="skill"
          title="技能体系智能助手"
          :quick-prompts="skillAssistantPrompts"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="skillName">
          <el-input v-model="form.skillName" placeholder="如：Java、研发技术" />
        </el-form-item>
        <el-form-item v-if="editingId === null || form.parentId !== 0" label="所属分类">
          <el-select v-model="form.parentId" placeholder="顶级分类不用选" clearable style="width: 100%">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="启用">启用</el-radio>
            <el-radio value="停用">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding-right: 6px;
  font-size: 13px;
}

.tree-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-count {
  flex: none;
  color: #888888;
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
}

:deep(.el-tree-node__content) {
  height: 34px;
  margin: 1px 0;
  border-radius: 8px;
}

:deep(.el-tree-node__content:hover) {
  background: #f5f5f5;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: #ececec;
  color: #111111;
}

:deep(.el-tree-node__children .el-tree-node__content) {
  height: 30px;
}
</style>
