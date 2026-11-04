<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, DataAnalysis, Reading, Share, Top, User } from '@element-plus/icons-vue'

interface NavChild {
  key: string
  label: string
  to: string
  tab: string
}

interface NavModule {
  path: string
  title: string
  icon: Component
  children: NavChild[]
}

const NAV_MODULES: NavModule[] = [
  {
    path: '/dashboard',
    title: '数据分析',
    icon: DataAnalysis,
    children: [
      { key: 'dashboard:pipeline', label: '梯队整体状态', to: '/dashboard?tab=pipeline', tab: 'pipeline' },
      { key: 'dashboard:flow', label: '人才流动趋势', to: '/dashboard?tab=flow', tab: 'flow' },
      { key: 'dashboard:model', label: '模型效果与日志', to: '/dashboard?tab=model', tab: 'model' },
      { key: 'dashboard:report', label: '自定义报表', to: '/dashboard?tab=report', tab: 'report' },
      { key: 'dashboard:overview', label: '基础盘点', to: '/dashboard?tab=overview', tab: 'overview' },
      { key: 'dashboard:assistant', label: '智能分析助手', to: '/dashboard?tab=assistant', tab: 'assistant' },
    ],
  },
  {
    path: '/employee',
    title: '员工档案',
    icon: User,
    children: [
      { key: 'employee:roster', label: '员工名册', to: '/employee?tab=roster', tab: 'roster' },
      { key: 'employee:org', label: '组织概览', to: '/employee?tab=org', tab: 'org' },
      { key: 'employee:performance', label: '绩效档案', to: '/employee?tab=performance', tab: 'performance' },
      { key: 'employee:potential', label: '潜力评估', to: '/employee?tab=potential', tab: 'potential' },
      { key: 'employee:talent', label: '人才结构', to: '/employee?tab=talent', tab: 'talent' },
      { key: 'employee:assistant', label: '智能助手', to: '/employee?tab=assistant', tab: 'assistant' },
    ],
  },
  {
    path: '/skill',
    title: '技能体系',
    icon: Collection,
    children: [
      { key: 'skill:overview', label: '技能全景', to: '/skill?tab=overview', tab: 'overview' },
      { key: 'skill:categories', label: '分类概览', to: '/skill?tab=categories', tab: 'categories' },
      { key: 'skill:frequent', label: '高频技能', to: '/skill?tab=frequent', tab: 'frequent' },
      { key: 'skill:demand', label: '岗位需求', to: '/skill?tab=demand', tab: 'demand' },
      { key: 'skill:gaps', label: '缺口分析', to: '/skill?tab=gaps', tab: 'gaps' },
      { key: 'skill:assistant', label: '智能助手', to: '/skill?tab=assistant', tab: 'assistant' },
    ],
  },
  {
    path: '/planning',
    title: '梯队规划',
    icon: Share,
    children: [
      { key: 'planning:dashboard', label: '决策看板', to: '/planning?tab=dashboard', tab: 'dashboard' },
      { key: 'planning:positions', label: '关键岗位', to: '/planning?tab=positions', tab: 'positions' },
      { key: 'planning:succession', label: '继任候选', to: '/planning?tab=succession', tab: 'succession' },
      { key: 'planning:pools', label: '人才池', to: '/planning?tab=pools', tab: 'pools' },
      { key: 'planning:risks', label: '风险预警', to: '/planning?tab=risks', tab: 'risks' },
      { key: 'planning:assistant', label: '规划助手', to: '/planning?tab=assistant', tab: 'assistant' },
    ],
  },
  {
    path: '/training',
    title: '智能培训',
    icon: Reading,
    children: [
      { key: 'training:recommend', label: '推荐计划', to: '/training?tab=recommend', tab: 'recommend' },
      { key: 'training:records', label: '学习记录', to: '/training?tab=records', tab: 'records' },
      { key: 'training:courses', label: '课程库', to: '/training?tab=courses', tab: 'courses' },
      { key: 'training:paths', label: '学习路径', to: '/training?tab=paths', tab: 'paths' },
      { key: 'training:effects', label: '培训效果', to: '/training?tab=effects', tab: 'effects' },
      { key: 'training:algorithm', label: '匹配算法', to: '/training?tab=algorithm', tab: 'algorithm' },
      { key: 'training:assistant', label: '推荐助手', to: '/training?tab=assistant', tab: 'assistant' },
    ],
  },
  {
    path: '/promotion',
    title: '晋升决策',
    icon: Top,
    children: [
      { key: 'promotion:ranking', label: '候选人排行', to: '/promotion?tab=ranking', tab: 'ranking' },
      { key: 'promotion:matrix', label: '绩效潜力矩阵', to: '/promotion?tab=matrix', tab: 'matrix' },
      { key: 'promotion:distribution', label: '部门分布', to: '/promotion?tab=distribution', tab: 'distribution' },
      { key: 'promotion:risk', label: '风险关注', to: '/promotion?tab=risk', tab: 'risk' },
      { key: 'promotion:suggestions', label: '决策建议', to: '/promotion?tab=suggestions', tab: 'suggestions' },
      { key: 'promotion:assistant', label: '晋升助手', to: '/promotion?tab=assistant', tab: 'assistant' },
    ],
  },
]

