import re
from pathlib import Path

root = Path(r"E:\deepseek\项目\书法培训")
schema = (root / "sql/schema.sql").read_text(encoding="utf-8")
out = root / "docs/01-表结构说明.md"

TABLE_DESC = {
    "sys_role": "系统角色字典。内置管理者、教务、教师、家长四种角色。管理者唯一，教师/家长用于小程序身份。",
    "sys_role_permission": "管理端模块权限矩阵。按模块记录查看、新增、编辑、删除和特殊操作权限，登录后下发到管理端。",
    "sys_user": "统一账号表。后台账号、教师账号、家长账号共用；一个账号只有一种 user_type，教师与家长身份互斥。",
    "sys_audit_log": "关键操作与登录日志。登录失败、绑定审核、点名消课、缴费、续费、权限变更等在此留痕。",
    "teacher_profile": "教师档案。与 sys_user 一对一关联，保存教龄、书体、简介、展示状态等小程序展示信息。",
    "parent_profile": "家长档案。微信首次登录自动创建，保存昵称与备注；子女关系在 parent_student。",
    "student": "学员档案。一个学员可被最多 2 位有效监护人绑定，primary_parent_user_id 为主联系人。",
    "parent_student": "家长与学员绑定关系。状态 PENDING/ACTIVE/REJECTED/REVOKED；SELF 为家长申请，ADMIN 为后台新增，UNBIND 表示解绑申请（绑定保持 ACTIVE，待审核通过后改为 REVOKED）。",
    "course_category": "课程分类字典（硬笔 / 软笔 / 幼小衔接等），小程序筛选项来源。",
    "course_stage": "课程阶段字典（启蒙 / 基础 / 提高 / 创作等）。",
    "class_type": "班型字典（一对一 / 小班 / 大班 / 体验班），含容量区间。",
    "classroom": "教室字典，含容量与用途说明，排课冲突校验使用。",
    "leave_reason": "请假原因字典，家长小程序请假时选择。",
    "payment_method": "线下缴费方式字典，只用于前台登记，不接线上支付。",
    "trial_slot": "试听时段字典，试听预约表单选择。",
    "course": "课程主档。课程名称、分类、阶段、适龄、课时、频率、封面字、标签、上架状态由后台维护。",
    "course_price": "课程班型价格。一个课程可维护多种班型/课时包，unit=TERM 表示按期，TIME 表示按次。",
    "course_outline": "课程内容大纲。按 sort 排序展示，visible=1 才在小程序课程详情展示。",
    "class_group": "班级。绑定课程、教师、教室与固定周时间；capacity 为班级容量。",
    "class_student": "班级学员关系。同一学员可加入多个班级；班级名单与课次点名以此为准。",
    "lesson": "课次。按班级生成，记录日期、起止时间、教师、教室、状态；原课次字段用于调课/补课关联。",
    "attendance": "点名记录与消课结果。同一课次+学员唯一；consumed_hours 为实际消课课时。",
    "leave_request": "请假申请。包含请假课次、原因、审批状态、是否扣课、审批人与补课状态。",
    "makeup_arrangement": "补课安排。请假通过后，教务可安排补课课次，不重复计费。",
    "enrollment": "报名申请。状态 PENDING_CONFIRM → PENDING_PAYMENT → ENROLLED；缴费由后台线下登记。",
    "trial_booking": "试听预约留资。状态 NEW/CONTACTED/VISITED/CONVERTED/CANCELLED，教务电话跟进。",
    "payment_record": "线下缴费记录。缴费成功可触发课时充值；支持首购 NEW 与续费 RENEW。",
    "hour_account": "学员课时账户。总课时、已消课时、剩余课时、有效期；每学员唯一。",
    "hour_ledger": "课时流水。充值、消课、修正均记录变动值与变动后余额，可追溯。",
    "work": "学员作品。须 authorized=1 且审核 DISPLAYED 才在小程序作品墙展示。",
    "work_comment": "作品留言。家长提交后 PENDING，教务审核 APPROVED 后展示。",
    "work_auth": "家长作品授权。按学员+家长唯一，记录授权范围、方式、日期、凭证与撤销状态。",
    "institution_info": "机构资料单行表。小程序首页、机构介绍、地图导航与联系电话来源。",
    "institution_image": "机构环境图片。小程序首页/机构介绍展示，按 sort 排序。",
    "qualification": "资质公示。visible=1 且 verify_status=VERIFIED 才在机构介绍展示。",
    "notice": "公告。DRAFT/PUBLISHED/HIDDEN，发布后在小程序展示。",
    "business_rule": "业务规则开关与参数。请假截止、续费预警、课时有效期、自动消课、评论与补课开关。",
    "upload_file": "上传文件元数据。记录原文件名、存储名、路径、URL、类型、大小与上传人。",
}

