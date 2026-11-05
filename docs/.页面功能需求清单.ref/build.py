# -*- coding: utf-8 -*-
# 生成《页面功能需求清单.xlsx》—— 由需求说明书第四章功能需求提炼
try:
    import openpyxl
except ImportError:
    import subprocess, sys
    subprocess.check_call([sys.executable, "-m", "pip", "install", "--quiet", "openpyxl>=3.1.0"])
    import openpyxl

from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.formatting.rule import CellIsRule
from openpyxl.utils import get_column_letter

def xl_color(css_hex: str) -> str:
    value = css_hex.removeprefix("#").upper()
    if len(value) != 6:
        raise ValueError(f"Expected #RRGGBB, got: {css_hex}")
    return "FF" + value

# 配色：商务蓝
XL_PRIMARY = xl_color("#4472C4")
XL_HEADER_TXT = xl_color("#FFFFFF")
XL_LIGHT = xl_color("#D9E2F3")
XL_SUM = xl_color("#2F5597")
XL_BORDER = xl_color("#BFBFBF")
XL_WHITE = xl_color("#FFFFFF")
XL_TEXT = xl_color("#1A1A1A")
XL_SUB = xl_color("#595959")
# 优先级条件格式色
XL_P0_BG, XL_P0_TX = xl_color("#FFC7CE"), xl_color("#9C0006")
XL_P1_BG, XL_P1_TX = xl_color("#FFEB9C"), xl_color("#9C6500")
XL_P2_BG, XL_P2_TX = xl_color("#C6EFCE"), xl_color("#006100")

thin_side = Side(style="thin", color=XL_BORDER)
BORDER = Border(left=thin_side, right=thin_side, top=thin_side, bottom=thin_side)

OUT = r"E:\deepseek\项目\书法培训\docs\页面功能需求清单.xlsx"

wb = openpyxl.Workbook()
ws = wb.active
ws.title = "功能清单"

# ============ Sheet 1: 功能清单（明细） ============
# 锚点：第1行标题（合并）、第2行表头、第3行起数据
headers = ["序号", "所属模块", "页面 / 界面", "功能点", "功能说明", "优先级", "建议分期", "承载端"]
widths = [6, 16, 18, 14, 44, 8, 8, 14]

ws.merge_cells("A1:H1")
c = ws["A1"]
c.value = "书法培训机构微信小程序 · 页面功能需求清单（V1.0）"
c.font = Font(name="微软雅黑", size=14, bold=True, color=XL_WHITE)
c.fill = PatternFill("solid", fgColor=XL_SUM)
c.alignment = Alignment(horizontal="center", vertical="center")
ws.row_dimensions[1].height = 30

for j, (h, w) in enumerate(zip(headers, widths), start=1):
    col = get_column_letter(j)
    ws.column_dimensions[col].width = w
    hc = ws.cell(row=2, column=j, value=h)
    hc.font = Font(name="微软雅黑", size=10.5, bold=True, color=XL_HEADER_TXT)
    hc.fill = PatternFill("solid", fgColor=XL_PRIMARY)
    hc.alignment = Alignment(horizontal="center", vertical="center")
    hc.border = BORDER

