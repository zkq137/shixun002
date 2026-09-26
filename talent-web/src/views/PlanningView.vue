<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Refresh, Search, View } from '@element-plus/icons-vue'
import { evaluatePositionRisks, fetchPlanningDashboard, fetchPositionDetail, fetchPositionRisks, fetchPositions, fetchRiskEmployee, fetchRiskEmployees, fetchSkillCoverage, fetchSuccession, fetchTalentPools, handleRiskEmployee, refreshSuccession, refreshTalentPools, updateKeyPosition } from '@/api/planning'
import type { PlanningDashboard, PlanningPosition, PositionRisk, RiskEmployee, RiskHandleForm, SkillCoverage, SuccessionCandidate, TalentPool } from '@/api/types'

type PositionKeyFilter = 'all' | 'key' | 'normal'
const activeTab = ref('dashboard')
const dashboard = ref<PlanningDashboard | null>(null)
const dashboardLoading = ref(false)
const positionLoading = ref(false)
const poolLoading = ref(false)
const riskLoading = ref(false)
const riskDetailLoading = ref(false)
const positionRiskLoading = ref(false)
const positions = ref<PlanningPosition[]>([])
const positionKeyword = ref('')
const positionKeyFilter = ref<PositionKeyFilter>('all')
const selectedPosition = ref<PlanningPosition | null>(null)
const positionDialogVisible = ref(false)
const positionCandidates = ref<SuccessionCandidate[]>([])
const positionCoverage = ref<SkillCoverage[]>([])
const positionDetailLoading = ref(false)
const positionRefreshLoading = ref<number | null>(null)
const talentPools = ref<TalentPool[]>([])
const riskList = ref<RiskEmployee[]>([])
const riskTotal = ref(0)
const riskPageNum = ref(1)
const riskPageSize = ref(10)
const riskQuery = reactive({ warningLevel: '', handleStatus: '', department: '', keyword: '' })
const selectedRisk = ref<RiskEmployee | null>(null)
const riskDetailVisible = ref(false)
const riskHandleVisible = ref(false)
const riskForm = reactive<RiskHandleForm>({ handleStatus: '处理中', handler: '', remark: '' })
const riskHandleLoading = ref(false)
const positionRisks = ref<PositionRisk[]>([])
const readinessLabels: Record<string, string> = { READY_NOW: '立即可继任', READY_1_YEAR: '一年内可继任', READY_2_YEARS: '两年内可继任', NOT_READY: '暂不适合' }

function valueOf(value?: number | null) { return value ?? 0 }
function riskTag(level?: string | null): 'success' | 'warning' | 'danger' | 'info' { if (level === '高' || level === '高风险') return 'danger'; if (level === '中' || level === '中风险') return 'warning'; return 'success' }
function readinessTag(value?: string | null): 'success' | 'warning' | 'danger' | 'info' { if (value === 'READY_NOW') return 'success'; if (value === 'READY_1_YEAR') return 'warning'; if (value === 'READY_2_YEARS') return 'info'; return 'danger' }
function readinessLabel(value?: string | null) { return value ? readinessLabels[value] ?? value : '-' }
function formatDate(value?: string | null) { return value ? value.replace('T', ' ').slice(0, 16) : '-' }

async function loadDashboard() { dashboardLoading.value = true; try { dashboard.value = await fetchPlanningDashboard() } finally { dashboardLoading.value = false } }
async function loadPositions() { positionLoading.value = true; try { positions.value = await fetchPositions({ keyword: positionKeyword.value || undefined, key: positionKeyFilter.value === 'all' ? null : positionKeyFilter.value === 'key' }) } finally { positionLoading.value = false } }
async function loadPools() { poolLoading.value = true; try { talentPools.value = await fetchTalentPools() } finally { poolLoading.value = false } }
async function loadPositionRisks() { positionRiskLoading.value = true; try { positionRisks.value = await fetchPositionRisks() } finally { positionRiskLoading.value = false } }
async function loadRisks() { riskLoading.value = true; try { const page = await fetchRiskEmployees({ pageNum: riskPageNum.value, pageSize: riskPageSize.value, warningLevel: riskQuery.warningLevel || undefined, handleStatus: riskQuery.handleStatus || undefined, department: riskQuery.department || undefined, keyword: riskQuery.keyword || undefined }); riskList.value = page.records; riskTotal.value = page.total } finally { riskLoading.value = false } }

