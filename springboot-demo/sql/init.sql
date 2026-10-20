-- ============================================================================
--  小确幸商城 · 数据库初始化脚本（MySQL 5.7 / 8.0 均可）
--
--  导入方式（需要 root 或等价的建库权限）：
--
--    mysql --default-character-set=utf8mb4 -u root -p < sql/init.sql
--
--  或者进 MySQL 之后执行：
--
--    source D:/Git-space/springboot-demo/sql/init.sql;
--
--  ⚠️ 一定要带 --default-character-set=utf8mb4，否则商品 emoji 会变成 ? 号。
--     脚本里也写了 SET NAMES utf8mb4，但客户端参数是更可靠的一道保险。
--
--  脚本做了什么：
--    1. 建库 mall_db（utf8mb4）
--    2. 建专用账号 mall / mall123，只授权 mall_db —— 应用用它连库，不用 root
--    3. 建三张表：t_user 用户、t_product 商品、t_cart_item 购物车
--    4. 灌入 18 条商品 + 2 个演示账号
--
--  ⚠️ 脚本开头会 DROP TABLE，重复导入 = 数据清空重来（演示项目够用）。
--     如果你已经导入过、库里有数据不想清空，改用增量脚本：
--         sql/migrate_v2_profile.sql   （只加字段，不动数据）
-- ============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 建库
-- ---------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS mall_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 2. 建专用账号（如果你的账号没有 CREATE USER 权限，这一段可以整段跳过，
--    改成用你自己的账号连库，并同步修改 application.properties）
-- ---------------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'mall'@'localhost' IDENTIFIED BY 'mall123';
-- 账号若已存在，上面那句不会改密码，这里再对齐一次，保证和配置文件一致
ALTER USER 'mall'@'localhost' IDENTIFIED BY 'mall123';
GRANT ALL PRIVILEGES ON mall_db.* TO 'mall'@'localhost';
FLUSH PRIVILEGES;

USE mall_db;

-- ---------------------------------------------------------------------------
-- 3. 建表
--    注意 DROP 顺序：先删有外键的子表，再删被引用的父表
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS t_cart_item;
DROP TABLE IF EXISTS t_product;
DROP TABLE IF EXISTS t_user;

-- 用户表：只存散列值和盐，永远不存明文密码
CREATE TABLE t_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户编号',
    username      VARCHAR(32)  NOT NULL                COMMENT '登录名，唯一',
    nickname      VARCHAR(32)  NOT NULL                COMMENT '显示昵称',
    avatar        VARCHAR(255) NULL                    COMMENT '头像访问路径，如 /uploads/avatar/1_a1b2.png',
    email         VARCHAR(64)  NULL                    COMMENT '邮箱',
    phone         VARCHAR(20)  NULL                    COMMENT '手机号',
    bio           VARCHAR(200) NULL                    COMMENT '个性签名',
    salt          VARCHAR(32)  NOT NULL                COMMENT '每个用户独立的随机盐',
    password_hash VARCHAR(64)  NOT NULL                COMMENT 'sha256(salt:密码) 的十六进制',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户';