# 数据：所属模块, 页面/界面, 功能点, 功能说明, 优先级, 建议分期, 承载端
MP = "小程序（家长/学生）"; MT = "小程序（教师端）"; MA = "管理后台（Web）"
rows = [
 # 模块一 机构介绍
 ("模块一 机构介绍", "首页 · 机构门户", "机构简介", "图文展示机构介绍、办学理念、发展历程、教学环境（后台维护图文与多图）", "P0", "一期", MP),
 ("模块一 机构介绍", "首页 · 机构门户", "联系方式", "展示电话、微信、营业时间，支持一键拨号", "P0", "一期", MP),
 ("模块一 机构介绍", "首页 · 机构门户", "地址与导航", "展示校区地址（含经纬度），调用微信地图导航（wx.openLocation）", "P0", "一期", MP),
 ("模块一 机构介绍", "首页 · 机构门户", "校区列表", "多校区时支持切换查看", "P2", "三期", MP),
 ("模块一 机构介绍", "师资列表页", "师资列表", "展示教师头像、姓名、教龄、擅长书体、简介", "P0", "一期", MP),
 ("模块一 机构介绍", "教师详情页", "教师详情", "查看教师完整履历、教学特点与代表作品", "P1", "二期", MP),
 ("模块一 机构介绍", "机构介绍页", "资质公示", "公示营业执照及主管部门要求的培训资质（须真实可核验）", "P1", "二期", MP),
 ("模块一 机构介绍", "在线咨询", "在线咨询", "客服消息或电话咨询入口", "P1", "二期", MP),
 # 模块二 培训内容
 ("模块二 培训内容", "课程列表页", "课程分类", "按硬笔书法、软笔书法、幼小衔接分类展示", "P0", "一期", MP),
 ("模块二 培训内容", "课程详情页", "课程详情", "课程名称、适龄段、教学目标、内容大纲、课时安排、价格", "P0", "一期", MP),
 ("模块二 培训内容", "试听预约页", "试听预约", "免费或低价试听预约，收集意向信息供教务跟进", "P0", "一期", MP),
 ("模块二 培训内容", "课程详情页", "班型展示", "一对一、小班、大班等班型及对应价格", "P1", "二期", MP),
 ("模块二 培训内容", "课程列表页", "课程筛选", "按年龄段、书体、上课时段筛选", "P1", "二期", MP),
 ("模块二 培训内容", "课程详情页", "内容大纲", "分阶段呈现教学目标（握笔坐姿、基本笔画、间架结构、偏旁部首、章法布局）", "P1", "二期", MP),
 ("模块二 培训内容", "课程对比页", "课程对比", "多课程同屏比较课时、价格、上课时间", "P2", "三期", MP),
 # 模块三 报名
 ("模块三 报名功能", "报名流程页", "课程报名", "选择课程/课时包 → 选择班级 → 填写学员信息 → 提交", "P0", "一期", MP),
 ("模块三 报名功能", "支付页", "在线支付", "微信支付下单（JSAPI），支付成功后自动生成学员档案与课时账户", "P0", "一期", MP),
 ("模块三 报名功能", "报名表单页", "报名表单", "学员姓名、性别、出生日期、就读学校与年级、家长姓名、手机号、备注", "P0", "一期", MP),
 ("模块三 报名功能", "试听报名页", "试听报名", "试听预约单独表单，留资后由教务跟进", "P0", "一期", MP),
 ("模块三 报名功能", "订单管理页", "线下缴费登记", "到店缴费由教务在后台登记订单并生成课时（记录登记人与时间）", "P1", "一期", MA),
 ("模块三 报名功能", "我的 · 订单记录", "报名记录", "家长可查看本人名下的报名与订单记录", "P1", "二期", MP),
 ("模块三 报名功能", "报名/支付页", "优惠券/活动价", "支持优惠券、老带新等优惠方式", "P2", "三期", MP),
 ("模块三 报名功能", "订单详情页", "退费/转班", "由教务在后台处理，小程序端展示处理状态", "P2", "三期", "双端"),
 # 模块四 学生信息
 ("模块四 学生信息", "学员档案页", "学员档案", "基本信息、家长信息、所属校区与班级（教务维护全部，家长查看本子女）", "P0", "一期", "双端"),
 ("模块四 学生信息", "我的 · 学员管理", "多子女管理", "一个家长账号绑定多个学员并快速切换", "P0", "一期", MP),
 ("模块四 学生信息", "课时账户页", "课时账户", "剩余课时、已消课时、有效期、充值记录", "P0", "一期", MP),
 ("模块四 学生信息", "上课记录页", "上课记录", "每次上课的时间、教师、状态（出勤/请假/缺勤）", "P0", "一期", MP),
 ("模块四 学生信息", "请假记录页", "请假记录", "历史请假申请与审批结果", "P1", "二期", MP),
 ("模块四 学生信息", "续费记录页", "续费记录", "历史购买记录、金额与课时数", "P1", "二期", MP),
 ("模块四 学生信息", "学员档案页", "信息编辑", "家长可修改部分信息，关键信息变更需教务审核", "P1", "二期", "双端"),
 ("模块四 学生信息", "成长档案页", "成长档案", "阶段性作品与教师评语（可选）", "P2", "三期", MP),
 # 模块五 排课与课表（排课端）
 ("模块五 排课课表", "班级管理页", "班级管理", "创建班级，设置课程、任课教师、固定上课时间、教室、班级容量", "P0", "一期", MA),
 ("模块五 排课课表", "排课页", "排课", "支持按周模板批量排课（如每周六 9:00）与单次排课", "P0", "一期", MA),
 ("模块五 排课课表", "排课页", "冲突校验", "同一教师、教室或学员在同一时段不可重复排课", "P0", "一期", MA),
 ("模块五 排课课表", "调课页", "调课", "修改课次的时间、教师或教室，生成调课记录并触发通知", "P0", "一期", MA),
 ("模块五 排课课表", "点名消课页", "点名与消课", "课后按学员名单签到，自动或手动消课", "P0", "一期", MT + " / " + MA),
 ("模块五 排课课表", "排课页", "停课/加课", "支持节假日停课与临时加课", "P1", "二期", MA),
 ("模块五 排课课表", "排课页", "代课安排", "指定代课教师并同步通知家长", "P1", "二期", MA),
 # 模块五（家长端）
 ("模块五 排课课表", "我的课表页", "我的课表", "按周或月查看孩子课程安排（时间、课程、教师、教室、状态）", "P0", "一期", MP),
 ("模块五 排课课表", "我的课表页", "上课记录同步", "消课后课次状态更新为「已完成」", "P0", "一期", MP),
 ("模块五 排课课表", "课次详情页", "课次详情", "查看单节课信息与上课提醒设置", "P1", "二期", MP),
 ("模块五 排课课表", "我的课表页", "剩余课时展示", "在课表页顶部展示剩余课时", "P1", "一期", MP),
 # 模块六 消息通知
 ("模块六 消息通知", "消息中心页", "站内消息中心", "小程序「我的消息」列表，按类型与时间展示，支持已读未读", "P0", "一期", MP),
 ("模块六 消息通知", "（推送通道）", "微信订阅消息", "通过微信订阅消息向家长微信推送服务通知（一次性订阅为主）", "P0", "一期", "后端 + " + MP),
 ("模块六 消息通知", "报名/课表页", "订阅授权引导", "在报名成功、查看课表等关键操作节点引导家长授权订阅（须用户点击触发）", "P0", "一期", MP),
 ("模块六 消息通知", "消息模板配置页", "消息模板配置", "后台维护消息模板与触发规则", "P1", "二期", MA),
 ("模块六 消息通知", "下发记录页", "消息下发记录", "后台可查询下发记录与失败原因（含未授权转站内兜底）", "P1", "二期", MA),
 ("模块六 消息通知", "消息设置页", "通知开关", "家长可自行关闭非必要类型的通知", "P2", "三期", MP),
 # 模块七 请假
 ("模块七 请假申请", "请假申请页", "提交请假", "选择学员、选择待上课程与日期、填写原因并提交", "P0", "一期", MP),
 ("模块七 请假申请", "请假申请页", "规则校验", "按机构规则判断是否允许请假以及是否扣课时（课前4小时以上不扣，可配置）", "P0", "一期", "后端"),
 ("模块七 请假申请", "请假审批页", "审批", "教师或教务审批，支持同意、驳回与批量处理", "P0", "一期", MT + " / " + MA),
 ("模块七 请假申请", "（通知）", "结果通知", "审批结果推送家长（站内 + 订阅消息）", "P0", "一期", MP),
 ("模块七 请假申请", "补课安排页", "补课安排", "请假后可申请补课，由教务安排补课时间（不重复计费）", "P1", "二期", MA),
 ("模块七 请假申请", "请假记录页", "请假记录", "家长可查看历史请假与状态", "P1", "二期", MP),
 ("模块七 请假申请", "（规则配置）", "请假次数限制", "可配置每月请假次数上限", "P2", "三期", MA),
 # 模块八 统计查询（管理端）
 ("模块八 统计查询", "统计报表页", "学员统计", "在读、停课、结课、新增与流失人数及趋势", "P0", "一期", MA),
 ("模块八 统计查询", "统计报表页", "课时统计", "总课时、已消课时、剩余课时、消课率（口径与课时账户一致）", "P0", "一期", MA),
 ("模块八 统计查询", "消课明细页", "消课明细查询", "按学员、班级、教师、时间范围查询消课记录", "P0", "一期", MA),
 ("模块八 统计查询", "统计报表页", "收入统计", "按时间段的报名与续费金额、支付方式分布", "P1", "二期", MA),
 ("模块八 统计查询", "统计报表页", "教师统计", "各教师课时数、带班数、出勤率", "P1", "二期", MA),
 ("模块八 统计查询", "统计报表页", "请假统计", "请假次数、请假率、审批情况", "P1", "二期", MA),
 ("模块八 统计查询", "统计报表页", "出勤统计", "出勤率与缺勤率", "P1", "二期", MA),
 ("模块八 统计查询", "统计报表页", "续费分析", "续费人数、续费率、剩余课时预警名单", "P1", "二期", MA),
 ("模块八 统计查询", "统计报表页", "报表导出", "支持导出 Excel（权限校验，敏感字段脱敏）", "P1", "二期", MA),
 ("模块八 统计查询", "数据看板页", "数据看板", "管理后台首页可视化图表", "P1", "二期", MA),
 ("模块八 统计查询", "统计报表页", "报名转化分析", "试听到报名的转化率与来源统计", "P2", "三期", MA),
 # 模块八（家长端）
 ("模块八 统计查询", "我的 · 课时统计", "课时统计（家长）", "剩余课时、已消课时、累计上课次数", "P1", "二期", MP),
 ("模块八 统计查询", "我的 · 记录查询", "记录查询（家长）", "上课、请假、续费的明细记录", "P1", "二期", MP),
 # 模块九 作品展示
 ("模块九 作品展示", "作品墙页", "作品墙", "展示学员书法作品，可按书体、学员、时间分类（须家长授权+审核）", "P0", "一期", MP),
 ("模块九 作品展示", "作品详情页", "作品详情", "大图查看，展示学员、班型、创作时间与教师点评", "P0", "一期", MP),
 ("模块九 作品展示", "作品上传页", "作品上传", "教师或教务上传学员作品", "P0", "一期", MT + " / " + MA),
 ("模块九 作品展示", "作品详情页", "分享传播", "作品页与活动页支持转发好友及分享到朋友圈", "P1", "二期", MP),
 ("模块九 作品展示", "作品详情页", "点赞互动", "家长可点赞，评论需经后台审核后展示", "P1", "二期", MP),
 ("模块九 作品展示", "成长相册页", "学员成长相册", "以时间线展示单个学员的作品变化", "P1", "二期", MP),
 ("模块九 作品展示", "机构动态页", "机构动态", "发布通知、活动与招生信息", "P1", "二期", MP),
 ("模块九 作品展示", "评选活动页", "优秀作品评选", "在线投票或评选活动", "P2", "三期", MP),
 ("模块九 作品展示", "招生海报页", "招生海报", "活动海报与落地页", "P2", "三期", MP),
]

