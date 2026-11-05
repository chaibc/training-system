/**
 * 基础数据页：维护课程分类、课程阶段、班型、教室、请假原因、缴费方式、试听时段七类字典。
 * 接口：api/index.js 的 basicApi（通用 CRUD，根路径见 DICTS 的 endpoint）
 *
 * 七类字典后端结构一致，因此用 DICTS 配置 + 一套通用增删改查复用同一个表格与弹窗；
 * 与「业务规则」页的区别是本页允许新增与删除。
 */
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { basicApi } from '../api'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'

// 字典配置：key 用于页签与状态，endpoint 是后端根路径，capacity 决定容量字段形态
// capacity: 'range' = 班型的最少/最多人数；'single' = 教室的单一容量；缺省表示该字典没有容量字段
const DICTS = [
  { key: 'categories', label: '课程分类', endpoint: basicApi.categories, add: '新增分类' },
  { key: 'stages', label: '课程阶段', endpoint: basicApi.stages, add: '新增阶段' },
  { key: 'classTypes', label: '班型', endpoint: basicApi.classTypes, add: '新增班型', capacity: 'range' },
  { key: 'classrooms', label: '教室', endpoint: basicApi.classrooms, add: '新增教室', capacity: 'single' },
  { key: 'leaveReasons', label: '请假原因', endpoint: basicApi.leaveReasons, add: '新增原因' },
  { key: 'paymentMethods', label: '缴费方式', endpoint: basicApi.paymentMethods, add: '新增方式' },
  { key: 'trialSlots', label: '试听时段', endpoint: basicApi.trialSlots, add: '新增时段' }
]

// 当前页签对应的字典 key，默认课程分类；切换页签会按新字典重新拉数据
const activeKey = ref('categories')
const loading = ref(false)
const rows = ref([])
// 当前字典配置；页签值异常时兜底到第一项，避免下面读 config.value.capacity 时报错
const config = computed(() => DICTS.find((d) => d.key === activeKey.value) || DICTS[0])

const formOpen = ref(false)
const formSaving = ref(false)
const editing = ref(null)
// 弹窗表单：容量字段同时预备区间（capacityMin/capacityMax）与单值（capacity），按当前字典取用
const form = reactive({ name: '', description: '', status: 'ENABLED', capacityMin: 1, capacityMax: 8, capacity: 12, sort: 0 })

// 列定义随字典变化：只有带容量字段的字典（班型、教室）才追加容量列
const columns = computed(() => [
  { title: '名称', dataIndex: 'name', key: 'name', width: 180 },
  ...(config.value.capacity ? [{ title: '容量 / 人数', key: 'capacity', width: 140 }] : []),
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 160 }
])

// 按当前字典拉取全量列表：字典数据量小，一次性返回不分页
async function load() {
  loading.value = true
  try {
    rows.value = await basicApi.list(config.value.endpoint)
  } finally { loading.value = false }
}

// 打开新增弹窗：清空并重置全部字段（含容量默认值），避免残留上一次编辑的内容
function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', description: '', status: 'ENABLED', capacityMin: 1, capacityMax: 8, capacity: 12, sort: 0 })
  formOpen.value = true
}

// 打开编辑弹窗：回填当前行；容量字段用 ?? 兜底，后端可能返回 null
function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    name: row.name, description: row.description || '', status: row.status,
    capacityMin: row.capacityMin ?? 1, capacityMax: row.capacityMax ?? 8,
    capacity: row.capacity ?? 12, sort: row.sort || 0
  })
  formOpen.value = true
}

// 提交新增/编辑：名称必填校验在本地做，其余交给后端
async function submit() {
  if (!form.name) { message.warning('请填写名称'); return }
  formSaving.value = true
  try {
    const payload = {
      name: form.name, description: form.description, status: form.status, sort: Number(form.sort || 0)
    }
    // 容量按字典类型只提交对应的字段：班型写区间，教室写单值，其余字典不带容量字段
    if (config.value.capacity === 'range') {
      payload.capacityMin = Number(form.capacityMin)
      payload.capacityMax = Number(form.capacityMax)
    }
    if (config.value.capacity === 'single') {
      payload.capacity = Number(form.capacity)
    }
    // editing 有值为编辑，走 PUT；否则走 POST 新增
    if (editing.value) {
      await basicApi.update(config.value.endpoint, editing.value.id, payload)
      message.success('已保存')
    } else {
      await basicApi.create(config.value.endpoint, payload)
      message.success('已新增')
    }
    formOpen.value = false
    await load()
  } finally { formSaving.value = false }
}

