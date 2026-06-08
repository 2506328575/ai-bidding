-- AI 招投标系统 — 数据库 Schema (MySQL 8.0)
-- 技术栈: FastAPI + SQLModel/SQLAlchemy

-- Prompt 模板表
CREATE TABLE prompt_template (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `key`       VARCHAR(100) NOT NULL COMMENT '模板标识，如 intent.classify',
    version     INT NOT NULL COMMENT '版本号',
    content     TEXT NOT NULL COMMENT 'Prompt 模板内容（含 {{占位符}}）',
    description VARCHAR(500) COMMENT '用途说明',
    status      TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿, 1=生产中, 2=历史',
    created_by  VARCHAR(50),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key_version (`key`, version)
) COMMENT 'Prompt 模板表';

-- 需求采集维度配置表
CREATE TABLE requirement_dimension (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(50) NOT NULL COMMENT '维度名称，如「行业」',
    data_type    VARCHAR(20) NOT NULL COMMENT 'TEXT/ENUM/NUMBER/DATE/TEXTAREA',
    enum_values  JSON COMMENT '枚举值列表，如["制造业","金融"]',
    sort_order   INT NOT NULL DEFAULT 0 COMMENT '追问顺序，越小越优先',
    is_required  TINYINT NOT NULL DEFAULT 1 COMMENT '0=可选 1=必填',
    prompt_hint  VARCHAR(500) COMMENT '追问话术模板',
    is_fixed     TINYINT NOT NULL DEFAULT 0 COMMENT '0=灵活维度 1=固定维度',
    status       TINYINT NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    created_by   VARCHAR(50),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '需求采集维度配置表';

-- 标书格式校验规则表
CREATE TABLE bid_format_rule (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name     VARCHAR(100) NOT NULL COMMENT '规则名称',
    check_target  VARCHAR(50) NOT NULL COMMENT '检查对象: FONT/MARGIN/PAGE_NUM/TOC/SEAL',
    check_method  VARCHAR(50) NOT NULL COMMENT '检查方式: PROPERTY_READ/RANGE_CHECK/EXISTENCE_CHECK',
    rule_config   JSON NOT NULL COMMENT '规则参数',
    severity      VARCHAR(20) NOT NULL DEFAULT 'WARN' COMMENT 'ERROR=阻塞导出 WARN=提示 INFO=建议',
    status        TINYINT NOT NULL DEFAULT 1,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '标书格式校验规则表';

-- 政策记录表
CREATE TABLE policy_record (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    province       VARCHAR(20) NOT NULL COMMENT '省',
    city           VARCHAR(20) COMMENT '市',
    category       VARCHAR(20) NOT NULL COMMENT '类别: 社保/公积金/积分落户',
    effective_date DATE NOT NULL COMMENT '生效日期',
    expire_date    DATE COMMENT '失效日期',
    content        TEXT NOT NULL COMMENT '全文',
    source_url     VARCHAR(500) COMMENT '来源URL',
    source_type    VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'AI_SCRAPE / MANUAL',
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PUBLISHED/EXPIRED/DRAFT',
    reviewed_by    VARCHAR(50),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_location (province, city),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_effective (effective_date, expire_date)
) COMMENT '政策记录表';

-- 方案生成记录表
CREATE TABLE proposal_record (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    industry       VARCHAR(50),
    pain_points    TEXT,
    budget         VARCHAR(50),
    outline        TEXT COMMENT '大纲结构',
    sections_json  JSON COMMENT '各章节内容',
    docx_file_id   VARCHAR(50) COMMENT '生成文件ID',
    total_tokens   BIGINT DEFAULT 0,
    total_latency_ms BIGINT DEFAULT 0,
    created_by     VARCHAR(50),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '方案生成记录表';

-- 标书生成记录表
CREATE TABLE bid_record (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name  VARCHAR(200) NOT NULL,
    industry       VARCHAR(50),
    project_info   TEXT,
    sections_json  JSON COMMENT '填充结果',
    docx_file_id   VARCHAR(50),
    total_tokens   BIGINT DEFAULT 0,
    total_latency_ms BIGINT DEFAULT 0,
    created_by     VARCHAR(50),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '标书生成记录表';

-- 模板文件表
CREATE TABLE template_file (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name      VARCHAR(200) NOT NULL,
    file_size      BIGINT,
    file_data      LONGBLOB COMMENT '文件二进制',
    category       VARCHAR(50) COMMENT '商务标/技术标/方案',
    placeholders   JSON COMMENT '提取的占位符列表',
    status         TINYINT DEFAULT 1,
    created_by     VARCHAR(50),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '模板文件表';

-- 对话会话表
CREATE TABLE chat_session (
    id             VARCHAR(50) PRIMARY KEY,
    title          VARCHAR(200),
    dimension_state JSON COMMENT '需求采集维度状态',
    status         VARCHAR(20) DEFAULT 'active',
    created_by     VARCHAR(50),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '对话会话表';

-- 对话消息表
CREATE TABLE chat_message (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id     VARCHAR(50) NOT NULL,
    role           VARCHAR(10) NOT NULL COMMENT 'user/assistant',
    content        TEXT NOT NULL,
    intent         VARCHAR(30),
    sources_json   JSON,
    tokens_used    INT DEFAULT 0,
    latency_ms     BIGINT DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id, created_at),
    FOREIGN KEY (session_id) REFERENCES chat_session(id)
) COMMENT '对话消息表';

-- LLM 调用日志表
CREATE TABLE llm_call_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    model          VARCHAR(50),
    prompt_key     VARCHAR(100) COMMENT '对应 prompt_template.key',
    tokens_in      INT DEFAULT 0,
    tokens_out     INT DEFAULT 0,
    latency_ms     BIGINT,
    success        TINYINT DEFAULT 1,
    error_msg      VARCHAR(500),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created (created_at),
    INDEX idx_model (model)
) COMMENT 'LLM 调用日志表';
