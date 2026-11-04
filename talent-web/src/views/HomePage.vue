<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, type Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Collection,
  Cpu,
  DataAnalysis,
  Promotion,
  Reading,
  Share,
  Top,
  User,
  Warning,
} from '@element-plus/icons-vue'

interface HomeModule {
  index: string
  code: string
  title: string
  english: string
  description: string
  route: string
  icon: Component
  tone: 'blue' | 'green' | 'amber' | 'red' | 'cyan'
}

const router = useRouter()

const navigation = [
  { label: '模块总览', target: '#modules' },
  { label: '智能体', target: '#agents' },
  { label: '风险预警', target: '#risk' },
]

const stats = [
  { value: '1,000', label: '在册人才', note: 'PROFILES' },
  { value: '72', label: '岗位画像', note: 'POSITIONS' },
  { value: '06', label: '业务模块', note: 'MODULES' },
  { value: '03', label: '在线智能体', note: 'AGENTS' },
]

const modules: HomeModule[] = [
  {
    index: '01',
    code: 'DATA / INSIGHT',
    title: '数据分析与报告',
    english: 'ANALYTICS',
    description: '查看梯队健康度、人才流动趋势与模型运行效果，让数据成为可追溯的决策依据。',
    route: '/dashboard',
    icon: DataAnalysis,
    tone: 'blue',
  },
  {
    index: '02',
    code: 'PEOPLE / PROFILE',
    title: '员工档案',
    english: 'PEOPLE',
    description: '汇总员工绩效、潜力、能力与组织信息，快速定位关键人才和成长机会。',
    route: '/employee',
    icon: User,
    tone: 'green',
  },
  {
    index: '03',
    code: 'SKILL / GRAPH',
    title: '技能体系',
    english: 'SKILLS',
    description: '梳理岗位能力图谱，识别高频技能、能力缺口与组织未来需要补齐的方向。',
    route: '/skill',
    icon: Collection,
    tone: 'amber',
  },
  {
    index: '04',
    code: 'SUCCESSION / MAP',
    title: '梯队规划',
    english: 'PLANNING',
    description: '围绕关键岗位与继任候选建立人才池，提前发现空位风险与覆盖断层。',
    route: '/planning',
    icon: Share,
    tone: 'red',
  },
  {
    index: '05',
    code: 'LEARNING / LOOP',
    title: '智能培训',
    english: 'TRAINING',
    description: '根据能力缺口生成培训建议，将课程、学习路径与效果评估连成完整闭环。',
    route: '/training',
    icon: Reading,
    tone: 'cyan',
  },
  {
    index: '06',
    code: 'DECISION / NEXT',
    title: '晋升决策',
    english: 'PROMOTION',
    description: '结合绩效、潜力与风险信号生成候选排序，让每一次晋升都有据可查。',
    route: '/promotion',
    icon: Top,
    tone: 'blue',
  },
]

const agents = [
  {
    name: '数据分析智能体',
    english: 'TALENT ANALYST',
    description: '回答梯队健康度、人才流动趋势与模型效果相关问题。',
    action: '发起分析',
    route: '/dashboard?tab=assistant',
    icon: DataAnalysis,
    tone: 'blue',
  },
  {
    name: '晋升决策智能体',
    english: 'PROMOTION ADVISOR',
    description: '综合绩效、潜力与风险，给出可解释的晋升建议。',
    action: '查看建议',
    route: '/promotion?tab=assistant',
    icon: Promotion,
    tone: 'green',
  },
  {
    name: '培训推荐智能体',
    english: 'LEARNING NAVIGATOR',
    description: '从能力缺口出发，为员工匹配课程与学习路径。',
    action: '生成方案',
    route: '/training?tab=assistant',
    icon: Cpu,
    tone: 'amber',
  },
]

const signalItems = [
  { value: '1,000', label: '员工数据', tone: 'blue' },
  { value: '72', label: '岗位画像', tone: 'green' },
  { value: '06', label: '业务模块', tone: 'amber' },
  { value: '24/7', label: '决策在线', tone: 'red' },
]

const proofItems = [
  { label: '员工数据', value: '1,000', note: '统一人才档案' },
  { label: '能力维度', value: '32', note: '岗位能力模型' },
  { label: '在线智能体', value: '03', note: '业务场景已接入' },
  { label: '决策组合', value: '∞', note: '持续生成建议' },
]

const now = ref(new Date())
const currentYear = new Date().getFullYear()
let clockTimer: ReturnType<typeof setInterval> | undefined

const clock = computed(() => now.value.toLocaleTimeString('zh-CN', { hour12: false }))