data_start = 3
for i, (mod, page, feat, desc, pri, phase, side) in enumerate(rows):
    r = data_start + i
    vals = [i + 1, mod, page, feat, desc, pri, phase, side]
    for j, v in enumerate(vals, start=1):
        cell = ws.cell(row=r, column=j, value=v)
        cell.border = BORDER
        cell.font = Font(name="微软雅黑", size=10, color=XL_TEXT)
        if j == 1:
            cell.alignment = Alignment(horizontal="center", vertical="center")
        elif j in (6, 7):
            cell.alignment = Alignment(horizontal="center", vertical="center")
        elif j == 5:
            cell.alignment = Alignment(horizontal="left", vertical="center", wrap_text=True)
        else:
            cell.alignment = Alignment(horizontal="left", vertical="center")

data_end = data_start + len(rows) - 1  # 数据区结束行

# 优先级条件格式（F 列）
ws.conditional_formatting.add(f"F{data_start}:F{data_end}",
    CellIsRule(operator="equal", formula=['"P0"'], fill=PatternFill("solid", fgColor=XL_P0_BG), font=Font(color=XL_P0_TX, bold=True)))
ws.conditional_formatting.add(f"F{data_start}:F{data_end}",
    CellIsRule(operator="equal", formula=['"P1"'], fill=PatternFill("solid", fgColor=XL_P1_BG), font=Font(color=XL_P1_TX)))
