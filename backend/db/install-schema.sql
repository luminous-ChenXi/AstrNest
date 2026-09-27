-- ============================================================================
-- AstrNest 安装向导专用建表脚本（install-schema.sql）
-- ============================================================================
-- 用途：
--   由后端安装向导（POST /api/install/database，见 com.chenxi.astrnest.install 包）
--   通过 JdbcTemplate 逐语句执行，服务于「手动 / 宝塔 / 无 init.sql 自动初始化」的
--   全新部署。前端向导地址：/install。
--
-- 与 init.sql / init_windows.sql 的关系（注意漂移风险）：
--   1. 本文件是 init.sql 的「纯 schema」提炼版：只建表 / 建视图 / 写无害种子。
--   2. 刻意去掉了 init.sql 中的：CREATE DATABASE / CREATE USER / GRANT / FLUSH
--      （应用数据库账号通常无此权限，账号初始化仍由部署者手工或 docker 完成）；
--      旧库迁移用的 @schema/PREPARE 条件 ALTER 块（全新库不需要）；
--      数据修复类 UPDATE（空表无意义）；BEFORE INSERT 触发器（含 DELIMITER，
--      无法经 JDBC 执行；media_uuid 由应用层 UploadRecord 实体生成，触发器非必需）。
--   3. 列定义以 JPA 实体（Hibernate 自动建表）为基准，包含 init.sql 缺失的
--      chenxi_mail_template 表及 duration_seconds / lock_count / guest_upload_enabled /
--      max_files_per_upload 等实体列，因此即使 ddl-auto=validate / none 也能正常启动；
--      init.sql 的历史遗留列（users.role/status、upload_records.view_count 等）
--      无任何代码引用，本脚本不再创建。
--   4. 种子完全幂等：角色 / 默认配置 / 内容策略用 WHERE NOT EXISTS（不覆盖已有值），
--      邮件配置占位用 INSERT IGNORE（绝不覆盖真实 SMTP 配置）。
--   5. 漂移风险：修改 JPA 实体或 init.sql 表结构时，请同步维护本文件；启动期的
--      SchemaAlignmentRunner 只兜底少量列，不做全量对齐。
-- ============================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 用户与角色
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(120) NOT NULL,
  nickname VARCHAR(120) NOT NULL,
  email VARCHAR(180) NULL,
  avatar_url VARCHAR(512) NULL,
  sso_sub VARCHAR(64) NULL,
  identity_source VARCHAR(32) NOT NULL DEFAULT 'local',
  website VARCHAR(255) NULL,
  signature VARCHAR(255) NULL,
  location VARCHAR(120) NULL,
  login_ip_history VARCHAR(1024) NULL,
  last_login_ip VARCHAR(64) NULL,
  last_login_at DATETIME(6) NULL,
  active BIT(1) NOT NULL DEFAULT b'1',
  email_verified BIT(1) NOT NULL DEFAULT b'0',
  daily_upload_limit INT NULL,
  storage_quota_mb BIGINT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email),
  UNIQUE KEY uk_users_sso_sub (sso_sub)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255) NULL,
  UNIQUE KEY uq_roles_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_user_roles_role (role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS api_keys (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  public_id VARCHAR(48) NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(255) NULL,
  secret_hash VARCHAR(255) NOT NULL,
  masked_key VARCHAR(80) NOT NULL,
  owner_id BIGINT NULL,
  active BIT(1) NOT NULL DEFAULT b'1',
  daily_quota INT NOT NULL DEFAULT 1000,
  requests_today INT NOT NULL DEFAULT 0,
  per_minute_quota INT NOT NULL DEFAULT 120,
  requests_current_minute INT NOT NULL DEFAULT 0,
  current_minute_window DATETIME(6) NULL,
  request_count BIGINT NOT NULL DEFAULT 0,
  last_request_date DATE NULL,
  last_used_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_api_keys_public_id (public_id),
  KEY idx_api_keys_owner (owner_id),
  CONSTRAINT fk_api_keys_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 图集（先于 upload_records 创建，便于内联 upload_records.album_id 外键）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS albums (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  album_uuid CHAR(36) NOT NULL,
  user_id BIGINT NOT NULL,
  path_slug VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT NULL,
  is_public BIT(1) NOT NULL DEFAULT b'0',
  cover_image_uuid VARCHAR(36) NULL,
  access_count BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_albums_album_uuid (album_uuid),
  UNIQUE KEY uq_albums_path_slug (path_slug),
  KEY idx_albums_user_id (user_id),
  KEY idx_albums_public (is_public),
  CONSTRAINT fk_albums_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS album_media (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  album_id BIGINT NOT NULL,
  media_uuid CHAR(36) NOT NULL,
  added_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  added_by BIGINT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  UNIQUE KEY uq_album_media (album_id, media_uuid),
  KEY idx_album_media_media_uuid (media_uuid),
  KEY idx_album_media_sort (album_id, sort_order),
  CONSTRAINT fk_album_media_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE,
  CONSTRAINT fk_album_media_user FOREIGN KEY (added_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS album_access_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  album_id BIGINT NOT NULL,
  media_uuid VARCHAR(36) NULL,
  client_ip VARCHAR(64) NULL,
  user_agent VARCHAR(512) NULL,
  referer VARCHAR(512) NULL,
  accessed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_album_access_logs_album_id (album_id),
  KEY idx_album_access_logs_accessed_at (accessed_at),
  CONSTRAINT fk_album_access_logs_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 媒体资源
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS upload_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  media_uuid CHAR(36) NOT NULL,
  user_id BIGINT NULL,
  api_key_id BIGINT NULL,
  album_id BIGINT NULL,
  storage_path VARCHAR(255) NOT NULL,
  image_link VARCHAR(255) NOT NULL,
  file_name VARCHAR(180) NOT NULL,
  content_type VARCHAR(120) NULL,
  media_type ENUM('IMAGE','VIDEO') NOT NULL,
  size BIGINT NOT NULL,
  width INT NULL,
  height INT NULL,
  duration_seconds INT NULL,
  review_status VARCHAR(40) NULL,
  ai_decision ENUM('BLOCK','PASS','REVIEW') NULL,
  ai_label_snapshot LONGTEXT NULL,
  ai_error_code VARCHAR(64) NULL,
  ai_error_message VARCHAR(255) NULL,
  ai_request_id VARCHAR(128) NULL,
  storage_provider VARCHAR(40) NULL DEFAULT 'LOCAL_DISK',
  storage_mode VARCHAR(40) NULL DEFAULT 'PUBLIC',
  storage_full_path VARCHAR(512) NULL,
  uploader_ip VARCHAR(64) NULL,
  thumbnail_url VARCHAR(500) NULL,
  thumbnail_storage_path VARCHAR(255) NULL,
  embed_url VARCHAR(512) NULL,
  is_violation BIT(1) NOT NULL DEFAULT b'0',
  is_public BIT(1) NOT NULL DEFAULT b'0',
  like_count BIGINT NOT NULL DEFAULT 0,
  invoke_count BIGINT NOT NULL DEFAULT 0,
  last_access_at DATETIME(6) NULL,
  uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_upload_record_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_upload_record_api_key FOREIGN KEY (api_key_id) REFERENCES api_keys(id) ON DELETE SET NULL,
  CONSTRAINT fk_upload_record_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE SET NULL,
  UNIQUE KEY uq_upload_records_media_uuid (media_uuid),
  KEY idx_upload_records_user (user_id),
  KEY idx_upload_records_api_key (api_key_id),
  KEY idx_upload_records_album (album_id),
  KEY idx_upload_records_album_public (album_id, is_public, is_violation)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS upload_likes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  upload_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  guest_token VARCHAR(64) NULL,
  guest_display_name VARCHAR(60) NULL,
  guest_avatar_url VARCHAR(255) NULL,
  liked_as_guest BIT(1) NOT NULL DEFAULT b'0',
  liked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_upload_like_record FOREIGN KEY (upload_id) REFERENCES upload_records(id) ON DELETE CASCADE,
  CONSTRAINT fk_upload_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_upload_like_user (upload_id, user_id),
  UNIQUE KEY uq_upload_like_guest (upload_id, guest_token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS tags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(180) NULL,
  description VARCHAR(255) NULL,
  media_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_tags_name (name),
  UNIQUE KEY uq_tags_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS upload_record_tags (
  upload_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  PRIMARY KEY (upload_id, tag_id),
  KEY idx_upload_record_tags_tag (tag_id),
  CONSTRAINT fk_upload_record_tags_upload FOREIGN KEY (upload_id) REFERENCES upload_records(id) ON DELETE CASCADE,
  CONSTRAINT fk_upload_record_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 系统配置与策略
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT PRIMARY KEY,
  max_upload_bytes BIGINT NOT NULL DEFAULT 20971520,
  max_video_upload_bytes BIGINT NOT NULL DEFAULT 104857600,
  video_chunk_upload_enabled BIT(1) NOT NULL DEFAULT b'1',
  video_chunk_size_mb INT NOT NULL DEFAULT 5,
  daily_upload_count_limit INT NOT NULL DEFAULT 5000,
  max_files_per_upload INT NOT NULL DEFAULT 30,
  user_storage_quota_bytes BIGINT NOT NULL DEFAULT 5368709120,
  registration_enabled BIT(1) NOT NULL DEFAULT b'0',
  registration_email_verify_required BIT(1) NOT NULL DEFAULT b'0',
  login_totp_required BIT(1) NOT NULL DEFAULT b'0',
  guest_like_enabled BIT(1) NOT NULL DEFAULT b'1',
  guest_upload_enabled BIT(1) NOT NULL DEFAULT b'0',
  auto_cleanup_days INT NOT NULL DEFAULT 30,
  asset_domain VARCHAR(255) NULL,
  custom_footer_html TEXT NULL,
  ai_moderation_enabled BIT(1) NOT NULL DEFAULT b'0',
  ai_labeling_enabled BIT(1) NOT NULL DEFAULT b'0',
  ai_tencent_secret_id VARCHAR(128) NULL,
  ai_tencent_secret_key VARCHAR(128) NULL,
  ai_tencent_region VARCHAR(64) NULL,
  ai_tencent_bucket VARCHAR(128) NULL,
  ai_tencent_detect_scenes VARCHAR(128) NULL,
  ai_moderation_block_confidence INT NULL DEFAULT 90,
  ai_moderation_review_confidence INT NULL DEFAULT 60,
  ai_label_min_confidence INT NULL DEFAULT 60,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(255) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS content_policy (
  policy_key VARCHAR(32) PRIMARY KEY,
  nsfw_detection_enabled BIT(1) NOT NULL DEFAULT b'1',
  violence_detection_enabled BIT(1) NOT NULL DEFAULT b'1',
  manual_review_threshold INT NOT NULL DEFAULT 3,
  webhook_url VARCHAR(255) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 安装向导完成标记（写操作见 com.chenxi.astrnest.install.InstallSetupService#finish）
CREATE TABLE IF NOT EXISTS install_state (
  id BIGINT PRIMARY KEY,
  installed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 认证 / 邮件 / 验证码 / 安全
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_login_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  ip_address VARCHAR(64) NULL,
  location VARCHAR(180) NULL,
  user_agent VARCHAR(255) NULL,
  occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_user_login_events_user (user_id),
  CONSTRAINT fk_user_login_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS chenxi_mail_config (
  id BIGINT PRIMARY KEY,
  smtp_host VARCHAR(200) NOT NULL,
  smtp_port INT NOT NULL,
  smtp_username VARCHAR(200) NOT NULL,
  smtp_password VARCHAR(200) NOT NULL,
  secure_type VARCHAR(20) NOT NULL,
  from_email VARCHAR(200) NOT NULL,
  from_name VARCHAR(120) NOT NULL,
  enabled BIT(1) NOT NULL DEFAULT b'0',
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(120) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 邮件模板（chenxi 包；启动期 ChenxiMailTemplateBootstrap 会补种默认注册模板）
CREATE TABLE IF NOT EXISTS chenxi_mail_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  type VARCHAR(60) NOT NULL,
  subject VARCHAR(200) NOT NULL,
  content LONGTEXT NOT NULL,
  variables_json LONGTEXT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  updated_by VARCHAR(120) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS chenxi_email_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(180) NOT NULL,
  scene ENUM('REGISTER','PASSWORD_RESET') NOT NULL,
  code VARCHAR(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  consumed_at DATETIME(6) NULL,
  consumed BIT(1) NOT NULL DEFAULT b'0',
  attempts INT NOT NULL DEFAULT 0,
  resend_available_at DATETIME(6) NOT NULL,
  captcha_token VARCHAR(64) NULL,
  link_token VARCHAR(64) NULL,
  KEY idx_chenxi_email_scene (email, scene)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 用户 TOTP（登录二步验证）绑定：一个用户至多一条；还原码仅存 BCrypt 哈希
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_totp (
  user_id BIGINT NOT NULL PRIMARY KEY,
  secret VARCHAR(64) NOT NULL,
  confirmed BIT(1) NOT NULL DEFAULT b'0',
  recovery_hashes TEXT NULL,
  last_used_counter BIGINT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  confirmed_at DATETIME(6) NULL,
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_user_totp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS chenxi_captcha_ticket (
  id VARCHAR(64) PRIMARY KEY,
  expected_offset DOUBLE NOT NULL,
  tolerance DOUBLE NOT NULL,
  captcha_code VARCHAR(16) NULL,
  attempts INT NOT NULL DEFAULT 0,
  expires_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  verified BIT(1) NOT NULL DEFAULT b'0',
  verification_token VARCHAR(64) NULL,
  verification_token_expires DATETIME(6) NOT NULL,
  cert_consumed BIT(1) NOT NULL DEFAULT b'0',
  verified_at DATETIME(6) NULL,
  KEY idx_chenxi_captcha_token (verification_token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS auth_lock_states (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(191) NOT NULL DEFAULT '',
  ip VARCHAR(64) NOT NULL DEFAULT '',
  dimension ENUM('IP_ONLY','USER_IP','CAPTCHA_IP') NOT NULL,
  stage ENUM('INITIAL','TIGHT','BLOCKED') NOT NULL,
  fail_count INT NOT NULL DEFAULT 0,
  lock_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME(6) NULL,
  last_failed_at DATETIME(6) NULL,
  window_date VARCHAR(10) NULL,
  lock_reason VARCHAR(255) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uniq_auth_lock (username, ip, dimension),
  KEY idx_auth_lock_ip (ip)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS security_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_type VARCHAR(64) NOT NULL,
  username VARCHAR(191) NULL,
  ip VARCHAR(64) NULL,
  message VARCHAR(512) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_security_logs_type (event_type),
  KEY idx_security_logs_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 存储策略 / 公告 / 互动（兼容）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS storage_strategy_profiles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  strategy VARCHAR(40) NOT NULL,
  name VARCHAR(120) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  description VARCHAR(255) NULL,
  active BIT(1) NOT NULL DEFAULT b'0',
  enabled BIT(1) NOT NULL DEFAULT b'1',
  config_json LONGTEXT NULL,
  created_by VARCHAR(120) NULL,
  updated_by VARCHAR(120) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_storage_strategy_profiles_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS announcements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(180) NOT NULL,
  summary VARCHAR(360) NULL,
  level ENUM('EMERGENCY','NOTICE') NOT NULL DEFAULT 'NOTICE',
  status ENUM('DRAFT','PUBLISHED') NOT NULL DEFAULT 'DRAFT',
  pinned BIT(1) NOT NULL DEFAULT b'0',
  content_markdown LONGTEXT NULL,
  published_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  author VARCHAR(120) NULL,
  author_user_id BIGINT NULL,
  author_role VARCHAR(120) NULL,
  author_avatar VARCHAR(512) NULL,
  updated_by VARCHAR(120) NULL,
  KEY idx_announcements_status_published (status, published_at),
  KEY idx_announcements_pinned_published (pinned, published_at),
  KEY idx_announcements_level (level),
  CONSTRAINT fk_announcements_author_user FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 用户互动表（历史遗留，无对应 JPA 实体；保留以兼容 init.sql 安装的外部依赖）
CREATE TABLE IF NOT EXISTS interactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  media_id BIGINT NOT NULL,
  media_uuid CHAR(36) NULL,
  type ENUM('like','favorite','view','download') NOT NULL,
  client_ip VARCHAR(45) NULL,
  user_agent TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY idx_user_media_type (user_id, media_id, type),
  KEY idx_interactions_media_type (media_id, type),
  KEY idx_interactions_user_created (user_id, created_at),
  CONSTRAINT fk_interactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_interactions_media FOREIGN KEY (media_id) REFERENCES upload_records(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- ---------------------------------------------------------------------------
-- 兼容视图（列以本脚本实际创建的表为准）
-- ---------------------------------------------------------------------------
DROP VIEW IF EXISTS media;
CREATE VIEW media AS
SELECT
  ur.media_uuid AS id,
  ur.user_id,
  ur.file_name AS original_filename,
  ur.storage_path AS storage_filename,
  ur.size AS file_size,
  ur.content_type AS mime_type,
  ur.media_type,
  ur.width,
  ur.height,
  ur.duration_seconds AS duration,
  ur.storage_provider,
  ur.image_link AS access_url,
  ur.thumbnail_url,
  ur.is_public,
  ur.is_violation AS is_sensitive,
  ur.review_status,
  ur.like_count,
  ur.invoke_count AS view_count,
  ur.uploaded_at AS created_at,
  ur.uploaded_at AS updated_at
FROM upload_records ur;

DROP VIEW IF EXISTS media_tags;
CREATE VIEW media_tags AS
SELECT
  upload_id AS media_id,
  tag_id
FROM upload_record_tags;

-- ---------------------------------------------------------------------------
-- 幂等种子（不覆盖任何已有数据）
-- ---------------------------------------------------------------------------
-- 角色种子：管理员由安装向导创建（/install 第 3 步），
-- 或第一个注册用户自动成为 ADMIN（应用内置兜底逻辑），不再内置任何默认账号。
INSERT INTO roles (id, name, description)
SELECT 1, 'ADMIN', '超级管理员' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

INSERT INTO roles (id, name, description)
SELECT 2, 'USER', '普通用户' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER');

-- 默认系统配置：仅在首次初始化插入；重复执行不覆盖任何已保存的配置。
-- 注意：显式列出 ai_moderation_enabled / ai_labeling_enabled / registration_email_verify_required / login_totp_required——
-- 当表结构由 Hibernate（ddl-auto=update）先行创建时这些列是 NOT NULL 且无 DEFAULT，缺列会整条 INSERT 失败。
INSERT INTO system_config (
    id,
    max_upload_bytes,
    max_video_upload_bytes,
    video_chunk_upload_enabled,
    video_chunk_size_mb,
    daily_upload_count_limit,
    max_files_per_upload,
    user_storage_quota_bytes,
    registration_enabled,
    registration_email_verify_required,
    login_totp_required,
    guest_like_enabled,
    guest_upload_enabled,
    auto_cleanup_days,
    ai_moderation_enabled,
    ai_labeling_enabled,
    ai_tencent_detect_scenes,
    ai_moderation_block_confidence,
    ai_moderation_review_confidence,
    ai_label_min_confidence,
    created_at,
    updated_at,
    updated_by
)
SELECT
    1,
    20971520,
    104857600,
    b'1',
    5,
    5000,
    30,
    5368709120,
    b'0',
    b'0',
    b'0',
    b'1',
    b'0',
    30,
    b'0',
    b'0',
    'web,camera,album,news',
    90,
    60,
    60,
    NOW(6),
    NOW(6),
    'install-wizard'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE id = 1);

-- 默认内容审查策略
INSERT INTO content_policy (policy_key, nsfw_detection_enabled, violence_detection_enabled, manual_review_threshold)
SELECT 'default', b'1', b'1', 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM content_policy WHERE policy_key = 'default');

-- 邮件配置占位（示例域名占位值，非真实密钥）：仅首次插入，绝不覆盖真实 SMTP 配置
INSERT IGNORE INTO chenxi_mail_config (id, smtp_host, smtp_port, smtp_username, smtp_password, secure_type, from_email, from_name, enabled, updated_at, updated_by)
VALUES (1, 'smtp.example.com', 465, 'no-reply@example.com', 'CHANGE_ME', 'ssl', 'no-reply@example.com', 'AstrNest Mailer', b'0', NOW(6), 'install-wizard');
