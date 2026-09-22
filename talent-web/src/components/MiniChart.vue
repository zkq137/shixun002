<script setup lang="ts">
import { computed } from 'vue'

/**
 * 轻量图表：柱状图 / 折线图。
 *
 * 不引 ECharts 这类重依赖，用 SVG 手画，够看趋势和对比就行。
 * 以后要做交互式图表（缩放、tooltip）再换成 ECharts。
 */
const props = withDefaults(
  defineProps<{
    labels: string[]
    values: number[]
    type?: 'bar' | 'line'
    height?: number
    color?: string
  }>(),
  { type: 'bar', height: 190, color: '#409eff' },
)

const CHART_WIDTH = 640
const PAD = 34

const maxValue = computed(() => Math.max(1, ...props.values.map((value) => Number(value) || 0)))

const plotHeight = computed(() => props.height - PAD - 14)

const bars = computed(() => {
  const count = Math.max(1, props.values.length)
  const slot = (CHART_WIDTH - PAD * 2) / count
  const width = Math.max(3, slot * 0.62)
  return props.values.map((value, index) => {
    const height = ((Number(value) || 0) / maxValue.value) * plotHeight.value
    return {
      x: PAD + slot * index + (slot - width) / 2,
      y: props.height - 14 - height,
      width,
      height: Math.max(1, height),
      value,
    }
  })
})

const points = computed(() => {
  const count = props.values.length
  const step = count <= 1 ? 0 : (CHART_WIDTH - PAD * 2) / (count - 1)
  return props.values.map((value, index) => ({
    x: PAD + index * step,
    y: props.height - 14 - ((Number(value) || 0) / maxValue.value) * plotHeight.value,
    value,
  }))
})

const linePath = computed(() =>
  points.value.map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(' '),
)

const ticks = computed(() =>
  [0, 0.5, 1].map((ratio) => ({
    y: props.height - 14 - ratio * plotHeight.value,
    label: Math.round(maxValue.value * ratio),
  })),
)

/** 标签太密时隔几个显示一个 */
const labelStep = computed(() => Math.ceil(props.labels.length / 12) || 1)
</script>

<template>
  <div>
    <svg :viewBox="`0 0 ${CHART_WIDTH} ${height}`" style="width: 100%; height: auto">
      <line
        v-for="tick in ticks"
        :key="'grid' + tick.y"
        :x1="PAD"
        :x2="CHART_WIDTH - PAD"
        :y1="tick.y"
        :y2="tick.y"
        stroke="#ebeef5"
        stroke-width="1"
      />
      <text
        v-for="tick in ticks"
        :key="'tick' + tick.y"
        :x="PAD - 6"
        :y="tick.y + 4"
        text-anchor="end"
        font-size="11"
        fill="#909399"
      >
        {{ tick.label }}
      </text>

      <template v-if="type === 'bar'">
        <rect
          v-for="(bar, index) in bars"
          :key="'bar' + index"
          :x="bar.x"
          :y="bar.y"
          :width="bar.width"
          :height="bar.height"
          :fill="color"
          rx="2"
          opacity="0.85"
        />
      </template>
      <template v-else>
        <path :d="linePath" fill="none" :stroke="color" stroke-width="2" />
        <circle
          v-for="(point, index) in points"
          :key="'dot' + index"
          :cx="point.x"
          :cy="point.y"
          r="3"
          :fill="color"
        />
      </template>

      <template v-for="(label, index) in labels" :key="'label' + index">
        <text
          v-if="index % labelStep === 0 || labels.length <= 12"
          :x="type === 'bar' ? (bars[index]?.x ?? 0) + (bars[index]?.width ?? 0) / 2 : points[index]?.x"
          :y="height - 2"
          text-anchor="middle"
          font-size="10"
          fill="#909399"
        >
          {{ label }}
        </text>
      </template>
    </svg>
  </div>
</template>
