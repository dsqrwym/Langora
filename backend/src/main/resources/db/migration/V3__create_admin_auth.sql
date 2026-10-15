CREATE TYPE admin_user_role AS ENUM (
    'SUPER_ADMIN', -- Full system owner: manages site configuration, enabled locales, and back-office accounts. / 系统完全管理者：管理站点配置、启用语言和后台账户。
    'ADMIN', -- CMS and school-operation manager; publishes content and handles public applications. / CMS 与学校业务管理者；发布内容并处理公开咨询。
    'EDITOR' -- Content author; creates and submits only their own articles for review. / 内容作者；仅创建并提交自己的文章供审核。
    );

CREATE TYPE admin_user_status AS ENUM (
    'ACTIVE', -- The account may authenticate and use its assigned permissions. / 账户可登录并使用被授予的权限。
    'DISABLED' -- The account cannot authenticate; its active sessions must be revoked. / 账户不可登录；其有效会话必须被撤销。
    );

CREATE TYPE admin_session_revocation_reason AS ENUM (
    'LOGOUT', -- The account holder explicitly signed out. / 账户持有人主动登出。
    'ADMIN_REVOKED', -- A SUPER_ADMIN manually invalidated the session. / SUPER_ADMIN 手动使该会话失效。
    'ACCOUNT_DISABLED', -- The owning account was disabled. / 所属账户已被停用。
    'PASSWORD_CHANGED', -- The account password changed and existing sessions were invalidated. / 账户密码已修改，现有会话被撤销。
    'TOKEN_REUSE_DETECTED', -- A rotated refresh token was reused, indicating possible token theft. / 已轮换的 Refresh Token 被再次使用，可能存在 Token 泄露。
    'SESSION_LIMIT_EXCEEDED'
    );

CREATE TYPE operation_log_actor_type AS ENUM (
    'ADMIN', -- 后台管理员
    'SYSTEM' -- 系统自动操作（定时任务、迁移脚本等）
    );


CREATE TYPE operation_log_action AS ENUM (
    'CREATE', -- A new resource was created. / 创建了新资源。
    'UPDATE', -- An existing resource or its translation was changed. / 修改了现有资源或其翻译。
    'DELETE', -- A resource was deleted or soft-deleted. / 删除或软删除了资源。
    'SUBMIT_FOR_REVIEW', -- Content was submitted by its author for ADMIN review. / 内容作者提交内容等待 ADMIN 审核。
    'PUBLISH', -- Content was made visible on the public website. / 内容被发布到公开网站。
    'REJECT', -- Submitted content or a comment was rejected. / 已提交内容或评论被拒绝。
    'ACTIVATE', -- A configurable resource such as a locale, banner, or account was enabled. / 启用了语言、Banner 或账户等可配置资源。
    'DEACTIVATE', -- A configurable resource such as a locale, banner, or account was disabled. / 停用了语言、Banner 或账户等可配置资源。
    'APPROVE', -- A pending public comment was approved for display. / 待审核公开评论被批准显示。
    'MARK_AS_SPAM', -- A public comment was classified as spam. / 公开评论被标记为垃圾信息。
    'RESOLVE', -- A public admission enquiry was resolved. / 公开入学咨询已解决。
    'CLOSE', -- A public admission enquiry was closed without further action. / 公开入学咨询已关闭，不再处理。
    'LOGIN', -- A successful back-office login created a session. / 后台登录成功并创建会话。
    'LOGOUT', -- A back-office logout revoked a session. / 后台登出并撤销会话。
    'REVOKE_SESSION' -- An active session was invalidated for an administrative or security reason. / 因管理或安全原因使有效会话失效。
    );

CREATE TYPE operation_log_resource_type AS ENUM (
    'ADMIN_USER', -- A back-office account. / 后台账户。
    'ADMIN_USER_SESSION', -- A refresh-token session. / Refresh Token 会话。
    'SITE_LOCALE', -- A language enabled for this institution. / 为该机构启用的语言。
    'SITE_CONFIGURATION', -- The singleton school website configuration. / 单例学校网站配置。
    'MEDIA_ASSET', -- An uploaded media file and its metadata. / 上传的媒体文件及其元数据。
    'CONTENT_CATEGORY', -- A category used to classify articles. / 用于分类文章的分类。
    'ARTICLE', -- A news article or announcement. / 新闻文章或公告。
    'PAGE', -- A stable public page such as About or Contact. / 如学校介绍或联系我们等稳定公开页面。
    'BANNER', -- A homepage banner. / 首页 Banner。
    'PROGRAM', -- A public educational-programme description. / 公开教育项目说明。
    'ADMISSION_APPLICATION',-- A public guardian enquiry, not a formal enrollment. / 公开监护人咨询，不是正式入学。
    'COMMENT' -- A public article comment. / 公开文章评论。
    );

CREATE TABLE admin_users
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)       NOT NULL,
    email         VARCHAR(255)      NOT NULL,
    password_hash VARCHAR(255)      NOT NULL,
    role          admin_user_role   NOT NULL DEFAULT 'EDITOR',
    status        admin_user_status NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ                DEFAULT NULL,
    created_at    TIMESTAMPTZ       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version       BIGINT            NOT NULL DEFAULT 0,

    CONSTRAINT ck_admin_users_username_not_blank
        CHECK (btrim(username) != ''),
    CONSTRAINT ck_admin_users_email_lowercase
        CHECK (email = lower(email)),
    CONSTRAINT ck_admin_users_email_not_blank
        CHECK (btrim(email) != '')
);

