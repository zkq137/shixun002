<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put, del } from '@/api/request'
interface Algorithm { id: number; name: string; skillWeight: number; positionWeight: number; goalWeight: number; matchThreshold: number; enabled: boolean; createdAt?: string }
const emit = defineEmits<{ changed: [] }>()
const rows = ref<Algorithm[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const editing = ref<number | null>(null)
const form = reactive({ name: '', skillWeight: 60, positionWeight: 25, goalWeight: 15, matchThreshold: 40, enabled: false })
async function load() { loading.value = true; try { rows.value = await get<Algorithm[]>('/training/algorithms') } finally { loading.value = false } }
function open(row?: Algorithm) {
  editing.value = row?.id ?? null
  Object.assign(form, row ? { name: row.name, skillWeight: row.skillWeight, positionWeight: row.positionWeight, goalWeight: row.goalWeight, matchThreshold: row.matchThreshold, enabled: row.enabled } : { name: '', skillWeight: 60, positionWeight: 25, goalWeight: 15, matchThreshold: 40, enabled: rows.value.length === 0 })
  dialog.value = true
}
async function save() {
  if (!form.name.trim()) return void ElMessage.warning('请输入方案名称')
  if (Math.abs(form.skillWeight + form.positionWeight + form.goalWeight - 100) > 0.001) return void ElMessage.warning('三个权重之和必须为 100')
  saving.value = true
  try { if (editing.value === null) await post('/training/algorithms', form); else await put('/training/algorithms/' + editing.value, form)
    dialog.value = false; await load(); emit('changed'); ElMessage.success('算法配置已保存')
  } finally { saving.value = false }
}
async function activate(row: Algorithm) { await put('/training/algorithms/' + row.id, { ...row, enabled: true }); await load(); emit('changed'); ElMessage.success('已启用，推荐结果将按新参数计算') }
async function remove(row: Algorithm) {
  try { await ElMessageBox.confirm('删除方案“' + row.name + '”？', '删除算法方案', { type: 'warning' }) } catch { return }
  await del('/training/algorithms/' + row.id); await load()
}
onMounted(load)
</script>
<template>
  <section>
    <div class="toolbar"><div><h2>资源匹配参数</h2><p>评分 = 技能匹配比例 × 技能权重 + 岗位适配比例 × 岗位权重 + 培养目标匹配比例 × 目标权重。</p></div><el-button type="primary" @click="open()">新增方案</el-button></div>
    <el-alert title="启用方案全局唯一；达到匹配阈值才推荐补充资源。岗位必修课程始终保留。默认权重是可编辑规则，并非训练所得参数。" type="info" :closable="false" />
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="name" label="方案" min-width="160" /><el-table-column prop="skillWeight" label="技能权重（%）" /><el-table-column prop="positionWeight" label="岗位权重（%）" /><el-table-column prop="goalWeight" label="目标权重（%）" /><el-table-column prop="matchThreshold" label="匹配阈值" />
      <el-table-column label="状态"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '已启用' : '未启用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="190"><template #default="{ row }"><el-button link type="primary" @click="open(row)">编辑</el-button><el-button v-if="!row.enabled" link type="success" @click="activate(row)">启用</el-button><el-button link type="danger" :disabled="row.enabled" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="dialog" :title="editing === null ? '新增算法方案' : '编辑算法方案'" width="520px">
      <el-form label-width="130px"><el-form-item label="方案名称"><el-input v-model="form.name" maxlength="64" /></el-form-item>
        <el-form-item label="技能缺口权重"><el-input-number v-model="form.skillWeight" :min="0" :max="100" :precision="2" /></el-form-item>
        <el-form-item label="岗位适配权重"><el-input-number v-model="form.positionWeight" :min="0" :max="100" :precision="2" /></el-form-item>
        <el-form-item label="培养目标权重"><el-input-number v-model="form.goalWeight" :min="0" :max="100" :precision="2" /></el-form-item>
        <el-form-item label="匹配阈值"><el-input-number v-model="form.matchThreshold" :min="0" :max="100" :precision="2" /></el-form-item>
        <el-form-item label="启用此方案"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form><template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>
<style scoped>.toolbar{display:flex;justify-content:space-between;align-items:center;gap:16px;margin:16px 0}.toolbar h2{font-size:18px;color:#203c57}.toolbar p{font-size:13px;color:#72808a;line-height:1.7}section :deep(.el-table){margin-top:16px}</style>