ws.conditional_formatting.add(f"F{data_start}:F{data_end}",
    CellIsRule(operator="equal", formula=['"P2"'], fill=PatternFill("solid", fgColor=XL_P2_BG), font=Font(color=XL_P2_TX)))

# 数据验证：优先级 / 分期下拉
dv_pri = DataValidation(type="list", formula1='"P0,P1,P2"', allow_blank=False)
dv_phase = DataValidation(type="list", formula1='"一期,二期,三期"', allow_blank=False)
ws.add_data_validation(dv_pri); ws.add_data_validation(dv_phase)
dv_pri.add(f"F{data_start}:F{data_end}")
dv_phase.add(f"G{data_start}:G{data_end}")

ws.freeze_panes = "A3"
ws.auto_filter.ref = f"A2:H{data_end}"

# ============ Sheet 2: 汇总（公式引用明细） ============
ws2 = wb.create_sheet("汇总")
# 锚点：第1行标题、第3行表头、第4~12行数据（9模块）、第13行合计
ws2.merge_cells("A1:F1")
c = ws2["A1"]
c.value = "功能需求优先级与分期汇总（自动统计，勿手改）"
c.font = Font(name="微软雅黑", size=13, bold=True, color=XL_WHITE)
c.fill = PatternFill("solid", fgColor=XL_SUM)
c.alignment = Alignment(horizontal="center", vertical="center")
ws2.row_dimensions[1].height = 26