// 启用/停用切换：整行回传并把 status 取反；row 本身不变，所以下面提示语仍按旧状态判断
async function toggle(row) {
  await basicApi.update(config.value.endpoint, row.id, { ...row, status: row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED' })
  message.success(row.status === 'ENABLED' ? '已停用' : '已启用')
  await load()
}

// 删除字典项：是否被课程/班级等引用由后端校验，被引用时接口报错并弹提示
async function remove(row) {
  await basicApi.remove(config.value.endpoint, row.id)
  message.success('已删除')
  await load()
}

// 切换字典页签：先切 key 再按新字典加载（columns/config 会随 activeKey 自动重算）
function onTab(key) {
  activeKey.value = key
  load()
}

// 进入页面先加载默认字典（课程分类）
onMounted(load)
</script>

<template>
  <div>
    <!-- 页头：说明、刷新与新增按钮（按钮文案随当前字典变化） -->
    <div class="page-head">
      <div>
        <div class="eyebrow">MASTER DATA</div>
        <h1>基础数据</h1>
        <p>课程分类、阶段、班型、教室、请假原因、缴费方式与试听时段维护</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <span class="tag gold">小程序下拉与筛选项来源</span>
        <a-button size="small" @click="load"><ReloadOutlined /> 刷新</a-button>
        <a-button v-if="$auth.can('basic','create')" type="primary" size="small" @click="openCreate"><PlusOutlined /> {{ config.add }}</a-button>
      </div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <!-- 字典页签：每个页签对应一类基础数据 -->
      <a-tabs :active-key="activeKey" @change="onTab">
        <a-tab-pane v-for="d in DICTS" :key="d.key" :tab="d.label" />
      </a-tabs>
      <!-- 字典表格：容量列按当前字典渲染区间或单值，操作列含编辑/启停/删除 -->
      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'capacity'">
            <span v-if="config.capacity === 'range'">{{ record.capacityMin }}–{{ record.capacityMax }} 人</span>
            <span v-else>{{ record.capacity }} 人</span>
          </template>
          <template v-if="column.key === 'status'"><span class="tag" :class="record.status === 'ENABLED' ? 'green' : 'gray'">{{ record.status === 'ENABLED' ? '启用' : '停用' }}</span></template>
          <template v-if="column.key === 'actions'">
            <div class="ops">
              <a-button v-if="$auth.can('basic','edit')" type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-button v-if="$auth.can('basic','edit')" type="link" size="small" @click="toggle(record)">{{ record.status === 'ENABLED' ? '停用' : '启用' }}</a-button>
              <a-popconfirm v-if="$auth.can('basic','delete')" title="确认删除该数据？" @confirm="remove(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
      <div class="footnote">基础数据用于课程、班级、报名、请假与缴费表单；停用后不再出现在新建表单中，历史数据保留。</div>
    </div>

    <!-- 新增/编辑弹窗：标题与容量字段都随当前字典变化 -->
    <a-modal v-model:open="formOpen" :title="(editing ? '编辑' : '新增') + config.label" ok-text="保存" cancel-text="取消" :confirm-loading="formSaving" @ok="submit">
      <a-form layout="vertical">
        <a-form-item label="名称" required><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="说明"><a-input v-model:value="form.description" /></a-form-item>
        <a-row v-if="config.capacity" :gutter="12">
          <template v-if="config.capacity === 'range'">
            <a-col :span="12"><a-form-item label="最少人数"><a-input-number v-model:value="form.capacityMin" style="width:100%" :min="1" /></a-form-item></a-col>
            <a-col :span="12"><a-form-item label="最多人数"><a-input-number v-model:value="form.capacityMax" style="width:100%" :min="1" /></a-form-item></a-col>
          </template>
          <a-col v-else :span="12"><a-form-item label="容量（人）"><a-input-number v-model:value="form.capacity" style="width:100%" :min="1" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="form.status"><a-select-option value="ENABLED">启用</a-select-option><a-select-option value="DISABLED">停用</a-select-option></a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="排序"><a-input-number v-model:value="form.sort" style="width:100%" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>
