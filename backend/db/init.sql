-- 数据库与账号初始化
CREATE DATABASE IF NOT EXISTS astrnest
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'astrnest'@'localhost' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON astrnest.* TO 'astrnest'@'%';
FLUSH PRIVILEGES;

USE astrnest;

-- 设置字符集和排序规则，避免 collation 冲突
-- 注意：MySQL 8.0 中 information_schema 使用 utf8mb3，需要特殊处理
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET collation_connection = utf8mb4_general_ci;

-- 获取数据库名，使用 utf8mb3 以匹配 information_schema
SET @schema := CONVERT(DATABASE() USING utf8mb3);

-- 核心表结构保障：确保全新库也可直接初始化
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
  media_uuid CHAR(36) NOT NULL  COMMENT '对外 UUID',
  user_id BIGINT NULL COMMENT '上传者',
  api_key_id BIGINT NULL COMMENT '使用的 API Key',
  album_id BIGINT NULL COMMENT '所属图集',
  storage_path VARCHAR(255) NOT NULL COMMENT '存储文件名（含路径）',
  image_link VARCHAR(255) NOT NULL COMMENT '访问 URL（相对或绝对）',
  file_name VARCHAR(180) NOT NULL COMMENT '原始文件名',
  file_hash VARCHAR(64) NULL COMMENT '文件哈希，用于去重',
  content_type VARCHAR(120),
  media_type ENUM('image','video','gif','other') NOT NULL DEFAULT 'image',
  size BIGINT NOT NULL COMMENT '文件大小（字节）',
  width INT NULL COMMENT '宽度（像素）',
  height INT NULL COMMENT '高度（像素）',
  duration INT NULL DEFAULT 0 COMMENT '时长（秒）',
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
  INDEX idx_upload_records_album (album_id),
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

-- system_config.max_video_upload_bytes
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'max_video_upload_bytes'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN max_video_upload_bytes BIGINT NOT NULL DEFAULT 104857600 AFTER max_upload_bytes;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.video_chunk_upload_enabled
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'video_chunk_upload_enabled'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN video_chunk_upload_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER max_video_upload_bytes;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.video_chunk_size_mb
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'video_chunk_size_mb'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN video_chunk_size_mb INT NOT NULL DEFAULT 5 AFTER video_chunk_upload_enabled;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.last_access_at
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'last_access_at'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN last_access_at DATETIME NULL AFTER invoke_count;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.thumbnail_storage_path
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'thumbnail_storage_path'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN thumbnail_storage_path VARCHAR(255) NULL AFTER thumbnail_url;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.album_id
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'album_id'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN album_id BIGINT NULL AFTER api_key_id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'idx_upload_records_album'),
  'SELECT 1;',
  'CREATE INDEX idx_upload_records_album ON upload_records(album_id);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'idx_upload_records_album_public'),
  'SELECT 1;',
  'CREATE INDEX idx_upload_records_album_public ON upload_records(album_id, is_public, is_violation);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'albums')
  AND NOT EXISTS (
      SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND CONSTRAINT_NAME = 'fk_upload_record_album'
  ),
  'ALTER TABLE upload_records
     ADD CONSTRAINT fk_upload_record_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE SET NULL;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.clear_legacy_video_thumbnails
UPDATE upload_records
SET thumbnail_url = NULL,
    thumbnail_storage_path = NULL
WHERE media_type = 'video'
  AND thumbnail_url = image_link
  AND (
    LOWER(thumbnail_url) LIKE '%.mp4'
    OR LOWER(thumbnail_url) LIKE '%.mp4?%'
    OR LOWER(thumbnail_url) LIKE '%.mov'
    OR LOWER(thumbnail_url) LIKE '%.mov?%'
    OR LOWER(thumbnail_url) LIKE '%.mkv'
    OR LOWER(thumbnail_url) LIKE '%.mkv?%'
    OR LOWER(thumbnail_url) LIKE '%.avi'
    OR LOWER(thumbnail_url) LIKE '%.avi?%'
    OR LOWER(thumbnail_url) LIKE '%.webm'
    OR LOWER(thumbnail_url) LIKE '%.webm?%'
    OR LOWER(thumbnail_url) LIKE '%.flv'
    OR LOWER(thumbnail_url) LIKE '%.flv?%'
    OR LOWER(thumbnail_url) LIKE '%.ts'
    OR LOWER(thumbnail_url) LIKE '%.ts?%'
    OR LOWER(thumbnail_url) LIKE '%.m4v'
    OR LOWER(thumbnail_url) LIKE '%.m4v?%'
  );

