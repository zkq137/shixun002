import { get } from './request'
import type { CourseItem, LearningPath, PageResult, TrainingEffect, TrainingPlan } from './types'

/** 学习计划：岗位必修 - 已完成 */
export function fetchTrainingPlan(employeeId: number) {
  return get<TrainingPlan>('/training/paths', { employeeId })
}

/** 已完成的培训 */
export function fetchTrainingRecords(employeeId: number) {
  return get<CourseItem[]>('/training/records', { employeeId })
}

/** 课程库分页 */
export function fetchCourses(params: {
  pageNum?: number
  pageSize?: number
  keyword?: string
  courseType?: string
}) {
  return get<PageResult<CourseItem>>('/training/courses', params as Record<string, unknown>)
}

/** 学习路径模板 */
export function fetchLearningPaths() {
  return get<LearningPath[]>('/training/path-templates')
}

/** 学习路径详情 */
export function fetchLearningPathDetail(id: number) {
  return get<LearningPath>(`/training/path-templates/${id}`)
}

/** 课程培训效果汇总 */
export function fetchTrainingEffects() {
  return get<TrainingEffect[]>('/training/effects')
}
