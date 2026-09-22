import { get } from './request'
import type { NameValue, RiskEmployee, SuccessionCandidate } from './types'

/** 关键岗位的继任候选人，按技能匹配度排序 */
export function fetchSuccession(positionId: number, limit = 10) {
  return get<SuccessionCandidate[]>('/planning/succession', { positionId, limit })
}

/** 流失风险名单 */
export function fetchRiskEmployees(limit = 10) {
  return get<RiskEmployee[]>('/planning/risk', { limit })
}

/** 岗位核心技能的全公司覆盖人数 */
export function fetchSkillCoverage(positionId: number) {
  return get<NameValue[]>('/planning/skill-coverage', { positionId })
}

/** 跨服务调用示例：从员工服务取档案 */
export function fetchEmployeeProfile(id: number) {
  return get<Record<string, unknown>>(`/planning/employee/${id}/profile`)
}
