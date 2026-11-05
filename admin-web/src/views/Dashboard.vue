/**
 * 数据看板：经营与教务核心指标（在读学员、本月消课、本月实收、待处理报名）、
 * 近 6 个月消课柱状图、今日概览与最近报名申请。
 * 接口：GET /api/admin/dashboard（api/index.js 的 dashboardApi.get）
 *
 * 页面只做展示，指标口径全部由后端计算，前端不做二次汇总。
 */
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { dashboardApi } from '../api'
import {
  TeamOutlined, CheckSquareOutlined, WalletOutlined, FileTextOutlined,
  CalendarOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const loading = ref(false)
const data = ref(null)

// 柱状图基准：取各月消课课时的最大值，用于把数值换算成柱高；最小取 1，避免全为 0 时除零
const maxChart = computed(() => {
  const values = (data.value?.chart || []).map((x) => Number(x.value || 0))
  return Math.max(1, ...values)
})

// 报名状态码 → 中文标签 + 标签配色类名，状态值由后端返回（PENDING_CONFIRM 等）
const statusMap = {
  PENDING_CONFIRM: { text: '待确认', cls: 'gold' },
  PENDING_PAYMENT: { text: '待缴费', cls: 'red' },
  ENROLLED: { text: '已入班', cls: 'green' },
  CANCELLED: { text: '已取消', cls: 'gray' }
}

// 最近报名表格列：金额补 ¥ 与千分位、时间统一成 MM-DD HH:mm，空值显示 —
const columns = [
  { title: '申请编号', dataIndex: 'enrollmentNo', key: 'enrollmentNo', width: 150 },
  { title: '学员', dataIndex: 'studentName', key: 'studentName' },
  { title: '意向课程', dataIndex: 'courseName', key: 'courseName' },
  { title: '应缴金额', dataIndex: 'amount', key: 'amount', width: 100,
    customRender: ({ text }) => `¥${Number(text || 0).toLocaleString()}` },
  { title: '提交时间', dataIndex: 'submittedAt', key: 'submittedAt', width: 140,
    customRender: ({ text }) => text ? dayjs(text).format('MM-DD HH:mm') : '—' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 }
]

// 拉取看板数据；接口失败时 http.js 已统一弹提示，这里保留上次数据不清空
async function load() {
  loading.value = true
  try { data.value = await dashboardApi.get() } finally { loading.value = false }
}

// 进入页面即加载一次；不做轮询，需要最新数据时点页头的刷新
onMounted(load)
</script>

<template>
  <div>
    <!-- 页头：标题与刷新按钮 -->
    <div class="page-head">
      <div>
        <div class="eyebrow">OVERVIEW · {{ dayjs().format('YYYY 年 M 月') }}</div>
        <h1>数据看板</h1>
        <p>经营与教务核心指标 · 统计口径与课时账户保持一致</p>
      </div>
      <div class="sp"></div>
      <div class="actions">
        <a-button size="small" :loading="loading" @click="load">刷新</a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <!-- 顶部指标卡：在读学员 / 本月消课 / 本月实收 / 待处理报名 -->
      <div class="stats">
        <div class="stat-card jade">
          <div class="lbl"><TeamOutlined /> 在读学员</div>
          <div class="v">{{ data?.students ?? '—' }}<em>人</em></div>
          <div class="foot">当前在读 · 含幼小衔接与软硬笔</div>
        </div>
        <div class="stat-card">
          <div class="lbl"><CheckSquareOutlined /> 本月消课</div>
          <div class="v">{{ data?.monthConsumedHours ?? '—' }}<em>课时</em></div>
          <div class="foot">来自点名与消课记录</div>
        </div>
        <div class="stat-card red">
          <div class="lbl"><WalletOutlined /> 本月实收（线下）</div>
          <div class="v"><em>¥</em>{{ Number(data?.monthRevenue || 0).toLocaleString() }}</div>
          <div class="foot">报名 / 续费 · 后台登记</div>
        </div>
        <div class="stat-card blue">
          <div class="lbl"><FileTextOutlined /> 待处理报名</div>
          <div class="v">{{ data?.pendingEnrollments ?? '—' }}<em>项</em></div>
          <div class="foot">待确认或待缴费</div>
        </div>
      </div>

      <!-- 中部两栏：近 6 个月消课柱状图 + 今日概览 -->
      <div class="grid-2">
        <div class="paper-card card-pad">
          <div class="card-title">近 6 个月消课课时</div>
          <div class="chart">
            <div v-for="item in (data?.chart || [])" :key="item.month" class="chart-col">
              <div class="cap">{{ item.value }}</div>
              <div class="bar" :class="{ zero: Number(item.value) === 0 }"
                   :style="{ height: Math.max(3, Number(item.value) / maxChart * 130) + 'px' }"></div>
              <div class="mon">{{ item.month }}</div>
            </div>
          </div>
          <div class="footnote">单位：课时 · 数据来自点名与消课记录</div>
        </div>
        <div class="paper-card card-pad">
          <div class="card-title">今日概览</div>
          <div style="display:flex;flex-direction:column;gap:10px">
            <div style="display:flex;align-items:center;gap:10px;padding:10px 0;border-bottom:1px dashed var(--line)">
              <div style="width:30px;height:30px;border-radius:8px;background:rgba(180,71,60,.08);color:var(--red);display:flex;align-items:center;justify-content:center"><CalendarOutlined /></div>
              <div style="flex:1"><b style="font-size:11px">今日课程</b><div class="muted" style="font-size:9px;margin-top:2px">按课表查看今日课次</div></div>
              <span class="tag green">{{ data?.todayLessons ?? 0 }} 节</span>
            </div>
            <div style="display:flex;align-items:center;gap:10px;padding:10px 0;border-bottom:1px dashed var(--line)">
              <div style="width:30px;height:30px;border-radius:8px;background:rgba(194,160,94,.14);color:#8B6828;display:flex;align-items:center;justify-content:center"><TeamOutlined /></div>
              <div style="flex:1"><b style="font-size:11px">教师团队</b><div class="muted" style="font-size:9px;margin-top:2px">在职教师与带班情况</div></div>
              <span class="tag blue">{{ data?.teachers ?? 0 }} 人</span>
            </div>
            <div style="display:flex;gap:8px;margin-top:10px">
              <a-button size="small" block @click="router.push('/students')">学员管理</a-button>
              <a-button size="small" block @click="router.push('/courses')">课程与定价</a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 最近报名申请：仅展示最近若干条，完整列表见报名与缴费页 -->
      <div class="paper-card card-pad mt14">
        <div class="card-title">最近报名申请</div>
        <!-- 报名表格：状态列用 statusMap 映射中文标签，未知状态原样显示 -->
        <a-table :columns="columns" :data-source="data?.recentEnrollments || []" :pagination="false" row-key="id" size="small">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <span class="tag" :class="statusMap[record.status]?.cls || 'gray'">{{ statusMap[record.status]?.text || record.status }}</span>
            </template>
            <template v-if="column.key === 'studentName'">
              <b>{{ record.studentName }}</b>
            </template>
          </template>
        </a-table>
      </div>
    </a-spin>
  </div>
</template>
