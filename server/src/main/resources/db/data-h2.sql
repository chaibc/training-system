-- ============================================================
-- 翰墨学堂 · 演示种子数据
-- 生产环境请按需修改账号、手机号、课程与机构信息。
-- 默认后台账号：
--   admin   / Admin@123456   （管理者）
--   jiaowu  / Jiaowu@123456  （教务）
-- ============================================================

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

-- 账号
INSERT INTO `sys_user`
(`id`,`username`,`password_hash`,`user_type`,`role_id`,`name`,`phone`,`avatar`,`wechat_openid`,`status`,`failed_attempts`,`force_change_password`,`remark`) VALUES
(1,'admin','$2b$12$s7kS9pQkjpImK4kSbtq1euRyArWTBf00W8OUpRa6F2pWxBamWn.tm','ADMIN',1,'张老师','13800000001',NULL,NULL,'ENABLED',0,0,'机构管理者'),
(2,'jiaowu','$2b$12$qPHsW4Xn7uTIccUd/E1oy.FIZK2FOmT34vI1CD/93LbnakNPXZFea','ADMIN',2,'李教务','13900000002',NULL,NULL,'ENABLED',0,0,'教务'),
(3,NULL,NULL,'TEACHER',3,'王砚秋','13800006621',NULL,'seed_teacher_wang','ENABLED',0,0,NULL),
(4,NULL,NULL,'TEACHER',3,'刘墨白','13900003371',NULL,'seed_teacher_liu','ENABLED',0,0,NULL),
(5,NULL,NULL,'TEACHER',3,'赵清和','13700004417',NULL,NULL,'ENABLED',0,0,NULL),
(6,NULL,NULL,'PARENT',4,'王女士','13800002648',NULL,'seed_parent_wang','ENABLED',0,0,NULL),
(7,NULL,NULL,'PARENT',4,'林女士','13800007720',NULL,'seed_parent_lin','ENABLED',0,0,NULL),
(8,NULL,NULL,'PARENT',4,'赵先生','13900009902',NULL,'seed_parent_zhao','ENABLED',0,0,NULL),
(9,NULL,NULL,'PARENT',4,'陈女士','18600009052',NULL,'seed_parent_chen','ENABLED',0,0,NULL);

-- 教师档案
INSERT INTO `teacher_profile`
(`id`,`user_id`,`teacher_no`,`name`,`title`,`years`,`styles`,`courses_text`,`bio`,`quote`,`tags`,`status`,`sort`) VALUES
(1,3,'T001','王砚秋','幼小衔接主讲',12,'楷书 / 硬笔','幼小衔接硬笔启蒙、硬笔提高','从事少儿硬笔书法教学 12 年，长期负责幼小衔接阶段课程。','“先把坐姿和执笔习惯养好，字自然会慢慢写好。写字这件事，急不得。”','硬笔楷书,铅笔启蒙,卷面规范','ACTIVE',1),
(2,4,'T002','刘墨白','软笔主讲',8,'行书 / 软笔','软笔书法基础','从事软笔书法教学 8 年，长期负责软笔基础与碑帖临摹课程。','“临帖不是抄字，是先看懂，再写像，最后写出自己的节奏。”','软笔行书,楷书临摹,碑帖','ACTIVE',2),
(3,5,'T003','赵清和','硬笔教师',6,'硬笔 / 楷书','硬笔书写提高','从事硬笔书法教学 6 年，关注小学阶段卷面书写与结构规范。','“把每一次作业都当成作品来写，卷面自然会越来越好。”','硬笔楷书,结构训练,卷面提升','ACTIVE',3);

-- 家长档案
INSERT INTO `parent_profile` (`user_id`,`name`,`phone`,`wechat_nickname`) VALUES
(6,'王女士','13800002648','王女士'),
(7,'林女士','13800007720','林女士'),
(8,'赵先生','13900009902','赵先生'),
(9,'陈女士','18600009052','陈女士');

