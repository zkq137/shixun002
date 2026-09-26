/** 后端统一返回格式：{ code, message, data, timestamp } */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页结果 */
export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

/** 名称-数值，统计图用 */
export interface NameValue {
  name: string
  value: number
}

/** 员工列表里的一行 */
export interface EmployeeRow {
  id: number
  empNo: string
  name: string
  gender?: string | null
  age?: number | null
  department?: string | null
  positionId?: number | null
  positionName?: string | null
  levelTier?: string | null
  jobRank?: string | null
  managerEmpNo?: string | null
  workMode?: string | null
  tenureYears?: number | null
  status?: string | null
  perfScore?: number | null
  potentialLevel?: string | null
  warningLevel?: string | null
  talentTag?: string | null
}

/** 员工详情 */
export interface EmployeeDetail extends EmployeeRow {
  baseSalary?: number | null
  salaryCoefficient?: number | null
  potentialScore?: number | null
  riskScore?: number | null
  managerName?: string | null
  hireDate?: string | null
  education?: string | null
  phone?: string | null
  skills: string[]
  courses: string[]
}

/** 员工查询条件 */
export interface EmployeeQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  department?: string
  positionId?: number | null
  talentTag?: string
  warningLevel?: string
  status?: string
}

/** 新增/修改员工提交的内容 */
export interface EmployeeForm {
  empNo: string
  name: string
  gender?: string
  age?: number | null
  department?: string
  positionId?: number | null
  levelTier?: string
  jobRank?: string
  managerEmpNo?: string
  workMode?: string
  tenureYears?: number | null
  education?: string
  phone?: string
  status?: string
}

/** 岗位下拉项 */
export interface PositionOption {
  id: number
  positionName: string
  department: string
  levelTier?: string | null
}

/** 继任候选人 */
export interface SuccessionCandidate {
  employeeId: number
  empNo: string
  employeeName?: string | null
  name?: string | null
  department?: string | null
  currentPosition?: string | null
  workYears?: number | null
  matchScore?: number | null
  skillScore?: number | null
  performanceScore?: number | null
  potentialScore?: number | null
  experienceScore?: number | null
  matchedSkillCount?: number
  requiredSkillCount?: number
  missingSkills?: string[]
  readiness?: string | null
  preparationMonths?: number | null
  recommendationReason?: string | null
  dataWarnings?: string[]
  jobRank?: string | null
  levelTier?: string | null
  tenureYears?: number | null
  perfScore?: number | null
  potentialLevel?: string | null
  warningLevel?: string | null
  coreRequire?: number
  matched?: number
}

/** 规划岗位及继任概况 */
export interface PlanningPosition {
  id: number
  positionName: string
  positionLevel?: string | null
  key?: boolean | null
  incumbentCount?: number | null
  successorCount?: number | null
  readyNowCount?: number | null
  successionCoverageRate?: number | null
  riskLevel?: string | null
  riskDescription?: string | null
  skills?: SkillCoverage[]
}

/** 流失风险名单 */
export interface RiskEmployee {
  id?: number
  employeeId: number
  empNo?: string
  employeeName?: string | null
  name?: string | null
  department?: string | null
  currentPosition?: string | null
  riskScore?: number | null
  warningLevel?: string | null
  handleStatus?: string | null
  handler?: string | null
  handleRemark?: string | null
  warningTime?: string | null
  handledAt?: string | null
}

export interface RiskEmployeeQuery {
  pageNum?: number
  pageSize?: number
  warningLevel?: string
  handleStatus?: string
  department?: string
  keyword?: string
}

export interface RiskHandleForm {
  handleStatus: string
  handler: string
  remark?: string
}

export interface SkillCoverage {
  skillId: number
  skillName: string
  requirementType?: string | null
  requiredLevel?: string | null
  requiredCount: number
  qualifiedCount: number
  gapCount: number
  coverageRate: number
}

