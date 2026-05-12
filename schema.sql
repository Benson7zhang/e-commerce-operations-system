-- ==============================================================================
-- 模块名称: 数据库表结构定义 (schema.sql)
-- 模块标识: [MOD_DB_SCHEMA]
-- ==============================================================================
--
-- 功能概述:
--   定义电商管理系统的所有数据表、索引、约束。
--
-- 数据表清单:
--   t_user           用户表 (买家/卖家/管理员)
--   t_address        收货地址表 (用户 1:N 地址)
--   t_supplier       供应商表
--   t_warehouse      仓库表
--   t_product        商品表 (关联供应商)
--   t_inventory      库存表 (商品+仓库组合唯一，含 quantity/locked_qty/alert_threshold)
--   t_inventory_log  库存变动日志 (入库/出库/锁定/解锁/调整，可追溯)
--   t_order          订单主表
--   t_order_item     订单明细表 (订单 1:N 明细)
--   t_logistics      物流主表 (订单 1:1 物流)
--   t_logistics_track 物流轨迹表 (物流 1:N 轨迹)
--   t_return         退货申请表 (订单 1:0..1 退货)
--   t_profit_report  利润报表 (预留)
--
-- 核心设计要点:
--   1. 库存三量模型: quantity(总库存), locked_qty(锁定量), available=quantity-locked_qty
--   2. 订单状态ENUM: 待支付/已支付/备货中/已发货/配送中/已签收/滞留/申请退货/退货中/已退货/已取消
--   3. 退货状态ENUM: 待处理/已批准/已拒绝/已完成
--   4. 软删除: t_product.is_deleted 标记删除，保留历史订单引用
--   5. 唯一约束: SKU、订单号、用户名、手机号、邮箱等
--
-- 使用方式:
--   mysql -u root -p ecommerce < schema.sql
--
-- 依赖:
--   MySQL 8.0+ (支持 ENUM, CHECK 约束, JSON 类型等)
-- ==============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------
-- 用户表
-- -------------------------------------------
CREATE TABLE `t_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(256) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `email` VARCHAR(128) DEFAULT NULL,
    `role` ENUM('买家', '卖家', '管理员') NOT NULL DEFAULT '买家',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常, 0=禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 收货地址表
-- -------------------------------------------
CREATE TABLE `t_address` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `receiver_name` VARCHAR(64) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `province` VARCHAR(32) NOT NULL,
    `city` VARCHAR(32) NOT NULL,
    `district` VARCHAR(32) NOT NULL,
    `detail` VARCHAR(256) NOT NULL,
    `is_default` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 供应商表
-- -------------------------------------------
CREATE TABLE `t_supplier` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `contact_name` VARCHAR(64) DEFAULT NULL,
    `contact_phone` VARCHAR(20) DEFAULT NULL,
    `address` VARCHAR(256) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=合作中, 0=停止合作',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 仓库表
-- -------------------------------------------
CREATE TABLE `t_warehouse` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `region` VARCHAR(64) NOT NULL,
    `address` VARCHAR(256) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用, 0=停用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 商品表
-- -------------------------------------------
CREATE TABLE `t_product` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `sku` VARCHAR(64) NOT NULL,
    `name` VARCHAR(256) NOT NULL,
    `type` VARCHAR(64) NOT NULL COMMENT '商品类型/分类',
    `unit_price` DECIMAL(12, 2) NOT NULL,
    `cost_price` DECIMAL(12, 2) NOT NULL,
    `supplier_id` BIGINT UNSIGNED DEFAULT NULL,
    `receiver_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '入库接收员',
    `status` ENUM('草稿', '在售', '下架') NOT NULL DEFAULT '草稿',
    `description` TEXT DEFAULT NULL,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku` (`sku`),
    KEY `idx_status` (`status`),
    KEY `idx_supplier` (`supplier_id`),
    KEY `idx_type` (`type`),
    CONSTRAINT `fk_product_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `t_supplier` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_product_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `t_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `chk_unit_price` CHECK (`unit_price` >= 0),
    CONSTRAINT `chk_cost_price` CHECK (`cost_price` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 库存表
-- -------------------------------------------
CREATE TABLE `t_inventory` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `warehouse_id` BIGINT UNSIGNED NOT NULL,
    `quantity` INT NOT NULL DEFAULT 0,
    `locked_qty` INT NOT NULL DEFAULT 0 COMMENT '下单锁定数量',
    `alert_threshold` INT NOT NULL DEFAULT 10 COMMENT '库存预警阈值',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_warehouse` (`product_id`, `warehouse_id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_warehouse` (`warehouse_id`),
    CONSTRAINT `fk_inventory_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_inventory_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `t_warehouse` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_quantity` CHECK (`quantity` >= 0),
    CONSTRAINT `chk_locked_qty` CHECK (`locked_qty` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 订单表
-- -------------------------------------------
CREATE TABLE `t_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(32) NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `warehouse_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '发货仓库',
    `total_amount` DECIMAL(14, 2) NOT NULL,
    `status` ENUM(
        '待支付', '已支付', '备货中', '已发货', 
        '配送中', '滞留', '已到达', '已签收',
        '申请退货', '退货中', '已退货', '已取消'
    ) NOT NULL DEFAULT '待支付',
    `receiver_name` VARCHAR(64) NOT NULL,
    `receiver_phone` VARCHAR(20) NOT NULL,
    `receiver_address` VARCHAR(512) NOT NULL COMMENT '收货地址快照',
    `shipper_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '发货人',
    `remark` VARCHAR(512) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `paid_at` DATETIME DEFAULT NULL,
    `shipped_at` DATETIME DEFAULT NULL,
    `completed_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_created` (`created_at`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_order_shipper` FOREIGN KEY (`shipper_id`) REFERENCES `t_user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_order_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `t_warehouse` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 订单明细表
-- -------------------------------------------
CREATE TABLE `t_order_item` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `product_name` VARCHAR(256) NOT NULL COMMENT '商品名称快照',
    `unit_price` DECIMAL(12, 2) NOT NULL COMMENT '购买时单价快照',
    `quantity` INT NOT NULL,
    `subtotal` DECIMAL(14, 2) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_product` (`product_id`),
    CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `t_order` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_item_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `chk_item_quantity` CHECK (`quantity` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 物流表
-- -------------------------------------------
CREATE TABLE `t_logistics` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `tracking_no` VARCHAR(64) NOT NULL,
    `carrier` VARCHAR(64) NOT NULL COMMENT '承运商',
    `shipper_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '发货人',
    `current_node` VARCHAR(128) DEFAULT NULL COMMENT '当前节点',
    `current_status` ENUM('已揽收', '配送中', '滞留', '已送达') DEFAULT '已揽收',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tracking_no` (`tracking_no`),
    KEY `idx_order` (`order_id`),
    CONSTRAINT `fk_logistics_order` FOREIGN KEY (`order_id`) REFERENCES `t_order` (`id`),
    CONSTRAINT `fk_logistics_shipper` FOREIGN KEY (`shipper_id`) REFERENCES `t_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 物流轨迹表
-- -------------------------------------------
CREATE TABLE `t_logistics_track` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `logistics_id` BIGINT UNSIGNED NOT NULL,
    `node` VARCHAR(128) NOT NULL COMMENT '节点名称',
    `status` ENUM('已到达', '已发出', '滞留') NOT NULL,
    `node_time` DATETIME NOT NULL,
    `remark` VARCHAR(256) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_logistics` (`logistics_id`),
    KEY `idx_node_time` (`logistics_id`, `node_time`),
    CONSTRAINT `fk_track_logistics` FOREIGN KEY (`logistics_id`) REFERENCES `t_logistics` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 退货表
-- -------------------------------------------
CREATE TABLE `t_return` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `reason` VARCHAR(512) NOT NULL,
    `status` ENUM('待处理', '已批准', '已拒绝', '已完成') NOT NULL DEFAULT '待处理',
    `return_tracking_no` VARCHAR(64) DEFAULT NULL COMMENT '退货物流单号',
    `refund_amount` DECIMAL(14, 2) DEFAULT NULL,
    `reject_reason` VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `processed_at` DATETIME DEFAULT NULL COMMENT '审核时间',
    `completed_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order` (`order_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_return_order` FOREIGN KEY (`order_id`) REFERENCES `t_order` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 库存变动日志表 (扩展)
-- -------------------------------------------
CREATE TABLE `t_inventory_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `inventory_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NOT NULL,
    `change_type` ENUM('入库', '出库', '锁定', '解锁', '调整') NOT NULL,
    `change_qty` INT NOT NULL,
    `before_qty` INT NOT NULL,
    `after_qty` INT NOT NULL,
    `ref_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型: order/return/adjust',
    `ref_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务ID',
    `operator_id` BIGINT UNSIGNED DEFAULT NULL,
    `remark` VARCHAR(256) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_inventory` (`inventory_id`),
    KEY `idx_product` (`product_id`),
    CONSTRAINT `fk_log_inventory` FOREIGN KEY (`inventory_id`) REFERENCES `t_inventory` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_log_product` FOREIGN KEY (`product_id`) REFERENCES `t_product` (`id`) ON DELETE CASCADE,
    KEY `idx_ref` (`ref_type`, `ref_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------
-- 利润报表 (扩展)
-- -------------------------------------------
CREATE TABLE `t_profit_report` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `period_type` ENUM('日', '周', '月') NOT NULL,
    `period_date` DATE NOT NULL,
    `product_id` BIGINT UNSIGNED DEFAULT NULL,
    `supplier_id` BIGINT UNSIGNED DEFAULT NULL,
    `sales_qty` INT NOT NULL DEFAULT 0,
    `sales_amount` DECIMAL(16, 2) NOT NULL DEFAULT 0,
    `cost_amount` DECIMAL(16, 2) NOT NULL DEFAULT 0,
    `profit_amount` DECIMAL(16, 2) NOT NULL DEFAULT 0,
    `profit_rate` DECIMAL(5, 4) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_period` (`period_type`, `period_date`, `product_id`),
    KEY `idx_period_date` (`period_date`),
    KEY `idx_product` (`product_id`),
    KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ===========================================
-- 初始数据
-- ===========================================

-- 默认仓库
INSERT INTO `t_warehouse` (`name`, `region`, `address`) VALUES
('华东仓', '华东', '上海市嘉定区'),
('华南仓', '华南', '广东省广州市'),
('华北仓', '华北', '北京市大兴区');

-- 管理员账号 (密码: admin123 的bcrypt hash)
INSERT INTO `t_user` (`username`, `password_hash`, `role`) VALUES
('admin', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.A.k.Oy.7q8QKmG', '管理员');

