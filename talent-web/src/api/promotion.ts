import { get } from './request'
import type { PromotionCandidate } from './types'

/** 目标岗位的晋升候选人，按综合分排序 */
export function fetchPromotionCandidates(positionId: number, limit = 10) {
  return get<PromotionCandidate[]>('/promotion/candidates', { positionId, limit })
}

/** 绩效潜力矩阵使用完整候选池，不受排行上限影响 */
export function fetchPromotionMatrixCandidates(positionId: number) {
  return get<PromotionCandidate[]>('/promotion/candidates/matrix', { positionId })
}