-- system_config.auto_cleanup_days
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'auto_cleanup_days'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN auto_cleanup_days INT NOT NULL DEFAULT 30 AFTER guest_like_enabled;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'announcements' AND COLUMN_NAME = 'author_user_id'),
  'SELECT 1;',
  'ALTER TABLE announcements ADD COLUMN author_user_id BIGINT NULL AFTER author;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'announcements' AND COLUMN_NAME = 'author_role'),
  'SELECT 1;',
  'ALTER TABLE announcements ADD COLUMN author_role VARCHAR(120) NULL AFTER author_user_id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'announcements' AND COLUMN_NAME = 'author_avatar'),
  'SELECT 1;',
  'ALTER TABLE announcements ADD COLUMN author_avatar VARCHAR(512) NULL AFTER author_role;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'announcements' AND CONSTRAINT_NAME = 'fk_announcements_author_user'
  ),
  'SELECT 1;',
  'ALTER TABLE announcements ADD CONSTRAINT fk_announcements_author_user FOREIGN KEY (author_user_id) REFERENCES users(id) ON DELETE SET NULL;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 元数据列自动对齐

-- users.nickname
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'display_name'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'nickname'
  ),
  'ALTER TABLE users CHANGE COLUMN display_name nickname VARCHAR(120) NOT NULL;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.nickname_backfill
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'display_name'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'nickname'
  ),
  'UPDATE users SET nickname = COALESCE(NULLIF(nickname, ''''), display_name) WHERE nickname IS NULL OR nickname = '''';',
  'SELECT 1;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.drop_display_name
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'display_name'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'nickname'
  ),
  'ALTER TABLE users DROP COLUMN display_name;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.login_ip_history
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'login_ip_history'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN login_ip_history TEXT NULL AFTER location;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.last_login_ip
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'last_login_ip'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN last_login_ip VARCHAR(64) NULL AFTER login_ip_history;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.last_login_at
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'last_login_at'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN last_login_at DATETIME NULL AFTER last_login_ip;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.daily_upload_limit
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'daily_upload_limit'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN daily_upload_limit INT NULL AFTER active;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.storage_quota_mb
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'storage_quota_mb'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN storage_quota_mb BIGINT NULL AFTER daily_upload_limit;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.daily_upload_limit_default
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'daily_upload_limit'
      AND (COLUMN_DEFAULT IS NULL OR COLUMN_DEFAULT = '0')
  ),
  'ALTER TABLE users MODIFY COLUMN daily_upload_limit INT NULL DEFAULT 100;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.storage_quota_mb_default
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'storage_quota_mb'
      AND (COLUMN_DEFAULT IS NULL OR COLUMN_DEFAULT = '0')
  ),
  'ALTER TABLE users MODIFY COLUMN storage_quota_mb BIGINT NULL DEFAULT 200;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.bio
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'bio'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN bio TEXT NULL AFTER signature;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.email_verified
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email_verified'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0 AFTER email;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.two_factor_enabled
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'two_factor_enabled'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN two_factor_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER email_verified;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.settings_json
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'settings'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN settings JSON NULL AFTER two_factor_enabled;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.role_label
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role'),
  'SELECT 1;',
  "ALTER TABLE users ADD COLUMN role ENUM('user','admin','moderator') NOT NULL DEFAULT 'user' AFTER active;"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.status_enum
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'status'),
  'SELECT 1;',
  "ALTER TABLE users ADD COLUMN status ENUM('active','inactive','banned') NOT NULL DEFAULT 'active' AFTER role;"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.storage_quota_bytes
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'storage_quota_bytes'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN storage_quota_bytes BIGINT NULL DEFAULT 1073741824 AFTER storage_quota_mb;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.used_storage
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND COLUMN_NAME = 'used_storage'),
  'SELECT 1;',
  'ALTER TABLE users ADD COLUMN used_storage BIGINT NOT NULL DEFAULT 0 AFTER storage_quota_bytes;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.email_unique
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND INDEX_NAME = 'uq_users_email'
  ),
  'SELECT 1;',
  'ALTER TABLE users ADD UNIQUE KEY uq_users_email (email);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- users.status_index
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'users' AND INDEX_NAME = 'idx_users_status'
  ),
  'SELECT 1;',
  'ALTER TABLE users ADD INDEX idx_users_status (status);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.storage_path
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'storage_path'
  ),
  'SELECT 1;',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'object_key'
    ),
    'ALTER TABLE upload_records CHANGE COLUMN object_key storage_path VARCHAR(255) NOT NULL;',
    'ALTER TABLE upload_records ADD COLUMN storage_path VARCHAR(255) NOT NULL AFTER image_name;'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.image_link
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'image_link'
  ),
  'SELECT 1;',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'public_url'
    ),
    'ALTER TABLE upload_records CHANGE COLUMN public_url image_link VARCHAR(255) NOT NULL;',
    'ALTER TABLE upload_records ADD COLUMN image_link VARCHAR(255) NOT NULL AFTER storage_path;'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.file_name
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'file_name'
  ),
  'SELECT 1;',
  IF(
    EXISTS (
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'image_name'
    ),
    'ALTER TABLE upload_records CHANGE COLUMN image_name file_name VARCHAR(180) NOT NULL;',
    'ALTER TABLE upload_records ADD COLUMN file_name VARCHAR(180) NOT NULL AFTER image_link;'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.storage_provider
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'storage_provider'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN storage_provider VARCHAR(40) NULL AFTER review_status;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.storage_mode
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'storage_mode'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN storage_mode VARCHAR(40) NULL AFTER storage_provider;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.is_violation
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'is_violation'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN is_violation TINYINT(1) NOT NULL DEFAULT 0 AFTER storage_mode;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.is_public
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'is_public'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN is_public TINYINT(1) NOT NULL DEFAULT 1 AFTER is_violation;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.like_count
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'like_count'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0 AFTER is_public;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.invoke_count
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'invoke_count'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN invoke_count BIGINT NOT NULL DEFAULT 0 AFTER like_count;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.uploader_ip
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'uploader_ip'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN uploader_ip VARCHAR(64) NULL AFTER invoke_count;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.storage_full_path
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'storage_full_path'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN storage_full_path VARCHAR(512) NULL AFTER uploader_ip;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.media_uuid
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'media_uuid'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN media_uuid CHAR(36) NULL AFTER id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE upload_records SET media_uuid = UUID() WHERE media_uuid IS NULL;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'media_uuid' AND IS_NULLABLE = 'NO'),
  'SELECT 1;',
  'ALTER TABLE upload_records MODIFY COLUMN media_uuid CHAR(36) NOT NULL AFTER id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'uq_upload_records_media_uuid'
  ),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD UNIQUE KEY uq_upload_records_media_uuid (media_uuid);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.file_hash
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'file_hash'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN file_hash VARCHAR(64) NULL AFTER file_name;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.media_type
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'media_type'),
  'SELECT 1;',
  "ALTER TABLE upload_records ADD COLUMN media_type ENUM('image','video','gif','other') NOT NULL DEFAULT 'image' AFTER content_type;"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.width
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'width'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN width INT NULL AFTER media_type;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.height
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'height'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN height INT NULL AFTER width;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.duration
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'duration'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN duration INT NULL DEFAULT 0 AFTER height;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.title
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'title'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN title VARCHAR(200) NULL AFTER duration;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.description
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'description'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN description TEXT NULL AFTER title;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.ai_description
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'ai_description'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN ai_description TEXT NULL AFTER description;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.thumbnail_url
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'thumbnail_url'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN thumbnail_url VARCHAR(500) NULL AFTER ai_description;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.embed_url
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'embed_url'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN embed_url VARCHAR(512) NULL AFTER thumbnail_url;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.is_sensitive
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'is_sensitive'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN is_sensitive TINYINT(1) NOT NULL DEFAULT 0 AFTER thumbnail_url;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.ai_decision
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'ai_decision'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN ai_decision VARCHAR(16) NULL AFTER review_status;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.ai_label_snapshot
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'ai_label_snapshot'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN ai_label_snapshot LONGTEXT NULL AFTER ai_decision;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.ai_error_code
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'ai_error_code'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN ai_error_code VARCHAR(64) NULL AFTER ai_label_snapshot;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.ai_error_message
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'ai_error_message'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN ai_error_message VARCHAR(255) NULL AFTER ai_error_code;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.ai_request_id
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'ai_request_id'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN ai_request_id VARCHAR(128) NULL AFTER ai_error_message;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.review_score
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'review_score'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN review_score FLOAT NULL AFTER ai_request_id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.review_notes
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'review_notes'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN review_notes TEXT NULL AFTER review_score;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.view_count
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'view_count'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0 AFTER like_count;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.download_count
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'download_count'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN download_count BIGINT NOT NULL DEFAULT 0 AFTER view_count;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.report_count
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'report_count'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN report_count BIGINT NOT NULL DEFAULT 0 AFTER download_count;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.metadata
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'metadata'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN metadata JSON NULL AFTER report_count;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.version
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'version'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN version INT NOT NULL DEFAULT 1 AFTER metadata;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.file_hash_index
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'idx_upload_records_file_hash'
  ),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD INDEX idx_upload_records_file_hash (file_hash);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.public_created_index
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'idx_upload_records_public_created'
  ),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD INDEX idx_upload_records_public_created (is_public, uploaded_at);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.media_type_index
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'idx_upload_records_media_type'
  ),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD INDEX idx_upload_records_media_type (media_type);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.fulltext_description
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'ft_upload_records_text'
  ),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD FULLTEXT KEY ft_upload_records_text (title, description, ai_description);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.size_check
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND CONSTRAINT_NAME = 'chk_upload_records_size'
  ),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD CONSTRAINT chk_upload_records_size CHECK (size <= 20971520);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_likes.guest_token
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_likes' AND COLUMN_NAME = 'guest_token'),
  'SELECT 1;',
  'ALTER TABLE upload_likes ADD COLUMN guest_token VARCHAR(64) NULL AFTER user_id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_likes.guest_display_name
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_likes' AND COLUMN_NAME = 'guest_display_name'),
  'SELECT 1;',
  'ALTER TABLE upload_likes ADD COLUMN guest_display_name VARCHAR(60) NULL AFTER guest_token;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_likes.guest_avatar_url
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_likes' AND COLUMN_NAME = 'guest_avatar_url'),
  'SELECT 1;',
  'ALTER TABLE upload_likes ADD COLUMN guest_avatar_url VARCHAR(255) NULL AFTER guest_display_name;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_likes.liked_as_guest
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_likes' AND COLUMN_NAME = 'liked_as_guest'),
  'SELECT 1;',
  'ALTER TABLE upload_likes ADD COLUMN liked_as_guest TINYINT(1) NOT NULL DEFAULT 0 AFTER guest_avatar_url;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_likes.user_id_nullable