export interface PlanningDashboard {
  positionCount: number
  keyPositionCount: number
  coveredKeyPositionCount: number
  keyPositionCoverageRate: number
  uncoveredKeyPositionCount: number
  readyNowCandidateCount: number
  totalTalentGap: number
  highRiskEmployeeCount: number
  unhandledWarningCount: number
  readinessDistribution: Record<string, number>
  highRiskPositions?: PositionRisk[]
  talentPoolGaps?: TalentPool[]
}

export interface PositionRisk {
  id: number
  positionId: number
  positionName?: string | null
  positionLevel?: string | null
  key?: boolean | null
  riskLevel?: string | null
  riskDescription?: string | null
  riskScope?: string | null
  incumbentCount?: number | null
  successorCount?: number | null
  readyNowCount?: number | null
  highRiskEmployeeCount?: number | null
  skillCoverageRate?: number | null
  checkedAt?: string | null
}

export interface TalentPool {
  level: string
  totalCapacity: number
  currentCount: number
  gapCount: number
  coverageRate: number
  readyNowCount: number
  readyOneYearCount: number
  updatedAt?: string | null
}

/** 课程条目 */
export interface CourseItem {
  id: number
  courseName: string
  courseType?: string | null
  forPosition?: string | null
  difficulty?: string | null
  duration?: number | null
  completed?: boolean | null
  attendRate?: number | null
  passRate?: number | null
  improveScore?: number | null
  completedAt?: string | null
}

/** 学习计划 */
export interface TrainingPlan {
  employeeId: number
  employeeName: string
  positionName: string
  completedCourses: CourseItem[]
  recommendedCourses: CourseItem[]
  /** 岗位要求但员工还没掌握的技能 */
  skillGaps: string[]
  recommendationReason?: string | null
}

/** 晋升候选人 */
export interface PromotionCandidate {
  employeeId: number
  empNo: string
  name: string
  department?: string | null
  levelTier?: string | null
  jobRank?: string | null
  tenureYears?: number | null
  perfScore?: number | null
  potentialLevel?: string | null
  potentialScore?: number | null
  warningLevel?: string | null
  totalScore?: number | null
}

/** 人才盘点看板 */
export interface Dashboard {
  totalEmployees: number
  positionCount: number
  keyPositionCount: number
  highRiskCount: number
  avgSalary?: number | null
  avgTenure?: number | null
  avgAge?: number | null
  departmentDistribution: NameValue[]
  positionTop: NameValue[]
  talentTagDistribution: NameValue[]
  potentialDistribution: NameValue[]
  warningDistribution: NameValue[]
  perfDistribution: NameValue[]
}

/** 技能树节点（分类或技能，带子节点） */
export interface SkillNode {
  id: number
  skillName: string
  parentId: number
  level: number
  sortOrder?: number | null
  status?: string | null
  description?: string | null
  skillCategory?: string | null
  employeeCount?: number | null
  positionCount?: number | null
  children: SkillNode[]
}

/** 技能节点（平铺，列表用） */
export interface SkillItem {
  id: number
  skillName: string
  parentId: number
  level: number
  sortOrder?: number | null
  status?: string | null
  description?: string | null
  skillCategory?: string | null
  employeeCount?: number | null
  positionCount?: number | null
  childCount?: number | null
}

/** 新增/修改技能提交的内容 */
export interface SkillForm {
  skillName: string
  parentId?: number | null
  description?: string
  status?: string
  sortOrder?: number | null
}

/** 批量操作结果 */
export interface BatchResult {
  total: number
  success: number
  failed: number
}

/** 批量修改提交的内容 */
export interface EmployeeBatchUpdateForm {
  ids: number[]
  department?: string
  status?: string
  workMode?: string
  levelTier?: string
  jobRank?: string
}

/** Excel 导入结果 */
export interface ImportRowError {
  row: number
  empNo: string
  message: string
}

export interface ImportResult {
  total: number
  success: number
  failed: number
  errors: ImportRowError[]
  unknownSkills: string[]
}

/* ==================== 数据分析与报告 ==================== */

