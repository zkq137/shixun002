import { get } from './request'
import type { CourseItem, TrainingPlan } from './types'

/** 学习计划：岗位必修 - 已完成 */
export function fetchTrainingPlan(employeeId: number) {
  return get<TrainingPlan>('/training/paths', { employeeId })
}

/** 已完成的培训 */
export function fetchTrainingRecords(employeeId: number) {
  return get<CourseItem[]>('/training/records', { employeeId })
}
