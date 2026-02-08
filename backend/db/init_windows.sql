/*
  AstrNest - Windows / Navicat 友好版初始化脚本

  目标：解决 Windows 端使用 Navicat 导入 init.sql 时常见的报错（权限不足、DELIMITER 解析、PREPARE 多语句等）。
  说明：
  1) 本脚本【不包含】CREATE USER / GRANT / FLUSH PRIVILEGES（Navicat 常用的普通连接账号通常没有这些权限）。
  2) 移除了大量用于“旧库字段自动对齐”的 PREPARE/EXECUTE 逻辑；在全新库初始化时不需要这些动态迁移段。
  3) 触发器改为无需 DELIMITER 的单语句形式，避免 Navicat 对 DELIMITER 的兼容性差异。

  使用方式（推荐）：
  - 先在 Navicat 手动创建数据库：astrnest（字符集 utf8mb4，排序规则 utf8mb4_general_ci 或 utf8mb4_unicode_ci）
  - 选择该库后导入本文件（或直接执行）
*/

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET FOREIGN_KEY_CHECKS = 0;

-- Windows/客户端初始化数据库（如无权限可手动执行本段）
CREATE DATABASE IF NOT EXISTS astrnest CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE astrnest;

-- =====================
-- Core tables
-- =====================


CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL COMMENT '用户名',
  password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  nickname VARCHAR(120) NOT NULL COMMENT '昵称',
  email VARCHAR(180) NOT NULL COMMENT '邮箱',
  avatar_url VARCHAR(512) COMMENT '头像URL',
  website VARCHAR(255),
  signature VARCHAR(255),
  location VARCHAR(120),
  bio TEXT,
  role ENUM('user','admin','moderator') NOT NULL DEFAULT 'user',
  status ENUM('active','inactive','banned') NOT NULL DEFAULT 'active',
  storage_quota_mb BIGINT NULL DEFAULT 1024 COMMENT '兼容字段：单位 MB',
  storage_quota_bytes BIGINT NULL DEFAULT 1073741824 COMMENT '存储配额(字节)',
  used_storage BIGINT NOT NULL DEFAULT 0 COMMENT '已用存储(字节)',
  daily_upload_limit INT NULL DEFAULT 100,
  active TINYINT(1) NOT NULL DEFAULT 1,
  login_ip_history VARCHAR(1024),
  last_login_ip VARCHAR(64),
  last_login_at DATETIME NULL,
  email_verified TINYINT(1) NOT NULL DEFAULT 0,
  two_factor_enabled TINYINT(1) NOT NULL DEFAULT 0,
  settings JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email),
  INDEX idx_email (email),
  INDEX idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  UNIQUE KEY uq_roles_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS api_keys (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  public_id VARCHAR(48) NOT NULL COMMENT '密钥对外 ID',
  name VARCHAR(120) NOT NULL COMMENT '密钥名称',
  description VARCHAR(255),
  secret_hash VARCHAR(255) NOT NULL COMMENT '加密后的密钥',
  masked_key VARCHAR(80) NOT NULL,
  prefix VARCHAR(10) NOT NULL DEFAULT 'AK' COMMENT '前缀',
  scopes JSON NOT NULL COMMENT '权限范围，如 ["upload","read"]',
  owner_id BIGINT NULL COMMENT '所属用户',
  active TINYINT(1) NOT NULL DEFAULT 1,
  status ENUM('active','revoked','expired') NOT NULL DEFAULT 'active',
  daily_quota INT NOT NULL DEFAULT 1000,
  requests_today INT NOT NULL DEFAULT 0,
  per_minute_quota INT NOT NULL DEFAULT 120,
  requests_current_minute INT NOT NULL DEFAULT 0,
  current_minute_window DATETIME NULL,
  request_count BIGINT NOT NULL DEFAULT 0,
  last_request_date DATE NULL,
  last_used_at DATETIME NULL,
  expires_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_api_keys_public_id (public_id),
  INDEX idx_api_keys_prefix (prefix),
  INDEX idx_api_keys_status_expires (status, expires_at),
  CONSTRAINT fk_api_keys_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='API 密钥表';

CREATE TABLE IF NOT EXISTS upload_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  media_uuid CHAR(36) NOT NULL COMMENT '对外 UUID',
  user_id BIGINT NULL COMMENT '上传者',
  api_key_id BIGINT NULL COMMENT '使用的 API Key',
  storage_path VARCHAR(255) NOT NULL COMMENT '存储文件名（含路径）',
  image_link VARCHAR(255) NOT NULL COMMENT '访问 URL（相对或绝对）',
  file_name VARCHAR(180) NOT NULL COMMENT '原始文件名',
  file_hash VARCHAR(64) NULL COMMENT '文件哈希，用于去重',
  content_type VARCHAR(120),
  media_type ENUM('image','video','gif','other') NOT NULL DEFAULT 'image',
  size BIGINT NOT NULL COMMENT '文件大小（字节）',
  width INT NULL COMMENT '宽度（像素）',
  height INT NULL COMMENT '高度（像素）',
  -- 兼容字段（旧脚本/视图仍可能引用 duration）
  duration INT NULL DEFAULT 0 COMMENT '时长（秒）',
  -- 与后端实体字段保持一致（UploadRecord.durationSeconds -> duration_seconds）
  duration_seconds INT NULL DEFAULT 0 COMMENT '时长（秒）',
  title VARCHAR(200) NULL,
  description TEXT NULL,
  ai_description TEXT NULL,
  storage_provider VARCHAR(40) DEFAULT 'LOCAL_DISK',
  storage_mode VARCHAR(40) DEFAULT 'PUBLIC',
  storage_full_path VARCHAR(512) NULL,
  uploader_ip VARCHAR(64),
  thumbnail_url VARCHAR(500) NULL,
  thumbnail_storage_path VARCHAR(255) NULL,
  embed_url VARCHAR(512) NULL,
  is_violation TINYINT(1) NOT NULL DEFAULT 0,
  is_public TINYINT(1) NOT NULL DEFAULT 1,
  is_sensitive TINYINT(1) NOT NULL DEFAULT 0,
  review_status ENUM('pending','approved','rejected','auto_approved') DEFAULT 'pending',
  ai_decision VARCHAR(16) NULL,
  ai_label_snapshot LONGTEXT NULL,
  ai_error_code VARCHAR(64) NULL,
  ai_error_message VARCHAR(255) NULL,
  ai_request_id VARCHAR(128) NULL,
  review_score FLOAT NULL,
  review_notes TEXT NULL,
  like_count BIGINT NOT NULL DEFAULT 0,
  view_count BIGINT NOT NULL DEFAULT 0,
  download_count BIGINT NOT NULL DEFAULT 0,
  report_count BIGINT NOT NULL DEFAULT 0,
  invoke_count BIGINT NOT NULL DEFAULT 0,
  last_access_at DATETIME NULL,
  metadata JSON NULL,
  version INT NOT NULL DEFAULT 1,
  uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_upload_record_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_upload_record_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE SET NULL,
  UNIQUE KEY uq_upload_records_media_uuid (media_uuid),
  INDEX idx_upload_records_user (user_id),
  INDEX idx_upload_records_api_key (api_key_id),
  INDEX idx_upload_records_file_hash (file_hash),
  INDEX idx_upload_records_public_created (is_public, uploaded_at),
  INDEX idx_upload_records_media_type (media_type),
  FULLTEXT KEY ft_upload_records_text (title, description, ai_description),
  CONSTRAINT chk_upload_records_size CHECK (size <= 20971520)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='媒体资源表（对应 media schema）';

CREATE TABLE IF NOT EXISTS upload_likes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  upload_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  guest_token VARCHAR(64) NULL,
  guest_display_name VARCHAR(60) NULL,
  guest_avatar_url VARCHAR(255) NULL,
  liked_as_guest TINYINT(1) NOT NULL DEFAULT 0,
  liked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_upload_like_record FOREIGN KEY (upload_id) REFERENCES upload_records(id) ON DELETE CASCADE,
  CONSTRAINT fk_upload_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_upload_like_user (upload_id, user_id),
  UNIQUE KEY uq_upload_like_guest (upload_id, guest_token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS tags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(180) NOT NULL,
  type ENUM('user','system','ai') NOT NULL DEFAULT 'user',
  media_count INT NOT NULL DEFAULT 0,
  description VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_tags_name (name),
  UNIQUE KEY uq_tags_slug (slug),
  INDEX idx_tags_name (name),
  INDEX idx_tags_type (type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='标签表';

CREATE TABLE IF NOT EXISTS upload_record_tags (
  upload_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  source ENUM('user','ai','system') NOT NULL DEFAULT 'user',
  confidence FLOAT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (upload_id, tag_id),
  INDEX idx_upload_record_tags_tag (tag_id),
  INDEX idx_upload_record_tags_source (upload_id, source),
  CONSTRAINT fk_upload_record_tags_upload FOREIGN KEY (upload_id) REFERENCES upload_records(id) ON DELETE CASCADE,
  CONSTRAINT fk_upload_record_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='媒体-标签关联表';

CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT PRIMARY KEY,
  max_upload_bytes BIGINT NOT NULL DEFAULT 5242880,
  max_video_upload_bytes BIGINT NOT NULL DEFAULT 104857600,
  video_chunk_upload_enabled TINYINT(1) NOT NULL DEFAULT 1,
  video_chunk_size_mb INT NOT NULL DEFAULT 5,
  daily_upload_count_limit INT NOT NULL DEFAULT 5000,
  user_storage_quota_bytes BIGINT NOT NULL DEFAULT 5368709120,
  registration_enabled TINYINT(1) NOT NULL DEFAULT 0,
  guest_like_enabled TINYINT(1) NOT NULL DEFAULT 1,
  auto_cleanup_days INT NOT NULL DEFAULT 30,
  asset_domain VARCHAR(255),
  custom_footer_html TEXT,
  ai_moderation_enabled TINYINT(1) NOT NULL DEFAULT 0,
  ai_labeling_enabled TINYINT(1) NOT NULL DEFAULT 0,
  ai_tencent_secret_id VARCHAR(128),
  ai_tencent_secret_key VARCHAR(128),
  ai_tencent_region VARCHAR(64),
  ai_tencent_bucket VARCHAR(128),
  ai_tencent_detect_scenes VARCHAR(128),
  ai_moderation_block_confidence INT NOT NULL DEFAULT 90,
  ai_moderation_review_confidence INT NOT NULL DEFAULT 60,
  ai_label_min_confidence INT NOT NULL DEFAULT 60,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by VARCHAR(120)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS content_policy (
  policy_key VARCHAR(32) PRIMARY KEY,
  nsfw_detection_enabled TINYINT(1) NOT NULL DEFAULT 1,
  violence_detection_enabled TINYINT(1) NOT NULL DEFAULT 1,
  manual_review_threshold INT NOT NULL DEFAULT 3,
  webhook_url VARCHAR(255)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS user_login_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  ip_address VARCHAR(64),
  location VARCHAR(180),
  user_agent VARCHAR(255),
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_login_events_user (user_id),
  CONSTRAINT fk_user_login_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS chenxi_mail_config (
  id BIGINT PRIMARY KEY,
  smtp_host VARCHAR(200) NOT NULL,
  smtp_port INT NOT NULL,
  smtp_username VARCHAR(200) NOT NULL,
  smtp_password VARCHAR(200) NOT NULL,
  secure_type VARCHAR(20) NOT NULL,
  from_email VARCHAR(200) NOT NULL,
  from_name VARCHAR(120) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by VARCHAR(120)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS chenxi_email_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(180) NOT NULL,
  scene VARCHAR(40) NOT NULL,
  code VARCHAR(6) NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  consumed_at DATETIME NULL,
  consumed TINYINT(1) NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0,
  resend_available_at DATETIME NOT NULL,
  captcha_token VARCHAR(64),
  INDEX idx_chenxi_email_scene (email, scene)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS chenxi_captcha_ticket (
  id VARCHAR(64) PRIMARY KEY,
  expected_offset DOUBLE NOT NULL,
  tolerance DOUBLE NOT NULL,
  captcha_code VARCHAR(16),
  attempts INT NOT NULL DEFAULT 0,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verified TINYINT(1) NOT NULL DEFAULT 0,
  verification_token VARCHAR(64),
  verification_token_expires DATETIME NOT NULL,
  cert_consumed TINYINT(1) NOT NULL DEFAULT 0,
  verified_at DATETIME NULL,
  INDEX idx_chenxi_captcha_token (verification_token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS storage_strategy_profiles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  strategy VARCHAR(40) NOT NULL,
  name VARCHAR(120) NOT NULL UNIQUE,
  display_name VARCHAR(120) NOT NULL,
  description VARCHAR(255),
  active TINYINT(1) NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  config_json LONGTEXT,
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS announcements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(180) NOT NULL COMMENT '公告标题',
  summary VARCHAR(360) NULL COMMENT '摘要',
  level ENUM('EMERGENCY','NOTICE') NOT NULL DEFAULT 'NOTICE' COMMENT '等级',
  status ENUM('DRAFT','PUBLISHED') NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '置顶',
  content_markdown LONGTEXT NULL COMMENT 'Markdown 正文',
  published_at DATETIME NULL COMMENT '发布时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  author VARCHAR(120) NULL,
  author_user_id BIGINT NULL,
  author_role VARCHAR(120) NULL,
  author_avatar VARCHAR(512) NULL,
  updated_by VARCHAR(120) NULL,
  INDEX idx_announcements_status_published (status, published_at),
  INDEX idx_announcements_pinned_published (pinned, published_at),
  INDEX idx_announcements_level (level),
  CONSTRAINT fk_announcements_author_user FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='站点公告';

CREATE TABLE IF NOT EXISTS interactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  media_id BIGINT NOT NULL,
  media_uuid CHAR(36) NULL COMMENT 'upload_records.media_uuid 映射',
  type ENUM('like','favorite','view','download') NOT NULL,
  client_ip VARCHAR(45) NULL,
  user_agent TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY idx_user_media_type (user_id, media_id, type),
  INDEX idx_interactions_media_type (media_id, type),
  INDEX idx_interactions_user_created (user_id, created_at),
  INDEX idx_interactions_media_uuid (media_uuid),
  CONSTRAINT fk_interactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_interactions_media FOREIGN KEY (media_id) REFERENCES upload_records(id) ON DELETE CASCADE,
  CONSTRAINT fk_interactions_media_uuid FOREIGN KEY (media_uuid) REFERENCES upload_records(media_uuid) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='用户互动表';

-- Auth and security throttling (auth_lock_states)
CREATE TABLE IF NOT EXISTS auth_lock_states (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(191) NOT NULL DEFAULT '',
  ip VARCHAR(64) NOT NULL DEFAULT '',
  dimension VARCHAR(32) NOT NULL,
  stage VARCHAR(16) NOT NULL,
  fail_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  last_failed_at DATETIME NULL,
  window_date DATE NULL,
  lock_reason VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uniq_auth_lock (username, ip, dimension),
  KEY idx_auth_lock_ip (ip),
  KEY idx_auth_lock_window (window_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS security_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_type VARCHAR(64) NOT NULL,
  username VARCHAR(191) NULL,
  ip VARCHAR(64) NULL,
  message VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_security_logs_type (event_type),
  KEY idx_security_logs_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================
-- Views
-- =====================

DROP VIEW IF EXISTS media;
CREATE VIEW media AS
SELECT
  ur.media_uuid AS id,
  ur.user_id,
  ur.file_name AS original_filename,
  ur.storage_path AS storage_filename,
  ur.file_hash,
  ur.size AS file_size,
  ur.content_type AS mime_type,
  ur.media_type,
  ur.width,
  ur.height,
  COALESCE(ur.duration_seconds, ur.duration) AS duration,
  ur.title,
  ur.description,
  ur.ai_description,
  ur.storage_provider,
  ur.storage_path,
  ur.image_link AS access_url,
  ur.thumbnail_url,
  ur.is_public,
  ur.is_sensitive,
  ur.review_status,
  ur.review_score,
  ur.review_notes,
  ur.like_count,
  ur.view_count,
  ur.download_count,
  ur.report_count,
  ur.metadata,
  ur.version,
  ur.uploaded_at AS created_at,
  ur.uploaded_at AS updated_at
FROM upload_records ur;

DROP VIEW IF EXISTS media_tags;
CREATE VIEW media_tags AS
SELECT
  upload_id AS media_id,
  tag_id,
  source,
  confidence,
  created_at
FROM upload_record_tags;

-- =====================
-- Seed data
-- =====================

INSERT INTO roles (id, name, description) VALUES
    (1, 'ADMIN', '管理员'),
    (2, 'USER', '用户'),
    (3, 'GUEST', '访客')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO users (id, username, password, nickname, email, active, created_at) VALUES (1, 'admin', '$2b$12$1uLnox51dsclaN4VP7wQnen64wtIuBZyp98vltJgmOgEoTjC6/En2', 'Admin', 'admin@example.com', 1, NOW()) ON DUPLICATE KEY UPDATE password=VALUES(password), nickname=VALUES(nickname), email=VALUES(email), active=1;

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ADMIN' WHERE u.username = 'admin';

-- 默认配额校准
UPDATE users u
JOIN user_roles ur_admin ON ur_admin.user_id = u.id
JOIN roles r_admin ON r_admin.id = ur_admin.role_id AND r_admin.name = 'ADMIN'
SET u.daily_upload_limit = NULL,
    u.storage_quota_mb = NULL;

UPDATE users u
JOIN user_roles ur_user ON ur_user.user_id = u.id
JOIN roles r_user ON r_user.id = ur_user.role_id AND r_user.name = 'USER'
SET
  u.daily_upload_limit = CASE WHEN u.daily_upload_limit IS NULL OR u.daily_upload_limit <= 0 THEN 100 ELSE u.daily_upload_limit END,
  u.storage_quota_mb = CASE WHEN u.storage_quota_mb IS NULL OR u.storage_quota_mb <= 0 THEN 200 ELSE u.storage_quota_mb END
WHERE NOT EXISTS (
  SELECT 1 FROM user_roles ur2
  JOIN roles r2 ON r2.id = ur2.role_id AND r2.name = 'ADMIN'
  WHERE ur2.user_id = u.id
);

-- 默认系统配置
INSERT INTO system_config (
    id,
    max_upload_bytes,
    daily_upload_count_limit,
    user_storage_quota_bytes,
    registration_enabled,
    guest_like_enabled,
    auto_cleanup_days,
    asset_domain,
    custom_footer_html,
    ai_moderation_enabled,
    ai_labeling_enabled,
    ai_tencent_secret_id,
    ai_tencent_secret_key,
    ai_tencent_region,
    ai_tencent_bucket,
    ai_tencent_detect_scenes,
    ai_moderation_block_confidence,
    ai_moderation_review_confidence,
    ai_label_min_confidence,
    created_at,
    updated_at,
    updated_by
)
VALUES (
    1,
    20971520,
    5000,
    5368709120,
    0,
    1,
    30,
    NULL,
    NULL,
    0,
    0,
    NULL,
    NULL,
    NULL,
    NULL,
    'web,camera,album,news',
    90,
    60,
    60,
    NOW(),
    NOW(),
    'init-script'
)
ON DUPLICATE KEY UPDATE
    max_upload_bytes = VALUES(max_upload_bytes),
    guest_like_enabled = VALUES(guest_like_enabled),
    auto_cleanup_days = VALUES(auto_cleanup_days),
    custom_footer_html = VALUES(custom_footer_html),
    ai_moderation_enabled = VALUES(ai_moderation_enabled),
    ai_labeling_enabled = VALUES(ai_labeling_enabled),
    ai_tencent_secret_id = VALUES(ai_tencent_secret_id),
    ai_tencent_secret_key = VALUES(ai_tencent_secret_key),
    ai_tencent_region = VALUES(ai_tencent_region),
    ai_tencent_bucket = VALUES(ai_tencent_bucket),
    ai_tencent_detect_scenes = VALUES(ai_tencent_detect_scenes),
    ai_moderation_block_confidence = VALUES(ai_moderation_block_confidence),
    ai_moderation_review_confidence = VALUES(ai_moderation_review_confidence),
    ai_label_min_confidence = VALUES(ai_label_min_confidence),
    updated_at = NOW();

INSERT INTO chenxi_mail_config (id, smtp_host, smtp_port, smtp_username, smtp_password, secure_type, from_email, from_name, enabled, updated_at, updated_by)
VALUES (1, 'smtp.example.com', 465, 'no-reply@example.com', 'CHANGE_ME', 'ssl', 'no-reply@example.com', 'AstrNest Mailer', 0, NOW(), 'init-script')
ON DUPLICATE KEY UPDATE
    smtp_host = VALUES(smtp_host),
    smtp_port = VALUES(smtp_port),
    smtp_username = VALUES(smtp_username),
    smtp_password = VALUES(smtp_password),
    secure_type = VALUES(secure_type),
    from_email = VALUES(from_email),
    from_name = VALUES(from_name),
    enabled = VALUES(enabled),
    updated_at = NOW();

INSERT INTO content_policy (policy_key, nsfw_detection_enabled, violence_detection_enabled, manual_review_threshold)
VALUES ('default', 1, 1, 3)
ON DUPLICATE KEY UPDATE
    nsfw_detection_enabled = VALUES(nsfw_detection_enabled),
    violence_detection_enabled = VALUES(violence_detection_enabled),
    manual_review_threshold = VALUES(manual_review_threshold);

-- 确保默认资产域为空，由外部代理或 API 前缀统一处理
UPDATE system_config
SET asset_domain = NULL
WHERE id = 1 AND (asset_domain IS NULL OR asset_domain IN ('http://localhost:8080', 'http://127.0.0.1:8080'));

-- 将本地文件链接对齐为可直连的 /upload/ 路径（新库为空时无影响）
UPDATE upload_records
SET image_link = CONCAT('/upload/', REPLACE(storage_path, '\\', '/'))
WHERE storage_path IS NOT NULL
  AND (storage_provider IS NULL OR storage_provider IN ('LOCAL', 'LOCAL_DISK'));

-- =====================
-- Trigger (no DELIMITER needed)
-- =====================

DROP TRIGGER IF EXISTS trg_upload_records_before_insert;
CREATE TRIGGER trg_upload_records_before_insert
BEFORE INSERT ON upload_records
FOR EACH ROW
  SET NEW.media_uuid = IF(NEW.media_uuid IS NULL OR NEW.media_uuid = '', UUID(), NEW.media_uuid);

SET FOREIGN_KEY_CHECKS = 1;
