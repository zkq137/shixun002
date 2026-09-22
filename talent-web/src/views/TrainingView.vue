<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchEmployeePage } from '@/api/employee'
import { fetchTrainingPlan } from '@/api/training'
import type { EmployeeRow, TrainingPlan } from '@/api/types'

const options = ref<EmployeeRow[]>([])
const searching = ref(false)
const employeeId = ref<number | null>(null)

const loading = ref(false)
const plan = ref<TrainingPlan | null>(null)

async function searchEmployees(keyword: string) {
  searching.value = true
  try {
    const page = await fetchEmployeePage({ keyword, pageNum: 1, pageSize: 20 })
    options.value = page.records
  } finally {
    searching.value = false
  }
}

async function loadPlan() {
  if (employeeId.value === null) return
  loading.value = true
  try {
    plan.value = await fetchTrainingPlan(employeeId.value)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 默认列出前 20 个人，方便直接点
  searchEmployees('')
})
</script>

<template>
  <div>
    <h2 class="page-title">智能培训推荐</h2>
    <p class="page-tip">
      推荐逻辑：员工所在岗位的「必备培训」减去他「已完成的培训」，剩下的就是要补的课。
      数据来自 pos_course_require / training_record / course。
    </p>

    <el-card shadow="never">
      <div class="search-bar">
        <el-select
          v-model="employeeId"
          filterable
          remote
          :remote-method="searchEmployees"
          :loading="searching"
          placeholder="输入姓名或工号搜员工"
          style="width: 320px"
          @change="loadPlan"
        >
          <el-option
            v-for="item in options"
            :key="item.id"
            :label="`${item.name}（${item.empNo}）- ${item.positionName}`"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" :disabled="employeeId === null" @click="loadPlan">查看学习计划</el-button>
      </div>
    </el-card>

    <el-card v-if="plan" v-loading="loading" shadow="never" style="margin-top: 16px">
      <template #header>
        {{ plan.employeeName }} · {{ plan.positionName }}
      </template>

      <el-alert
        v-if="plan.recommendationReason"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
        :title="plan.recommendationReason"
      />

      <el-row :gutter="24">
        <el-col :span="10">
          <h3 style="font-size: 15px; margin: 0 0 12px">
            已完成的培训（{{ (plan.completedCourses ?? []).length }} 门）
          </h3>
          <el-tag
            v-for="item in plan.completedCourses ?? []"
            :key="item.id"
            type="success"
            effect="plain"
            style="margin: 0 8px 8px 0"
          >
            {{ item.courseName }}
          </el-tag>
          <el-empty
            v-if="(plan.completedCourses ?? []).length === 0"
            description="还没有已完成记录"
            :image-size="60"
          />

          <h3 style="font-size: 15px; margin: 20px 0 12px">
            技能缺口（{{ (plan.skillGaps ?? []).length }} 项）
          </h3>
          <el-tag
            v-for="skill in plan.skillGaps ?? []"
            :key="skill"
            type="warning"
            effect="plain"
            style="margin: 0 8px 8px 0"
          >
            {{ skill }}
          </el-tag>
          <p v-if="(plan.skillGaps ?? []).length === 0" class="page-tip">
            岗位要求的技能都已经具备
          </p>
        </el-col>
        <el-col :span="14">
          <h3 style="font-size: 15px; margin: 0 0 12px">
            需要补的培训（{{ (plan.recommendedCourses ?? []).length }} 门）
          </h3>
          <el-table :data="plan.recommendedCourses ?? []" stripe border max-height="420">
            <el-table-column type="index" label="#" width="60" />
            <el-table-column prop="courseName" label="课程" min-width="180" />
            <el-table-column prop="courseType" label="类型" width="100" />
            <el-table-column prop="difficulty" label="难度" width="90" />
            <el-table-column prop="duration" label="时长(小时)" width="110" />
            <template #empty>岗位必修都学完了，没有缺口</template>
          </el-table>
        </el-col>
      </el-row>
    </el-card>

    <el-empty v-else description="先在上方选一个员工" />
  </div>
</template>
