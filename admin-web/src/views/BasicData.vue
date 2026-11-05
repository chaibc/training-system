<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { basicApi } from '../api'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const DICTS = [
  { key: 'categories', label: '课程分类', endpoint: basicApi.categories, add: '新增分类' },
  { key: 'stages', label: '课程阶段', endpoint: basicApi.stages, add: '新增阶段' },
  { key: 'classTypes', label: '班型', endpoint: basicApi.classTypes, add: '新增班型', capacity: 'range' },
  { key: 'classrooms', label: '教室', endpoint: basicApi.classrooms, add: '新增教室', capacity: 'single' },
  { key: 'leaveReasons', label: '请假原因', endpoint: basicApi.leaveReasons, add: '新增原因' },
  { key: 'paymentMethods', label: '缴费方式', endpoint: basicApi.paymentMethods, add: '新增方式' },
  { key: 'trialSlots', label: '试听时段', endpoint: basicApi.trialSlots, add: '新增时段' }
]

const activeKey = ref('categories')
const loading = ref(false)
const rows = ref([])
const config = computed(() => DICTS.find((d) => d.key === activeKey.value) || DICTS[0])

const formOpen = ref(false)
const formSaving = ref(false)
const editing = ref(null)
const form = reactive({ name: '', description: '', status: 'ENABLED', capacityMin: 1, capacityMax: 8, capacity: 12, sort: 0 })

const columns = computed(() => [
  { title: '名称', dataIndex: 'name', key: 'name', width: 180 },
  ...(config.value.capacity ? [{ title: '容量 / 人数', key: 'capacity', width: 140 }] : []),
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'actions', width: 160 }
])

async function load() {
  loading.value = true
  try {
    rows.value = await basicApi.list(config.value.endpoint)
  } finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', description: '', status: 'ENABLED', capacityMin: 1, capacityMax: 8, capacity: 12, sort: 0 })
  formOpen.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    name: row.name, description: row.description || '', status: row.status,
    capacityMin: row.capacityMin ?? 1, capacityMax: row.capacityMax ?? 8,
    capacity: row.capacity ?? 12, sort: row.sort || 0
  })
  formOpen.value = true
}

async function submit() {
  if (!form.name) { message.warning('请填写名称'); return }
  formSaving.value = true
  try {
    const payload = {
      name: form.name, description: form.description, status: form.status, sort: Number(form.sort || 0)
    }
    if (config.value.capacity === 'range') {
      payload.capacityMin = Number(form.capacityMin)
      payload.capacityMax = Number(form.capacityMax)
    }
    if (config.value.capacity === 'single') {
      payload.capacity = Number(form.capacity)
    }
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

async function toggle(row) {
  await basicApi.update(config.value.endpoint, row.id, { ...row, status: row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED' })
  message.success(row.status === 'ENABLED' ? '已停用' : '已启用')
  await load()
}

async function remove(row) {
  await basicApi.remove(config.value.endpoint, row.id)
  message.success('已删除')
  await load()
}

function onTab(key) {
  activeKey.value = key
  load()
}

onMounted(load)
</script>

<template>
  <div>
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
        <a-button type="primary" size="small" @click="openCreate"><PlusOutlined /> {{ config.add }}</a-button>
      </div>
    </div>

    <div class="paper-card" style="padding:0 16px 16px">
      <a-tabs :active-key="activeKey" @change="onTab">
        <a-tab-pane v-for="d in DICTS" :key="d.key" :tab="d.label" />
      </a-tabs>
      <a-table :columns="columns" :data-source="rows" :loading="loading" row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'capacity'">
            <span v-if="config.capacity === 'range'">{{ record.capacityMin }}–{{ record.capacityMax }} 人</span>
            <span v-else>{{ record.capacity }} 人</span>
          </template>
          <template v-if="column.key === 'status'"><span class="tag" :class="record.status === 'ENABLED' ? 'green' : 'gray'">{{ record.status === 'ENABLED' ? '启用' : '停用' }}</span></template>
          <template v-if="column.key === 'actions'">
            <div class="ops">
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="toggle(record)">{{ record.status === 'ENABLED' ? '停用' : '启用' }}</a-button>
              <a-popconfirm title="确认删除该数据？" @confirm="remove(record)"><a-button type="link" size="small" danger>删除</a-button></a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
      <div class="footnote">基础数据用于课程、班级、报名、请假与缴费表单；停用后不再出现在新建表单中，历史数据保留。</div>
    </div>

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