-- 学员
INSERT INTO `student`
(`id`,`student_no`,`name`,`gender`,`birth_date`,`school`,`grade`,`status`,`enroll_date`,`primary_parent_user_id`,`remark`) VALUES
(1,'HX20240018','王小墨','男','2017-05-12','实验小学','三年级','ACTIVE','2024-03-15',6,'软笔基础'),
(2,'HX20250036','王梓涵','女','2020-08-20','机关幼儿园','大班','ACTIVE','2025-03-15',6,'幼小衔接'),
(3,'HX20260012','林小满','男','2020-06-01','阳光幼儿园','大班','ACTIVE','2026-10-28',7,'新建学员'),
(4,'HX20250041','张沐宸','男','2021-02-11','实验小学','一年级','ACTIVE','2025-09-01',NULL,'幼小衔接'),
(5,'HX20250024','赵悠悠','女','2018-08-08','实验小学','二年级','ACTIVE','2025-05-08',8,'软笔基础'),
(6,'HX20250028','陈一诺','女','2020-11-02','机关幼儿园','大班','ACTIVE','2025-05-08',9,'幼小衔接');

-- 家长-学员绑定
INSERT INTO `parent_student`
(`parent_user_id`,`student_id`,`relation`,`is_primary`,`status`,`source`,`verified_by`,`verified_at`) VALUES
(6,1,'MOTHER',1,'ACTIVE','ADMIN',1,'2024-03-15 10:00:00'),
(6,2,'MOTHER',0,'ACTIVE','ADMIN',1,'2025-03-15 10:00:00'),
(7,3,'MOTHER',1,'PENDING','SELF',NULL,NULL),
(8,5,'FATHER',1,'ACTIVE','ADMIN',1,'2025-05-08 10:00:00'),
(9,6,'MOTHER',1,'ACTIVE','ADMIN',1,'2025-05-08 10:00:00');

-- 基础数据
INSERT INTO `course_category` (`id`,`name`,`code`,`description`,`sort`,`status`) VALUES
(1,'硬笔书法','HARD','硬笔书写课程',1,'ENABLED'),
(2,'软笔书法','SOFT','软笔书写课程',2,'ENABLED'),
(3,'幼小衔接','KIDS','入学前书写准备',3,'ENABLED');

INSERT INTO `course_stage` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'启蒙','兴趣与习惯养成',1,'ENABLED'),
(2,'基础','笔画与结构入门',2,'ENABLED'),
(3,'提高','结构与卷面提升',3,'ENABLED'),
(4,'创作','章法与作品创作',4,'ENABLED');

INSERT INTO `class_type` (`id`,`name`,`capacity_min`,`capacity_max`,`description`,`sort`,`status`) VALUES
(1,'一对一',1,1,'时间可预约',1,'ENABLED'),
(2,'小班',6,8,'逐字点评',2,'ENABLED'),
(3,'大班',12,16,'集体教学',3,'ENABLED'),
(4,'体验班',6,6,'试听体验',4,'ENABLED');

INSERT INTO `classroom` (`id`,`name`,`capacity`,`description`,`sort`,`status`) VALUES
(1,'教室 1',12,'硬笔 / 幼小衔接',1,'ENABLED'),
(2,'教室 2',8,'软笔 / 小班',2,'ENABLED'),
(3,'教室 3',6,'一对一 / 体验课',3,'ENABLED');

INSERT INTO `leave_reason` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'生病','孩子身体不适',1,'ENABLED'),
(2,'家中有事','家庭事务',2,'ENABLED'),
(3,'外出','出行或旅行',3,'ENABLED'),
(4,'其他','其他原因',4,'ENABLED');

INSERT INTO `payment_method` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'现金','前台现金收款',1,'ENABLED'),
(2,'银行转账','对公或对私转账',2,'ENABLED'),
(3,'POS 刷卡','前台 POS 收款',3,'ENABLED'),
(4,'微信转账','线下微信转账，非线上支付',4,'ENABLED');

INSERT INTO `trial_slot` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'周六上午 09:00–10:00','幼小衔接 / 硬笔',1,'ENABLED'),
(2,'周六下午 14:00–15:00','软笔 / 硬笔',2,'ENABLED'),
(3,'周日上午 09:00–10:00','软笔 / 幼小衔接',3,'ENABLED'),
(4,'工作日晚间 18:30–19:30','硬笔提高',4,'ENABLED');