async function openPosition(position: PlanningPosition) {
  selectedPosition.value = position; positionDialogVisible.value = true; positionDetailLoading.value = true
  try { const [detail, candidates, coverage] = await Promise.all([fetchPositionDetail(position.id), fetchSuccession(position.id, 50), fetchSkillCoverage(position.id)]); selectedPosition.value = detail; positionCandidates.value = candidates; positionCoverage.value = coverage } finally { positionDetailLoading.value = false }
}
async function toggleKey(position: PlanningPosition) { await updateKeyPosition(position.id, !position.key); ElMessage.success(position.key ? '已取消关键岗位标记' : '已标记为关键岗位'); await Promise.all([loadPositions(), loadDashboard()]) }
async function refreshPosition(position: PlanningPosition) { positionRefreshLoading.value = position.id; try { const candidates = await refreshSuccession(position.id); ElMessage.success('继任候选快照已刷新'); if (selectedPosition.value?.id === position.id) positionCandidates.value = candidates; await loadDashboard() } finally { positionRefreshLoading.value = null } }
async function refreshPools() { poolLoading.value = true; try { talentPools.value = await refreshTalentPools(); await loadDashboard(); ElMessage.success('人才池统计已刷新') } finally { poolLoading.value = false } }
async function evaluateRisks() { positionRiskLoading.value = true; try { positionRisks.value = await evaluatePositionRisks(); await Promise.all([loadDashboard(), loadPositions()]); ElMessage.success('岗位风险已重新评估') } finally { positionRiskLoading.value = false } }
async function showRiskDetail(row: RiskEmployee) { if (!row.id) return; riskDetailVisible.value = true; riskDetailLoading.value = true; try { selectedRisk.value = await fetchRiskEmployee(row.id) } finally { riskDetailLoading.value = false } }
function openRiskHandle(row: RiskEmployee) { selectedRisk.value = row; riskForm.handleStatus = row.handleStatus ?? '处理中'; riskForm.handler = row.handler ?? ''; riskForm.remark = row.handleRemark ?? ''; riskHandleVisible.value = true }
async function saveRiskHandle() { if (!selectedRisk.value?.id || !riskForm.handler.trim()) { ElMessage.warning('请填写处理人'); return }; riskHandleLoading.value = true; try { await handleRiskEmployee(selectedRisk.value.id, { handleStatus: riskForm.handleStatus, handler: riskForm.handler.trim(), remark: riskForm.remark?.trim() }); riskHandleVisible.value = false; ElMessage.success('预警处理结果已保存'); await Promise.all([loadRisks(), loadDashboard()]) } finally { riskHandleLoading.value = false } }
function searchRisks() { riskPageNum.value = 1; loadRisks() }
async function loadAll() { await Promise.all([loadDashboard(), loadPositions(), loadPools(), loadRisks(), loadPositionRisks()]) }
onMounted(loadAll)
</script>