const route = useRoute()
const router = useRouter()

const activeModule = computed(() => {
  const segment = route.path.split('/').filter(Boolean)[0]
  return segment ? `/${segment}` : ''
})

const activeMenuKey = computed(() => {
  const currentModule = NAV_MODULES.find((item) => item.path === activeModule.value)
  if (!currentModule) return ''

  const currentTab = typeof route.query.tab === 'string' ? route.query.tab : ''
  const currentChild = currentModule.children.find((item) => item.tab === currentTab)
  return currentChild?.key ?? currentModule.children[0]?.key ?? ''
})

function handleMenuSelect(index: string) {
  const target = NAV_MODULES.flatMap((item) => item.children).find((item) => item.key === index)
  if (target) router.push(target.to)
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="224px" class="aside">
      <el-menu
        :key="activeModule"
        :default-active="activeMenuKey"
        :default-openeds="activeModule ? [activeModule] : []"
        class="menu"
        @select="handleMenuSelect"
      >
        <el-sub-menu v-for="module in NAV_MODULES" :key="module.path" :index="module.path">
          <template #title>
            <el-icon><component :is="module.icon" /></el-icon>
            <span>{{ module.title }}</span>
          </template>
          <el-menu-item v-for="child in module.children" :key="child.key" :index="child.key">
            {{ child.label }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="aside-foot">智能企业人才梯队建设</div>
    </el-aside>

    <el-main class="main">
      <div class="page-container">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #111 0%, #050505 100%);
  box-shadow: 1px 0 0 rgba(255, 255, 255, 0.04);
}

.menu {
  flex: 1;
  padding: 16px 10px 0;
  border-right: none;
  background: transparent;
}

.menu :deep(.el-sub-menu__title) {
  height: 44px;
  margin: 4px 0;
  padding-left: 14px !important;
  border-radius: 10px;
  color: #a3a3a3;
  font-size: 13.5px;
}

.menu :deep(.el-sub-menu__title > .el-icon:not(.el-sub-menu__icon-arrow)) {
  margin-right: 10px;
  color: #60a5fa;
  font-size: 17px;
}

.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  background: rgba(37, 99, 235, 0.16);
  color: #fff;
  font-weight: 500;
}

.menu :deep(.el-sub-menu .el-menu) {
  padding: 2px 0 4px;
  background: rgba(255, 255, 255, 0.025);
  border-radius: 10px;
}

.menu :deep(.el-sub-menu .el-menu-item) {
  height: 36px;
  margin: 2px 0;
  padding-left: 46px !important;
  border-radius: 8px;
  color: #8f8f8f;
  font-size: 12.5px;
}

.menu :deep(.el-sub-menu .el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background: rgba(37, 99, 235, 0.24);
  box-shadow: inset 2px 0 0 #60a5fa;
  color: #fff;
  font-weight: 500;
}

.aside-foot {
  padding: 16px 18px 20px;
  color: rgba(147, 197, 253, 0.5);
  font-size: 11px;
  letter-spacing: 0.4px;
}

.main {
  padding: 28px 32px 40px;
  overflow: auto;
  background: var(--app-bg);
}

.page-container {
  max-width: 1560px;
  margin: 0 auto;
}

@media (max-width: 1280px) {
  .main {
    padding: 22px 22px 32px;
  }
}
</style>
