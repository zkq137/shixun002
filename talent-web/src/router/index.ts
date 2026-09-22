import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由表：一个菜单对应一个页面。
 * meta.title 会显示在顶栏，同时也是侧边菜单的名字。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
      meta: { title: '数据分析与报告' },
    },
    {
      path: '/employee',
      name: 'employee',
      component: () => import('@/views/EmployeeView.vue'),
      meta: { title: '员工档案' },
    },
    {
      path: '/skill',
      name: 'skill',
      component: () => import('@/views/SkillView.vue'),
      meta: { title: '技能体系' },
    },
    {
      path: '/planning',
      name: 'planning',
      component: () => import('@/views/PlanningView.vue'),
      meta: { title: '梯队规划' },
    },
    {
      path: '/training',
      name: 'training',
      component: () => import('@/views/TrainingView.vue'),
      meta: { title: '智能培训' },
    },
    {
      path: '/promotion',
      name: 'promotion',
      component: () => import('@/views/PromotionView.vue'),
      meta: { title: '晋升决策' },
    },
  ],
})

export default router
