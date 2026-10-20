-- ============================================================================
--  小确幸商城 · 增量迁移 v2：给用户加上「个人资料」字段
--
--  什么时候用这个脚本？
--    你的库是【导入过旧版 init.sql】的，里面有数据（用户、购物车），
--    不想重新导入（init.sql 会 DROP TABLE 清空数据），就用这个。
--
--  如果你是全新安装，直接用 init.sql 就行 —— 里面已经包含这些字段了。
--
--  导入方式（同样注意 utf8mb4）：
--    cmd /c "sql\migrate_v2_profile.cmd"
--    或：mysql --default-character-set=utf8mb4 -u root -p mall_db < sql\migrate_v2_profile.sql
--
--  本脚本是【可重复执行】的：字段已存在时会跳过，不会报错、不会丢数据。
--  （MySQL 5.7 没有 ADD COLUMN IF NOT EXISTS，所以用存储过程判断 information_schema）
-- ============================================================================

SET NAMES utf8mb4;
USE mall_db;

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_ddl    TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND COLUMN_NAME  = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 头像地址。存的是访问路径（如 /uploads/avatar/1_xxx.png），
-- 不存文件本身 —— 图片放磁盘，数据库只存路径，这是常规做法。
CALL add_column_if_missing('t_user', 'avatar', '`avatar` VARCHAR(255) NULL COMMENT ''头像访问路径'' AFTER `nickname`');

-- 联系方式
CALL add_column_if_missing('t_user', 'email',  '`email`  VARCHAR(64)  NULL COMMENT ''邮箱''     AFTER `avatar`');
CALL add_column_if_missing('t_user', 'phone',  '`phone`  VARCHAR(20)  NULL COMMENT ''手机号''   AFTER `email`');

-- 个性签名
CALL add_column_if_missing('t_user', 'bio',    '`bio`    VARCHAR(200) NULL COMMENT ''个性签名'' AFTER `phone`');

DROP PROCEDURE IF EXISTS add_column_if_missing;

-- ---------------------------------------------------------------------------
-- 自检：应该能看到 avatar / email / phone / bio 四行
-- ---------------------------------------------------------------------------
SELECT COLUMN_NAME AS 字段, COLUMN_TYPE AS 类型, IS_NULLABLE AS 可空, COLUMN_COMMENT AS 说明
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user'
ORDER BY ORDINAL_POSITION;
