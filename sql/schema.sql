-- ============================================================
-- 翰墨学堂 · 书法培训管理系统
-- MySQL 8.0 表结构
-- 说明：
--   1. 所有表使用 utf8mb4，金额 DECIMAL，课时 DECIMAL(8,1)；
--   2. 核心业务表使用 deleted 做逻辑删除，日志/关系表不做逻辑删除；
--   3. 外键以逻辑关联为主（不建物理外键），便于迁移与演示数据导入；
--   4. 时间统一使用服务器本地时间，生产环境请在部署时设置时区。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `hanmo_calligraphy`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
USE `hanmo_calligraphy`;

-- ------------------------------------------------------------
-- 1. 角色与权限
-- ------------------------------------------------------------
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL COMMENT 'ADMIN/ACADEMIC/TEACHER/PARENT',
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `data_scope` VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/OWN_CLASS/OWN_CHILDREN/SELF',
  `built_in` TINYINT NOT NULL DEFAULT 1,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内置角色';

CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `module_key` VARCHAR(50) NOT NULL COMMENT 'dashboard/reports/students/...',
  `can_view` TINYINT NOT NULL DEFAULT 0,
  `can_create` TINYINT NOT NULL DEFAULT 0,
  `can_edit` TINYINT NOT NULL DEFAULT 0,
  `can_delete` TINYINT NOT NULL DEFAULT 0,
  `can_special` TINYINT NOT NULL DEFAULT 0,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_module` (`role_id`, `module_key`),
  KEY `idx_role_permission_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色模块权限';

