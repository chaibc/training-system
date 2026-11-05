/**
 * 统计报表页：核心指标卡、报名与试听趋势、线下收入构成、教师课时统计、班级课时与出勤、续费分析。
 * 接口：reportApi.overview（GET /api/admin/reports/overview）、reportApi.renew（POST /api/admin/reports/renew）
 * 辅助接口：studentApi.options、basicApi.list(缴费方式)
 * 交互：切换统计区间、刷新、导出 CSV、登记线下续费（弹窗）
 *
 * 指标口径全部由后端计算，前端只负责展示与导出；CSV 为纯前端拼装，不调导出接口。
 */
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { reportApi, studentApi, basicApi } from '../api'
import { ReloadOutlined, DownloadOutlined, PlusOutlined } from '@ant-design/icons-vue'

const loading = ref(false)
const data = ref(null)
const months = ref(6)
// 趋势图基准：取报名与试听两个系列的最大值，同一组两根柱子共用比例尺；最小取 1 避免除零
const maxChart = computed(() => Math.max(1, ...((data.value?.chart || []).flatMap((x) => [Number(x.enrollments || 0), Number(x.trials || 0)]))))
// 收入构成条形图基准：取各缴费方式金额的最大值，最小取 1 避免除零
const maxRevenue = computed(() => Math.max(1, ...((data.value?.revenueByMethod || []).map((x) => Number(x.amount || 0)))))

const students = ref([])
const methods = ref([])
const renewOpen = ref(false)
const renewSaving = ref(false)
// 续费登记表单：24 课时 / 2880 元只是常用套餐的默认值，打开弹窗时会被重新赋值
const renewForm = reactive({
  studentId: undefined, lessons: 24, amount: 2880, paymentMethodId: undefined, item: '', note: ''
})
// 弹窗里展示的学员名：从续费名单点进来时有值，从页头按钮打开时为空
const renewStudentName = ref('')

// 按当前月份跨度拉取报表；失败提示由 http.js 统一弹出，这里只负责 loading
async function load() {
  loading.value = true
  try {
    data.value = await reportApi.overview(months.value)
  } finally { loading.value = false }
}

// 加载续费弹窗的数据源：学员下拉 + 缴费方式字典（两者互不依赖，并发请求）
async function loadBasics() {
  const [s, m] = await Promise.all([studentApi.options(), basicApi.list(basicApi.paymentMethods)])
  students.value = s
  methods.value = m
}

// 从续费名单某行打开弹窗：预填学员与该行班级，缴费方式默认取字典第一项
function openRenew(row) {
  renewStudentName.value = row.studentName
  Object.assign(renewForm, {
    studentId: row.studentId,
    lessons: 24,
    amount: 2880,
    paymentMethodId: methods.value[0]?.id,
    item: (row.classNames || '课程') + ' 续费 24 课时',
    note: '续费预警跟进'
  })
  renewOpen.value = true
}

// 页头「登记续费」打开空表单：不预选学员，适合临时到店缴费
function openRenewBlank() {
  renewStudentName.value = ''
  Object.assign(renewForm, {
    studentId: undefined, lessons: 24, amount: 2880,
    paymentMethodId: methods.value[0]?.id, item: '续费 24 课时', note: ''
  })
  renewOpen.value = true
}

// 提交续费登记：学员、缴费方式、课时、金额四项必填，课时与金额转成数字后再提交
async function submitRenew() {
  if (!renewForm.studentId) return message.warning('请选择学员')
  if (!renewForm.paymentMethodId) return message.warning('请选择缴费方式')
  if (!renewForm.lessons || renewForm.lessons <= 0) return message.warning('续费课时必须大于 0')
  if (!renewForm.amount || Number(renewForm.amount) <= 0) return message.warning('续费金额必须大于 0')
  renewSaving.value = true
  try {
    const res = await reportApi.renew({
      ...renewForm,
      lessons: Number(renewForm.lessons),
      amount: Number(renewForm.amount)
    })
    message.success(`续费已登记，充值 ${res.lessons} 课时，剩余 ${res.remainingHours} 课时`)
    renewOpen.value = false
    await load()
  } finally { renewSaving.value = false }
}