SET @sql := IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema
      AND TABLE_NAME = 'upload_likes'
      AND COLUMN_NAME = 'user_id'
      AND IS_NULLABLE = 'NO'
  ),
  'ALTER TABLE upload_likes MODIFY COLUMN user_id BIGINT NULL;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_likes.unique_indexes
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_likes' AND INDEX_NAME = 'uq_upload_like_user'),
  'SELECT 1;',
  IF(
    EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_likes' AND INDEX_NAME = 'uq_upload_like'),
    'ALTER TABLE upload_likes RENAME INDEX uq_upload_like TO uq_upload_like_user;',
    'SELECT 1;'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_likes' AND INDEX_NAME = 'uq_upload_like_user'),
  'SELECT 1;',
  'ALTER TABLE upload_likes ADD UNIQUE KEY uq_upload_like_user (upload_id, user_id);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_likes' AND INDEX_NAME = 'uq_upload_like_guest'),
  'SELECT 1;',
  'ALTER TABLE upload_likes ADD UNIQUE KEY uq_upload_like_guest (upload_id, guest_token);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- chenxi_captcha_ticket.captcha_code
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'chenxi_captcha_ticket' AND COLUMN_NAME = 'captcha_code'),
  'SELECT 1;',
  'ALTER TABLE chenxi_captcha_ticket ADD COLUMN captcha_code VARCHAR(16) NULL AFTER tolerance;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- chenxi_captcha_ticket.attempts
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'chenxi_captcha_ticket' AND COLUMN_NAME = 'attempts'),
  'SELECT 1;',
  'ALTER TABLE chenxi_captcha_ticket ADD COLUMN attempts INT NOT NULL DEFAULT 0 AFTER captcha_code;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.asset_domain
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'asset_domain'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN asset_domain VARCHAR(255) NULL AFTER registration_enabled;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.custom_footer_html
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'custom_footer_html'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN custom_footer_html TEXT NULL AFTER asset_domain;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.guest_like_enabled
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'guest_like_enabled'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN guest_like_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER registration_enabled;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_moderation_enabled
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_moderation_enabled'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_moderation_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER custom_footer_html;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_labeling_enabled
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_labeling_enabled'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_labeling_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER ai_moderation_enabled;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_tencent_secret_id
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_tencent_secret_id'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_tencent_secret_id VARCHAR(128) NULL AFTER ai_labeling_enabled;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_tencent_secret_key
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_tencent_secret_key'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_tencent_secret_key VARCHAR(128) NULL AFTER ai_tencent_secret_id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_tencent_region
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_tencent_region'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_tencent_region VARCHAR(64) NULL AFTER ai_tencent_secret_key;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_tencent_bucket
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_tencent_bucket'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_tencent_bucket VARCHAR(128) NULL AFTER ai_tencent_region;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_tencent_detect_scenes
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_tencent_detect_scenes'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_tencent_detect_scenes VARCHAR(128) NULL AFTER ai_tencent_bucket;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_moderation_block_confidence
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_moderation_block_confidence'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_moderation_block_confidence INT NOT NULL DEFAULT 90 AFTER ai_tencent_detect_scenes;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_moderation_review_confidence
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_moderation_review_confidence'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_moderation_review_confidence INT NOT NULL DEFAULT 60 AFTER ai_moderation_block_confidence;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- system_config.ai_label_min_confidence
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'system_config' AND COLUMN_NAME = 'ai_label_min_confidence'),
  'SELECT 1;',
  'ALTER TABLE system_config ADD COLUMN ai_label_min_confidence INT NOT NULL DEFAULT 60 AFTER ai_moderation_review_confidence;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE system_config
