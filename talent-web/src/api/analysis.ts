import { del, get, post } from './request'
import type {
  Dashboard,
  FlowForecastData,
  FlowTrendData,
  ModelEffectData,
  ModelArtifact,
  ModelPrediction,
  ModelLogItem,
  ModelTrainResult,
  PositionRecommendation,
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

/** 生成未来人才流动预测 */
export function generateFlowForecast(horizonMonths: number) {
  return post<FlowForecastData>('/analysis/forecast/generate', undefined, {
    params: { horizonMonths },
  })
}

/** 最近一次人才流动预测 */
export function fetchLatestFlowForecast() {
  return get<FlowForecastData | null>('/analysis/forecast/latest')
}

export const FLOW_FORECAST_EXPORT_URL = '/api/analysis/forecast/export'
export const FLOW_FORECAST_IMPORT_URL = '/api/analysis/forecast/import'

/** 模型运行效果 */
export function fetchModelEffect() {
  return get<ModelEffectData>('/analysis/model/effect')
}

/** 训练全部人才模型 */
export function trainAllModels() {
  return post<ModelTrainResult[]>('/analysis/model/train-all')
}

/** 训练指定模型 */
export function trainModel(modelCode: string) {
  return post<ModelTrainResult>(`/analysis/model/train/${modelCode}`)
}

/** 最新已部署模型 */
export function fetchModelArtifacts() {
  return get<ModelArtifact[]>('/analysis/model/artifacts')
}

/** 流失风险预测 */
export function predictAttrition(employee: string | number) {
  return get<ModelPrediction>(`/analysis/model/predict/attrition/${encodeURIComponent(employee)}`)
}

/** 高潜人才预测 */
export function predictHighPotential(employee: string | number) {
  return get<ModelPrediction>(`/analysis/model/predict/potential/${encodeURIComponent(employee)}`)
}

/** 岗位匹配推荐 */
export function recommendPositions(employee: string | number, topN = 5) {
  return get<PositionRecommendation>(
    `/analysis/model/recommend/positions/${encodeURIComponent(employee)}`,
    { topN },
  )
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