CREATE UNIQUE INDEX uq_admin_users_username
    ON admin_users (username);

CREATE UNIQUE INDEX uq_admin_users_email
    ON admin_users (email);

CREATE TABLE admin_user_sessions
(
    id                          UUID PRIMARY KEY,
    admin_user_id               BIGINT       NOT NULL,
    refresh_token_hash          VARCHAR(255) NOT NULL,
    previous_refresh_token_hash VARCHAR(255)                    DEFAULT NULL,
    device_name                 VARCHAR(150)                    DEFAULT NULL,
    device_id                   VARCHAR(64)  NOT NULL,
    user_agent                  VARCHAR(500) NOT NULL,
    last_ip                     VARCHAR(50)  NOT NULL,
    created_at                  TIMESTAMPTZ  NOT NULL           DEFAULT CURRENT_TIMESTAMP,
    last_active_at              TIMESTAMPTZ  NOT NULL           DEFAULT CURRENT_TIMESTAMP,
    expires_at                  TIMESTAMPTZ  NOT NULL,
    revoked_at                  TIMESTAMPTZ                     DEFAULT NULL,
    revoked_reason              admin_session_revocation_reason DEFAULT NULL,

    CONSTRAINT uq_admin_user_sessions_user_device
        UNIQUE (admin_user_id, device_id),
    CONSTRAINT uq_admin_user_sessions_refresh_token_hash
        UNIQUE (refresh_token_hash),
    CONSTRAINT ck_admin_user_sessions_expiry_after_creation
        CHECK (expires_at > created_at),
    CONSTRAINT ck_admin_user_sessions_revocation_pair
        CHECK (
            (revoked_at IS NULL AND revoked_reason IS NULL)
                OR (revoked_at IS NOT NULL AND revoked_reason IS NOT NULL)
            ),
    CONSTRAINT fk_admin_user_sessions_admin_user
        FOREIGN KEY (admin_user_id)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_admin_user_sessions_active_user
    ON admin_user_sessions (admin_user_id)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_admin_user_sessions_user_last_active
    ON admin_user_sessions (admin_user_id, last_active_at DESC);

CREATE INDEX idx_admin_user_sessions_expires_at
    ON admin_user_sessions (expires_at);

CREATE UNIQUE INDEX uq_admin_user_sessions_prev_refresh_token_hash
    ON admin_user_sessions (previous_refresh_token_hash)
    WHERE previous_refresh_token_hash IS NOT NULL;

CREATE TABLE operation_logs
(
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    actor_type                operation_log_actor_type    NOT NULL,
    actor_id                  TEXT                                 DEFAULT NULL,
    actor_display_name        VARCHAR(100)                         DEFAULT NULL,

    action                    operation_log_action        NOT NULL,

    resource_type             operation_log_resource_type NOT NULL,
    resource_id               TEXT                                 DEFAULT NULL,
    resource_display_name     VARCHAR(255)                         DEFAULT NULL,

    target_actor_type         operation_log_actor_type             DEFAULT NULL,
    target_actor_id           TEXT                                 DEFAULT NULL,
    target_actor_display_name VARCHAR(100)                         DEFAULT NULL,

    before_data               JSONB                                DEFAULT NULL,
    after_data                JSONB                                DEFAULT NULL,
    ip_address                VARCHAR(50)                          DEFAULT NULL,
    user_agent                TEXT                                 DEFAULT NULL,
    created_at                TIMESTAMPTZ                 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_operation_logs_actor
    ON operation_logs (actor_type, actor_id, created_at DESC);

CREATE INDEX idx_operation_logs_target
    ON operation_logs (target_actor_type, target_actor_id, created_at DESC)
    WHERE target_actor_id IS NOT NULL;

CREATE INDEX idx_operation_logs_resource
    ON operation_logs (resource_type, resource_id, created_at DESC)
    WHERE resource_id IS NOT NULL;

CREATE INDEX idx_operation_logs_created_at
    ON operation_logs (created_at DESC);

COMMENT ON TABLE admin_users IS
    'Internal accounts allowed to access the Langora back office. / 允许访问 Langora 后台的内部账户。';
COMMENT ON COLUMN admin_users.email IS
    'Case-insensitive contact and recovery email. / 不区分大小写的联系与找回邮箱。';
COMMENT ON COLUMN admin_user_sessions.device_id IS
    'Client-generated persistent UUID for device tracking. / 客户端生成的持久化设备 UUID。';

COMMENT ON TABLE operation_logs IS
    'Append-only audit trail for successful state-changing back-office actions. / 后台成功状态变更操作的只追加审计记录。';
COMMENT ON COLUMN operation_logs.before_data IS
    'Safe summary of relevant values before the change; secrets and unnecessary personal data are excluded. / 变更前相关字段的安全摘要；不包含密钥和不必要的个人资料。';
COMMENT ON COLUMN operation_logs.after_data IS
    'Safe summary of relevant values after the change; secrets and unnecessary personal data are excluded. / 变更后相关字段的安全摘要；不包含密钥和不必要的个人资料。';