SET asset_domain = NULL
WHERE asset_domain IN ('http://localhost:8080', 'http://127.0.0.1:8080');

UPDATE system_config SET guest_like_enabled = IFNULL(guest_like_enabled, 1);

-- tags.type
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'tags' AND COLUMN_NAME = 'type'),
  'SELECT 1;',
  "ALTER TABLE tags ADD COLUMN type ENUM('user','system','ai') NOT NULL DEFAULT 'user' AFTER slug;"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tags.media_count
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'tags' AND COLUMN_NAME = 'media_count'),
  'SELECT 1;',
  'ALTER TABLE tags ADD COLUMN media_count INT NOT NULL DEFAULT 0 AFTER type;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- tags.type_index
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'tags' AND INDEX_NAME = 'idx_tags_type'
  ),
  'SELECT 1;',
  'ALTER TABLE tags ADD INDEX idx_tags_type (type);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_record_tags.source
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_record_tags' AND COLUMN_NAME = 'source'),
  'SELECT 1;',
  "ALTER TABLE upload_record_tags ADD COLUMN source ENUM('user','ai','system') NOT NULL DEFAULT 'user' AFTER tag_id;"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_record_tags.confidence
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_record_tags' AND COLUMN_NAME = 'confidence'),
  'SELECT 1;',
  'ALTER TABLE upload_record_tags ADD COLUMN confidence FLOAT NULL AFTER source;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_record_tags.created_at
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_record_tags' AND COLUMN_NAME = 'created_at'),
  'SELECT 1;',
  'ALTER TABLE upload_record_tags ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER confidence;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_record_tags.tag_index
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_record_tags' AND INDEX_NAME = 'idx_upload_record_tags_tag'
  ),
  'SELECT 1;',
  'ALTER TABLE upload_record_tags ADD INDEX idx_upload_record_tags_tag (tag_id);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.per_minute_quota
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'per_minute_quota'),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD COLUMN per_minute_quota INT NOT NULL DEFAULT 120 AFTER requests_today;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.requests_current_minute
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'requests_current_minute'),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD COLUMN requests_current_minute INT NOT NULL DEFAULT 0 AFTER per_minute_quota;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.current_minute_window
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'current_minute_window'),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD COLUMN current_minute_window DATETIME NULL AFTER requests_current_minute;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.owner_id
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'owner_id'),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD COLUMN owner_id BIGINT NULL AFTER id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.prefix
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'prefix'),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD COLUMN prefix VARCHAR(10) NULL AFTER masked_key;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.scopes
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'scopes'),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD COLUMN scopes JSON NULL AFTER prefix;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.status
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'status'),
  'SELECT 1;',
  "ALTER TABLE api_keys ADD COLUMN status ENUM('active','revoked','expired') NOT NULL DEFAULT 'active' AFTER active;"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.expires_at
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND COLUMN_NAME = 'expires_at'),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD COLUMN expires_at DATETIME NULL AFTER last_used_at;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.prefix_index
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND INDEX_NAME = 'idx_api_keys_prefix'
  ),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD INDEX idx_api_keys_prefix (prefix);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_keys.status_index
SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'api_keys' AND INDEX_NAME = 'idx_api_keys_status_expires'
  ),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD INDEX idx_api_keys_status_expires (status, expires_at);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA COLLATE utf8mb3_general_ci = @schema COLLATE utf8mb3_general_ci
      AND CONSTRAINT_NAME COLLATE utf8mb3_general_ci = 'fk_api_keys_owner'
  ),
  'SELECT 1;',
  'ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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

-- interactions.media_uuid retro-fit
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'interactions' AND COLUMN_NAME = 'media_uuid'),
  'SELECT 1;',
  'ALTER TABLE interactions ADD COLUMN media_uuid CHAR(36) NULL AFTER media_id;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE interactions i
JOIN upload_records ur ON ur.id = i.media_id
SET i.media_uuid = ur.media_uuid
WHERE i.media_uuid IS NULL;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'interactions' AND COLUMN_NAME = 'media_uuid' AND IS_NULLABLE = 'YES'
  ),
  'ALTER TABLE interactions MODIFY COLUMN media_uuid CHAR(36) NOT NULL;',
  'SELECT 1;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'interactions' AND INDEX_NAME = 'idx_interactions_media_uuid'
  ),
  'SELECT 1;',
  'ALTER TABLE interactions ADD INDEX idx_interactions_media_uuid (media_uuid);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (
    SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA COLLATE utf8mb3_general_ci = @schema COLLATE utf8mb3_general_ci
      AND CONSTRAINT_NAME COLLATE utf8mb3_general_ci = 'fk_interactions_media_uuid'
  ),
  'SELECT 1;',
  'ALTER TABLE interactions ADD CONSTRAINT fk_interactions_media_uuid FOREIGN KEY (media_uuid) REFERENCES upload_records(media_uuid) ON DELETE CASCADE;'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
  ur.duration,
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