/** 梯队整体状态 */
export interface PipelineData {
  totalEmployees: number
  positionCount: number
  keyPositionCount: number
  coreTalentCount: number
  reserveTalentCount: number
  highPotentialCount: number
  avgAge?: number | null
  avgTenure?: number | null
  avgSalary?: number | null
  avgPerf?: number | null
  highRiskCount: number
  middleRiskCount: number
  highRiskRate: number
  healthScore: number
  healthLevel: string
  healthSuggestions: string[]
  departmentDistribution: NameValue[]
  levelTierDistribution: NameValue[]
  positionTypeDistribution: NameValue[]
  ageRangeDistribution: NameValue[]
  tenureRangeDistribution: NameValue[]
  potentialDistribution: NameValue[]
  warningDistribution: NameValue[]
  talentTagDistribution: NameValue[]
  /** 九宫格：3 行（绩效低/中/高）× 3 列（潜力低/中/高） */
  nineBox: number[][]
}

/** 某一批入职员工的现状 */
export interface FlowCohort {
  hireYear: string
  employeeCount: number
  avgTenure?: number | null
  avgPerf?: number | null
  highRiskCount: number
  highRiskRate?: number | null
  coreTalentCount: number
}

/** 人才流动趋势 */
export interface FlowTrendData {
  joinMonthly: NameValue[]
  joinYearly: NameValue[]
  movementByType: NameValue[]
  tenureDistribution: NameValue[]
  riskByTenure: NameValue[]
  cohorts: FlowCohort[]
  recentJoinCount?: number | null
  totalJoin?: number | null
  leaveCount?: number | null
  promoteCount?: number | null
  avgAnnualJoin?: number | null
}

/** 单个模型的效果汇总 */
export interface ModelSummary {
  modelName: string
  modelType?: string | null
  runCount: number
  successCount: number
  latestVersion?: string | null
  latestStatus?: string | null
  latestTrainTime?: string | null
  latestAuc?: number | null
  bestAuc?: number | null
  latestDatasetVersion?: string | null
}

/** 模型运行效果 */
export interface ModelEffectData {
  totalRuns: number
  successRuns: number
  failedRuns: number
  successRate: number
  lastTrainTime?: string | null
  avgAuc?: number | null
  bestModel?: string | null
  bestVersion?: string | null
  bestAuc?: number | null
  modelSummary: ModelSummary[]
  aucTrend: NameValue[]
  durationTrend: NameValue[]
}

/** 一条模型训练日志 */
export interface ModelLogItem {
  id: number
  modelName: string
  modelType?: string | null
  version: string
  trainTime: string
  status: string
  sampleCount?: number | null
  trainDurationMs?: number | null
  metrics?: string | null
  params?: string | null
  datasetVersion?: string | null
  remark?: string | null
}

/** 自定义报表可选维度/指标 */
export interface ReportOption {
  key: string
  label: string
  type: 'dimension' | 'metric'
}

export interface ReportColumn {
  key: string
  label: string
  type: string
}

export interface ReportSeries {
  name: string
  data: (number | string | null)[]
}

export interface ReportChart {
  categories: string[]
  series: ReportSeries[]
}

/** 报表生成结果 */
export interface ReportResult {
  reportName: string
  columns: ReportColumn[]
  rows: Record<string, unknown>[]
  chart: ReportChart
  reportId?: number | null
  generatedAt?: string | null
  dataVersion?: string | null
  modelVersion?: string | null
}

/** 自定义报表请求 */
export interface ReportRequest {
  reportName: string
  dimensions: string[]
  metrics: string[]
  filters: Record<string, string>
  save?: boolean
}

/** 已保存的报表 / 报表模板 */
export interface SavedReport {
  id: number
  reportName: string
  templateId?: number | null
  dimensions?: string | null
  metrics?: string | null
  filters?: string | null
  reportData?: string | null
  chartData?: string | null
  rowCount?: number | null
  modelVersion?: string | null
  dataVersion?: string | null
  generatedBy?: string | null
  createdAt?: string | null
  content?: string | null
}
