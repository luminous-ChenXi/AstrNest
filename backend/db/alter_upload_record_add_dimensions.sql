-- 为 upload_records 表添加 width 和 height 字段
ALTER TABLE upload_records
    ADD COLUMN width INT NULL COMMENT '图片宽度（像素）',
    ADD COLUMN height INT NULL COMMENT '图片高度（像素）';

-- 创建索引以优化按尺寸查询
CREATE INDEX idx_upload_records_width ON upload_records(width);
CREATE INDEX idx_upload_records_height ON upload_records(height);