-- 角色与管理员账号（若已存在则更新描述）
INSERT INTO roles (id, name, description) VALUES
    (1, 'ADMIN', '管理员'),
    (2, 'USER', '用户'),
    (3, 'GUEST', '访客')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 种子管理员（仅为首次部署占位）：用户名 admin，初始密码 ChangeMe_123!
-- ⚠️ 首次部署后必须立即运行 init-admin.py 重设管理员密码，或登录后在安全中心修改！
INSERT INTO users (id, username, password, nickname, email, active, created_at) VALUES (1, 'admin', '$2b$12$SOG1m96Ip1sr1kANhmDyQOVLGs.C4OJW8FiTuHmL7cN6DHTRztnF6', 'Admin', 'admin@example.com', 1, NOW()) ON DUPLICATE KEY UPDATE password=VALUES(password), nickname=VALUES(nickname), email=VALUES(email), active=1;

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

UPDATE system_config
SET max_upload_bytes = 20971520
WHERE id = 1 AND max_upload_bytes <> 20971520;

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

-- 将本地文件链接对齐为可直连的 /upload/ 路径，便于使用统一反代规则
UPDATE upload_records
SET image_link = CONCAT('/upload/', REPLACE(storage_path, '\\', '/'))
WHERE storage_path IS NOT NULL
  AND (storage_provider IS NULL OR storage_provider IN ('LOCAL', 'LOCAL_DISK'));

