-- Flyway migration: add owner_id to folders and cited_count/created_at to achievements
-- Use information_schema + dynamic SQL to conditionally add columns/indexes (compatible across MySQL versions)

-- add folders.owner_id if missing
SELECT COUNT(*) INTO @c FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'folders' AND COLUMN_NAME = 'owner_id';
SET @stmt = IF(@c = 0, 'ALTER TABLE folders ADD COLUMN owner_id VARCHAR(36);', 'SELECT 0;');
PREPARE s1 FROM @stmt; EXECUTE s1; DEALLOCATE PREPARE s1;

-- add achievements.cited_count if missing
SELECT COUNT(*) INTO @c FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'achievements' AND COLUMN_NAME = 'cited_count';
SET @stmt = IF(@c = 0, 'ALTER TABLE achievements ADD COLUMN cited_count INT DEFAULT 0;', 'SELECT 0;');
PREPARE s2 FROM @stmt; EXECUTE s2; DEALLOCATE PREPARE s2;

-- add achievements.created_at if missing
SELECT COUNT(*) INTO @c FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'achievements' AND COLUMN_NAME = 'created_at';
SET @stmt = IF(@c = 0, 'ALTER TABLE achievements ADD COLUMN created_at BIGINT;', 'SELECT 0;');
PREPARE s3 FROM @stmt; EXECUTE s3; DEALLOCATE PREPARE s3;

-- create index on folders.owner_id if missing
SELECT COUNT(*) INTO @c FROM INFORMATION_SCHEMA.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'folders' AND INDEX_NAME = 'idx_folders_owner_id';
SET @stmt = IF(@c = 0, 'CREATE INDEX idx_folders_owner_id ON folders(owner_id);', 'SELECT 0;');
PREPARE s4 FROM @stmt; EXECUTE s4; DEALLOCATE PREPARE s4;

-- create index on achievements.created_at if missing
SELECT COUNT(*) INTO @c FROM INFORMATION_SCHEMA.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'achievements' AND INDEX_NAME = 'idx_achievements_created_at';
SET @stmt = IF(@c = 0, 'CREATE INDEX idx_achievements_created_at ON achievements(created_at);', 'SELECT 0;');
PREPARE s5 FROM @stmt; EXECUTE s5; DEALLOCATE PREPARE s5;
