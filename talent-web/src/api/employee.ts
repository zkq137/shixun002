import { del, get, post, put } from './request'
import type {
  BatchResult,
  EmployeeDetail,
  EmployeeBatchUpdateForm,
  EmployeeForm,
  EmployeeQuery,
  EmployeeRow,
  NameValue,
  PageResult,
  PositionOption,
  SkillItem,
} from './types'

/** 分页查询员工 */
export function fetchEmployeePage(query: EmployeeQuery) {
  return get<PageResult<EmployeeRow>>('/employee/page', query as Record<string, unknown>)
}

/** 员工详情（含薪酬、技能、培训） */
export function fetchEmployeeDetail(id: number) {
  return get<EmployeeDetail>(`/employee/${id}`)
}

/** 新增员工，返回新 id */
export function createEmployee(form: EmployeeForm) {
  return post<number>('/employee', form)
}

/** 修改员工 */
export function updateEmployee(id: number, form: EmployeeForm) {
  return put<void>(`/employee/${id}`, form)
}

/** 删除员工 */
export function deleteEmployee(id: number) {
  return del<void>(`/employee/${id}`)
}

/** 部门下拉 */
export function fetchDepartments() {
  return get<string[]>('/employee/options/departments')
}

/** 岗位下拉 */
export function fetchPositionOptions() {
  return get<PositionOption[]>('/employee/options/positions')
}

/** 按部门统计人数 */
export function fetchDepartmentStats() {
  return get<NameValue[]>('/employee/stats/department')
}

/** 批量删除 */
export function batchDeleteEmployees(ids: number[]) {
  return post<BatchResult>('/employee/batch/delete', { ids })
}

/** 批量修改部门 / 状态 / 办公方式 / 职级 */
export function batchUpdateEmployees(form: EmployeeBatchUpdateForm) {
  return put<BatchResult>('/employee/batch/update', form)
}

/** 某员工会的技能 */
export function fetchEmployeeSkills(id: number) {
  return get<SkillItem[]>(`/employee/${id}/skills`)
}

/** 给员工配技能（整体覆盖） */
export function updateEmployeeSkills(id: number, skillIds: number[]) {
  return put<void>(`/employee/${id}/skills`, { skillIds })
}

/** 导入接口地址（给 el-upload 用） */
export const IMPORT_URL = '/api/employee/import'

/**
 * 导出地址：导出的是当前筛选条件下的全部数据，不只是当前页。
 * 直接让浏览器下载，所以拼成 URL 用 window.open 打开。
 */
export function employeeExportUrl(query: EmployeeQuery) {
  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '' && key !== 'pageNum' && key !== 'pageSize') {
      params.append(key, String(value))
    }
  })
  return `/api/employee/export?${params.toString()}`
}

/** 导入模板地址 */
export const IMPORT_TEMPLATE_URL = '/api/employee/import-template'
