<template>
  <div ref="chartRef" class="chart"></div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{ option: Record<string, unknown> }>()
const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (!chart && chartRef.value) {
    chart = echarts.init(chartRef.value)
  }
  chart?.setOption(props.option, true)
}

function resize() {
  chart?.resize()
}

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})

watch(() => props.option, render, { deep: true })
</script>