// 导出 CSV：全部在前端拼装，字段顺序与页面表格一致，不请求导出接口
function exportCsv() {
  if (!data.value) return
  // CSV 行结构：先抬头与核心指标，再依次拼接教师、班级、续费名单三个明细表
  const lines = []
  const s = data.value.stats
  lines.push(['翰墨学堂统计报表'])
  lines.push(['统计区间', data.value.period.startDate, data.value.period.endDate])
  lines.push([])
  lines.push(['核心指标'])
  lines.push(['报名申请', s.enrollments])
  lines.push(['累计消课（课时）', s.consumedHours])
  lines.push(['平均出勤率', s.attendanceRate + '%'])
  lines.push(['续费率', s.renewalRate + '%'])
  lines.push(['本月实收（元）', s.revenue])
  lines.push(['续费预警人数', s.warningStudents])
  lines.push([])
  lines.push(['教师课时统计'])
  lines.push(['教师', '带班数', '课次', '出勤率', '请假审批通过', '请假申请'])
  data.value.teacherStats.forEach((t) => lines.push([t.teacherName, t.classCount, t.lessonCount, t.attendanceRate + '%', t.leaveApproved, t.leaveTotal]))
  lines.push([])
  lines.push(['班级课时与出勤'])
  lines.push(['班级', '在读人数', '已消课时', '剩余课时', '出勤率', '请假率'])
  data.value.classStats.forEach((c) => lines.push([c.className, c.studentCount, c.consumedHours, c.remainingHours, c.attendanceRate + '%', c.leaveRate + '%']))
  lines.push([])
  lines.push(['续费预警名单'])
  lines.push(['学员', '班级', '剩余课时', '有效期至', '上次缴费'])
  data.value.renewals.forEach((r) => lines.push([r.studentName, r.classNames, r.remainingHours, r.validUntil || '', r.lastPaymentAt || '']))
  const csv = '﻿' + lines.map((row) => row.map((cell) => `"${String(cell ?? '').replaceAll('"', '""')}"`).join(',')).join('\n')
  // 报表内容已前置 UTF-8 BOM，Excel 直接双击打开不会乱码
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  // 用临时 a 标签触发浏览器下载，文件名带导出日期便于区分
  link.download = `翰墨学堂-统计报表-${dayjs().format('YYYYMMDD')}.csv`
  link.click()
  // 下载已开始，立即释放 object URL，避免内存泄漏
  URL.revokeObjectURL(url)
  message.success('报表已导出，可用 Excel 打开')
}

// 先加载下拉数据源再取报表：openRenew 里要用到 methods[0] 作默认缴费方式
onMounted(async () => {
  await loadBasics()
  await load()
})
</script>