RELATIONS = {
    "sys_user": "role_id → sys_role.id；teacher_profile.user_id → sys_user.id；parent_profile.user_id → sys_user.id；student.primary_parent_user_id → sys_user.id。",
    "teacher_profile": "user_id → sys_user.id（1:1）。",
    "student": "primary_parent_user_id → sys_user.id（主联系人）；parent_student.student_id → student.id；class_student.student_id → student.id。",
    "parent_student": "parent_user_id → sys_user.id；student_id → student.id。",
    "course": "category_id → course_category.id；stage_id → course_stage.id。",
    "course_price": "course_id → course.id；class_type_id → class_type.id。",
    "course_outline": "course_id → course.id。",
    "class_group": "course_id → course.id；teacher_id → teacher_profile.id；classroom_id → classroom.id。",
    "class_student": "class_id → class_group.id；student_id → student.id。",
    "lesson": "class_id → class_group.id；teacher_id → teacher_profile.id；classroom_id → classroom.id；original_lesson_id → lesson.id（调课/补课来源）。",
    "attendance": "lesson_id → lesson.id；student_id → student.id；recorded_by → sys_user.id。",
    "leave_request": "student_id → student.id；lesson_id → lesson.id；reason_id → leave_reason.id；approver_id → sys_user.id。",
    "makeup_arrangement": "leave_request_id → leave_request.id；student_id → student.id；lesson_id → lesson.id。",
    "enrollment": "student_id → student.id；course_id → course.id；class_id → class_group.id；parent_user_id → sys_user.id。",
    "trial_booking": "course_id → course.id；slot_id → trial_slot.id。",
    "payment_record": "enrollment_id → enrollment.id（可空）；student_id → student.id；payment_method_id → payment_method.id；registered_by → sys_user.id。",
    "hour_account": "student_id → student.id（1:1）。",
    "hour_ledger": "student_id → student.id；lesson_id → lesson.id（可空）；operator_id → sys_user.id。",
    "work": "student_id → student.id；class_id → class_group.id；teacher_id → teacher_profile.id。",
    "work_comment": "work_id → work.id；parent_user_id → sys_user.id；reviewed_by → sys_user.id。",
    "work_auth": "student_id → student.id；parent_user_id → sys_user.id。",
    "qualification": "无外键，按 sort 展示。",
    "notice": "created_by → sys_user.id。",
    "upload_file": "uploaded_by → sys_user.id。",
}

FIELD_EXTRA = {
    "id": "主键，自增",
    "created_at": "创建时间",
    "updated_at": "更新时间",
    "deleted": "逻辑删除：0 正常，1 已删除",
    "status": "业务状态，取值见数据库 COMMENT 或状态字典",
    "type": "类型",
    "sort": "展示排序，越小越靠前",
}