-- ------------------------------------------------------------
-- 2. 账号与审计
-- ------------------------------------------------------------
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) DEFAULT NULL COMMENT '后台账号用户名；教师/家长为空',
  `password_hash` VARCHAR(100) DEFAULT NULL COMMENT 'BCrypt；教师/家长为空',
  `user_type` VARCHAR(20) NOT NULL COMMENT 'ADMIN/TEACHER/PARENT',
  `role_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `avatar` VARCHAR(500) DEFAULT NULL,
  `wechat_openid` VARCHAR(100) DEFAULT NULL,
  `wechat_unionid` VARCHAR(100) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED/LOCKED',
  `failed_attempts` INT NOT NULL DEFAULT 0,
  `locked_until` DATETIME DEFAULT NULL,
  `last_login_at` DATETIME DEFAULT NULL,
  `last_login_ip` VARCHAR(64) DEFAULT NULL,
  `force_change_password` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_openid` (`wechat_openid`),
  KEY `idx_user_phone` (`phone`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_user_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号（后台/教师/家长）';

CREATE TABLE `sys_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `operator_name` VARCHAR(50) DEFAULT NULL,
  `action_type` VARCHAR(30) NOT NULL COMMENT '中文动作名：登录/账号变更/绑定审核/点名消课/报名登记/缴费登记/续费登记/课时调整/作品上传/作品审核/授权补录/授权撤回/补课安排',
  `target_type` VARCHAR(30) DEFAULT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `content` VARCHAR(1000) DEFAULT NULL,
  `ip` VARCHAR(64) DEFAULT NULL,
  `result` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAIL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_audit_action` (`action_type`),
  KEY `idx_audit_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录与账号变更审计';

-- ------------------------------------------------------------
-- 3. 教师 / 家长 / 学员
-- ------------------------------------------------------------
CREATE TABLE `teacher_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `teacher_no` VARCHAR(32) DEFAULT NULL,
  `name` VARCHAR(50) NOT NULL,
  `title` VARCHAR(50) DEFAULT NULL,
  `years` INT NOT NULL DEFAULT 0,
  `styles` VARCHAR(100) DEFAULT NULL,
  `courses_text` VARCHAR(255) DEFAULT NULL,
  `bio` TEXT,
  `quote` VARCHAR(255) DEFAULT NULL,
  `tags` VARCHAR(255) DEFAULT NULL,
  `avatar` VARCHAR(500) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_user` (`user_id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师档案';

CREATE TABLE `parent_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `wechat_nickname` VARCHAR(100) DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parent_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长档案';

CREATE TABLE `student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_no` VARCHAR(32) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `gender` VARCHAR(10) DEFAULT NULL,
  `birth_date` DATE DEFAULT NULL,
  `school` VARCHAR(100) DEFAULT NULL,
  `grade` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/GRADUATED/LEFT',
  `enroll_date` DATE DEFAULT NULL,
  `primary_parent_user_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_student_name` (`name`),
  KEY `idx_student_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员档案';

CREATE TABLE `parent_student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_user_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `relation` VARCHAR(20) NOT NULL DEFAULT 'OTHER' COMMENT 'MOTHER/FATHER/GRANDPARENT/OTHER',
  `is_primary` TINYINT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACTIVE/REJECTED/REVOKED',
  `source` VARCHAR(20) NOT NULL DEFAULT 'SELF' COMMENT 'SELF/ADMIN/UNBIND',
  `verified_by` BIGINT DEFAULT NULL,
  `verified_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parent_student` (`parent_user_id`, `student_id`),
  KEY `idx_bind_student` (`student_id`),
  KEY `idx_bind_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长-学员绑定';

-- ------------------------------------------------------------
-- 4. 基础数据
-- ------------------------------------------------------------
CREATE TABLE `course_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `code` VARCHAR(32) DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类';

CREATE TABLE `course_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程阶段';

CREATE TABLE `class_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `capacity_min` INT DEFAULT NULL,
  `capacity_max` INT DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班型';

CREATE TABLE `classroom` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `capacity` INT DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室';

CREATE TABLE `leave_reason` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假原因';

CREATE TABLE `payment_method` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下缴费方式';

CREATE TABLE `trial_slot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试听时段';

-- ------------------------------------------------------------
-- 5. 课程 / 定价 / 大纲
-- ------------------------------------------------------------
CREATE TABLE `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `category_id` BIGINT NOT NULL,
  `stage_id` BIGINT DEFAULT NULL,
  `age_range` VARCHAR(50) DEFAULT NULL,
  `lessons` INT NOT NULL DEFAULT 0,
  `frequency` VARCHAR(100) DEFAULT NULL,
  `duration_minutes` INT DEFAULT 60,
  `cover_char` VARCHAR(8) DEFAULT NULL,
  `cover_color` VARCHAR(100) DEFAULT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `goal` TEXT,
  `tags` VARCHAR(255) DEFAULT NULL,
  `featured` TINYINT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON' COMMENT 'ON/OFF',
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_course_category` (`category_id`),
  KEY `idx_course_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程';

CREATE TABLE `course_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `class_type_id` BIGINT NOT NULL,
  `lessons` INT NOT NULL DEFAULT 1,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `unit` VARCHAR(20) NOT NULL DEFAULT 'TERM' COMMENT 'TERM/TIME',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON',
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_price_course` (`course_id`),
  KEY `idx_price_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班型与价格';

CREATE TABLE `course_outline` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `lesson_range` VARCHAR(50) DEFAULT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `visible` TINYINT NOT NULL DEFAULT 1,
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_outline_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程内容大纲';

-- ------------------------------------------------------------
-- 6. 班级 / 课次 / 点名 / 请假
-- ------------------------------------------------------------
CREATE TABLE `class_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `course_id` BIGINT NOT NULL,
  `teacher_id` BIGINT NOT NULL COMMENT 'teacher_profile.id',
  `classroom_id` BIGINT DEFAULT NULL,
  `weekday` TINYINT DEFAULT NULL COMMENT '1=周一 ... 7=周日',
  `start_time` TIME DEFAULT NULL,
  `end_time` TIME DEFAULT NULL,
  `capacity` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/FULL/FINISHED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_class_course` (`course_id`),
  KEY `idx_class_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级';

CREATE TABLE `class_student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `joined_at` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_student` (`class_id`, `student_id`),
  KEY `idx_class_student_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学员';

CREATE TABLE `lesson` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_id` BIGINT NOT NULL,
  `lesson_no` INT NOT NULL DEFAULT 1,
  `lesson_date` DATE NOT NULL,
  `start_time` TIME NOT NULL,
  `end_time` TIME NOT NULL,
  `teacher_id` BIGINT NOT NULL,
  `classroom_id` BIGINT DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED/DONE/CANCELLED',
  `original_lesson_id` BIGINT DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_lesson_no` (`class_id`, `lesson_no`),
  KEY `idx_lesson_date` (`lesson_date`),
  KEY `idx_lesson_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课次';

CREATE TABLE `attendance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lesson_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PRESENT' COMMENT 'PRESENT/LEAVE/ABSENT',
  `consumed_hours` DECIMAL(6,1) NOT NULL DEFAULT 0.0,
  `note` VARCHAR(255) DEFAULT NULL,
  `recorded_by` BIGINT DEFAULT NULL,
  `recorded_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attendance_lesson_student` (`lesson_id`, `student_id`),
  KEY `idx_attendance_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点名与消课';

CREATE TABLE `leave_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `leave_no` VARCHAR(32) NOT NULL,
  `student_id` BIGINT NOT NULL,
  `lesson_id` BIGINT NOT NULL,
  `reason_id` BIGINT DEFAULT NULL,
  `reason_text` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
  `deduct_hours` TINYINT NOT NULL DEFAULT 0,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `approver_id` BIGINT DEFAULT NULL,
  `approved_at` DATETIME DEFAULT NULL,
  `reject_reason` VARCHAR(255) DEFAULT NULL,
  `makeup_status` VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PENDING/ARRANGED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leave_no` (`leave_no`),
  KEY `idx_leave_student` (`student_id`),
  KEY `idx_leave_lesson` (`lesson_id`),
  KEY `idx_leave_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请';

CREATE TABLE `makeup_arrangement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `leave_request_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `lesson_id` BIGINT NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ARRANGED',
  `arranged_by` BIGINT DEFAULT NULL,
  `arranged_at` DATETIME DEFAULT NULL,
  `note` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_makeup_leave` (`leave_request_id`),
  KEY `idx_makeup_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补课安排';

-- ------------------------------------------------------------
-- 7. 报名 / 试听 / 缴费 / 课时
-- ------------------------------------------------------------
CREATE TABLE `enrollment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `enrollment_no` VARCHAR(32) NOT NULL,
  `student_id` BIGINT NOT NULL,
  `course_id` BIGINT NOT NULL,
  `class_id` BIGINT DEFAULT NULL,
  `parent_user_id` BIGINT DEFAULT NULL,
  `source` VARCHAR(20) NOT NULL DEFAULT 'MINI' COMMENT 'MINI/ADMIN',
  `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT 'PENDING_CONFIRM/PENDING_PAYMENT/ENROLLED/CANCELLED',
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirmed_by` BIGINT DEFAULT NULL,
  `confirmed_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enrollment_no` (`enrollment_no`),
  KEY `idx_enrollment_student` (`student_id`),
  KEY `idx_enrollment_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名申请';

CREATE TABLE `trial_booking` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `booking_no` VARCHAR(32) NOT NULL,
  `student_name` VARCHAR(50) NOT NULL,
  `age` VARCHAR(20) DEFAULT NULL,
  `parent_name` VARCHAR(50) DEFAULT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `course_id` BIGINT DEFAULT NULL,
  `slot_id` BIGINT DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/CONTACTED/VISITED/CONVERTED/CANCELLED',
  `follow_up_note` VARCHAR(500) DEFAULT NULL,
  `next_follow_up` DATE DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trial_no` (`booking_no`),
  KEY `idx_trial_status` (`status`),
  KEY `idx_trial_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试听预约';

CREATE TABLE `payment_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(32) NOT NULL,
  `enrollment_id` BIGINT DEFAULT NULL,
  `student_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/RENEW/MAKEUP',
  `item` VARCHAR(255) DEFAULT NULL,
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `payment_method_id` BIGINT DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `registered_by` BIGINT DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_payment_student` (`student_id`),
  KEY `idx_payment_time` (`paid_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下缴费记录';

CREATE TABLE `hour_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `total_hours` DECIMAL(8,1) NOT NULL DEFAULT 0.0,
  `used_hours` DECIMAL(8,1) NOT NULL DEFAULT 0.0,
  `remaining_hours` DECIMAL(8,1) NOT NULL DEFAULT 0.0,
  `valid_until` DATE DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hour_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课时账户';

CREATE TABLE `hour_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `lesson_id` BIGINT DEFAULT NULL,
  `type` VARCHAR(20) NOT NULL COMMENT 'PURCHASE/CONSUME/CORRECT',
  `change_hours` DECIMAL(6,1) NOT NULL DEFAULT 0.0,
  `balance_after` DECIMAL(8,1) NOT NULL DEFAULT 0.0,
  `operator_id` BIGINT DEFAULT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ledger_student` (`student_id`),
  KEY `idx_ledger_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课时流水';

-- ------------------------------------------------------------
-- 8. 作品 / 授权 / 评论
-- ------------------------------------------------------------
CREATE TABLE `work` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `class_id` BIGINT DEFAULT NULL,
  `teacher_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(100) NOT NULL,
  `work_type` VARCHAR(20) NOT NULL DEFAULT 'HARD' COMMENT 'HARD/SOFT',
  `content_char` VARCHAR(255) DEFAULT NULL,
  `image_url` VARCHAR(500) DEFAULT NULL,
  `comment` TEXT,
  `authorized` TINYINT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DISPLAYED/HIDDEN/REJECTED',
  `like_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_work_student` (`student_id`),
  KEY `idx_work_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员作品';

CREATE TABLE `work_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `work_id` BIGINT NOT NULL,
  `parent_user_id` BIGINT DEFAULT NULL,
  `content` VARCHAR(500) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/HIDDEN/REJECTED',
  `reviewed_by` BIGINT DEFAULT NULL,
  `reviewed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_work` (`work_id`),
  KEY `idx_comment_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品评论';

CREATE TABLE `work_auth` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `parent_user_id` BIGINT NOT NULL,
  `scope` VARCHAR(100) DEFAULT NULL,
  `method` VARCHAR(30) DEFAULT NULL COMMENT 'ELECTRONIC/PAPER/WECHAT',
  `authorized_at` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REVOKED',
  `evidence_url` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_auth_student_parent` (`student_id`, `parent_user_id`),
  KEY `idx_work_auth_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长作品授权';

-- ------------------------------------------------------------
-- 9. 机构内容 / 公告 / 配置 / 文件
-- ------------------------------------------------------------
CREATE TABLE `institution_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `brand` VARCHAR(50) DEFAULT NULL,
  `slogan` VARCHAR(100) DEFAULT NULL,
  `hero_intro` VARCHAR(500) DEFAULT NULL,
  `intro` TEXT,
  `philosophy` TEXT,
  `phone` VARCHAR(30) DEFAULT NULL,
  `wechat` VARCHAR(50) DEFAULT NULL,
  `hours` VARCHAR(100) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `longitude` DECIMAL(10,6) DEFAULT NULL,
  `latitude` DECIMAL(10,6) DEFAULT NULL,
  `traffic` VARCHAR(255) DEFAULT NULL,
  `since_year` INT DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构资料（单行）';

CREATE TABLE `institution_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `institution_id` BIGINT NOT NULL DEFAULT 1,
  `image_url` VARCHAR(500) NOT NULL,
  `title` VARCHAR(50) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学环境图片';

CREATE TABLE `qualification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `code` VARCHAR(100) DEFAULT NULL,
  `valid_until` VARCHAR(50) DEFAULT NULL,
  `verify_status` VARCHAR(20) NOT NULL DEFAULT 'VERIFIED' COMMENT 'VERIFIED/PENDING',
  `visible` TINYINT NOT NULL DEFAULT 1,
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构资质';

CREATE TABLE `notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(150) NOT NULL,
  `type` VARCHAR(20) NOT NULL DEFAULT 'NOTICE' COMMENT 'NOTICE/ADMISSION/HOLIDAY',
  `content` TEXT,
  `publish_date` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED/HIDDEN',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notice_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构公告';

CREATE TABLE `business_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_key` VARCHAR(50) NOT NULL,
  `rule_value` VARCHAR(255) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_key` (`rule_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务规则';

CREATE TABLE `upload_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `original_name` VARCHAR(255) DEFAULT NULL,
  `stored_name` VARCHAR(255) NOT NULL,
  `file_path` VARCHAR(500) NOT NULL,
  `url` VARCHAR(500) NOT NULL,
  `mime_type` VARCHAR(100) DEFAULT NULL,
  `size` BIGINT DEFAULT NULL,
  `biz_type` VARCHAR(50) DEFAULT NULL,
  `uploaded_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_upload_stored` (`stored_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传文件';
