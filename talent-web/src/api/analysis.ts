import { del, get, post } from './request'
import type {
  Dashboard,
  FlowTrendData,
  ModelEffectData,
  ModelLogItem,
  PageResult,
  PipelineData,
  ReportOption,
  ReportRequest,
  ReportResult,
  SavedReport,
} from './types'

/** 人才盘点看板 */
export function fetchDashboard() {
  return get<Dashboard>('/analysis/dashboard')
}

/** 梯队整体状态：结构 + 质量 + 风险 + 健康度 */
export function fetchPipeline() {
  return get<PipelineData>('/analysis/pipeline')
}

/** 人才流动趋势 */
export function fetchFlowTrend() {
  return get<FlowTrendData>('/analysis/flow-trend')
}

/** 模型运行效果 */
export function fetchModelEffect() {
  return get<ModelEffectData>('/analysis/model/effect')
}

/** 模型训练日志分页 */
export function fetchModelLogs(params: {
  pageNum?: number
  pageSize?: number
  modelName?: string
  status?: string
  version?: string
}) {
  return get<PageResult<ModelLogItem>>('/analysis/model/logs', params as Record<string, unknown>)
}

/** 训练日志详情（溯源） */
export function fetchModelLogDetail(id: number) {
  return get<ModelLogItem>(`/analysis/model/logs/${id}`)
}

/** 自定义报表可选的维度和指标 */
export function fetchReportOptions() {
  return get<ReportOption[]>('/analysis/report/options')
}

/** 生成报表 */
export function generateReport(body: ReportRequest) {
  return post<ReportResult>('/analysis/report/generate', body)
}

/** 按模板生成报表 */
export function generateReportFromTemplate(templateId: number) {
  return post<ReportResult>(`/analysis/report/from-template/${templateId}`)
}

/** 历史报表 */
export function fetchReportHistory() {
  return get<SavedReport[]>('/analysis/report/history')
}

/** 报表详情 */
export function fetchReportDetail(id: number) {
  return get<SavedReport>(`/analysis/report/${id}`)
}

export function deleteReport(id: number) {
  return del<void>(`/analysis/report/${id}`)
}

/** 报表模板 */
export function fetchReportTemplates() {
  return get<SavedReport[]>('/analysis/report/templates')
}

export function saveReportTemplate(body: ReportRequest) {
  return post<number>('/analysis/report/templates', body)
}

export function deleteReportTemplate(id: number) {
  return del<void>(`/analysis/report/templates/${id}`)
}
