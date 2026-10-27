<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Collection, DataAnalysis, Reading, Share, Top, User } from '@element-plus/icons-vue'

const route = useRoute()
</script>

<template>
  <el-container class="layout">
    <el-aside width="224px" class="aside">
      <div class="brand">
        <div class="brand-mark">T</div>
        <div class="brand-text">
          <div class="brand-title">人才梯队平台</div>
          <div class="brand-sub">TALENT PLATFORM</div>
        </div>
      </div>

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
  background: linear-gradient(180deg, #17223c 0%, #111a2e 100%);
  box-shadow: 1px 0 0 rgba(255, 255, 255, 0.04);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px 24px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f7cff, #7aa2ff);
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  line-height: 34px;
  text-align: center;
  box-shadow: 0 6px 16px rgba(79, 124, 255, 0.35);
}

.brand-title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.brand-sub {
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.35);
  font-size: 9px;
  letter-spacing: 1.4px;
}

.menu {
  flex: 1;
  padding: 0 10px;
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  height: 44px;
  margin: 4px 0;
  padding-left: 14px !important;
  border-radius: 10px;
  color: #aeb9cf;
  font-size: 13.5px;
}

.menu :deep(.el-menu-item .el-icon) {
  margin-right: 10px;
  font-size: 17px;
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(79, 124, 255, 0.2);
  color: #fff;
  font-weight: 500;
}

.aside-foot {
  padding: 16px 18px 20px;
  color: rgba(255, 255, 255, 0.28);
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
