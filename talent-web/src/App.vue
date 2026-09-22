<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Collection, DataAnalysis, Reading, Share, Top, User } from '@element-plus/icons-vue'

const route = useRoute()
const title = computed(() => (route.meta.title as string) ?? '人才梯队平台')
</script>

<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo">人才梯队平台</div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据分析</span>
        </el-menu-item>
        <el-menu-item index="/employee">
          <el-icon><User /></el-icon>
          <span>员工档案</span>
        </el-menu-item>
        <el-menu-item index="/skill">
          <el-icon><Collection /></el-icon>
          <span>技能体系</span>
        </el-menu-item>
        <el-menu-item index="/planning">
          <el-icon><Share /></el-icon>
          <span>梯队规划</span>
        </el-menu-item>
        <el-menu-item index="/training">
          <el-icon><Reading /></el-icon>
          <span>智能培训</span>
        </el-menu-item>
        <el-menu-item index="/promotion">
          <el-icon><Top /></el-icon>
          <span>晋升决策</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="header-title">{{ title }}</span>
        <span class="header-sub">智能企业人才梯队建设平台</span>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background-color: #1f2d3d;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

.menu {
  border-right: none;
  background-color: transparent;
}

.menu :deep(.el-menu-item) {
  color: #c0c4cc;
}

.menu :deep(.el-menu-item:hover) {
  background-color: #263445;
}

.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background-color: #263445;
}

.header {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-sub {
  font-size: 13px;
  color: #909399;
}

.main {
  padding: 20px;
}
</style>
