-- ============================================================
-- 翰墨学堂 · 书法培训管理系统
-- MySQL 8.0 表结构
-- 说明：
--   1. 所有表使用 utf8mb4，金额 DECIMAL，课时 DECIMAL(8,1)；
--   2. 核心业务表使用 deleted 做逻辑删除，日志/关系表不做逻辑删除；
--   3. 外键以逻辑关联为主（不建物理外键），便于迁移与演示数据导入；
--   4. 时间统一使用服务器本地时间，生产环境请在部署时设置时区。
-- ============================================================


-- ------------------------------------------------------------
-- 1. 角色与权限
-- ------------------------------------------------------------
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `data_scope` VARCHAR(20) NOT NULL DEFAULT 'ALL',
  `built_in` TINYINT NOT NULL DEFAULT 1,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_role_code` UNIQUE (`code`)
);

CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `module_key` VARCHAR(50) NOT NULL,
  `can_view` TINYINT NOT NULL DEFAULT 0,
  `can_create` TINYINT NOT NULL DEFAULT 0,
  `can_edit` TINYINT NOT NULL DEFAULT 0,
  `can_delete` TINYINT NOT NULL DEFAULT 0,
  `can_special` TINYINT NOT NULL DEFAULT 0,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_role_module` UNIQUE (`role_id`, `module_key`)
);

-- ------------------------------------------------------------
-- 2. 账号与审计
-- ------------------------------------------------------------
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) DEFAULT NULL,
  `password_hash` VARCHAR(100) DEFAULT NULL,
  `user_type` VARCHAR(20) NOT NULL,
  `role_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `avatar` VARCHAR(500) DEFAULT NULL,
  `wechat_openid` VARCHAR(100) DEFAULT NULL,
  `wechat_unionid` VARCHAR(100) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `failed_attempts` INT NOT NULL DEFAULT 0,
  `locked_until` DATETIME DEFAULT NULL,
  `last_login_at` DATETIME DEFAULT NULL,
  `last_login_ip` VARCHAR(64) DEFAULT NULL,
  `force_change_password` TINYINT NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_user_username` UNIQUE (`username`),
  CONSTRAINT `uk_user_openid` UNIQUE (`wechat_openid`)
);

CREATE TABLE `sys_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL,
  `operator_name` VARCHAR(50) DEFAULT NULL,
  `action_type` VARCHAR(30) NOT NULL,
  `target_type` VARCHAR(30) DEFAULT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `content` VARCHAR(1000) DEFAULT NULL,
  `ip` VARCHAR(64) DEFAULT NULL,
  `result` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

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
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_teacher_user` UNIQUE (`user_id`),
  CONSTRAINT `uk_teacher_no` UNIQUE (`teacher_no`)
);

CREATE TABLE `parent_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `wechat_nickname` VARCHAR(100) DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_parent_user` UNIQUE (`user_id`)
);

CREATE TABLE `student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_no` VARCHAR(32) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `gender` VARCHAR(10) DEFAULT NULL,
  `birth_date` DATE DEFAULT NULL,
  `school` VARCHAR(100) DEFAULT NULL,
  `grade` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `enroll_date` DATE DEFAULT NULL,
  `primary_parent_user_id` BIGINT DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_student_no` UNIQUE (`student_no`)
);

CREATE TABLE `parent_student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_user_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `relation` VARCHAR(20) NOT NULL DEFAULT 'OTHER',
  `is_primary` TINYINT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `source` VARCHAR(20) NOT NULL DEFAULT 'SELF',
  `verified_by` BIGINT DEFAULT NULL,
  `verified_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_parent_student` UNIQUE (`parent_user_id`, `student_id`)
);

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
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `course_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `class_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `capacity_min` INT DEFAULT NULL,
  `capacity_max` INT DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `classroom` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `capacity` INT DEFAULT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `leave_reason` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `payment_method` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `trial_slot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

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
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON',
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE TABLE `course_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `class_type_id` BIGINT NOT NULL,
  `lessons` INT NOT NULL DEFAULT 1,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `unit` VARCHAR(20) NOT NULL DEFAULT 'TERM',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON',
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE TABLE `course_outline` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `lesson_range` VARCHAR(50) DEFAULT NULL,
  `description` VARCHAR(500) DEFAULT NULL,
  `visible` TINYINT NOT NULL DEFAULT 1,
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

-- ------------------------------------------------------------
-- 6. 班级 / 课次 / 点名 / 请假
-- ------------------------------------------------------------
CREATE TABLE `class_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `course_id` BIGINT NOT NULL,
  `teacher_id` BIGINT NOT NULL,
  `classroom_id` BIGINT DEFAULT NULL,
  `weekday` TINYINT DEFAULT NULL,
  `start_time` TIME DEFAULT NULL,
  `end_time` TIME DEFAULT NULL,
  `capacity` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE TABLE `class_student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `joined_at` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_class_student` UNIQUE (`class_id`, `student_id`)
);

CREATE TABLE `lesson` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `class_id` BIGINT NOT NULL,
  `lesson_no` INT NOT NULL DEFAULT 1,
  `lesson_date` DATE NOT NULL,
  `start_time` TIME NOT NULL,
  `end_time` TIME NOT NULL,
  `teacher_id` BIGINT NOT NULL,
  `classroom_id` BIGINT DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
  `original_lesson_id` BIGINT DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_class_lesson_no` UNIQUE (`class_id`, `lesson_no`)
);