-- 课程
INSERT INTO `course`
(`id`,`name`,`category_id`,`stage_id`,`age_range`,`lessons`,`frequency`,`duration_minutes`,`cover_char`,`cover_color`,`description`,`goal`,`tags`,`featured`,`status`,`sort`) VALUES
(1,'幼小衔接硬笔启蒙班',3,1,'5–7 岁',24,'每周 2 次 × 60 分钟',60,'永','145deg,#75422F,#A05B40','从握笔坐姿到规范书写，帮助孩子平稳过渡小学书写要求。','掌握正确的坐姿与握笔姿势，书写规范的横、竖、撇、捺、点等基本笔画；理解独体字与常用合体字的结构规律，养成先观察后书写的习惯，为小学书写做好准备。','小班 6 人,含教材,幼小衔接',1,'ON',1),
(2,'软笔书法基础班',2,2,'7–12 岁',48,'每周 1 次 × 90 分钟',90,'和','145deg,#28342F,#4D6456','以经典碑帖为范本，从执笔运笔到章法创作，系统建立软笔书写能力。','掌握软笔执笔、运笔与蘸墨方法，理解楷书基本笔画与间架结构；通过碑帖临摹建立观察与临写能力，完成从单字到完整作品的进阶。','小班 8 人,碑帖临摹,系统进阶',1,'ON',2),
(3,'硬笔书写提高班',1,3,'7–10 岁',24,'每周 1 次 × 60 分钟',60,'正','145deg,#2E3742,#475C71','面向已有基础的学员，聚焦偏旁结构、篇章布局与卷面书写提升。','在规范笔画的基础上，进一步掌握偏旁部首与合体字结构；提升篇章布局与卷面书写质量，形成稳定、整洁、有节奏的日常书写。','小班 8 人,卷面提升,结构训练',1,'ON',3);

-- 班型与价格
INSERT INTO `course_price` (`id`,`course_id`,`class_type_id`,`lessons`,`price`,`unit`,`status`,`sort`) VALUES
(1,1,2,24,2880.00,'TERM','ON',1),
(2,1,1,1,360.00,'TIME','ON',2),
(3,2,2,48,4800.00,'TERM','ON',1),
(4,2,1,1,520.00,'TIME','ON',2),
(5,3,2,24,3200.00,'TERM','ON',1),
(6,3,1,1,300.00,'TIME','ON',2);

-- 内容大纲
INSERT INTO `course_outline` (`course_id`,`title`,`lesson_range`,`description`,`visible`,`sort`) VALUES
(1,'握笔坐姿','第 1–4 课时','正确执笔、坐姿与书写习惯养成',1,1),
(1,'基本笔画','第 5–10 课时','横竖撇捺点钩折的起收笔',1,2),
(1,'间架结构','第 11–16 课时','独体字与合体字的结构规律',1,3),
(1,'偏旁部首','第 17–20 课时','常用偏旁与部件组合',1,4),
(1,'章法布局','第 21–24 课时','作品书写、卷面与阶段展示',1,5),
(2,'执笔运笔','第 1–6 课时','执笔姿势、运笔与水墨控制',1,1),
(2,'楷书笔画','第 7–18 课时','楷书基本笔画与起收转承',1,2),
(2,'间架结构','第 19–30 课时','独体字与合体字结构规律',1,3),
(2,'碑帖临摹','第 31–42 课时','经典碑帖临摹与结构观察',1,4),
(2,'章法创作','第 43–48 课时','章法、落款与完整作品',1,5),
(3,'书写诊断','第 1–3 课时','书写问题诊断与目标制定',1,1),
(3,'偏旁部首','第 4–9 课时','常用偏旁的比例与避让',1,2),
(3,'间架结构','第 10–16 课时','合体字结构与重心控制',1,3),
(3,'篇章布局','第 17–21 课时','字距行距与整篇书写',1,4),
(3,'卷面实战','第 22–24 课时','作业卷面与限时书写',1,5);

-- 班级
INSERT INTO `class_group` (`id`,`name`,`course_id`,`teacher_id`,`classroom_id`,`weekday`,`start_time`,`end_time`,`capacity`,`status`) VALUES
(1,'幼小衔接硬笔A班',1,1,1,6,'09:00:00','10:00:00',12,'ACTIVE'),
(2,'幼小衔接硬笔B班',1,1,1,7,'14:00:00','15:00:00',12,'ACTIVE'),
(3,'软笔基础班',2,2,2,6,'09:00:00','10:30:00',8,'ACTIVE'),
(4,'硬笔提高班',3,3,1,1,'16:30:00','17:30:00',10,'ACTIVE');

