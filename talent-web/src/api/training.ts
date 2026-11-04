import { del, get, post, put } from './request'
import type {
  CourseCategory,
  CourseForm,
  CourseImportResult,
  CourseItem,
  LearningPathForm,
  LearningPathTemplate,
  PageResult,
  TrainingEffect,
  TrainingPlan,
  TrainingRecordForm,
} from './types'

/** 学习计划：岗位必修 - 已完成 */
export function fetchTrainingPlan(employeeId: number, developmentGoal?: string) {
  return get<TrainingPlan>('/training/paths', { employeeId, developmentGoal })
}

/** 已完成的培训 */
export function fetchTrainingRecords(employeeId: number) {
  return get<CourseItem[]>('/training/records', { employeeId })
}

export function fetchCourses(query: { pageNum: number; pageSize: number; keyword?: string; courseType?: string; categoryId?: number }) {
  return get<PageResult<CourseItem>>('/training/courses', query)
}

export function createCourse(form: CourseForm) {
  return post<number>('/training/courses', form)
}

export function updateCourse(id: number, form: CourseForm) {
  return put<void>(`/training/courses/${id}`, form)
}

export function deleteCourse(id: number) {
  return del<void>(`/training/courses/${id}`)
}

export function fetchCourseCategories() {
  return get<CourseCategory[]>('/training/categories')
}

export function createCourseCategory(form: Pick<CourseCategory, 'categoryName' | 'description'>) {
  return post<number>('/training/categories', form)
}

export function updateCourseCategory(id: number, form: Pick<CourseCategory, 'categoryName' | 'description'>) {
  return put<void>(`/training/categories/${id}`, form)
}

export function deleteCourseCategory(id: number) {
  return del<void>(`/training/categories/${id}`)
}

export function importCourses(file: File) {
  const body = new FormData()
  body.append('file', file)
  return post<CourseImportResult>('/training/courses/import', body)
}

export function courseExportUrl(query: { keyword?: string; courseType?: string; categoryId?: number }) {
  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value))
  })
  return `/api/training/courses/export?${params.toString()}`
}

export function fetchLearningPaths() {
  return get<LearningPathTemplate[]>('/training/path-templates')
}

export function createLearningPath(form: LearningPathForm) {
  return post<number>('/training/path-templates', form)
}

export function updateLearningPath(id: number, form: LearningPathForm) {
  return put<void>(`/training/path-templates/${id}`, form)
}

export function deleteLearningPath(id: number) {
  return del<void>(`/training/path-templates/${id}`)
}

export function fetchTrainingEffects() {
  return get<TrainingEffect[]>('/training/effects')
}

export function createTrainingRecord(form: TrainingRecordForm) {
  return post<number>('/training/records', form)
}

export function fetchLearningPathDetail(id: number) {
  return get<LearningPathTemplate>(`/training/path-templates/${id}`)
}

export function importLearningPaths(templates: LearningPathForm[]) {
  return post<number>('/training/path-templates/import', templates)
}
