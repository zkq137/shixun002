<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createSkill,
  deleteSkill,
  fetchSkillTree,
  searchSkills,
  updateSkill,
} from '@/api/skill'
import type { SkillForm, SkillItem, SkillNode } from '@/api/types'

const loading = ref(false)
const tree = ref<SkillNode[]>([])
const treeRef = ref()
const filterText = ref('')
const selected = ref<SkillNode | null>(null)
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

onMounted(() => loadTree(false))
</script>

<template>
  <div>
    <h2 class="page-title">技能体系</h2>
    <p class="page-tip">
      企业标准化技能树：分类 → 子技能，共 {{ categoryOptions.length }} 个分类、{{ totalSkills }} 个技能。
      员工会什么、岗位要求什么，都从这棵树里选，保证全公司叫法统一。
    </p>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card v-loading="loading" shadow="never">
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
            default-expand-all
            highlight-current
            style="max-height: 620px; overflow: auto"
            @node-click="onNodeClick"
          >
            <template #default="{ data }">
              <span style="display: flex; align-items: center; gap: 6px; flex: 1">
                <span>{{ data.skillName }}</span>
                <el-tag v-if="data.level === 1" size="small" type="info">
                  {{ data.children.length }}
                </el-tag>
                <el-tag v-else size="small" effect="plain">
                  {{ data.employeeCount ?? 0 }} 人 / {{ data.positionCount ?? 0 }} 岗
                </el-tag>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <el-col :span="16">
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

          <el-table v-loading="childrenLoading" :data="children" stripe border height="560">
            <el-table-column prop="skillName" label="技能名称" min-width="160" />
            <el-table-column prop="description" label="说明" min-width="160" />
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