<template>
  <div>
    <h2 class="page-title">梯队规划与预测</h2>
    <p class="page-tip">围绕关键岗位、继任候选、人才池和风险预警，为梯队建设提供可解释的决策依据。</p>
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="决策看板" name="dashboard">
        <div v-loading="dashboardLoading">
          <el-row v-if="dashboard" :gutter="12">
            <el-col v-for="item in [['岗位总数', dashboard.positionCount], ['关键岗位', dashboard.keyPositionCount], ['无继任岗位', dashboard.uncoveredKeyPositionCount], ['立即可继任', dashboard.readyNowCandidateCount], ['人才池缺口', dashboard.totalTalentGap], ['高风险员工', dashboard.highRiskEmployeeCount], ['未处理预警', dashboard.unhandledWarningCount]]" :key="item[0]" :span="3"><el-card shadow="never" style="margin-bottom: 12px"><div class="stat-label">{{ item[0] }}</div><div class="stat-value">{{ valueOf(item[1] as number) }}</div></el-card></el-col>
            <el-col :span="3"><el-card shadow="never" style="margin-bottom: 12px"><div class="stat-label">关键岗位覆盖率</div><div class="stat-value">{{ valueOf(dashboard.keyPositionCoverageRate) }}%</div></el-card></el-col>
          </el-row>
          <el-empty v-else description="暂无看板数据" />
          <el-row v-if="dashboard" :gutter="16">
            <el-col :span="12"><el-card shadow="never"><template #header>候选人准备度分布</template><div v-for="key in ['READY_NOW', 'READY_1_YEAR', 'READY_2_YEARS', 'NOT_READY']" :key="key" class="dist-row"><span class="dist-name">{{ readinessLabel(key) }}</span><el-progress :percentage="valueOf(dashboard.readinessDistribution?.[key])" :show-text="false" style="flex: 1" /><span class="dist-count">{{ valueOf(dashboard.readinessDistribution?.[key]) }} 人</span></div></el-card></el-col>
            <el-col :span="12"><el-card shadow="never"><template #header>高风险岗位</template><el-table :data="dashboard.highRiskPositions ?? []" stripe max-height="260"><el-table-column prop="positionName" label="岗位" min-width="130" /><el-table-column prop="riskLevel" label="等级" width="80"><template #default="{ row }"><el-tag :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag></template></el-table-column><el-table-column prop="riskDescription" label="触发原因" min-width="220" /><template #empty><el-empty description="暂无高风险岗位" :image-size="55" /></template></el-table></el-card></el-col>
          </el-row>
          <el-card v-if="dashboard" shadow="never" style="margin-top: 16px"><template #header>各层级人才池缺口</template><el-table :data="dashboard.talentPoolGaps ?? []" stripe><el-table-column prop="level" label="岗位层级" /><el-table-column prop="totalCapacity" label="目标容量" /><el-table-column prop="currentCount" label="当前储备" /><el-table-column prop="gapCount" label="缺口" /><el-table-column label="覆盖率"><template #default="{ row }">{{ valueOf(row.coverageRate) }}%</template></el-table-column><template #empty><el-empty description="暂无人才池统计" :image-size="55" /></template></el-table></el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="关键岗位" name="positions">
        <el-card shadow="never"><div class="search-bar"><el-input v-model="positionKeyword" clearable placeholder="按岗位名称搜索" style="width: 260px" @keyup.enter="loadPositions" /><el-select v-model="positionKeyFilter" style="width: 150px" @change="loadPositions"><el-option label="全部岗位" value="all" /><el-option label="关键岗位" value="key" /><el-option label="普通岗位" value="normal" /></el-select><el-button type="primary" :icon="Search" @click="loadPositions">查询</el-button></div></el-card>
        <el-card shadow="never" style="margin-top: 16px"><el-table v-loading="positionLoading" :data="positions" stripe border><el-table-column prop="positionName" label="岗位" min-width="150" /><el-table-column prop="positionLevel" label="层级" width="110" /><el-table-column label="关键岗位" width="100"><template #default="{ row }"><el-switch :model-value="row.key" @change="toggleKey(row)" /></template></el-table-column><el-table-column prop="incumbentCount" label="在岗人数" width="95" /><el-table-column prop="successorCount" label="合格继任" width="95" /><el-table-column prop="readyNowCount" label="立即可继任" width="110" /><el-table-column label="风险" width="85"><template #default="{ row }"><el-tag v-if="row.riskLevel" :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag><span v-else>-</span></template></el-table-column><el-table-column label="操作" width="220" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="View" @click="openPosition(row)">详情</el-button><el-button link type="primary" :icon="Refresh" :loading="positionRefreshLoading === row.id" @click="refreshPosition(row)">刷新候选</el-button></template></el-table-column><template #empty><el-empty description="暂无岗位数据" /></template></el-table></el-card>
      </el-tab-pane>

      <el-tab-pane label="人才池" name="pools">
        <el-card shadow="never"><div class="search-bar"><el-button type="primary" :icon="Refresh" :loading="poolLoading" @click="refreshPools">刷新人才池</el-button><span class="page-tip" style="margin: 0">统计基于最新继任候选快照</span></div></el-card>
        <el-card shadow="never" style="margin-top: 16px"><el-table v-loading="poolLoading" :data="talentPools" stripe border><el-table-column prop="level" label="岗位层级" min-width="130" /><el-table-column prop="totalCapacity" label="目标容量" /><el-table-column prop="currentCount" label="当前合格候选" /><el-table-column prop="gapCount" label="人才缺口" /><el-table-column label="覆盖率"><template #default="{ row }">{{ valueOf(row.coverageRate) }}%</template></el-table-column><el-table-column prop="readyNowCount" label="立即可继任" /><el-table-column prop="readyOneYearCount" label="一年内可继任" /><el-table-column label="更新时间" min-width="150"><template #default="{ row }">{{ formatDate(row.updatedAt) }}</template></el-table-column><template #empty><el-empty description="暂无人才池数据，请先刷新" /></template></el-table></el-card>
      </el-tab-pane>

      <el-tab-pane label="风险预警" name="risks">
        <el-card shadow="never"><div class="search-bar"><el-input v-model="riskQuery.keyword" clearable placeholder="姓名或工号" style="width: 190px" /><el-input v-model="riskQuery.department" clearable placeholder="部门" style="width: 150px" /><el-select v-model="riskQuery.warningLevel" clearable placeholder="预警等级" style="width: 130px"><el-option label="高风险" value="高" /><el-option label="中风险" value="中" /><el-option label="低风险" value="低" /></el-select><el-select v-model="riskQuery.handleStatus" clearable placeholder="处理状态" style="width: 130px"><el-option label="未处理" value="未处理" /><el-option label="处理中" value="处理中" /><el-option label="已处理" value="已处理" /></el-select><el-button type="primary" :icon="Search" @click="searchRisks">查询</el-button></div></el-card>
        <el-card shadow="never" style="margin-top: 16px"><template #header>员工流失风险</template><el-table v-loading="riskLoading" :data="riskList" stripe border><el-table-column prop="employeeName" label="姓名" width="100" /><el-table-column prop="empNo" label="工号" width="120" /><el-table-column prop="department" label="部门" width="130" /><el-table-column prop="currentPosition" label="当前岗位" min-width="130" /><el-table-column prop="riskScore" label="风险分" width="90" /><el-table-column label="等级" width="90"><template #default="{ row }"><el-tag :type="riskTag(row.warningLevel)">{{ row.warningLevel ?? '-' }}</el-tag></template></el-table-column><el-table-column prop="handleStatus" label="处理状态" width="105" /><el-table-column label="操作" width="170" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="View" @click="showRiskDetail(row)">详情</el-button><el-button link type="primary" :icon="EditPen" @click="openRiskHandle(row)">处理</el-button></template></el-table-column><template #empty><el-empty description="暂无流失风险预警" /></template></el-table><el-pagination v-if="riskTotal > 0" v-model:current-page="riskPageNum" v-model:page-size="riskPageSize" style="margin-top: 16px; justify-content: flex-end" layout="total, sizes, prev, pager, next" :total="riskTotal" @current-change="loadRisks" @size-change="searchRisks" /></el-card>
        <el-card shadow="never" style="margin-top: 16px"><template #header><div style="display: flex; align-items: center; justify-content: space-between"><span>岗位断层风险</span><el-button type="primary" plain :icon="Refresh" :loading="positionRiskLoading" @click="evaluateRisks">重新评估</el-button></div></template><el-table v-loading="positionRiskLoading" :data="positionRisks" stripe border><el-table-column prop="positionName" label="岗位" min-width="140" /><el-table-column prop="positionLevel" label="层级" width="100" /><el-table-column label="关键岗位" width="100"><template #default="{ row }">{{ row.key ? '是' : '否' }}</template></el-table-column><el-table-column label="风险" width="85"><template #default="{ row }"><el-tag :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag></template></el-table-column><el-table-column prop="riskDescription" label="触发原因" min-width="260" /><el-table-column prop="incumbentCount" label="在岗" width="70" /><el-table-column prop="successorCount" label="继任" width="70" /><el-table-column prop="readyNowCount" label="立即继任" width="95" /><el-table-column label="技能覆盖" width="100"><template #default="{ row }">{{ valueOf(row.skillCoverageRate) }}%</template></el-table-column><template #empty><el-empty description="暂无岗位风险记录，请先评估" /></template></el-table></el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="positionDialogVisible" :title="selectedPosition ? `${selectedPosition.positionName} · 岗位画像` : '岗位画像'" width="900px"><div v-loading="positionDetailLoading"><el-descriptions v-if="selectedPosition" :column="4" border><el-descriptions-item label="岗位层级">{{ selectedPosition.positionLevel ?? '-' }}</el-descriptions-item><el-descriptions-item label="在岗人数">{{ valueOf(selectedPosition.incumbentCount) }}</el-descriptions-item><el-descriptions-item label="合格继任">{{ valueOf(selectedPosition.successorCount) }}</el-descriptions-item><el-descriptions-item label="立即可继任">{{ valueOf(selectedPosition.readyNowCount) }}</el-descriptions-item></el-descriptions><h3 style="font-size: 15px; margin: 18px 0 10px">技能覆盖</h3><el-table :data="positionCoverage" stripe max-height="220"><el-table-column prop="skillName" label="技能" /><el-table-column prop="requiredCount" label="要求人数" /><el-table-column prop="qualifiedCount" label="具备人数" /><el-table-column prop="gapCount" label="缺口" /><el-table-column label="覆盖率"><template #default="{ row }">{{ valueOf(row.coverageRate) }}%</template></el-table-column><template #empty><el-empty description="岗位未配置技能要求" :image-size="50" /></template></el-table><h3 style="font-size: 15px; margin: 18px 0 10px">继任候选</h3><el-table :data="positionCandidates" stripe max-height="300"><el-table-column prop="employeeName" label="姓名" width="90" /><el-table-column prop="department" label="部门" width="110" /><el-table-column label="综合分" width="90"><template #default="{ row }">{{ valueOf(row.matchScore) }}</template></el-table-column><el-table-column label="评分明细" min-width="240"><template #default="{ row }">技能 {{ valueOf(row.skillScore) }} / 绩效 {{ valueOf(row.performanceScore) }} / 潜力 {{ valueOf(row.potentialScore) }} / 经验 {{ valueOf(row.experienceScore) }}</template></el-table-column><el-table-column label="准备度" width="120"><template #default="{ row }"><el-tag :type="readinessTag(row.readiness)">{{ readinessLabel(row.readiness) }}</el-tag></template></el-table-column><el-table-column label="缺失技能" min-width="160"><template #default="{ row }">{{ row.missingSkills?.join('、') || '-' }}</template></el-table-column><template #empty><el-empty description="暂无继任候选" :image-size="50" /></template></el-table></div></el-dialog>
    <el-dialog v-model="riskDetailVisible" title="流失预警详情" width="560px"><el-descriptions v-loading="riskDetailLoading" v-if="selectedRisk" :column="2" border><el-descriptions-item label="姓名">{{ selectedRisk.employeeName }}</el-descriptions-item><el-descriptions-item label="工号">{{ selectedRisk.empNo }}</el-descriptions-item><el-descriptions-item label="部门">{{ selectedRisk.department }}</el-descriptions-item><el-descriptions-item label="岗位">{{ selectedRisk.currentPosition }}</el-descriptions-item><el-descriptions-item label="风险分">{{ selectedRisk.riskScore }}</el-descriptions-item><el-descriptions-item label="预警等级">{{ selectedRisk.warningLevel }}</el-descriptions-item><el-descriptions-item label="处理状态">{{ selectedRisk.handleStatus }}</el-descriptions-item><el-descriptions-item label="处理人">{{ selectedRisk.handler || '-' }}</el-descriptions-item><el-descriptions-item label="处理备注" :span="2">{{ selectedRisk.handleRemark || '-' }}</el-descriptions-item></el-descriptions></el-dialog>
    <el-dialog v-model="riskHandleVisible" title="处理流失预警" width="500px"><el-form label-width="80px"><el-form-item label="处理状态"><el-select v-model="riskForm.handleStatus" style="width: 100%"><el-option label="未处理" value="未处理" /><el-option label="处理中" value="处理中" /><el-option label="已处理" value="已处理" /></el-select></el-form-item><el-form-item label="处理人"><el-input v-model="riskForm.handler" maxlength="64" /></el-form-item><el-form-item label="处理备注"><el-input v-model="riskForm.remark" type="textarea" maxlength="500" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="riskHandleVisible = false">取消</el-button><el-button type="primary" :loading="riskHandleLoading" @click="saveRiskHandle">保存</el-button></template></el-dialog>
  </div>
</template>
