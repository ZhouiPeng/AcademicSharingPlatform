CREATE TABLE IF NOT EXISTS files (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  type VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  uploader_id VARCHAR(128),
  url VARCHAR(1024),
  object_key VARCHAR(512) UNIQUE,
  size BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_uploader (uploader_id),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS file_permissions (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  file_id VARCHAR(36) NOT NULL,
  principal_type VARCHAR(16) NOT NULL,
  principal_id VARCHAR(128) NOT NULL,
  permission VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_file_permissions_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
  INDEX idx_file_permissions_file (file_id),
  INDEX idx_file_permissions_principal (principal_type, principal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
