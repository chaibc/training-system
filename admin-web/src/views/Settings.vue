<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ruleApi } from '../api'
import { ReloadOutlined } from '@ant-design/icons-vue'

const loading = ref(false)
const rules = ref([])
const editOpen = ref(false)
const editSaving = ref(false)
const editing = ref(null)
const form = reactive({ ruleValue: '', description: '' })

const contentIndex = [
  ['机构资料 / 首页文案 / 联系方式 / 地址', '机构内容 → 机构资料', '编辑、保存'],
  ['师资团队', '机构内容 → 师资团队', '新增、编辑、停用'],
  ['资质公示', '机构内容 → 资质公示', '新增、编辑、显隐、删除'],
  ['机构公告', '机构内容 → 公告管理', '新增、编辑、发布 / 隐藏、删除'],
  ['课程 / 分类 / 阶段 / 首页推荐', '课程与定价 → 课程管理；基础数据', '新增、编辑、上下架、删除'],
  ['班型与价格', '课程与定价 → 班型与价格；基础数据 → 班型', '新增、编辑、停售、删除'],
  ['内容大纲', '课程与定价 → 内容大纲', '新增、编辑、删除、显示 / 隐藏、排序'],
  ['作品 / 授权 / 评论', '作品管理', '上传、审核、下架、授权补录、评论审核'],
  ['请假原因 / 试听时段 / 缴费方式 / 教室', '基础数据', '新增、编辑、停用、删除'],
  ['账号 / 角色 / 家长绑定', '用户与权限', '新建后台账号、编辑、停用、重置密码、绑定审核']
]

async function load() {
  loading.value = true
  try { rules.value = await ruleApi.list() } finally { loading.value = false }
}
function openEdit(row) {
  editing.value = row
  Object.assign(form, { ruleValue: row.ruleValue, description: row.description || '' })
  editOpen.value = true
}
async function submit() {
  editSaving.value = true
  try {
    await ruleApi.update(editing.value.id, { ...editing.value, ...form })
    message.success('规则已保存，已同步全机构')
    editOpen.value = false
    await load()
  } finally { editSaving.value = false }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div>
        <div class="eyebrow">BUSINESS RULES</div>
        <h1>业务规则</h1>
        <p>请假、课时、报名规则与小程序内容维护索引 · 变更影响全机构</p>
      </div>
      <div class="sp"></div>
      <div class="actions"><a-button size="small" @click="load"><ReloadOutlined /> 刷新</a-button></div>
    </div>

    <div class="paper-card card-pad">
      <div class="card-title">课时与请假规则</div>
      <a-table :data-source="rules" :loading="loading" row-key="id" size="small" :pagination="false">
        <a-table-column title="规则键" data-index="ruleKey" key="ruleKey" width="220" />
        <a-table-column title="当前值" data-index="ruleValue" key="ruleValue" width="150" />
        <a-table-column title="说明" data-index="description" key="description" />
        <a-table-column title="操作" key="actions" width="80">
          <template #default="{ record }"><a-button type="link" size="small" @click="openEdit(record)">编辑</a-button></template>
        </a-table-column>
      </a-table>
      <div class="footnote">规则变更影响全机构；修改后新业务立即按新规则执行，历史记录不变。</div>
    </div>

    <div class="paper-card card-pad mt14">
      <div class="card-title">小程序内容维护索引</div>
      <a-table :data-source="contentIndex.map((r, i) => ({ key: i, content: r[0], entry: r[1], action: r[2] }))" :pagination="false" size="small">
        <a-table-column title="小程序内容" data-index="content" key="content" />
        <a-table-column title="维护入口" data-index="entry" key="entry" width="300" />
        <a-table-column title="维护能力" data-index="action" key="action" width="320" />
      </a-table>
      <div class="footnote">小程序端只读展示；上述内容修改均为真实后端数据，交付后可在管理后台直接维护。</div>
    </div>

    <a-modal v-model:open="editOpen" title="编辑业务规则" ok-text="保存" cancel-text="取消" :confirm-loading="editSaving" @ok="submit">
      <a-form layout="vertical">
        <a-form-item label="规则键"><a-input :value="editing?.ruleKey" disabled /></a-form-item>
        <a-form-item label="规则值" required><a-input v-model:value="form.ruleValue" /></a-form-item>
        <a-form-item label="说明"><a-input v-model:value="form.description" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