h2 = ["所属模块", "功能点总数", "P0（必做）", "P1（重要）", "P2（可选）", "建议分期"]
w2 = [22, 11, 11, 11, 11, 14]
for j, (h, w) in enumerate(zip(h2, w2), start=1):
    ws2.column_dimensions[get_column_letter(j)].width = w
    hc = ws2.cell(row=3, column=j, value=h)
    hc.font = Font(name="微软雅黑", size=10.5, bold=True, color=XL_HEADER_TXT)
    hc.fill = PatternFill("solid", fgColor=XL_PRIMARY)
    hc.alignment = Alignment(horizontal="center", vertical="center")
    hc.border = BORDER

mods = ["模块一 机构介绍", "模块二 培训内容", "模块三 报名功能", "模块四 学生信息",
        "模块五 排课课表", "模块六 消息通知", "模块七 请假申请", "模块八 统计查询", "模块九 作品展示"]
src = f"功能清单!$B${data_start}:$B${data_end}"
pri = f"功能清单!$F${data_start}:$F${data_end}"
pha = f"功能清单!$G${data_start}:$G${data_end}"
r0, r1 = 4, 4 + len(mods) - 1  # 数据 4~12 行
for i, m in enumerate(mods):
    r = r0 + i
    ws2.cell(row=r, column=1, value=m)
    ws2.cell(row=r, column=2, value=f"=COUNTIF({src},A{r})")
    ws2.cell(row=r, column=3, value=f'=COUNTIFS({src},A{r},{pri},"P0")')
    ws2.cell(row=r, column=4, value=f'=COUNTIFS({src},A{r},{pri},"P1")')
    ws2.cell(row=r, column=5, value=f'=COUNTIFS({src},A{r},{pri},"P2")')
    # 分期以该模块多数为准：一期为主则写 一期（二期为P1增强）
    phase_label = ["一期", "一期", "一期", "一期", "一期", "一期（消息中心二期完善）",
                   "一期（补课二期）", "一期（报表二期）", "一期（互动二期）"][i]
    ws2.cell(row=r, column=6, value=phase_label)

total_row = r1 + 1  # 第13行合计
ws2.cell(row=total_row, column=1, value="合计")
for j in "BCDE":
    ws2.cell(row=total_row, column=openpyxl.utils.column_index_from_string(j),
             value=f"=SUM({j}{r0}:{j}{r1})")

# 样式
for r in range(r0, total_row + 1):
    for j in range(1, 7):
        cell = ws2.cell(row=r, column=j)
        cell.border = BORDER
        cell.font = Font(name="微软雅黑", size=10, color=XL_TEXT,
                         bold=(r == total_row))
        cell.alignment = Alignment(horizontal="center" if j > 1 else "left",
                                   vertical="center")
        if r == total_row:
            cell.fill = PatternFill("solid", fgColor=XL_LIGHT)
        if j == 6 and r != total_row:
            cell.alignment = Alignment(horizontal="left", vertical="center", wrap_text=True)
ws2.cell(row=total_row, column=6, value="—")

# 备注说明
note_row = total_row + 2
ws2.merge_cells(f"A{note_row}:F{note_row}")
nc = ws2.cell(row=note_row, column=1,
    value="口径说明：优先级与分期均取自需求说明书 V1.0；P0=一期必须实现，P1=重要功能，P2=可选增强；「建议分期」栏以模块主体口径标注，括号内为该模块后置到二期/三期的增强点。")
nc.font = Font(name="微软雅黑", size=9, color=XL_SUB)
nc.alignment = Alignment(horizontal="left", vertical="center", wrap_text=True)
ws2.row_dimensions[note_row].height = 32

wb.properties.title = "页面功能需求清单"
wb.save(OUT)
print("saved:", OUT, "| rows:", len(rows))