INSERT INTO `class_student` (`class_id`,`student_id`,`joined_at`,`status`) VALUES
(1,2,'2025-03-15','ACTIVE'),(1,4,'2025-09-01','ACTIVE'),(1,6,'2025-05-08','ACTIVE'),
(2,6,'2025-05-08','ACTIVE'),
(3,1,'2024-03-15','ACTIVE'),(3,5,'2025-05-08','ACTIVE');

-- 课次
INSERT INTO `lesson` (`id`,`class_id`,`lesson_no`,`lesson_date`,`start_time`,`end_time`,`teacher_id`,`classroom_id`,`status`,`note`) VALUES
(1,1,12,'2026-10-24','09:00:00','10:00:00',1,1,'DONE','已点名'),
(2,1,13,'2026-10-31','09:00:00','10:00:00',1,1,'SCHEDULED',NULL),
(3,3,12,'2026-10-29','14:00:00','15:30:00',2,2,'SCHEDULED',NULL),
(4,3,13,'2026-10-31','14:00:00','15:30:00',2,2,'SCHEDULED',NULL),
(5,4,9,'2026-10-27','16:30:00','17:30:00',3,1,'DONE','已点名'),
(6,4,10,'2026-11-03','16:30:00','17:30:00',3,1,'SCHEDULED',NULL);

INSERT INTO `attendance` (`lesson_id`,`student_id`,`status`,`consumed_hours`,`note`,`recorded_by`,`recorded_at`) VALUES
(1,2,'PRESENT',1.0,'出勤',1,'2026-10-24 10:05:00'),
(1,4,'LEAVE',0.0,'课前 4h+ · 不扣',1,'2026-10-24 10:05:00'),
(1,6,'PRESENT',1.0,'出勤',1,'2026-10-24 10:05:00'),
(5,1,'PRESENT',1.0,'出勤',3,'2026-10-27 18:10:00');

-- 请假
INSERT INTO `leave_request` (`id`,`leave_no`,`student_id`,`lesson_id`,`reason_id`,`reason_text`,`status`,`deduct_hours`,`submitted_at`,`approver_id`,`approved_at`,`reject_reason`,`makeup_status`) VALUES
(1,'QJ2026102901',4,2,2,'家中有事，需回老家','PENDING',0,'2026-10-29 08:12:00',NULL,NULL,NULL,'NONE'),
(2,'QJ2026102802',6,3,1,'孩子发烧，在家休息','APPROVED',1,'2026-10-28 15:40:00',1,'2026-10-28 16:20:00',NULL,'PENDING'),
(3,'QJ2026101803',1,3,3,'外出','REJECTED',1,'2026-10-18 13:20:00',1,'2026-10-18 13:40:00','课次即将开始，按机构规则按消课处理','NONE');

-- 报名
INSERT INTO `enrollment` (`enrollment_no`,`student_id`,`course_id`,`class_id`,`parent_user_id`,`source`,`status`,`amount`,`submitted_at`,`confirmed_by`,`confirmed_at`) VALUES
('BM20261029-0031',1,1,1,6,'MINI','PENDING_CONFIRM',2880.00,'2026-10-29 09:22:00',NULL,NULL),
('BM20261028-0027',5,2,3,8,'MINI','PENDING_PAYMENT',4800.00,'2026-10-28 18:40:00',1,'2026-10-29 09:00:00'),
('BM20261028-0019',2,1,1,6,'MINI','ENROLLED',2880.00,'2026-10-28 15:12:00',1,'2026-10-28 16:00:00'),
('BM20261027-0012',3,1,NULL,7,'MINI','PENDING_CONFIRM',2880.00,'2026-10-27 20:05:00',NULL,NULL);