onMounted(() => {
  clockTimer = setInterval(() => {
    now.value = new Date()
  }, 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})

function go(path: string) {
  router.push(path)
}

function scrollTo(target: string) {
  document.querySelector(target)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <div class="home-page">
    <header class="site-header">
      <div class="header-inner">
        <nav class="header-nav" aria-label="首页导航">
          <button v-for="item in navigation" :key="item.target" type="button" @click="scrollTo(item.target)">
            {{ item.label }}
          </button>
        </nav>

        <div class="header-actions">
          <span class="service-state"><i></i> 数据服务在线</span>
          <button class="header-cta" type="button" @click="go('/dashboard')">
            进入控制台
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main>
      <section class="hero-section">
        <div class="page-container hero-grid">
          <div class="hero-copy">
            <div class="eyebrow"><span></span> AI-POWERED TALENT INTELLIGENCE</div>
            <h1>让人才价值<br /><span>进入超频状态</span></h1>
            <p class="hero-lead">
              把员工数据、岗位画像与智能体连接成一套实时决策系统。看见梯队、预判风险，让每一次成长都有清晰的下一步。
            </p>

            <div class="hero-actions">
              <button class="btn btn-secondary" type="button" @click="scrollTo('#modules')">
                浏览业务模块
              </button>
            </div>

            <div class="hero-stat-grid">
              <div v-for="stat in stats" :key="stat.note" class="hero-stat">
                <strong>{{ stat.value }}</strong>
                <span>{{ stat.label }}</span>
                <small>{{ stat.note }}</small>
              </div>
            </div>
          </div>

          <div class="dashboard-preview">
            <div class="preview-card">
              <div class="preview-head">
                <div>
                  <span class="preview-kicker">TALENT OVERVIEW</span>
                  <h2>人才驾驶舱</h2>
                </div>
                <span class="live-chip"><i></i> 实时</span>
              </div>

              <div class="preview-summary">
                <div>
                  <span>在册人才</span>
                  <strong>1,000</strong>
                  <small><b>+2.4%</b> 较上月</small>
                </div>
                <div class="summary-ring">
                  <span>健康度</span>
                  <strong>88.6</strong>
                  <small>优秀</small>
                </div>
              </div>

              <div class="preview-chart">
                <div class="chart-head">
                  <span>人才结构趋势</span>
                  <span>近 12 个月</span>
                </div>
                <svg viewBox="0 0 480 160" role="img" aria-label="人才结构趋势示意图">
                  <defs>
                    <linearGradient id="home-area" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0" stop-color="#2563eb" stop-opacity="0.2" />
                      <stop offset="1" stop-color="#2563eb" stop-opacity="0" />
                    </linearGradient>
                  </defs>
                  <path d="M0 126 C40 120 62 92 96 98 C130 104 155 76 190 82 C225 89 242 60 280 67 C318 75 342 42 382 48 C421 54 446 28 480 34 L480 160 L0 160 Z" fill="url(#home-area)" />
                  <path d="M0 126 C40 120 62 92 96 98 C130 104 155 76 190 82 C225 89 242 60 280 67 C318 75 342 42 382 48 C421 54 446 28 480 34" fill="none" stroke="#2563eb" stroke-width="3" stroke-linecap="round" />
                  <circle cx="280" cy="67" r="5" fill="#ffffff" stroke="#2563eb" stroke-width="3" />
                </svg>
                <div class="chart-axis"><span>1月</span><span>4月</span><span>7月</span><span>10月</span><span>12月</span></div>
              </div>

              <div class="preview-metrics">
                <div><span>核心骨干</span><strong class="success">142</strong></div>
                <div><span>高潜人才</span><strong class="info">335</strong></div>
                <div><span>关键岗位</span><strong>72</strong></div>
                <div><span>高风险</span><strong class="danger">06</strong></div>
              </div>
            </div>

            <div class="floating-alert">
              <span class="alert-icon"><el-icon><Warning /></el-icon></span>
              <div>
                <span>风险提醒</span>
                <strong>3 个关键岗位待关注</strong>
              </div>
              <button type="button" @click="go('/planning?tab=risks')">查看</button>
            </div>

            <span class="preview-time">{{ clock }}</span>
          </div>
        </div>
      </section>
      <section id="agents" class="section agents-section">
        <div class="page-container">
          <div class="section-heading">
            <div>
              <span class="section-kicker">02 / 智能体矩阵</span>
              <h2>不是工具，<span>是业务决策搭子</span></h2>
            </div>
            <p>智能体带着业务上下文工作：理解问题、读取真实数据，并给出有证据链的答案。</p>
          </div>

          <div class="agent-grid">
            <button
              v-for="agent in agents"
              :key="agent.name"
              class="agent-card"
              type="button"
              @click="go(agent.route)"
            >
              <span class="agent-top">
                <span class="agent-icon" :class="`tone-${agent.tone}`">
                  <el-icon><component :is="agent.icon" /></el-icon>
                </span>
                <span class="agent-online"><i></i> 在线</span>
              </span>
              <span class="agent-english">{{ agent.english }}</span>
              <strong>{{ agent.name }}</strong>
              <span class="agent-desc">{{ agent.description }}</span>
              <span class="agent-action">
                {{ agent.action }}
                <el-icon><ArrowRight /></el-icon>
              </span>
            </button>
          </div>

          <div class="proof-card">
            <div class="proof-intro">
              <span>EVIDENCE STREAM</span>
              <strong>从数据到行动，全程留痕</strong>
            </div>
            <div class="proof-grid">
              <div v-for="item in proofItems" :key="item.label" class="proof-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <small>{{ item.note }}</small>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="signal-strip">
        <div class="page-container signal-inner">
          <span class="signal-label">系统概览</span>
          <div v-for="item in signalItems" :key="item.label" class="signal-item">
            <i :class="`tone-${item.tone}`"></i>
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
          <span class="signal-status"><i></i> 所有服务运行正常</span>
        </div>
      </section>

      <section id="modules" class="section modules-section">
        <div class="page-container">
          <div class="section-heading">
            <div>
              <span class="section-kicker">01 / 业务模块</span>
              <h2>六大业务模块，<span>同一个数据底座</span></h2>
            </div>
            <p>从人才盘点到组织决策，各模块独立运转，又通过统一的数据与权限体系彼此连接。</p>
          </div>

          <div class="module-grid">
            <button
              v-for="module in modules"
              :key="module.index"
              class="module-card"
              type="button"
              @click="go(module.route)"
            >
              <span class="module-head">
                <span class="module-icon" :class="`tone-${module.tone}`">
                  <el-icon><component :is="module.icon" /></el-icon>
                </span>
                <span class="module-code">{{ module.index }} / {{ module.code }}</span>
                <el-icon class="module-arrow"><ArrowRight /></el-icon>
              </span>
              <span class="module-english">{{ module.english }}</span>
              <strong>{{ module.title }}</strong>
              <span class="module-desc">{{ module.description }}</span>
              <span class="module-footer">
                <span>6 个子模块</span>
                <span>进入模块</span>
              </span>
            </button>
          </div>
        </div>
      </section>

      <section id="risk" class="section risk-section">
        <div class="page-container">
          <div class="risk-card">
            <div class="risk-copy">
              <span class="section-kicker">03 / 风险预警</span>
              <h2>风险，<br />在发生前被看见</h2>
              <p>以关键岗位为坐标，持续扫描继任覆盖、人才流动与能力断层。当风险出现，系统先一步给出信号。</p>
              <div class="risk-facts">
                <div><strong>72</strong><span>岗位画像已映射</span></div>
                <div><strong>06</strong><span>风险维度持续监测</span></div>
                <div><strong>实时</strong><span>关键岗位覆盖更新</span></div>
              </div>
              <button class="btn btn-primary" type="button" @click="go('/planning?tab=risks')">
                打开风险雷达
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>

            <div class="radar-panel">
              <div class="radar-head">
                <span>关键岗位风险扫描</span>
                <span class="scan-state"><i></i> 扫描中</span>
              </div>
              <div class="radar-board">
                <div class="radar-grid" aria-hidden="true"></div>
                <span class="radar-ring ring-outer" aria-hidden="true"></span>
                <span class="radar-ring ring-middle" aria-hidden="true"></span>
                <span class="radar-ring ring-inner" aria-hidden="true"></span>
                <span class="radar-axis axis-x" aria-hidden="true"></span>
                <span class="radar-axis axis-y" aria-hidden="true"></span>
                <span class="radar-dot dot-coverage dot-a"></span>
                <span class="radar-dot dot-coverage dot-b"></span>
                <span class="radar-dot dot-risk dot-c"></span>
                <span class="radar-dot dot-risk dot-d"></span>
                <span class="radar-dot dot-coverage dot-e"></span>
                <div class="radar-center">
                  <span>关键岗位</span>
                  <strong>72</strong>
                  <small>持续扫描中</small>
                </div>
              </div>
              <div class="radar-legend">
                <span><i class="legend-green"></i> 继任覆盖</span>
                <span><i class="legend-red"></i> 待关注风险</span>
                <span class="radar-updated">刚刚更新</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="cta-section">
        <div class="page-container">
          <div class="cta-card">
            <div class="cta-copy">
              <span>READY WHEN YOU ARE</span>
              <h2>下一步，从看见开始。</h2>
              <p>进入人才战略中枢，让数据、岗位与智能体协同工作。</p>
            </div>
            <button class="cta-button" type="button" @click="go('/dashboard')">
              进入控制台
              <el-icon><ArrowRight /></el-icon>
            </button>
            <div class="cta-orbit" aria-hidden="true"><i></i><i></i><i></i></div>
          </div>
        </div>
      </section>
    </main>

    <footer class="site-footer">
      <div class="page-container footer-inner">
        <div class="footer-brand">
          <span class="brand-symbol small" aria-hidden="true"><i></i><i></i><i></i></span>
          <strong>人才战略中枢</strong>
        </div>
        <div class="footer-links">
          <button type="button" @click="scrollTo('#modules')">模块总览</button>
          <button type="button" @click="scrollTo('#agents')">智能体</button>
          <button type="button" @click="scrollTo('#risk')">风险预警</button>
          <button type="button" @click="go('/dashboard')">控制台</button>
        </div>
        <span class="footer-meta">© {{ currentYear }} TALENT CORE</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.home-page {
  --page-bg: #f3f3f2;
  --surface: #ffffff;
  --ink: #111111;
  --title: #16213c;
  --text: #454545;
  --muted: #777777;
  --line: #e5e5e5;
  --line-soft: #eef0f3;
  --navy: #1e3a8a;
  --blue: #2563eb;
  --green: #15803d;
  --amber: #b45309;
  --red: #c2410c;
  --cyan: #0e7490;
  min-height: 100vh;
  overflow-x: clip;
  background: var(--page-bg);
  color: var(--text);
  font-family: Inter, 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.home-page *,
.home-page *::before,
.home-page *::after {
  box-sizing: border-box;
}

.home-page button {
  font-family: inherit;
}

.page-container {
  width: min(calc(100% - 48px), 1440px);
  margin: 0 auto;
}

main {
  display: flex;
  flex-direction: column;
}

.hero-section { order: 1; }
.signal-strip { order: 2; }
.modules-section { order: 3; }
.agents-section { order: 4; }
.risk-section { order: 5; }
.cta-section { order: 6; }

.site-header {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid rgba(229, 229, 229, 0.9);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
}

.header-inner {
  width: min(calc(100% - 48px), 1440px);
  height: 68px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 38px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  padding: 0;
  border: 0;
  background: none;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.brand-symbol {
  width: 36px;
  height: 36px;
  padding: 8px 9px;
  display: flex;
  align-items: flex-end;
  gap: 3px;
  border-radius: 9px;
  background: #111111;
  box-shadow: 0 3px 10px rgba(17, 17, 17, 0.14);
}

.brand-symbol i {
  width: 4px;
  display: block;
  border-radius: 2px;
  background: #ffffff;
}

.brand-symbol i:nth-child(1) { height: 8px; opacity: 0.62; }
.brand-symbol i:nth-child(2) { height: 13px; background: #60a5fa; }
.brand-symbol i:nth-child(3) { height: 19px; }

.brand-symbol.small {
  width: 28px;
  height: 28px;
  padding: 6px 7px;
  border-radius: 7px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  color: var(--title);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.2;
  letter-spacing: -0.2px;
}

.brand-copy small {
  color: #9a9a9a;
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 0.15em;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 30px;
}

.header-nav button {
  padding: 7px 0;
  border: 0;
  background: none;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.header-nav button:hover {
  color: var(--blue);
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 18px;
}

.service-state {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 10px;
  border-radius: 999px;
  background: #f0fdf4;
  color: #166534;
  font-size: 10.5px;
  font-weight: 500;
}

.service-state i,
.signal-status i,
.agent-online i,
.scan-state i {
  width: 6px;
  height: 6px;
  display: inline-block;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.12);
}

.header-cta {
  height: 38px;
  padding: 0 14px 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  border: 0;
  border-radius: 8px;
  background: var(--ink);
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.header-cta:hover {
  background: #2a2a2a;
  transform: translateY(-1px);
}

.header-cta .el-icon {
  font-size: 13px;
}

.hero-section {
  position: relative;
  padding: 72px 0 66px;
  overflow: hidden;
  border-bottom: 1px solid var(--line);
  background:
    radial-gradient(circle at 86% 28%, rgba(37, 99, 235, 0.09), transparent 27%),
    linear-gradient(180deg, #ffffff 0%, #f8f9fb 58%, #f3f3f2 100%);
}

.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(37, 99, 235, 0.11) 0.7px, transparent 0.7px);
  background-size: 22px 22px;
  mask-image: linear-gradient(90deg, transparent 40%, black 100%);
  opacity: 0.55;
}

.hero-grid {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.03fr) minmax(470px, 0.97fr);
  align-items: center;
  gap: clamp(44px, 6vw, 92px);
}

.hero-copy {
  padding: 8px 0;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--blue);
  font-size: 10px;
  font-weight: 650;
  letter-spacing: 0.12em;
}

.eyebrow span {
  width: 28px;
  height: 2px;
  border-radius: 2px;
  background: var(--blue);
}

.hero-copy h1 {
  margin: 18px 0 0;
  color: var(--title);
  font-size: clamp(48px, 5.1vw, 74px);
  font-weight: 750;
  line-height: 1.08;
  letter-spacing: -0.055em;
}

.hero-copy h1 span {
  color: var(--navy);
  background-image: linear-gradient(transparent 82%, rgba(37, 99, 235, 0.13) 82%);
}

.hero-lead {
  max-width: 610px;
  margin: 24px 0 0;
  color: #5f6673;
  font-size: 15px;
  line-height: 1.9;
}

.hero-actions {
  margin-top: 29px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn {
  height: 44px;
  padding: 0 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.btn-primary {
  background: var(--ink);
  color: #ffffff;
  box-shadow: 0 5px 14px rgba(17, 17, 17, 0.12);
}

.btn-primary:hover {
  background: #2a2a2a;
  transform: translateY(-1px);
}

.btn-secondary {
  border-color: #d9dde4;
  background: rgba(255, 255, 255, 0.78);
  color: #303845;
}

.btn-secondary:hover {
  border-color: #b9c1cc;
  background: #ffffff;
}

.btn .el-icon {
  font-size: 14px;
}

.hero-stat-grid {
  margin-top: 38px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.hero-stat {
  min-width: 0;
  padding: 14px 15px 13px;
  border: 1px solid rgba(222, 225, 230, 0.86);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.035);
}

.hero-stat strong,
.hero-stat span,
.hero-stat small {
  display: block;
}

.hero-stat strong {
  color: var(--title);
  font-size: 24px;
  font-weight: 650;
  line-height: 1.1;
  letter-spacing: -0.6px;
  font-variant-numeric: tabular-nums;
}

.hero-stat span {
  margin-top: 7px;
  color: #606875;
  font-size: 11px;
}

.hero-stat small {
  margin-top: 4px;
  color: #a0a6ae;
  font-size: 7px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.dashboard-preview {
  position: relative;
  width: 100%;
  max-width: 570px;
  margin-left: auto;
  padding: 6px 0 78px 20px;
}

.preview-card {
  overflow: hidden;
  border: 1px solid rgba(219, 223, 230, 0.92);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 18px 50px rgba(21, 31, 52, 0.1), 0 4px 12px rgba(21, 31, 52, 0.04);
}

.preview-head {
  padding: 18px 20px 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  border-bottom: 1px solid #eef0f3;
}

.preview-kicker {
  color: #8c94a1;
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.13em;
}

.preview-head h2 {
  margin: 5px 0 0;
  color: var(--title);
  font-size: 18px;
  font-weight: 650;
  letter-spacing: -0.2px;
}

.live-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: var(--blue);
  font-size: 9px;
  font-weight: 500;
}

.live-chip i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--blue);
  animation: live-pulse 1.8s ease-in-out infinite;
}

.preview-summary {
  padding: 20px 20px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.preview-summary > div:first-child > span,
.preview-summary > div:first-child > strong,
.preview-summary > div:first-child > small {
  display: block;
}

.preview-summary > div:first-child > span {
  color: #68717f;
  font-size: 11px;
}

.preview-summary > div:first-child > strong {
  margin-top: 4px;
  color: var(--title);
  font-size: 40px;
  font-weight: 650;
  line-height: 1;
  letter-spacing: -1.4px;
  font-variant-numeric: tabular-nums;
}

.preview-summary > div:first-child > small {
  margin-top: 8px;
  color: #8b929c;
  font-size: 9px;
}

.preview-summary > div:first-child > small b {
  color: var(--green);
  font-weight: 600;
}

.summary-ring {
  position: relative;
  width: 104px;
  height: 104px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: conic-gradient(#22c55e 0 88.6%, #edf0f3 88.6% 100%);
}

.summary-ring::before {
  content: '';
  position: absolute;
  inset: 8px;
  border-radius: 50%;
  background: #ffffff;
}

.summary-ring span,
.summary-ring strong,
.summary-ring small {
  position: relative;
  z-index: 1;
  display: block;
}

.summary-ring span {
  color: #8a929d;
  font-size: 8px;
}

.summary-ring strong {
  margin-top: 2px;
  color: #166534;
  font-size: 21px;
  font-weight: 650;
  line-height: 1.1;
}

.summary-ring small {
  margin-top: 2px;
  color: var(--green);
  font-size: 7px;
}

.preview-chart {
  margin: 0 20px;
  padding: 13px 14px 9px;
  border: 1px solid #edf0f3;
  border-radius: 10px;
  background: #fbfcfe;
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-head span:first-child {
  color: #3f4855;
  font-size: 10px;
  font-weight: 600;
}

.chart-head span:last-child {
  color: #a0a6af;
  font-size: 8px;
}

.preview-chart svg {
  width: 100%;
  height: 100px;
  display: block;
  margin-top: 3px;
  overflow: visible;
}

.chart-axis {
  display: flex;
  justify-content: space-between;
  color: #a8adb5;
  font-size: 7px;
}

.preview-metrics {
  margin-top: 16px;
  padding: 14px 20px 16px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid #eef0f3;
}

.preview-metrics div {
  padding: 0 11px;
  border-right: 1px solid #eef0f3;
}

.preview-metrics div:first-child { padding-left: 0; }
.preview-metrics div:last-child { padding-right: 0; border-right: 0; }

.preview-metrics span,
.preview-metrics strong {
  display: block;
}

.preview-metrics span {
  color: #8a929d;
  font-size: 8px;
}

.preview-metrics strong {
  margin-top: 5px;
  color: var(--title);
  font-size: 17px;
  font-weight: 650;
  line-height: 1;
}

.preview-metrics .success { color: var(--green); }
.preview-metrics .info { color: var(--blue); }
.preview-metrics .danger { color: var(--red); }

.floating-alert {
  position: absolute;
  left: -28px;
  bottom: 0;
  width: 286px;
  padding: 12px 13px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #f2ded8;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 30px rgba(21, 31, 52, 0.12);
  backdrop-filter: blur(9px);
}

.alert-icon {
  width: 30px;
  height: 30px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #fff7ed;
  color: var(--red);
  font-size: 15px;
}

.floating-alert > div {
  min-width: 0;
  flex: 1;
}

.floating-alert > div span,
.floating-alert > div strong {
  display: block;
}

.floating-alert > div span {
  color: #9a7d72;
  font-size: 8px;
}

.floating-alert > div strong {
  margin-top: 2px;
  overflow: hidden;
  color: #6b3222;
  font-size: 10.5px;
  font-weight: 550;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.floating-alert button {
  padding: 4px 0;
  border: 0;
  background: none;
  color: var(--red);
  font-size: 9px;
  cursor: pointer;
}

.preview-time {
  position: absolute;
  top: 18px;
  right: 18px;
  color: #a7adb6;
  font-size: 8px;
  font-variant-numeric: tabular-nums;
}

.signal-strip {
  border-bottom: 1px solid var(--line);
  background: #ffffff;
}

.signal-inner {
  min-height: 64px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.signal-label {
  color: var(--title);
  font-size: 12px;
  font-weight: 600;
}

.signal-item {
  display: flex;
  align-items: center;
  gap: 7px;
}

.signal-item i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.signal-item i.tone-blue { background: var(--blue); }
.signal-item i.tone-green { background: var(--green); }
.signal-item i.tone-amber { background: var(--amber); }
.signal-item i.tone-red { background: var(--red); }

.signal-item strong {
  color: var(--title);
  font-size: 15px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.signal-item span {
  color: #8a919b;
  font-size: 10px;
}

.signal-status {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #6b7280;
  font-size: 10px;
}

.section {
  padding: 82px 0 88px;
}

.section-heading {
  margin-bottom: 32px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 50px;
}

.section-heading > div {
  max-width: 760px;
}

.section-kicker {
  color: var(--blue);
  font-size: 9.5px;
  font-weight: 650;
  letter-spacing: 0.12em;
}

.section-heading h2 {
  margin: 10px 0 0;
  color: var(--title);
  font-size: clamp(28px, 2.8vw, 38px);
  font-weight: 680;
  line-height: 1.25;
  letter-spacing: -0.8px;
}

.section-heading h2 span {
  color: #8b929d;
}

.section-heading > p {
  max-width: 430px;
  margin: 0 0 3px;
  color: #7b828d;
  font-size: 12.5px;
  line-height: 1.8;
}

.modules-section {
  background: var(--page-bg);
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.module-card {
  min-height: 272px;
  padding: 20px 21px 17px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.035), 0 5px 18px rgba(16, 24, 40, 0.035);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.module-card:hover {
  transform: translateY(-3px);
  border-color: #c9d2df;
  box-shadow: 0 2px 4px rgba(16, 24, 40, 0.04), 0 14px 32px rgba(16, 24, 40, 0.08);
}

.module-head {
  width: 100%;
  display: flex;
  align-items: center;
}

.module-icon,
.agent-icon {
  width: 40px;
  height: 40px;
  flex: none;
  display: grid;
  place-items: center;
  border-radius: 10px;
  font-size: 19px;
}

.module-icon.tone-blue,
.agent-icon.tone-blue {
  background: #eff6ff;
  color: var(--blue);
}

.module-icon.tone-green,
.agent-icon.tone-green {
  background: #f0fdf4;
  color: var(--green);
}

.module-icon.tone-amber,
.agent-icon.tone-amber {
  background: #fffbeb;
  color: var(--amber);
}

.module-icon.tone-red {
  background: #fff7ed;
  color: var(--red);
}

.module-icon.tone-cyan {
  background: #ecfeff;
  color: var(--cyan);
}

.module-code {
  margin-left: 11px;
  color: #9aa1ab;
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.module-arrow {
  margin-left: auto;
  color: #b4bac3;
  font-size: 17px;
  transition: color 0.2s ease, transform 0.2s ease;
}

.module-card:hover .module-arrow {
  color: var(--blue);
  transform: translateX(3px);
}

.module-english {
  margin-top: 25px;
  color: #98a0ab;
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.module-card > strong {
  margin-top: 6px;
  color: var(--title);
  font-size: 20px;
  font-weight: 650;
  line-height: 1.3;
  letter-spacing: -0.35px;
}

.module-desc {
  margin-top: 10px;
  color: #737b87;
  font-size: 12px;
  line-height: 1.8;
}

.module-footer {
  width: 100%;
  margin-top: auto;
  padding-top: 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f0f1f3;
  color: #9ba1aa;
  font-size: 9px;
}

.module-footer span:last-child {
  color: var(--blue);
  font-weight: 500;
}

.agents-section {
  border-top: 1px solid #e7e9ed;
  border-bottom: 1px solid #e7e9ed;
  background: #eef2f7;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.agent-card {
  min-height: 238px;
  padding: 20px 21px 17px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  border: 1px solid #dfe4ec;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.025), 0 5px 18px rgba(16, 24, 40, 0.03);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.agent-card:hover {
  transform: translateY(-3px);
  border-color: #c7d0dd;
  box-shadow: 0 14px 30px rgba(16, 24, 40, 0.075);
}

.agent-top {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.agent-online {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #f0fdf4;
  color: #166534;
  font-size: 8.5px;
}

.agent-english {
  margin-top: 21px;
  color: #98a0ab;
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.agent-card > strong {
  margin-top: 6px;
  color: var(--title);
  font-size: 19px;
  font-weight: 650;
  line-height: 1.3;
  letter-spacing: -0.3px;
}

.agent-desc {
  margin-top: 9px;
  color: #737b87;
  font-size: 11.5px;
  line-height: 1.75;
}

.agent-action {
  width: 100%;
  margin-top: auto;
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f0f1f3;
  color: var(--blue);
  font-size: 10px;
  font-weight: 500;
}

.agent-action .el-icon {
  font-size: 14px;
  transition: transform 0.2s ease;
}

.agent-card:hover .agent-action .el-icon {
  transform: translateX(3px);
}

.proof-card {
  margin-top: 14px;
  padding: 18px 21px;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  align-items: center;
  gap: 26px;
  border: 1px solid #dfe4ec;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.025);
}

.proof-intro span,
.proof-intro strong {
  display: block;
}

.proof-intro span {
  color: #9aa1ab;
  font-size: 8px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.proof-intro strong {
  margin-top: 6px;
  color: var(--title);
  font-size: 13px;
  font-weight: 600;
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.proof-item {
  padding: 0 20px;
  border-left: 1px solid #e6e9ee;
}

.proof-item span,
.proof-item strong,
.proof-item small {
  display: block;
}

.proof-item span {
  color: #8c949f;
  font-size: 8.5px;
}

.proof-item strong {
  margin-top: 4px;
  color: var(--title);
  font-size: 22px;
  font-weight: 650;
  line-height: 1.1;
}

.proof-item small {
  margin-top: 4px;
  color: #a2a8b1;
  font-size: 8px;
}

.risk-section {
  background: #ffffff;
}

.risk-card {
  padding: 44px;
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(460px, 1.1fr);
  align-items: center;
  gap: 64px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background:
    radial-gradient(circle at 8% 86%, rgba(194, 65, 12, 0.055), transparent 26%),
    linear-gradient(135deg, #ffffff, #f8f9fb);
  box-shadow: 0 2px 4px rgba(16, 24, 40, 0.025), 0 16px 45px rgba(16, 24, 40, 0.055);
}

.risk-copy h2 {
  margin: 14px 0 0;
  color: var(--title);
  font-size: clamp(38px, 4.1vw, 58px);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -1.5px;
}

.risk-copy > p {
  max-width: 480px;
  margin: 21px 0 0;
  color: #6f7783;
  font-size: 13px;
  line-height: 1.9;
}

.risk-facts {
  margin: 29px 0;
  padding: 17px 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid #e7e9ed;
  border-bottom: 1px solid #e7e9ed;
}

.risk-facts div {
  padding: 0 15px;
  border-right: 1px solid #e7e9ed;
}

.risk-facts div:first-child { padding-left: 0; }
.risk-facts div:last-child { border-right: 0; }

.risk-facts strong,
.risk-facts span {
  display: block;
}

.risk-facts strong {
  color: var(--title);
  font-size: 25px;
  font-weight: 650;
  line-height: 1;
  letter-spacing: -0.5px;
}

.risk-facts span {
  margin-top: 7px;
  color: #8b929d;
  font-size: 8.5px;
  line-height: 1.4;
}

.radar-panel {
  padding: 17px 18px 14px;
  display: flex;
  flex-direction: column;
  border: 1px solid #e2e6ec;
  border-radius: 14px;
  background: #fbfcfe;
}

.radar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.radar-head > span:first-child {
  color: #44505f;
  font-size: 11px;
  font-weight: 600;
}

.scan-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--blue);
  font-size: 8.5px;
}

.scan-state i {
  background: var(--blue);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.11);
  animation: live-pulse 1.8s ease-in-out infinite;
}

.radar-board {
  position: relative;
  min-height: 364px;
  margin: 13px 0 11px;
  overflow: hidden;
  border: 1px solid #eef0f3;
  border-radius: 11px;
  background: #ffffff;
}

.radar-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.045) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(circle at center, black, transparent 76%);
}

.radar-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px solid rgba(37, 99, 235, 0.19);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.ring-outer {
  width: 304px;
  height: 304px;
}

.ring-middle {
  width: 210px;
  height: 210px;
  border-style: dashed;
}

.ring-inner {
  width: 126px;
  height: 126px;
  border-color: rgba(37, 99, 235, 0.25);
}

.radar-axis {
  position: absolute;
  top: 50%;
  left: 50%;
  background: rgba(37, 99, 235, 0.13);
  transform: translate(-50%, -50%);
}

.axis-x { width: 304px; height: 1px; }
.axis-y { width: 1px; height: 304px; }

.radar-dot {
  position: absolute;
  width: 8px;
  height: 8px;
  border: 2px solid #ffffff;
  border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.07);
}

.dot-coverage { background: #22c55e; }
.dot-risk { background: #ef4444; box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.09); }

.dot-a { top: 26%; left: 64%; }
.dot-b { top: 70%; left: 34%; }
.dot-c { top: 39%; left: 28%; }
.dot-d { top: 33%; left: 75%; }
.dot-e { top: 75%; left: 67%; }

.radar-center {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 102px;
  height: 102px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid #dce3ef;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 7px 22px rgba(37, 99, 235, 0.11);
  transform: translate(-50%, -50%);
}

.radar-center span,
.radar-center strong,
.radar-center small {
  display: block;
}

.radar-center span {
  color: #788391;
  font-size: 8px;
}

.radar-center strong {
  margin: 2px 0;
  color: var(--navy);
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -1px;
}

.radar-center small {
  color: #9ba2ac;
  font-size: 7px;
}

.radar-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #79818c;
  font-size: 8.5px;
}

.radar-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.radar-legend i {
  width: 6px;
  height: 6px;
  display: inline-block;
  border-radius: 50%;
}

.legend-green { background: #22c55e; }
.legend-red { background: #ef4444; }

.radar-legend .radar-updated {
  margin-left: auto;
  color: #a0a6af;
}

.cta-section {
  padding: 76px 0 80px;
  background: var(--page-bg);
}

.cta-card {
  position: relative;
  min-height: 250px;
  padding: 44px 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
  overflow: hidden;
  border-radius: 18px;
  background:
    radial-gradient(circle at 86% 46%, rgba(37, 99, 235, 0.2), transparent 24%),
    #111111;
  box-shadow: 0 18px 42px rgba(17, 17, 17, 0.15);
  color: #ffffff;
}

.cta-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(90deg, transparent 15%, black);
  pointer-events: none;
}

.cta-copy {
  position: relative;
  z-index: 2;
}

.cta-copy > span {
  color: #7da6f8;
  font-size: 8.5px;
  font-weight: 600;
  letter-spacing: 0.15em;
}

.cta-copy h2 {
  margin: 10px 0 0;
  color: #ffffff;
  font-size: clamp(32px, 3.7vw, 49px);
  font-weight: 680;
  line-height: 1.15;
  letter-spacing: -1.3px;
}

.cta-copy p {
  margin: 12px 0 0;
  color: #9a9a9a;
  font-size: 12.5px;
}

.cta-button {
  position: relative;
  z-index: 3;
  height: 46px;
  min-width: 170px;
  padding: 0 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  flex: none;
  border: 0;
  border-radius: 8px;
  background: #ffffff;
  color: #111111;
  font-size: 13px;
  font-weight: 550;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.cta-button:hover {
  background: #f0f2f5;
  transform: translateY(-2px);
}

.cta-orbit {
  position: absolute;
  top: 50%;
  right: 110px;
  width: 360px;
  height: 360px;
  transform: translateY(-50%);
  opacity: 0.34;
  pointer-events: none;
}

.cta-orbit i {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px solid rgba(125, 166, 248, 0.22);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.cta-orbit i:nth-child(1) { width: 220px; height: 220px; }
.cta-orbit i:nth-child(2) { width: 300px; height: 300px; border-style: dashed; }
.cta-orbit i:nth-child(3) { width: 360px; height: 360px; }

.site-footer {
  border-top: 1px solid var(--line);
  background: #ffffff;
}

.footer-inner {
  min-height: 82px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.footer-brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
}

.footer-brand strong {
  color: var(--title);
  font-size: 12px;
  font-weight: 600;
}

.footer-links {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 24px;
}

.footer-links button {
  padding: 4px 0;
  border: 0;
  background: none;
  color: #858c96;
  font-size: 10px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.footer-links button:hover {
  color: var(--blue);
}

.footer-meta {
  color: #a3a8af;
  font-size: 8px;
  letter-spacing: 0.08em;
}

@keyframes live-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

#modules,
#agents,
#risk {
  scroll-margin-top: 82px;
}

@media (max-width: 1180px) {
  .header-inner {
    gap: 24px;
  }

  .header-nav {
    gap: 20px;
  }

  .hero-grid {
    grid-template-columns: minmax(0, 1fr) minmax(430px, 0.92fr);
    gap: 44px;
  }

  .hero-copy h1 {
    font-size: clamp(46px, 5.2vw, 62px);
  }

  .risk-card {
    gap: 44px;
  }

  .risk-copy h2 {
    font-size: 46px;
  }
}

@media (max-width: 980px) {
  .header-nav {
    display: none;
  }

  .hero-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-copy {
    max-width: 760px;
  }

  .dashboard-preview {
    max-width: 680px;
    margin: 10px auto 0;
    padding-left: 0;
  }

  .module-grid,
  .agent-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .agent-card:last-child {
    grid-column: 1 / -1;
    min-height: 210px;
  }

  .risk-card {
    grid-template-columns: minmax(0, 1fr);
    padding: 36px;
  }

  .risk-copy {
    max-width: 720px;
  }

  .risk-copy h2 br {
    display: none;
  }

  .radar-panel {
    width: 100%;
  }
}

@media (max-width: 860px) {
  .service-state {
    display: none;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }

  .section-heading > p {
    max-width: 620px;
  }

  .proof-card {
    grid-template-columns: minmax(0, 1fr);
    gap: 18px;
  }

  .proof-item:first-child {
    padding-left: 0;
    border-left: 0;
  }

  .cta-card {
    align-items: flex-start;
    flex-direction: column;
    gap: 28px;
  }

  .cta-orbit {
    right: -70px;
  }
}

@media (max-width: 700px) {
  .page-container,
  .header-inner {
    width: calc(100% - 32px);
  }

  .header-inner {
    height: 62px;
  }

  .brand-symbol {
    width: 33px;
    height: 33px;
  }

  .hero-section {
    padding: 44px 0 48px;
  }

  .hero-copy h1 {
    font-size: clamp(40px, 10vw, 54px);
  }

  .hero-lead {
    margin-top: 19px;
    font-size: 13.5px;
  }

  .hero-actions {
    align-items: stretch;
  }

  .btn {
    flex: 1;
    padding-inline: 12px;
  }

  .hero-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    margin-top: 28px;
  }

  .dashboard-preview {
    margin-top: 4px;
    padding: 4px 0 78px;
  }

  .signal-inner {
    padding: 13px 0;
    flex-wrap: wrap;
    gap: 10px 22px;
  }

  .signal-label {
    width: 100%;
  }

  .signal-status {
    width: 100%;
    margin-left: 0;
  }

  .section {
    padding: 60px 0 64px;
  }

  .module-grid,
  .agent-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .agent-card:last-child {
    grid-column: auto;
    min-height: 218px;
  }

  .module-card {
    min-height: 250px;
  }

  .proof-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 18px;
  }

  .proof-item:nth-child(odd) {
    padding-left: 0;
    border-left: 0;
  }

  .risk-card {
    padding: 25px 20px;
    gap: 32px;
  }

  .risk-copy h2 {
    font-size: clamp(34px, 9vw, 46px);
  }

  .risk-facts div {
    padding-inline: 9px;
  }

  .risk-facts strong {
    font-size: 21px;
  }

  .radar-board {
    min-height: 308px;
  }

  .ring-outer { width: 256px; height: 256px; }
  .ring-middle { width: 180px; height: 180px; }
  .ring-inner { width: 110px; height: 110px; }
  .axis-x { width: 256px; }
  .axis-y { height: 256px; }

  .radar-legend {
    flex-wrap: wrap;
    row-gap: 8px;
  }

  .cta-section {
    padding: 56px 0 60px;
  }

  .cta-card {
    min-height: 280px;
    padding: 32px 26px;
  }

  .footer-inner {
    padding: 20px 0;
    flex-wrap: wrap;
    gap: 15px 24px;
  }

  .footer-links {
    order: 3;
    width: 100%;
    margin-left: 0;
    flex-wrap: wrap;
  }

  .footer-meta {
    margin-left: auto;
  }
}

@media (max-width: 520px) {
  .brand-copy small {
    display: none;
  }

  .header-cta {
    width: 38px;
    padding: 0;
    justify-content: center;
    font-size: 0;
  }

  .header-cta .el-icon {
    font-size: 15px;
  }

  .hero-copy h1 {
    font-size: clamp(35px, 10.8vw, 45px);
  }

  .hero-actions {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  .btn {
    width: 100%;
  }

  .summary-ring {
    width: 88px;
    height: 88px;
  }

  .summary-ring strong {
    font-size: 18px;
  }

  .preview-summary {
    padding: 18px;
  }

  .preview-summary > div:first-child > strong {
    font-size: 34px;
  }

  .preview-chart {
    margin-inline: 18px;
  }

  .preview-chart svg {
    height: 82px;
  }

  .preview-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 13px;
  }

  .preview-metrics div:nth-child(2) {
    border-right: 0;
  }

  .preview-metrics div:nth-child(3) {
    padding-left: 0;
  }

  .floating-alert {
    position: relative;
    left: auto;
    bottom: auto;
    width: 100%;
    margin-top: 12px;
  }

  .dashboard-preview {
    padding-bottom: 0;
  }

  .section-heading h2 {
    font-size: 27px;
  }

  .proof-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .proof-item,
  .proof-item:nth-child(odd) {
    padding: 0;
    border-left: 0;
  }

  .proof-item + .proof-item {
    padding-top: 12px;
    border-top: 1px solid #eef0f3;
  }

  .radar-board {
    min-height: 276px;
  }

  .ring-outer { width: 226px; height: 226px; }
  .ring-middle { width: 158px; height: 158px; }
  .ring-inner { width: 96px; height: 96px; }
  .axis-x { width: 226px; }
  .axis-y { height: 226px; }

  .radar-center {
    width: 88px;
    height: 88px;
  }

  .radar-center strong {
    font-size: 25px;
  }

  .cta-button {
    width: 100%;
  }

  .footer-meta {
    width: 100%;
    margin-left: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-page *,
  .home-page *::before,
  .home-page *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
