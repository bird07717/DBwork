<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">基础数据管理</h1>
        <p class="page-subtitle">维护线路、站点、车辆、司机、班次，查询乘车记录</p>
      </div>
      <el-button v-if="activeMeta.editable" type="primary" @click="openCreate">新增</el-button>
    </div>
    <div class="panel admin-data-panel">
      <div class="table-toolbar">
        <div>
          <span>当前模块</span>
          <strong>{{ activeMeta.label }}</strong>
        </div>
        <div class="table-toolbar-actions">
          <el-tag>{{ total }} 条记录</el-tag>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>
      <el-tabs v-model="active" @tab-change="load">
        <el-tab-pane v-for="meta in metas" :key="meta.key" :label="meta.label" :name="meta.key" />
      </el-tabs>
      <el-alert v-if="errorText" :title="errorText" type="error" show-icon style="margin-bottom: 12px" />
      <el-table :data="records" height="560" border v-loading="loading" empty-text="暂无数据，请确认已重新登录后台">
        <el-table-column v-for="col in activeMeta.columns" :key="col.prop" :prop="col.prop" :label="col.label" min-width="120" show-overflow-tooltip />
        <el-table-column v-if="activeMeta.editable" label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next, total" style="margin-top: 12px" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑' : '新增'" width="640px">
      <el-form label-position="top">
        <div class="form-row">
          <el-form-item v-for="field in activeMeta.fields" :key="field" :label="fieldLabel(field)">
            <el-input v-model="form[field]" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { del, get, post, put } from '../../services/api'
import type { PageResult } from '../../services/domain'

interface Meta {
  key: string
  label: string
  path: string
  editable: boolean
  columns: { prop: string; label: string }[]
  fields: string[]
}

const labels: Record<string, string> = {
  id: 'ID',
  route_code: '线路编号',
  route_name: '线路名称',
  direction: '方向',
  start_station_name: '起点站',
  end_station_name: '终点站',
  station_code: '站点编号',
  station_name: '站点名称',
  area_name: '所属区域',
  longitude: '经度',
  latitude: '纬度',
  vehicle_code: '车辆编号',
  plate_no: '车牌号',
  capacity: '核载人数',
  employee_no: '工号',
  driver_name: '司机姓名',
  phone: '联系电话',
  schedule_date: '班次日期',
  depart_time: '发车时间',
  period_type: '时段类型',
  status: '状态',
  boarding_station_name: '上车站点',
  ride_time: '乘车时间',
  pay_type: '支付方式',
  routeCode: '线路编号',
  routeName: '线路名称',
  startStationName: '起点站',
  endStationName: '终点站',
  stationCode: '站点编号',
  stationName: '站点名称',
  areaName: '所属区域',
  vehicleCode: '车辆编号',
  plateNo: '车牌号',
  employeeNo: '工号',
  driverName: '司机姓名',
  routeId: '线路 ID',
  vehicleId: '车辆 ID',
  driverId: '司机 ID',
  scheduleDate: '班次日期',
  departTime: '发车时间',
  periodType: '时段类型'
}

const metas: Meta[] = [
  { key: 'routes', label: '线路', path: '/api/admin/routes', editable: true, columns: cols('id', 'route_code', 'route_name', 'start_station_name', 'end_station_name', 'status'), fields: ['routeCode', 'routeName', 'direction', 'startStationName', 'endStationName', 'status'] },
  { key: 'stations', label: '站点', path: '/api/admin/stations', editable: true, columns: cols('id', 'station_code', 'station_name', 'longitude', 'latitude', 'status'), fields: ['stationCode', 'stationName', 'areaName', 'longitude', 'latitude', 'status'] },
  { key: 'vehicles', label: '车辆', path: '/api/admin/vehicles', editable: true, columns: cols('id', 'vehicle_code', 'plate_no', 'capacity', 'status'), fields: ['vehicleCode', 'plateNo', 'capacity', 'status'] },
  { key: 'drivers', label: '司机', path: '/api/admin/drivers', editable: true, columns: cols('id', 'employee_no', 'driver_name', 'phone', 'status'), fields: ['employeeNo', 'driverName', 'phone', 'status'] },
  { key: 'schedules', label: '班次', path: '/api/admin/schedules', editable: true, columns: cols('id', 'route_name', 'vehicle_code', 'driver_name', 'schedule_date', 'depart_time', 'period_type', 'status'), fields: ['routeId', 'vehicleId', 'driverId', 'scheduleDate', 'departTime', 'periodType', 'status'] },
  { key: 'rideRecords', label: '乘车记录', path: '/api/admin/ride-records', editable: false, columns: cols('id', 'route_name', 'boarding_station_name', 'ride_time', 'period_type', 'pay_type'), fields: [] }
]

const active = ref('routes')
const page = ref(1)
const size = 10
const total = ref(0)
const records = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const errorText = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<Record<string, string>>({})
const activeMeta = computed(() => metas.find((item) => item.key === active.value) || metas[0])

function cols(...props: string[]) {
  return props.map((prop) => ({ prop, label: labels[prop] || prop }))
}

function fieldLabel(field: string) {
  return labels[field] || field
}

async function load() {
  try {
    loading.value = true
    errorText.value = ''
    const data = await get<PageResult<Record<string, unknown>>>(`${activeMeta.value.path}?page=${page.value}&size=${size}`)
    records.value = data.records
    total.value = data.total
  } catch (error) {
    records.value = []
    total.value = 0
    errorText.value = (error as Error).message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.keys(form).forEach((key) => delete form[key])
  dialogVisible.value = true
}

function openEdit(row: Record<string, unknown>) {
  editingId.value = Number(row.id)
  Object.keys(form).forEach((key) => delete form[key])
  activeMeta.value.fields.forEach((field) => {
    form[field] = String(row[toSnake(field)] ?? '')
  })
  dialogVisible.value = true
}

async function save() {
  if (editingId.value) {
    await put(`${activeMeta.value.path}/${editingId.value}`, form)
  } else {
    await post(activeMeta.value.path, form)
  }
  dialogVisible.value = false
  ElMessage.success('已保存')
  await load()
}

async function remove(row: Record<string, unknown>) {
  await ElMessageBox.confirm('确认停用该记录？', '提示')
  await del(`${activeMeta.value.path}/${row.id}`)
  ElMessage.success('已停用')
  await load()
}

function toSnake(value: string) {
  return value.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`)
}

onMounted(load)
</script>
