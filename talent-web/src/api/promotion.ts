import { get } from './request'
import type { PromotionCandidate } from './types'

/** 目标岗位的晋升候选人，按综合分排序 */
export function fetchPromotionCandidates(positionId: number, limit = 10) {
  return get<PromotionCandidate[]>('/promotion/candidates', { positionId, limit })
}