FIELD_EXTRA.update({
    "username": "登录用户名（后台账号）",
    "password_hash": "BCrypt 密码哈希",
    "user_type": "账号身份：ADMIN/ACADEMIC/TEACHER/PARENT",
    "role_id": "角色 ID",
    "name": "名称 / 姓名",
    "phone": "手机号",
    "avatar": "头像 URL",
    "wechat_openid": "微信 OpenID（小程序登录）",
    "wechat_unionid": "微信 UnionID",
    "failed_attempts": "连续登录失败次数",
    "locked_until": "锁定截止时间",
    "last_login_at": "最后登录时间",
    "last_login_ip": "最后登录 IP",
    "force_change_password": "是否强制修改密码：0 否，1 是",
    "remark": "备注",
    "user_id": "关联 sys_user.id",
    "teacher_no": "教师工号",
    "title": "职称 / 课程标题",
    "years": "教龄（年）",
    "styles": "擅长书体",
    "courses_text": "所授课程（展示文本）",
    "bio": "个人简介",
    "quote": "教学格言",
    "tags": "标签，英文逗号分隔",
    "parent_user_id": "家长账号 ID",
    "student_id": "学员 ID",
    "student_no": "学员编号",
    "gender": "性别",
    "birth_date": "出生日期",
    "school": "学校",
    "grade": "年级 / 班级",
    "enroll_date": "入班 / 建档日期",
    "primary_parent_user_id": "主联系人家长账号 ID",
    "relation": "与学员关系：MOTHER/FATHER/GRANDPARENT/OTHER",
    "is_primary": "是否主联系人：0 否，1 是",
    "source": "数据来源",
    "verified_by": "审核人 ID",
    "verified_at": "审核时间",
    "code": "编码",
    "description": "说明",
    "capacity_min": "最小开班人数",
    "capacity_max": "最大开班人数",
    "capacity": "容量",
    "category_id": "课程分类 ID",
    "stage_id": "课程阶段 ID",
    "age_range": "适龄范围",
    "lessons": "课时数",
    "frequency": "上课频率说明",
    "duration_minutes": "单次时长（分钟）",
    "cover_char": "封面展示字",
    "cover_color": "封面渐变色",
    "goal": "教学目标",
    "featured": "是否首页推荐：0 否，1 是",
    "class_type_id": "班型 ID",
    "price": "价格 / 金额",
    "unit": "计价单位：TERM 按期 / TIME 按次",
    "course_id": "课程 ID",
    "lesson_range": "对应课时范围",
    "visible": "是否展示：0 隐藏，1 展示",
    "class_id": "班级 ID",
    "teacher_id": "教师档案 ID",
    "classroom_id": "教室 ID",
    "weekday": "星期：1=周一 … 7=周日",
    "start_time": "开始时间",
    "end_time": "结束时间",
    "lesson_id": "课次 ID",
    "lesson_no": "课次序号",
    "lesson_date": "上课日期",
    "original_lesson_id": "原课次 ID（调课/补课来源）",
    "note": "备注",
    "consumed_hours": "本次消课课时",
    "recorded_by": "记录人账号 ID",
    "recorded_at": "记录时间",
    "leave_no": "请假单号",
    "reason_id": "请假原因字典 ID",
    "reason_text": "请假原因说明",
    "deduct_hours": "是否扣课时：0 否，1 是",
    "submitted_at": "提交时间",
    "approver_id": "审批人账号 ID",
    "approved_at": "审批时间",
    "reject_reason": "驳回原因",
    "makeup_status": "补课状态：NONE/PENDING/ARRANGED",
    "leave_request_id": "请假申请 ID",
    "arranged_by": "安排人账号 ID",
    "arranged_at": "安排时间",
    "enrollment_no": "报名申请编号",
    "amount": "金额",
    "confirmed_by": "确认人账号 ID",
    "confirmed_at": "确认时间",
    "booking_no": "试听预约编号",
    "age": "年龄",
    "parent_name": "家长称呼",
    "slot_id": "试听时段 ID",
    "follow_up_note": "跟进备注",
    "next_follow_up": "下次跟进日期",
    "payment_no": "缴费单号",
    "enrollment_id": "报名申请 ID（续费可为空）",
    "item": "缴费项目",
    "payment_method_id": "缴费方式 ID",
    "paid_at": "缴费时间",
    "registered_by": "登记人账号 ID",
    "total_hours": "累计购买课时",
    "used_hours": "累计已消课时",
    "remaining_hours": "剩余课时",
    "valid_until": "课时有效期",
    "change_hours": "变动课时（正数增加，负数扣减）",
    "balance_after": "变动后余额",
    "operator_id": "操作人账号 ID",
    "reason": "流水原因",
    "work_type": "作品类型：HARD 硬笔 / SOFT 软笔",
    "content_char": "作品内容 / 单字",
    "image_url": "图片 URL",
    "comment": "教师点评",
    "authorized": "家长是否授权：0 否，1 是",
    "like_count": "点赞数",
    "work_id": "作品 ID",
    "content": "内容",
    "reviewed_by": "审核人账号 ID",
    "reviewed_at": "审核时间",
    "scope": "授权范围",
    "method": "授权方式",
    "authorized_at": "授权日期",
    "evidence_url": "授权凭证 URL / 编号",
    "brand": "品牌简称",
    "slogan": "主标语",
    "hero_intro": "首页简介",
    "intro": "机构简介",
    "philosophy": "办学理念",
    "wechat": "机构微信",
    "hours": "营业时间",
    "address": "地址",
    "longitude": "经度",
    "latitude": "纬度",
    "traffic": "交通指引",
    "since_year": "创办年份",
    "institution_id": "机构 ID",
    "valid_until_text": "有效期说明",
    "verify_status": "核验状态",
    "publish_date": "发布日期",
    "created_by": "创建人账号 ID",
    "rule_key": "规则键",
    "rule_value": "规则值",
    "original_name": "原始文件名",
    "stored_name": "存储文件名",
    "file_path": "服务器文件路径",
    "url": "访问 URL",
    "mime_type": "MIME 类型",
    "size": "文件大小（字节）",
    "biz_type": "业务类型",
    "uploaded_by": "上传人账号 ID",
})