CREATE TABLE `attendance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lesson_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
  `consumed_hours` DECIMAL(6,1) NOT NULL DEFAULT 0.0,
  `note` VARCHAR(255) DEFAULT NULL,
  `recorded_by` BIGINT DEFAULT NULL,
  `recorded_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_attendance_lesson_student` UNIQUE (`lesson_id`, `student_id`)
);

CREATE TABLE `leave_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `leave_no` VARCHAR(32) NOT NULL,
  `student_id` BIGINT NOT NULL,
  `lesson_id` BIGINT NOT NULL,
  `reason_id` BIGINT DEFAULT NULL,
  `reason_text` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `deduct_hours` TINYINT NOT NULL DEFAULT 0,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `approver_id` BIGINT DEFAULT NULL,
  `approved_at` DATETIME DEFAULT NULL,
  `reject_reason` VARCHAR(255) DEFAULT NULL,
  `makeup_status` VARCHAR(20) NOT NULL DEFAULT 'NONE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_leave_no` UNIQUE (`leave_no`)
);

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
  PRIMARY KEY (`id`)
);

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
  `source` VARCHAR(20) NOT NULL DEFAULT 'MINI',
  `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIRM',
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirmed_by` BIGINT DEFAULT NULL,
  `confirmed_at` DATETIME DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_enrollment_no` UNIQUE (`enrollment_no`)
);

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
  `status` VARCHAR(20) NOT NULL DEFAULT 'NEW',
  `follow_up_note` VARCHAR(500) DEFAULT NULL,
  `next_follow_up` DATE DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_trial_no` UNIQUE (`booking_no`)
);

CREATE TABLE `payment_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(32) NOT NULL,
  `enrollment_id` BIGINT DEFAULT NULL,
  `student_id` BIGINT NOT NULL,
  `type` VARCHAR(20) NOT NULL DEFAULT 'NEW',
  `item` VARCHAR(255) DEFAULT NULL,
  `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `payment_method_id` BIGINT DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `registered_by` BIGINT DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_payment_no` UNIQUE (`payment_no`)
);

CREATE TABLE `hour_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `total_hours` DECIMAL(8,1) NOT NULL DEFAULT 0.0,
  `used_hours` DECIMAL(8,1) NOT NULL DEFAULT 0.0,
  `remaining_hours` DECIMAL(8,1) NOT NULL DEFAULT 0.0,
  `valid_until` DATE DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_hour_student` UNIQUE (`student_id`)
);

CREATE TABLE `hour_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `lesson_id` BIGINT DEFAULT NULL,
  `type` VARCHAR(20) NOT NULL,
  `change_hours` DECIMAL(6,1) NOT NULL DEFAULT 0.0,
  `balance_after` DECIMAL(8,1) NOT NULL DEFAULT 0.0,
  `operator_id` BIGINT DEFAULT NULL,
  `reason` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

-- ------------------------------------------------------------
-- 8. 作品 / 授权 / 评论
-- ------------------------------------------------------------
CREATE TABLE `work` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `class_id` BIGINT DEFAULT NULL,
  `teacher_id` BIGINT DEFAULT NULL,
  `title` VARCHAR(100) NOT NULL,
  `work_type` VARCHAR(20) NOT NULL DEFAULT 'HARD',
  `content_char` VARCHAR(255) DEFAULT NULL,
  `image_url` VARCHAR(500) DEFAULT NULL,
  `comment` TEXT,
  `authorized` TINYINT NOT NULL DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `like_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE TABLE `work_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `work_id` BIGINT NOT NULL,
  `parent_user_id` BIGINT DEFAULT NULL,
  `content` VARCHAR(500) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `reviewed_by` BIGINT DEFAULT NULL,
  `reviewed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `work_auth` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT NOT NULL,
  `parent_user_id` BIGINT NOT NULL,
  `scope` VARCHAR(100) DEFAULT NULL,
  `method` VARCHAR(30) DEFAULT NULL,
  `authorized_at` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `evidence_url` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_work_auth_student_parent` UNIQUE (`student_id`, `parent_user_id`)
);

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
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `institution_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `institution_id` BIGINT NOT NULL DEFAULT 1,
  `image_url` VARCHAR(500) NOT NULL,
  `title` VARCHAR(50) DEFAULT NULL,
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `qualification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `code` VARCHAR(100) DEFAULT NULL,
  `valid_until` VARCHAR(50) DEFAULT NULL,
  `verify_status` VARCHAR(20) NOT NULL DEFAULT 'VERIFIED',
  `visible` TINYINT NOT NULL DEFAULT 1,
  `sort` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `notice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(150) NOT NULL,
  `type` VARCHAR(20) NOT NULL DEFAULT 'NOTICE',
  `content` TEXT,
  `publish_date` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  `created_by` BIGINT DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);

CREATE TABLE `business_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_key` VARCHAR(50) NOT NULL,
  `rule_value` VARCHAR(255) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_rule_key` UNIQUE (`rule_key`)
);

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
  CONSTRAINT `uk_upload_stored` UNIQUE (`stored_name`)
);