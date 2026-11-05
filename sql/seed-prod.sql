-- ============================================================
-- 翰墨学堂 · 生产初始化数据（干净数据，不含演示学员/家长/作品）
-- 执行顺序：先执行 schema.sql，再执行本文件。
-- 默认后台账号：admin / Admin@123456（首次登录后请立即修改密码）
-- 基础字典可按机构实际情况在「基础数据」「机构内容」中继续维护。
-- ============================================================
USE `hanmo_calligraphy`;
SET NAMES utf8mb4;

-- 角色
INSERT INTO `sys_role` (`id`,`code`,`name`,`description`,`data_scope`,`built_in`,`status`) VALUES
(1,'ADMIN','管理者','唯一账号，全部权限，不可新增/停用','ALL',1,'ENABLED'),
(2,'ACADEMIC','教务','业务模块；课程只读；不可见配置、规则与用户','ALL',1,'ENABLED'),
(3,'TEACHER','教师','小程序教师端；仅所带班级与学员','OWN_CLASS',1,'ENABLED'),
(4,'PARENT','家长','小程序家长端；仅已绑定子女','OWN_CHILDREN',1,'ENABLED');

-- 角色模块权限（模块：dashboard/reports/students/enrollments/courses/classes/attendance/leave/works/org/basic/settings/users）
INSERT INTO `sys_role_permission` (`role_id`,`module_key`,`can_view`,`can_create`,`can_edit`,`can_delete`,`can_special`) VALUES
(1,'dashboard',1,1,1,1,1),(1,'reports',1,1,1,1,1),(1,'students',1,1,1,1,1),(1,'enrollments',1,1,1,1,1),
(1,'courses',1,1,1,1,1),(1,'classes',1,1,1,1,1),(1,'attendance',1,1,1,1,1),(1,'leave',1,1,1,1,1),
(1,'works',1,1,1,1,1),(1,'org',1,1,1,1,1),(1,'basic',1,1,1,1,1),(1,'settings',1,1,1,1,1),(1,'users',1,1,1,1,1),
(2,'dashboard',1,0,0,0,1),(2,'reports',1,0,0,0,1),(2,'students',1,1,1,1,1),(2,'enrollments',1,1,1,1,1),
(2,'courses',1,0,0,0,0),(2,'classes',1,1,1,1,1),(2,'attendance',1,1,1,1,1),(2,'leave',1,1,1,1,1),
(2,'works',1,1,1,1,1),(2,'org',0,0,0,0,0),(2,'basic',0,0,0,0,0),(2,'settings',0,0,0,0,0),(2,'users',0,0,0,0,0);

-- 唯一管理者账号（密码哈希对应 Admin@123456，请登录后修改）
INSERT INTO `sys_user`
(`id`,`username`,`password_hash`,`user_type`,`role_id`,`name`,`phone`,`status`,`failed_attempts`,`force_change_password`,`remark`) VALUES
(1,'admin','$2b$12$s7kS9pQkjpImK4kSbtq1euRyArWTBf00W8OUpRa6F2pWxBamWn.tm','ADMIN',1,'机构管理者','13800000000','ENABLED',0,0,'首次登录后请修改密码');

-- 课程分类
INSERT INTO `course_category` (`id`,`name`,`code`,`description`,`sort`,`status`) VALUES
(1,'硬笔书法','HARD','硬笔书写课程',1,'ENABLED'),
(2,'软笔书法','SOFT','软笔书写课程',2,'ENABLED'),
(3,'幼小衔接','KIDS','入学前书写准备',3,'ENABLED');

-- 课程阶段
INSERT INTO `course_stage` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'启蒙','兴趣与习惯养成',1,'ENABLED'),
(2,'基础','笔画与结构入门',2,'ENABLED'),
(3,'提高','结构与卷面提升',3,'ENABLED'),
(4,'创作','章法与作品创作',4,'ENABLED');

-- 班型
INSERT INTO `class_type` (`id`,`name`,`capacity_min`,`capacity_max`,`description`,`sort`,`status`) VALUES
(1,'一对一',1,1,'时间可预约',1,'ENABLED'),
(2,'小班',6,8,'逐字点评',2,'ENABLED'),
(3,'大班',12,16,'集体教学',3,'ENABLED'),
(4,'体验班',6,6,'试听体验',4,'ENABLED');

-- 教室
INSERT INTO `classroom` (`id`,`name`,`capacity`,`description`,`sort`,`status`) VALUES
(1,'教室 1',12,'硬笔 / 幼小衔接',1,'ENABLED'),
(2,'教室 2',8,'软笔 / 小班',2,'ENABLED'),
(3,'教室 3',6,'一对一 / 体验课',3,'ENABLED');

-- 请假原因
INSERT INTO `leave_reason` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'生病','孩子身体不适',1,'ENABLED'),
(2,'家中有事','家庭事务',2,'ENABLED'),
(3,'外出','出行或旅行',3,'ENABLED'),
(4,'其他','其他原因',4,'ENABLED');

-- 线下缴费方式
INSERT INTO `payment_method` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'现金','前台现金收款',1,'ENABLED'),
(2,'银行转账','对公或对私转账',2,'ENABLED'),
(3,'POS 刷卡','前台 POS 收款',3,'ENABLED'),
(4,'微信转账','线下微信转账，非线上支付',4,'ENABLED');

-- 试听时段
INSERT INTO `trial_slot` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'周六上午 09:00–10:00','幼小衔接 / 硬笔',1,'ENABLED'),
(2,'周六下午 14:00–15:00','软笔 / 硬笔',2,'ENABLED'),
(3,'周日上午 09:00–10:00','软笔 / 幼小衔接',3,'ENABLED'),
(4,'工作日晚间 18:30–19:30','硬笔提高',4,'ENABLED');

-- 机构资料（请逐项改为机构真实信息）
INSERT INTO `institution_info`
(`id`,`name`,`brand`,`slogan`,`hero_intro`,`intro`,`philosophy`,`phone`,`wechat`,`hours`,`address`,`longitude`,`latitude`,`traffic`,`since_year`,`status`) VALUES
(1,'机构名称（请修改）','机构简称','一笔一画，写好中国字','专注少儿书法启蒙，以软笔与硬笔课程，陪孩子养成受用一生的书写习惯。','请在此填写机构真实简介。建议说明创办时间、教学特色、班型与服务范围。','以传统书法教育，陪孩子养成专注与耐心；让书写成为一生的修养。','请填写联系电话','请填写机构微信','请填写营业时间','请填写机构详细地址',NULL,NULL,'请填写交通指引',YEAR(CURDATE()),'OPEN');

-- 业务规则
INSERT INTO `business_rule` (`rule_key`,`rule_value`,`description`) VALUES
('leave_cutoff_hours','4','课前 4 小时以上请假不扣课'),
('renewal_threshold','3','剩余课时低于或等于 3 进入续费预警'),
('validity_months','12','课时有效期 12 个月'),
('slot_hold_hours','48','未缴费报名保留 48 小时'),
('lesson_duration','60','默认单次课时长（分钟）'),
('auto_consume','1','点名出勤后自动消课'),
('comment_enabled','1','作品评论审核后展示'),
('makeup_enabled','1','已通过请假可申请补课');