STATUSES = {
    "sys_user.user_type": "ADMIN（管理者后台）/ ACADEMIC（教务后台）/ TEACHER（教师小程序）/ PARENT（家长小程序）",
    "sys_user.status": "ENABLED / DISABLED / LOCKED",
    "parent_student.status": "PENDING / ACTIVE / REJECTED / REVOKED",
    "parent_student.source": "SELF（家长自助申请）/ ADMIN（后台新增）/ UNBIND（家长解绑申请，绑定保持 ACTIVE）",
    "student.status": "ACTIVE / PAUSED / GRADUATED / LEFT",
    "course.status": "ON（上架）/ OFF（下架）",
    "course_price.unit": "TERM（按期）/ TIME（按次）",
    "class_group.status": "ACTIVE / FULL / FINISHED",
    "lesson.status": "SCHEDULED / DONE / CANCELLED",
    "attendance.status": "PRESENT（出勤）/ LEAVE（请假）/ ABSENT（缺勤）",
    "leave_request.status": "PENDING / APPROVED / REJECTED / CANCELLED",
    "leave_request.makeup_status": "NONE / PENDING / ARRANGED",
    "enrollment.status": "PENDING_CONFIRM / PENDING_PAYMENT / ENROLLED / CANCELLED",
    "enrollment.source": "MINI（家长小程序）/ ADMIN（后台）",
    "trial_booking.status": "NEW / CONTACTED / VISITED / CONVERTED / CANCELLED",
    "payment_record.type": "NEW（首购）/ RENEW（续费）/ MAKEUP（补费）",
    "hour_ledger.type": "PURCHASE（充值）/ CONSUME（消课）/ CORRECT（修正）",
    "work.status": "PENDING / DISPLAYED / HIDDEN / REJECTED",
    "work_comment.status": "PENDING / APPROVED / HIDDEN / REJECTED",
    "work_auth.status": "ACTIVE / REVOKED",
    "qualification.verify_status": "VERIFIED / PENDING",
    "notice.status": "DRAFT / PUBLISHED / HIDDEN",
    "institution_info.status": "OPEN / PAUSED",
}


def parse_tables(text):
    tables = []
    pattern = re.compile(r"CREATE TABLE `(\w+)` \((.*?)\) ENGINE=[^;]*?COMMENT='([^']*)';", re.S)
    for m in pattern.finditer(text):
        name, body, comment = m.group(1), m.group(2), m.group(3)
        fields = []
        for line in body.splitlines():
            line = line.strip()
            if not line.startswith("`"):
                continue
            fm = re.match(r"`(\w+)`\s+([A-Z]+(?:\([^)]*\))?)\s*(.*?),?$", line)
            if not fm:
                continue
            field, typ, rest = fm.group(1), fm.group(2), fm.group(3)
            nullable = "NOT NULL" not in rest
            dm = re.search(r"DEFAULT\s+('(?:[^']*)'|[^\s,]+)", rest)
            default = dm.group(1) if dm else ""
            cm = re.search(r"COMMENT\s+'([^']*)'", rest)
            field_comment = cm.group(1) if cm else ""
            meaning = FIELD_EXTRA.get(field, "") or field_comment
            if name == "qualification" and field == "valid_until":
                meaning = "资质有效期（文本，如“长期”或日期）"
            if field_comment and field_comment not in meaning:
                meaning = (meaning + "；" + field_comment).strip("；")
            fields.append({
                "name": field,
                "type": typ,
                "null": "是" if nullable else "否",
                "default": default,
                "meaning": meaning,
            })
        tables.append({"name": name, "comment": comment, "desc": TABLE_DESC.get(name, comment),
                       "relation": RELATIONS.get(name, ""), "fields": fields})
    return tables


