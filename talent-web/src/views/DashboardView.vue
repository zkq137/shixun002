<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import OverviewTab from './analysis/OverviewTab.vue'
import PipelineTab from './analysis/PipelineTab.vue'
import FlowTab from './analysis/FlowTab.vue'
import ModelTab from './analysis/ModelTab.vue'
import ReportTab from './analysis/ReportTab.vue'

const route = useRoute()

/** 支持 /dashboard?tab=model 直接打开某个页签，方便分享链接 */
const TABS = ['pipeline', 'flow', 'model', 'report', 'overview']
const tabFromUrl = String(route.query.tab ?? '')
const activeTab = ref(TABS.includes(tabFromUrl) ? tabFromUrl : 'pipeline')
</script>

<template>
  <div>
    <h2 class="page-title">数据分析与报告</h2>
    <p class="page-tip">
      数据实时来自 talent-analysis-service（8085）：梯队整体状态、人才流动趋势、模型运行效果，
      并支持自定义报表与模型日志溯源。
    </p>

    <el-tabs v-model="activeTab" type="border-card">
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
  </div>
</template>
