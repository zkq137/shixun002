import { get, post, put } from './request'
import type {
  PageResult,
  PlanningDashboard,
  PlanningPosition,
  PositionRisk,
  RiskEmployee,
  RiskEmployeeQuery,
  RiskHandleForm,
  SkillCoverage,
  SuccessionCandidate,
  TalentPool,
} from './types'

export function fetchPositions(params: { keyword?: string; key?: boolean | null } = {}) {
  return get<PlanningPosition[]>('/planning/positions', params)
}

export function fetchPositionDetail(id: number) {
  return get<PlanningPosition>(`/planning/positions/${id}`)
}

export function updateKeyPosition(id: number, key: boolean) {
  return put<void>(`/planning/positions/${id}/key`, { key })
}

export function fetchKeyPositions() {
  return get<PlanningPosition[]>('/planning/key-positions')
}

/** 关键岗位的继任候选人，按技能匹配度排序 */
export function fetchSuccession(positionId: number, limit = 10) {
  return get<SuccessionCandidate[]>('/planning/succession', { positionId, limit })
}

export function refreshSuccession(positionId: number) {
  return post<SuccessionCandidate[]>(`/planning/succession/refresh?positionId=${positionId}`)
}

/** 流失风险名单 */
export function fetchRiskEmployees(query: RiskEmployeeQuery = {}) {
  return get<PageResult<RiskEmployee>>('/planning/risks/employees', {
    pageNum: query.pageNum ?? 1,
    pageSize: query.pageSize ?? 10,
    warningLevel: query.warningLevel,
    handleStatus: query.handleStatus,
    department: query.department,
    keyword: query.keyword,
  })
}

export function fetchRiskEmployee(id: number) {
  return get<RiskEmployee>(`/planning/risks/employees/${id}`)
}

export function handleRiskEmployee(id: number, form: RiskHandleForm) {
  return put<void>(`/planning/risks/employees/${id}/handle`, form)
}

/** 岗位核心技能的全公司覆盖人数 */
export function fetchSkillCoverage(positionId: number) {
  return get<SkillCoverage[]>('/planning/skill-coverage', { positionId })
}

export function fetchPlanningDashboard() {
  return get<PlanningDashboard>('/planning/dashboard')
}

export function fetchTalentPools() {
  return get<TalentPool[]>('/planning/talent-pools')
}

export function fetchTalentPool(level: string) {
  return get<TalentPool>(`/planning/talent-pools/${encodeURIComponent(level)}`)
}

export function refreshTalentPools() {
  return post<TalentPool[]>('/planning/talent-pools/refresh')
}

export function fetchPositionRisks() {
  return get<PositionRisk[]>('/planning/risks/positions')
}

export function evaluatePositionRisks() {
  return post<PositionRisk[]>('/planning/risks/positions/evaluate')
}

/** 跨服务调用示例：从员工服务取档案 */
export function fetchEmployeeProfile(id: number) {
  return get<Record<string, unknown>>(`/planning/employee/${id}/profile`)
}
