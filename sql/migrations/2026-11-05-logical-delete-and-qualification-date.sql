-- ============================================================
-- 迁移脚本：2026-11-05
--   1) 10 张字典/内容表补 deleted 逻辑删除列
--   2) qualification.valid_until 由 VARCHAR(50) 改为 DATE
--
-- 适用：已有数据的 MySQL 8 生产/测试库（全新部署直接用 sql/schema.sql 即可，无需执行本脚本）
-- 执行：mysql -u<用户> -p hanmo_calligraphy < 本文件
-- ============================================================

USE `hanmo_calligraphy`;

-- ---------- 1. 逻辑删除列 ----------
ALTER TABLE `classroom`          ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `class_type`         ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `course_category`    ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `course_stage`       ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `leave_reason`       ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `payment_method`     ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `trial_slot`         ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `notice`             ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `qualification`      ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `institution_image`  ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0;

-- ---------- 2. 资质有效期改 DATE ----------
-- 先把非日期文本（历史数据里的「长期」）转成 NULL（NULL 表示长期有效），再改列类型
UPDATE `qualification`
   SET `valid_until` = NULL
 WHERE `valid_until` IS NOT NULL
   AND `valid_until` NOT REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$';

ALTER TABLE `qualification`
  MODIFY COLUMN `valid_until` DATE DEFAULT NULL COMMENT '有效期至；NULL 表示长期有效';
