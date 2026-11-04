<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { get, post } from '@/api/request'
const props = defineProps<{ simulation: { learners: number; attendRate: number; passRate: number; improveScore: number } }>()
interface Dimension { dimension: string; name: string; recordCount: number; employeeCount: number; attendRate: number | null; passRate: number | null; improveScore: number | null }
interface Report { generatedAt: string; note: string; dimensions: Dimension[]; simulation?: { estimatedAttendees: number; estimatedPassed: number; totalImprovement: number; overallPassRate: number } }
const report = ref<Report | null>(null)
const dimension = ref('总体')
const loading = ref(false)
const exporting = ref(false)
const rows = computed(() => report.value?.dimensions.filter(row => row.dimension === dimension.value) ?? [])
async function load(simulate = false) { loading.value = true; try { report.value = simulate ? await post<Report>('/training/reports/simulate', props.simulation) : await get<Report>('/training/reports') } finally { loading.value = false } }
async function download(format: 'xlsx' | 'pdf') {
  exporting.value = true
  try {
    const blob = await post<Blob>('/training/reports/export?format=' + format, props.simulation, { responseType: 'blob', timeout: 60000 })
    if (blob.type.includes('json')) { const failure = JSON.parse(await blob.text()); throw new Error(failure.message || '报告导出失败') }
    const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = 'training-report.' + format; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
    ElMessage.success('报告已导出，包含历史统计和当前模拟假设')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '报告导出失败') } finally { exporting.value = false }
}
onMounted(() => load())
defineExpose({ refresh: () => load() })
</script>
<template>
  <section class="report" v-loading="loading">
    <div class="toolbar"><h3>多维度效果报告</h3><div><el-button @click="load(true)">生成模拟报告</el-button><el-button :loading="exporting" @click="download('xlsx')">导出 Excel</el-button><el-button :loading="exporting" @click="download('pdf')">导出 PDF</el-button></div></div>
    <p class="note">{{ report?.note }} 生成时间：{{ report?.generatedAt?.replace('T', ' ') ?? '—' }}</p>
    <el-radio-group v-model="dimension"><el-radio-button v-for="item in ['总体', '课程', '部门', '岗位', '分类']" :key="item" :value="item">{{ item }}</el-radio-button></el-radio-group>
    <el-table :data="rows" stripe><el-table-column prop="name" label="统计对象" min-width="180" /><el-table-column prop="recordCount" label="培训记录数" /><el-table-column prop="employeeCount" label="去重员工数" /><el-table-column prop="attendRate" label="参与率（%）" /><el-table-column prop="passRate" label="通过率（%）" /><el-table-column prop="improveScore" label="能力提升分值" /></el-table>
    <el-alert v-if="report?.simulation" :closable="false" type="success" :title="'本次模拟：预计参与 ' + report.simulation.estimatedAttendees + ' 人，通过 ' + report.simulation.estimatedPassed + ' 人，总提升 ' + report.simulation.totalImprovement + ' 分，整体通过率 ' + report.simulation.overallPassRate + '%'" />
    <p class="note">导出采用下方当前模拟输入。PDF 为分页图像报告，支持中文；可编辑数值请使用 Excel。</p>
  </section>
</template>
<style scoped>.report{padding:18px;border:1px solid #dce5e8;border-radius:6px;background:#fff}.toolbar{display:flex;justify-content:space-between;gap:16px;flex-wrap:wrap;align-items:center}.toolbar h3{color:#203c57;margin:0}.note{font-size:12px;color:#72808a;line-height:1.7}.el-table{margin:16px 0}</style>