<template>
  <div>
    <!-- 页头：统计区间选择、刷新与导出 CSV -->
    <div class="page-head">
      <div>
        <div class="eyebrow">REPORTS & ANALYTICS</div>
        <h1>统计报表</h1>
        <p>学员、课时、收入、教师、出勤与续费分析 · 口径与课时账户/点名记录一致</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-select v-model:value="months" size="small" style="width:110px" @change="load">
          <a-select-option :value="1">本月</a-select-option>
          <a-select-option :value="6">近 6 个月</a-select-option>
          <a-select-option :value="12">近 12 个月</a-select-option>
        </a-select>
        <a-button size="small" @click="load"><ReloadOutlined /> 刷新</a-button>
        <a-button type="primary" size="small" @click="exportCsv"><DownloadOutlined /> 导出 CSV</a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <!-- 顶部指标卡：报名申请 / 累计消课 / 本期实收 / 续费预警 -->
      <div class="stats">
        <div class="stat-card"><div class="lbl">报名申请</div><div class="v">{{ data?.stats.enrollments ?? '—' }}<em>条</em></div><div class="foot">其中续费缴费 {{ data?.stats.renewPayments ?? 0 }} 笔</div></div>
        <div class="stat-card jade"><div class="lbl">累计消课</div><div class="v">{{ data?.stats.consumedHours ?? '—' }}<em>课时</em></div><div class="foot">平均出勤率 {{ data?.stats.attendanceRate ?? 0 }}%</div></div>
        <div class="stat-card blue"><div class="lbl">本期实收</div><div class="v"><em>¥</em>{{ Number(data?.stats.revenue || 0).toLocaleString() }}</div><div class="foot">线下登记 · 不含线上支付</div></div>
        <div class="stat-card red"><div class="lbl">续费预警</div><div class="v">{{ data?.stats.warningStudents ?? '—' }}<em>人</em></div><div class="foot">剩余 ≤ {{ data?.stats.renewalThreshold ?? 3 }} 课时 · 待跟进</div></div>
      </div>

      <div class="grid-2">
        <div class="paper-card card-pad">
          <!-- 报名与试听趋势：双柱图，柱高按 maxChart 等比换算 -->
          <div class="card-title">报名与试听趋势</div>
          <div class="legend">
            <span><i class="dot red"></i>报名申请</span>
            <span><i class="dot gold"></i>试听预约</span>
            <span class="sp"></span>
            <span class="muted" style="font-size:9px">单位：条</span>
          </div>
          <div class="chart2">
            <div v-for="item in (data?.chart || [])" :key="item.key" class="chart2-col">
              <div class="chart2-bars">
                <div class="cbar red" :style="{ height: Math.max(3, Number(item.enrollments) / maxChart * 140) + 'px' }"><span>{{ item.enrollments }}</span></div>
                <div class="cbar gold" :style="{ height: Math.max(3, Number(item.trials) / maxChart * 140) + 'px' }"><span>{{ item.trials }}</span></div>
              </div>
              <div class="chart2-label">{{ item.month }}</div>
            </div>
          </div>
          <div class="footnote">报名申请含待确认与已入班；试听预约按提交时间统计。</div>
        </div>

        <div class="paper-card card-pad">
          <!-- 线下收入构成：按缴费方式展示金额占比，宽度按 maxRevenue 等比换算 -->
          <div class="card-title">线下收入构成</div>
          <div class="funnel">
            <div v-for="item in (data?.revenueByMethod || [])" :key="item.name" class="funnel-row">
              <div class="fl">{{ item.name }}</div>
              <div class="track"><div class="fill" :style="{ width: Math.max(4, Number(item.amount) / maxRevenue * 100) + '%' }">¥{{ Number(item.amount).toLocaleString() }}</div></div>
              <div class="rate">{{ item.rate }}%</div>
            </div>
            <div v-if="!data?.revenueByMethod?.length" class="empty" style="padding:30px 0">本期暂无缴费记录</div>
          </div>
          <div class="footnote">按线下缴费登记统计，合计 <b>¥{{ Number(data?.stats.revenue || 0).toLocaleString() }}</b>；缴费方式在「基础数据」维护。</div>
        </div>
      </div>

      <div class="grid-2 mt14">
        <div class="paper-card card-pad">
          <!-- 教师课时统计表：出勤率 ≥ 95% 标绿，否则标黄 -->
          <div class="card-title">教师课时统计</div>
          <a-table :data-source="data?.teacherStats || []" row-key="teacherId" size="small" :pagination="false">
            <a-table-column title="教师" data-index="teacherName" key="teacherName" />
            <a-table-column title="带班数" data-index="classCount" key="classCount" width="70" />
            <a-table-column title="课次" data-index="lessonCount" key="lessonCount" width="60" />
            <a-table-column title="出勤率" key="attendanceRate" width="75">
              <template #default="{ record }"><span class="tag" :class="Number(record.attendanceRate) >= 95 ? 'green' : 'gold'">{{ record.attendanceRate }}%</span></template>
            </a-table-column>
            <a-table-column title="请假审批" key="leave" width="90">
              <template #default="{ record }">{{ record.leaveApproved }} / {{ record.leaveTotal }}</template>
            </a-table-column>
          </a-table>
        </div>

        <div class="paper-card card-pad">
          <!-- 班级课时与出勤统计表 -->
          <div class="card-title">课时与出勤（按班级）</div>
          <a-table :data-source="data?.classStats || []" row-key="classId" size="small" :pagination="false">
            <a-table-column title="班级" data-index="className" key="className" />
            <a-table-column title="在读" data-index="studentCount" key="studentCount" width="55" />
            <a-table-column title="已消" data-index="consumedHours" key="consumedHours" width="55" />
            <a-table-column title="剩余" data-index="remainingHours" key="remainingHours" width="55" />
            <a-table-column title="出勤率" key="attendanceRate" width="70">
              <template #default="{ record }">{{ record.attendanceRate }}%</template>
            </a-table-column>
            <a-table-column title="请假率" key="leaveRate" width="70">
              <template #default="{ record }">{{ record.leaveRate }}%</template>
            </a-table-column>
          </a-table>
        </div>
      </div>

      <div class="paper-card card-pad mt14">
        <div class="between" style="margin-bottom:10px">
          <!-- 续费分析：剩余课时预警名单，行内可直接登记续费 -->
          <div class="card-title" style="margin:0">续费分析</div>
          <a-button size="small" @click="openRenewBlank"><PlusOutlined /> 登记续费</a-button>
        </div>
        <a-table :data-source="data?.renewals || []" row-key="studentId" size="small" :pagination="false">
          <a-table-column title="学员" key="studentName" width="110">
            <template #default="{ record }"><b>{{ record.studentName }}</b></template>
          </a-table-column>
          <a-table-column title="班级" data-index="classNames" key="classNames" />
          <a-table-column title="剩余课时" key="remainingHours" width="90">
            <template #default="{ record }"><span class="tag red">{{ record.remainingHours }}</span></template>
          </a-table-column>
          <a-table-column title="有效期至" data-index="validUntil" key="validUntil" width="110" />
          <a-table-column title="上次缴费" key="lastPayment" width="160">
            <template #default="{ record }">{{ record.lastPaymentAt ? dayjs(record.lastPaymentAt).format('YYYY-MM-DD') : '—' }}<div class="muted" style="font-size:8px">{{ record.lastPaymentItem }}</div></template>
          </a-table-column>
          <a-table-column title="操作" key="actions" width="100">
            <template #default="{ record }"><a-button type="link" size="small" @click="openRenew(record)">登记续费</a-button></template>
          </a-table-column>
        </a-table>
        <div class="footnote">续费名单由课时账户自动计算；本期通过电话/到店跟进，不发送微信提醒。导出不含手机号。</div>
      </div>
    </a-spin>

    <!-- 登记续费弹窗：提交后立即加课时并写流水，所以提示需确认线下已收款 -->
    <a-modal v-model:open="renewOpen" title="登记线下续费" ok-text="确认充值" cancel-text="取消"
             :confirm-loading="renewSaving" @ok="submitRenew">
      <a-form layout="vertical">
        <a-form-item label="学员" required>
          <a-select v-model:value="renewForm.studentId" show-search option-filter-prop="label" placeholder="选择学员">
            <a-select-option v-for="s in students" :key="s.id" :value="s.id" :label="s.name + ' ' + (s.studentNo || '')">
              {{ s.name }} <span class="muted">{{ s.studentNo }}</span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="充值课时" required><a-input-number v-model:value="renewForm.lessons" style="width:100%" :min="1" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="实收金额（元）" required><a-input-number v-model:value="renewForm.amount" style="width:100%" :min="0.01" :precision="2" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="缴费方式" required>
          <a-select v-model:value="renewForm.paymentMethodId">
            <a-select-option v-for="m in methods" :key="m.id" :value="m.id">{{ m.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="缴费项目"><a-input v-model:value="renewForm.item" placeholder="如 软笔基础 24 课时续费" /></a-form-item>
        <a-form-item label="备注"><a-textarea v-model:value="renewForm.note" :rows="2" /></a-form-item>
      </a-form>
      <div class="form-tip">登记后立即增加课时并写入流水。线下收款请确认已实际到账。</div>
    </a-modal>
  </div>
</template>

<style scoped>
.legend { display: flex; align-items: center; gap: 14px; font-size: 10px; color: var(--muted); margin: 4px 0 12px; }
.legend .dot { display: inline-block; width: 8px; height: 8px; border-radius: 2px; margin-right: 4px; }
.legend .dot.red { background: #B4473C; }
.legend .dot.gold { background: #C2A05E; }
.chart2 { display: flex; align-items: flex-end; justify-content: space-between; gap: 8px; padding: 20px 8px 0; border-bottom: 1px solid #F0E9DC; height: 185px; }
.chart2-col { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; }
.chart2-bars { display: flex; align-items: flex-end; gap: 4px; height: 150px; }
.cbar { width: 14px; border-radius: 3px 3px 0 0; position: relative; min-height: 3px; }
.cbar.red { background: linear-gradient(180deg, #C97468, #B4473C); }
.cbar.gold { background: linear-gradient(180deg, #D9C28F, #C2A05E); }
.cbar span { position: absolute; top: -14px; left: 50%; transform: translateX(-50%); font-size: 8px; color: var(--ink2); }
.chart2-label { font-size: 9px; color: var(--muted); margin-top: 8px; }
.form-tip { padding: 8px 10px; border-radius: 7px; background: rgba(194,160,94,.1); color: #6F5A32; font-size: 10px; line-height: 1.7; }
</style>