-- 试听预约
INSERT INTO `trial_booking` (`booking_no`,`student_name`,`age`,`parent_name`,`phone`,`course_id`,`slot_id`,`note`,`status`,`follow_up_note`,`next_follow_up`) VALUES
('ST20261029-0061','林小满','6 岁','林女士','13800007720',1,1,'希望周六上午','NEW',NULL,'2026-10-31'),
('ST20261029-0060','张沐宸','5 岁','张先生','15900002216',1,1,'','CONTACTED','已电话确认到课','2026-11-01'),
('ST20261028-0055','许知夏','8 岁','许女士','18600005601',2,2,'','CONVERTED','已转报名','2026-10-31'),
('ST20261027-0048','周小满','7 岁','周先生','13700008890',3,4,'','CANCELLED','时间冲突','2026-10-30');

-- 线下缴费
INSERT INTO `payment_record` (`payment_no`,`enrollment_id`,`student_id`,`type`,`item`,`amount`,`payment_method_id`,`paid_at`,`registered_by`,`note`) VALUES
('JF20261029-0216',2,5,'NEW','软笔基础 48 课时',4800.00,2,'2026-10-29 10:12:00',1,'银行转账尾号 2216'),
('JF20261029-0215',3,2,'NEW','幼小衔接硬笔 24 课时',2880.00,3,'2026-10-29 09:40:00',1,'POS 刷卡'),
('JF20261028-0211',NULL,2,'RENEW','幼小衔接硬笔 24 课时',2880.00,1,'2026-10-28 17:20:00',1,'现金'),
('JF20261027-0208',NULL,3,'NEW','幼小衔接硬笔 24 课时',2880.00,2,'2026-10-27 16:03:00',1,'银行转账');

-- 课时账户与流水
INSERT INTO `hour_account` (`student_id`,`total_hours`,`used_hours`,`remaining_hours`,`valid_until`) VALUES
(1,48.0,30.0,18.0,'2027-01-02'),
(2,24.0,23.0,1.0,'2027-03-15'),
(3,24.0,0.0,24.0,'2027-10-28'),
(4,24.0,12.0,12.0,'2027-02-11'),
(5,48.0,14.0,34.0,'2027-05-08'),
(6,24.0,13.0,11.0,'2027-05-08');

INSERT INTO `hour_ledger` (`student_id`,`lesson_id`,`type`,`change_hours`,`balance_after`,`operator_id`,`reason`,`created_at`) VALUES
(1,3,'CONSUME',-1.0,18.0,NULL,NULL,'2026-10-28 15:34:00'),
(1,NULL,'CORRECT',1.0,20.0,1,'请假扣课修正','2026-10-16 10:20:00'),
(1,NULL,'PURCHASE',24.0,24.0,1,'续费充值','2026-09-10 16:42:00'),
(2,1,'CONSUME',-1.0,1.0,NULL,NULL,'2026-10-24 10:05:00'),
(2,NULL,'PURCHASE',24.0,24.0,1,'首次购买','2025-03-15 10:12:00'),
(3,NULL,'PURCHASE',24.0,24.0,1,'新报名','2026-10-27 16:03:00');

-- 作品与授权
INSERT INTO `work` (`id`,`student_id`,`class_id`,`teacher_id`,`title`,`work_type`,`content_char`,`comment`,`authorized`,`status`,`like_count`,`created_at`) VALUES
(1,2,1,1,'《永字八法》','HARD','永','结体端正，笔画起收清晰，继续保持。',1,'DISPLAYED',21,'2026-10-24 10:00:00'),
(2,1,3,2,'《诫子书》节选','SOFT','静以修身','结体沉稳，横画起收有度，“静”字左右呼应处理得不错。',1,'DISPLAYED',32,'2026-10-25 10:00:00'),
(3,5,3,2,'《上善若水》','SOFT','上善若水','用笔舒展，章法完整，落款位置还需再练习。',1,'DISPLAYED',45,'2026-10-22 10:00:00'),
(4,6,1,1,'《每日一练》','HARD','每日一练','结构进步明显，注意横画间距。',0,'PENDING',0,'2026-10-23 10:00:00'),
(5,1,3,2,'《和》','SOFT','和','左右结构处理协调，竖画可再挺拔一些。',1,'DISPLAYED',12,'2026-10-18 10:00:00'),
(6,6,1,1,'《学而时习之》','HARD','学而时习之','结构匀称，笔画起收清晰，注意字距。',1,'DISPLAYED',18,'2026-10-20 10:00:00'),
(7,2,1,1,'《点画之间》','HARD','点画之间','笔画起收清晰，字距可再均匀。',1,'DISPLAYED',16,'2026-10-15 10:00:00'),
(8,5,3,2,'《春日》','HARD','春','图片模糊，建议重新拍摄后上传。',1,'REJECTED',0,'2026-10-15 10:00:00');

