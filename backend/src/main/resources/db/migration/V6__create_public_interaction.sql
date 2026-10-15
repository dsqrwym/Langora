CREATE TYPE admission_application_status AS ENUM (
    'NEW', -- The public form was received and has not been handled. / 已收到公开表单，尚未处理。
    'IN_PROGRESS', -- An ADMIN is contacting or following up with the guardian. / ADMIN 正在联系或跟进监护人。
    'RESOLVED', -- The enquiry was successfully answered or completed. / 咨询已得到答复或完成处理。
    'CLOSED' -- The enquiry was closed without further action. / 咨询已关闭，不再处理。
    );

CREATE TYPE comment_status AS ENUM (
    'PENDING', -- The comment is waiting for ADMIN moderation. / 评论正在等待 ADMIN 审核。
    'APPROVED', -- The comment may be shown publicly below the article. / 评论可在文章下方公开展示。
    'REJECTED', -- The comment is not shown publicly. / 评论不公开展示。
    'SPAM' -- The comment was identified as spam or abuse. / 评论被识别为垃圾或滥用信息。
    );

CREATE TABLE admission_applications
(
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    program_id            BIGINT                                DEFAULT NULL,
    submitted_locale_code VARCHAR(10)                  NOT NULL,
    guardian_name         VARCHAR(150)                 NOT NULL,
    guardian_email        VARCHAR(255)                 NOT NULL,
    guardian_phone        VARCHAR(50)                           DEFAULT NULL,
    child_name            VARCHAR(150)                          DEFAULT NULL,
    child_age             SMALLINT                              DEFAULT NULL,
    message               TEXT                                  DEFAULT NULL,
    status                admission_application_status NOT NULL DEFAULT 'NEW',
    handled_by            BIGINT                                DEFAULT NULL,
    handled_at            TIMESTAMPTZ                           DEFAULT NULL,
    internal_note         TEXT                                  DEFAULT NULL,
    created_at            TIMESTAMPTZ                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ                  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_admission_applications_guardian_name_not_blank
        CHECK (btrim(guardian_name) != ''),
    CONSTRAINT ck_admission_applications_guardian_email_not_blank
        CHECK (btrim(guardian_email) != ''),
    CONSTRAINT ck_admission_applications_child_age_range
        CHECK (child_age IS NULL OR child_age BETWEEN 0 AND 120),
    CONSTRAINT fk_admission_applications_program
        FOREIGN KEY (program_id)
            REFERENCES programs (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_admission_applications_submitted_locale
        FOREIGN KEY (submitted_locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT,
    CONSTRAINT fk_admission_applications_handled_by
        FOREIGN KEY (handled_by)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_admission_applications_status_created_at
    ON admission_applications (status, created_at DESC);

CREATE INDEX idx_admission_applications_program_created_at
    ON admission_applications (program_id, created_at DESC);

CREATE TABLE comments
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    article_id      BIGINT         NOT NULL,
    locale_code     VARCHAR(10)    NOT NULL,
    display_name    VARCHAR(100)   NOT NULL,
    email           VARCHAR(255)   NOT NULL,
    content         TEXT           NOT NULL,
    status          comment_status NOT NULL DEFAULT 'PENDING',
    moderated_by    BIGINT                  DEFAULT NULL,
    moderated_at    TIMESTAMPTZ             DEFAULT NULL,
    moderation_note TEXT                    DEFAULT NULL,
    ip_hash         VARCHAR(255)            DEFAULT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_comments_display_name_not_blank
        CHECK (btrim(display_name) != ''),
    CONSTRAINT ck_comments_email_not_blank
        CHECK (btrim(email) != ''),
    CONSTRAINT ck_comments_content_not_blank
        CHECK (btrim(content) != ''),
    CONSTRAINT fk_comments_article
        FOREIGN KEY (article_id)
            REFERENCES articles (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_comments_locale
        FOREIGN KEY (locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT,
    CONSTRAINT fk_comments_moderated_by
        FOREIGN KEY (moderated_by)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_comments_public_article_created_at
    ON comments (article_id, created_at DESC)
    WHERE status = 'APPROVED';

CREATE INDEX idx_comments_moderation_queue
    ON comments (status, created_at ASC)
    WHERE status = 'PENDING';