tables = parse_tables(schema)
lines = []
lines.append("# 翰墨学堂 · 数据库表结构说明")
lines.append("")
lines.append("> 适用版本：MySQL 8.0；字符集 utf8mb4；排序规则 utf8mb4_general_ci。")
lines.append("> 与建表脚本 `sql/schema.sql` 一一对应，共 **%d 张表**。" % len(tables))
lines.append("")
lines.append("## 一、设计约定")
lines.append("")
lines.append("- 主键统一使用 `BIGINT AUTO_INCREMENT`，业务编号（报名号、缴费单号、请假单号等）单独存放并建唯一索引。")
lines.append("- 金额使用 `DECIMAL(10,2)`，课时使用 `DECIMAL(8,1)`，避免浮点误差。")
lines.append("- **不建立物理外键**，通过应用层与索引保证关联完整性，便于数据迁移、导入与演示环境切换。")
lines.append("- 核心业务表使用 `deleted` 逻辑删除；日志、关系与流水表不做逻辑删除。")
lines.append("- 时间字段使用 `DATETIME` / `DATE` / `TIME`，生产服务器统一设置 `Asia/Shanghai` 时区。")
lines.append("- 家长与学员、家长与作品授权等多对多关系均有唯一索引，防止重复绑定或重复授权。")
lines.append("- 小程序公开内容全部来自数据库：机构资料、课程、价格、大纲、师资、作品、公告、资质。")
lines.append("")
lines.append("## 二、业务关系总览")
lines.append("")
lines.append("```text")
lines.append("sys_user（管理者/教务/教师/家长）")
lines.append("  ├─ teacher_profile 1:1 教师档案")
lines.append("  ├─ parent_profile  1:1 家长档案")
lines.append("  └─ parent_student  N:N 家长-学员（最多 2 位有效监护人）")
lines.append("student 学员")
lines.append("  ├─ parent_student 绑定家长")
lines.append("  ├─ class_student 加入班级 → class_group → course")
lines.append("  ├─ lesson 课次 → attendance 点名 → hour_ledger 课时流水")
lines.append("  ├─ leave_request 请假 → makeup_arrangement 补课")
lines.append("  ├─ enrollment 报名 → payment_record 缴费 → hour_account 充值")
lines.append("  ├─ work 作品 → work_auth 家长授权 → work_comment 留言")
lines.append("  └─ hour_account 课时账户 1:1 / hour_ledger 流水")
lines.append("course 课程 → course_price 价格 / course_outline 大纲")
lines.append("class_group 班级 → class_student / lesson / class_group.teacher_id")
lines.append("institution_info / institution_image / qualification / notice / business_rule 客户端内容与规则")
lines.append("```")
lines.append("")
lines.append("## 三、表清单")
lines.append("")
lines.append("| # | 表名 | 说明 |")
lines.append("|---|------|------|")
for idx, t in enumerate(tables, 1):
    lines.append("| %d | [`%s`](#%s) | %s |" % (idx, t["name"], t["name"].replace("_", "-"), t["desc"]))
lines.append("")
lines.append("## 四、状态字典")
lines.append("")
lines.append("| 字段 | 取值 |")
lines.append("|------|------|")
for k, v in STATUSES.items():
    lines.append("| `%s` | %s |" % (k, v))
lines.append("")
lines.append("## 五、表结构明细")
lines.append("")
for t in tables:
    lines.append("### %s" % t["name"])
    lines.append("")
    lines.append("**用途**：%s" % t["desc"])
    if t["relation"]:
        lines.append("")
        lines.append("**关联**：%s" % t["relation"])
    lines.append("")
    lines.append("| 字段 | 类型 | 允许空 | 默认值 | 说明 |")
    lines.append("|------|------|:------:|--------|------|")
    for f in t["fields"]:
        lines.append("| `%s` | `%s` | %s | %s | %s |" % (
            f["name"], f["type"], f["null"], f["default"] if f["default"] else "—",
            f["meaning"] if f["meaning"] else "—"))
    lines.append("")
lines.append("## 六、索引与数据完整性")
lines.append("")
lines.append("- `sys_user.username`、`sys_user.wechat_openid`、`student.student_no`、`parent_student(parent_user_id,student_id)`、`class_student(class_id,student_id)`、`attendance(lesson_id,student_id)`、`hour_account.student_id`、`work_auth(student_id,parent_user_id)` 均有唯一约束。")
lines.append("- 报名申请号、缴费单号、请假单号、试听预约号均有唯一索引；生成规则见后端服务。")
lines.append("- 业务查询常用索引：学员状态、班级课程、课次日期、课次教师、请假状态、课时流水时间、缴费时间。")
lines.append("- 逻辑删除由 MyBatis-Plus 自动拼接 `deleted=0`；查询原生 SQL 时需自行加上该条件。")
lines.append("")
lines.append("## 七、备份与迁移注意")
lines.append("")
lines.append("- 使用 `mysqldump --single-transaction --routines --triggers --default-character-set=utf8mb4` 导出。")
lines.append("- 上传文件目录 `UPLOAD_DIR` 与数据库必须同时备份；只恢复数据库会导致作品/环境图片 404。")
lines.append("- 迁移后核对 `sys_user`、`student`、`hour_account`、`hour_ledger`、`payment_record`、`attendance` 的行数与金额/课时合计。")
lines.append("- 生产环境禁止直接执行 `sql/seed.sql`（演示数据）；使用 `sql/seed-prod.sql` 初始化基础字典与管理员账号。")
lines.append("")
out.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("wrote", out, "tables", len(tables), "lines", len(lines))