INSERT INTO `work_comment` (`work_id`,`parent_user_id`,`content`,`status`,`reviewed_by`,`reviewed_at`) VALUES
(2,6,'写得真好，能看出孩子的进步！','PENDING',NULL,NULL),
(2,8,'“静”字的结构很稳，向小墨学习。','PENDING',NULL,NULL),
(3,8,'这个班的老师很负责，推荐！','APPROVED',1,'2026-10-24 21:10:00');

INSERT INTO `work_auth` (`student_id`,`parent_user_id`,`scope`,`method`,`authorized_at`,`status`) VALUES
(1,6,'作品展示 / 宣传','ELECTRONIC','2026-03-15','ACTIVE'),
(2,6,'作品展示','ELECTRONIC','2026-03-15','ACTIVE'),
(4,9,'—','—',NULL,'REVOKED'),
(5,8,'作品展示','PAPER','2026-09-02','ACTIVE'),
(6,9,'作品展示','ELECTRONIC','2025-05-08','ACTIVE');

-- 机构资料
INSERT INTO `institution_info`
(`id`,`name`,`brand`,`slogan`,`hero_intro`,`intro`,`philosophy`,`phone`,`wechat`,`hours`,`address`,`longitude`,`latitude`,`traffic`,`since_year`,`status`) VALUES
(1,'翰墨学堂书法培训中心','翰墨学堂','一笔一画，写好中国字','专注 5–12 岁书法启蒙，以软笔与硬笔课程，陪孩子养成受用一生的书写习惯。','翰墨学堂创办于 2014 年，专注 5–12 岁少儿书法教育。坚持小班教学、逐字点评，从握笔坐姿、基本笔画到间架结构与章法布局，帮助孩子建立规范的书写习惯。','以传统书法教育，陪孩子养成专注与耐心；让书写成为一生的修养。','0755-8888-6666','hanmo-edu','周二至周日 9:00–19:00','××市××区××路 88 号 2 层',114.057900,22.543100,'距地铁 2 号线 ××站 B 口步行约 300 米',2014,'OPEN');

INSERT INTO `institution_image` (`institution_id`,`image_url`,`title`,`sort`) VALUES
(1,'/uploads/demo/classroom.jpg','书法教室',1),
(1,'/uploads/demo/copybook.jpg','临帖区',2),
(1,'/uploads/demo/gallery.jpg','作品墙',3);

INSERT INTO `qualification` (`name`,`code`,`valid_until`,`verify_status`,`visible`,`sort`) VALUES
('营业执照','9144**********',NULL,'VERIFIED',1,1),
('非学科类培训备案','××-2024-×××','2028-06-30','VERIFIED',1,2),
('消防备案','XF-2024-1182','2027-12-31','VERIFIED',1,3);

INSERT INTO `notice` (`title`,`type`,`content`,`publish_date`,`status`,`created_by`) VALUES
('11 月课表已更新','NOTICE','家长可在「我的课表」查看孩子 11 月的课程安排，如有疑问请联系教务。','2026-10-28','PUBLISHED',1),
('教务前台服务时间调整','NOTICE','周三至周日 9:00–19:00 在前台接待；周一闭馆，请提前电话联系。','2026-10-08','PUBLISHED',1);

INSERT INTO `business_rule` (`rule_key`,`rule_value`,`description`) VALUES
('leave_cutoff_hours','4','课前 4 小时以上请假不扣课'),
('renewal_threshold','3','剩余课时低于或等于 3 进入续费预警'),
('validity_months','12','课时有效期 12 个月'),
('slot_hold_hours','48','未缴费报名保留 48 小时'),
('lesson_duration','60','默认单次课时长（分钟）'),
('auto_consume','1','点名出勤后自动消课'),
('comment_enabled','1','作品评论审核后展示'),
('makeup_enabled','1','已通过请假可申请补课');