-- 商品表：主页列表和详情页都读它
CREATE TABLE t_product (
    id             BIGINT        NOT NULL COMMENT '商品编号',
    name           VARCHAR(64)   NOT NULL COMMENT '商品名称',
    price          DECIMAL(10,2) NOT NULL COMMENT '现价（元）',
    original_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价（元），0 表示不促销',
    category       VARCHAR(16)   NOT NULL COMMENT '分类',
    emoji          VARCHAR(16)   NOT NULL COMMENT '商品图（用 emoji 代替图片资源）',
    rating         DECIMAL(2,1)  NOT NULL DEFAULT 5.0 COMMENT '评分 0~5',
    sales          INT           NOT NULL DEFAULT 0 COMMENT '累计销量',
    tag            VARCHAR(16)   NULL COMMENT '促销标签，可为空',
    description    VARCHAR(128)  NULL COMMENT '一句话卖点',
    PRIMARY KEY (id),
    KEY idx_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '商品';

-- 购物车表：一个用户对一件商品最多一行，(user_id, product_id) 唯一
CREATE TABLE t_cart_item (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL COMMENT '所属用户',
    product_id BIGINT   NOT NULL COMMENT '商品',
    quantity   INT      NOT NULL DEFAULT 1 COMMENT '数量',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_user (user_id),
    CONSTRAINT fk_cart_user    FOREIGN KEY (user_id)    REFERENCES t_user (id)    ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES t_product (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '购物车';

-- ---------------------------------------------------------------------------
-- 4. 演示账号
--
--    散列值怎么来的？sha256( salt + ":" + 明文密码 )，实现见
--    src/main/java/com/example/demo/service/Passwords.java
--    想加账号，用那个类算出 salt 和 password_hash 再 INSERT 即可。
--
--      demo  / 123456     体验用户
--      admin / admin123   管理员
-- ---------------------------------------------------------------------------
INSERT INTO t_user (username, nickname, salt, password_hash) VALUES
('demo',  '体验用户', '9f2c1d5a7b3e4f6081a2b3c4d5e6f708',
 'c5503badb2344b42070ce239ca8cdb49266029809da20965fb6a3ec75b798985'),
('admin', '商城管理员', '1a2b3c4d5e6f70819f2c1d5a7b3e4f60',
 '4b9f45f8b13b8fa4548a8bda9e267a788b187ba25c0176a2e7bd89f488c2e38a');

-- ---------------------------------------------------------------------------
-- 5. 商品数据（18 条，和原来的内存版一致）
-- ---------------------------------------------------------------------------
INSERT INTO t_product (id, name, price, original_price, category, emoji, rating, sales, tag, description) VALUES
(1,  '无线蓝牙耳机 Pro',      399.00, 599.00, '数码', '🎧', 4.8, 12800, '限时直降', '主动降噪 · 单次续航 36 小时'),
(2,  '机械键盘 87 键',        289.00, 349.00, '数码', '⌨️', 4.7,  8600, '热销',     '青轴手感 · 全键无冲 · 白色背光'),
(3,  '便携充电宝 20000mAh',   149.00, 199.00, '数码', '🔋', 4.6, 23400, NULL,      '双向快充 · 可登机 · 三口输出'),
(4,  '智能手表 运动版',        699.00, 899.00, '数码', '⌚', 4.5,  5400, '新品',     '心率血氧 · 100+ 运动模式'),
(5,  '1080P 家用投影仪',     1299.00,1899.00, '数码', '📽️', 4.4,  2100, '限时直降', '自动对焦 · 内置音响 · 手机投屏'),
(6,  '北欧风布艺沙发',       1899.00,2599.00, '家居', '🛋️', 4.6,  1300, '包安装',   '三人位 · 可拆洗 · 高回弹海绵'),
(7,  '实木书架 五层',          459.00, 599.00, '家居', '📚', 4.7,  3800, NULL,      '橡胶木 · 加厚层板 · 承重 40kg'),
(8,  '香薰加湿器',             129.00, 169.00, '家居', '🕯️', 4.5, 15600, '热销',     '静音 30dB · 定时 · 七彩夜灯'),
(9,  '记忆棉护颈枕',            99.00, 139.00, '家居', '🛏️', 4.8, 28900, '限时直降', '慢回弹 · 人体工学 · 可拆洗外套'),
(10, '纯棉基础款 T 恤',          79.00,  99.00, '服饰', '👕', 4.6, 42100, '热销',     '220g 重磅 · 不易变形 · 五色可选'),
(11, '轻薄羽绒服',             599.00, 899.00, '服饰', '🧥', 4.7,  6700, '限时直降', '90 白鸭绒 · 可收纳 · 防钻绒'),
(12, '休闲帆布鞋',             199.00, 259.00, '服饰', '👟', 4.5, 11200, NULL,      '防滑橡胶底 · 透气内里 · 情侣款'),
(13, '云南小粒咖啡豆 500g',      89.00, 119.00, '食品', '☕', 4.9, 33200, '热销',     '中深烘焙 · 现磨手冲 · 坚果回甘'),
(14, '每日坚果混合装 30 袋',    129.00, 159.00, '食品', '🥜', 4.8, 56700, '囤货装',   '独立小包 · 六种坚果 · 无添加'),
(15, '山姆同款海盐饼干',         39.90,  49.90, '食品', '🍪', 4.6, 78100, NULL,      '咸甜交织 · 独立包装 · 下午茶点心'),
(16, '氨基酸温和洁面乳',         89.00, 119.00, '美妆', '🧴', 4.7, 19800, '新品',     '弱酸性 · 敏感肌可用 · 泡沫绵密'),
(17, '玻尿酸补水面膜 10 片',     69.00,  99.00, '美妆', '🎭', 4.8, 61200, '限时直降', '深层补水 · 服帖不滴液 · 天天用'),
(18, '雾面丝绒唇釉',             99.00, 129.00, '美妆', '💄', 4.5, 24700, NULL,      '显白正红 · 不拔干 · 持久 8 小时');

-- ---------------------------------------------------------------------------
-- 6. 导入结果自检
-- ---------------------------------------------------------------------------
SELECT '用户' AS 表, COUNT(*) AS 行数 FROM t_user
UNION ALL SELECT '商品', COUNT(*) FROM t_product
UNION ALL SELECT '购物车', COUNT(*) FROM t_cart_item;