DROP TRIGGER IF EXISTS trg_upload_records_before_insert;
DELIMITER $$
CREATE TRIGGER trg_upload_records_before_insert
BEFORE INSERT ON upload_records
FOR EACH ROW
BEGIN
  IF NEW.media_uuid IS NULL OR NEW.media_uuid = '' THEN
    SET NEW.media_uuid = UUID();
  END IF;
END$$
DELIMITER ;

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

-- ==================== 图集功能表 ====================

CREATE TABLE IF NOT EXISTS albums (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  album_uuid CHAR(36) NOT NULL UNIQUE COMMENT '图集UUID',
  user_id BIGINT NOT NULL COMMENT '创建者ID',
  path_slug VARCHAR(50) UNIQUE NOT NULL COMMENT 'URL路径标识，如 "pc"',
  name VARCHAR(100) NOT NULL COMMENT '图集名称',
  description TEXT COMMENT '描述',
  is_public BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否公开',
  cover_image_uuid CHAR(36) COMMENT '封面图UUID',
  access_count BIGINT NOT NULL DEFAULT 0 COMMENT '访问次数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_albums_user_id (user_id),
  INDEX idx_albums_path_slug (path_slug),
  INDEX idx_albums_public (is_public),
  CONSTRAINT fk_albums_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='用户图集表';

CREATE TABLE IF NOT EXISTS album_media (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  album_id BIGINT NOT NULL COMMENT '图集ID',
  media_uuid CHAR(36) NOT NULL COMMENT '图片UUID',
  added_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  added_by BIGINT COMMENT '操作者用户ID',
  sort_order INT DEFAULT 0 COMMENT '自定义排序',
  UNIQUE KEY uq_album_media (album_id, media_uuid),
  INDEX idx_album_media_media_uuid (media_uuid),
  INDEX idx_album_media_sort (album_id, sort_order),
  CONSTRAINT fk_album_media_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE,
  CONSTRAINT fk_album_media_user FOREIGN KEY (added_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='图集与图片关联表';

CREATE TABLE IF NOT EXISTS album_access_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  album_id BIGINT NOT NULL COMMENT '图集ID',
  media_uuid CHAR(36) COMMENT '返回的图片UUID',
  client_ip VARCHAR(64) COMMENT '访问者IP',
  user_agent VARCHAR(512) COMMENT 'User-Agent',
  referer VARCHAR(512) COMMENT '来源页面',
  accessed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_album_access_logs_album_id (album_id),
  INDEX idx_album_access_logs_accessed_at (accessed_at),
  CONSTRAINT fk_album_access_logs_album FOREIGN KEY (album_id) REFERENCES albums(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='图集访问日志表';

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

-- upload_records.width
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'width'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN width INT NULL COMMENT \'图片宽度（像素）\';'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records.height
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND COLUMN_NAME = 'height'),
  'SELECT 1;',
  'ALTER TABLE upload_records ADD COLUMN height INT NULL COMMENT \'图片高度（像素）\';'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- upload_records width/height indexes
SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'idx_upload_records_width'),
  'SELECT 1;',
  'CREATE INDEX idx_upload_records_width ON upload_records(width);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
  EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema AND TABLE_NAME = 'upload_records' AND INDEX_NAME = 'idx_upload_records_height'),
  'SELECT 1;',
  'CREATE INDEX idx_upload_records_height ON upload_records(height);'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
