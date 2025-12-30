-- Flyway migration: create achievements and folders tables
-- 风格参照其他服务的 V1 脚本，使用 IF NOT EXISTS 和 utf8mb4

CREATE TABLE IF NOT EXISTS achievements (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  title VARCHAR(512) NOT NULL,
  user_id VARCHAR(36),
  file_id VARCHAR(36),
  categories TEXT,
  type INT,
  authors TEXT,
  abstract_text TEXT,
  created_at BIGINT,
  download_count BIGINT DEFAULT 0,
  collect_count INT DEFAULT 0,
  cited_count INT DEFAULT 0,
  INDEX idx_achievements_user_id (user_id),
  INDEX idx_achievements_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS folders (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  owner_id VARCHAR(36),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_folders_owner_id (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 关联表：成就 <-> 收藏夹 的多对多关系
CREATE TABLE IF NOT EXISTS achievements_folders (
  achievement_id VARCHAR(36) NOT NULL,
  folder_id VARCHAR(36) NOT NULL,
  PRIMARY KEY (achievement_id, folder_id),
  CONSTRAINT fk_af_achievement FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
  CONSTRAINT fk_af_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
  INDEX idx_af_folder (folder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
