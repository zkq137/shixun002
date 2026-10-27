<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import OverviewTab from './analysis/OverviewTab.vue'
import PipelineTab from './analysis/PipelineTab.vue'
import FlowTab from './analysis/FlowTab.vue'
import ModelTab from './analysis/ModelTab.vue'
import ReportTab from './analysis/ReportTab.vue'
import ModuleAssistant from '@/components/ModuleAssistant.vue'

const route = useRoute()

/** 支持 /dashboard?tab=model 直接打开某个页签，方便分享链接 */
const TABS = ['pipeline', 'flow', 'model', 'report', 'overview']
const tabFromUrl = String(route.query.tab ?? '')
const activeTab = ref(TABS.includes(tabFromUrl) ? tabFromUrl : 'pipeline')
const analysisAssistantPrompts = [
  '分析当前人才梯队健康度',
  '按部门生成人数和平均绩效报表',
  '查看人才流动趋势和主要风险',
  '查看模型运行效果最好的版本',
]
</script>

<template>
  <div>
    <h2 class="page-title">数据分析与报告</h2>
    <p class="page-tip">
      数据实时来自 talent-analysis-service（8085）：梯队整体状态、人才流动趋势、模型运行效果，
      并支持自定义报表与模型日志溯源。
    </p>

    <el-tabs v-model="activeTab" class="analysis-tabs">
      <el-tab-pane label="梯队整体状态" name="pipeline">
        <PipelineTab v-if="activeTab === 'pipeline'" />
      </el-tab-pane>
      <el-tab-pane label="人才流动趋势" name="flow">
        <FlowTab v-if="activeTab === 'flow'" />
      </el-tab-pane>
      <el-tab-pane label="模型效果与日志" name="model">
        <ModelTab v-if="activeTab === 'model'" />
      </el-tab-pane>
      <el-tab-pane label="自定义报表" name="report">
        <ReportTab v-if="activeTab === 'report'" />
      </el-tab-pane>
      <el-tab-pane label="基础盘点" name="overview">
        <OverviewTab v-if="activeTab === 'overview'" />
      </el-tab-pane>
    </el-tabs>

    <ModuleAssistant
      module-key="analysis"
      title="数据分析智能体"
      :quick-prompts="analysisAssistantPrompts"
      status-endpoint="/analysis/ai/status"
      chat-endpoint="/analysis/ai/chat"
      class="analysis-assistant"
    />
  </div>
</template>

<style scoped>
.analysis-tabs :deep(.el-tabs__header) {
  margin: 0 0 18px;
  padding: 0 16px;
  background: #fff;
  border: 1px solid var(--app-line);
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}

.analysis-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.analysis-tabs :deep(.el-tabs__item) {
  height: 48px;
}

.analysis-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
}

.analysis-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.analysis-assistant {
  margin-top: 22px;
}
</style>
