CREATE TYPE publication_status AS ENUM (
    'DRAFT', -- The author is still preparing the content and it is not reviewable yet. / 作者仍在准备内容，尚未进入审核。
    'PENDING_REVIEW', -- The author submitted the content and it awaits ADMIN review. / 作者已提交内容，等待 ADMIN 审核。
    'PUBLISHED', -- The content is visible on the public website. / 内容已在公开网站展示。
    'REJECTED' -- ADMIN returned the submission with a review note; the author may revise it. / ADMIN 已连同审核意见退回该提交；作者可修改后再次提交。
    );

CREATE TABLE content_categories
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sort_order SMALLINT    NOT NULL DEFAULT 0,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_content_categories_sort_order_non_negative
        CHECK (sort_order >= 0)
);

CREATE TABLE content_category_translations
(
    category_id BIGINT       NOT NULL,
    locale_code VARCHAR(10)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    description TEXT                  DEFAULT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_content_category_translations
        PRIMARY KEY (category_id, locale_code),
    CONSTRAINT uq_content_category_translations_locale_slug
        UNIQUE (locale_code, slug),
    CONSTRAINT ck_content_category_translations_name_not_blank
        CHECK (btrim(name) != ''),
    CONSTRAINT ck_content_category_translations_slug_not_blank
        CHECK (btrim(slug) != ''),
    CONSTRAINT fk_content_category_translations_category
        FOREIGN KEY (category_id)
            REFERENCES content_categories (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_content_category_translations_locale
        FOREIGN KEY (locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT
);

CREATE TABLE articles
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id    BIGINT                      DEFAULT NULL,
    cover_media_id BIGINT                      DEFAULT NULL,
    status         publication_status NOT NULL DEFAULT 'DRAFT',
    author_id      BIGINT             NOT NULL,
    submitted_at   TIMESTAMPTZ                 DEFAULT NULL,
    reviewed_by    BIGINT                      DEFAULT NULL,
    reviewed_at    TIMESTAMPTZ                 DEFAULT NULL,
    review_note    TEXT                        DEFAULT NULL,
    published_by   BIGINT                      DEFAULT NULL,
    published_at   TIMESTAMPTZ                 DEFAULT NULL,
    created_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ                 DEFAULT NULL,

    CONSTRAINT ck_articles_published_metadata
        CHECK (
            status != 'PUBLISHED'
                OR (published_by IS NOT NULL AND published_at IS NOT NULL)
            ),
    CONSTRAINT fk_articles_category
        FOREIGN KEY (category_id)
            REFERENCES content_categories (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_articles_cover_media
        FOREIGN KEY (cover_media_id)
            REFERENCES media_assets (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_articles_author
        FOREIGN KEY (author_id)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_articles_reviewer
        FOREIGN KEY (reviewed_by)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_articles_publisher
        FOREIGN KEY (published_by)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_articles_public_list
    ON articles (published_at DESC)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

CREATE INDEX idx_articles_category_public_list
    ON articles (category_id, published_at DESC)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

CREATE INDEX idx_articles_author_status
    ON articles (author_id, status, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE article_translations
(
    article_id       BIGINT       NOT NULL,
    locale_code      VARCHAR(10)  NOT NULL,
    slug             VARCHAR(200) NOT NULL,
    title            VARCHAR(300) NOT NULL,
    excerpt          TEXT                  DEFAULT NULL,
    content          TEXT         NOT NULL,
    meta_title       VARCHAR(200)          DEFAULT NULL,
    meta_description VARCHAR(300)          DEFAULT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_article_translations
        PRIMARY KEY (article_id, locale_code),
    CONSTRAINT uq_article_translations_locale_slug
        UNIQUE (locale_code, slug),
    CONSTRAINT ck_article_translations_slug_not_blank
        CHECK (btrim(slug) != ''),
    CONSTRAINT ck_article_translations_title_not_blank
        CHECK (btrim(title) != ''),
    CONSTRAINT ck_article_translations_content_not_blank
        CHECK (btrim(content) != ''),
    CONSTRAINT fk_article_translations_article
        FOREIGN KEY (article_id)
            REFERENCES articles (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_article_translations_locale
        FOREIGN KEY (locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT
);

CREATE TABLE pages
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_key       VARCHAR(100)       NOT NULL,
    cover_media_id BIGINT                      DEFAULT NULL,
    status         publication_status NOT NULL DEFAULT 'DRAFT',
    author_id      BIGINT             NOT NULL,
    published_by   BIGINT                      DEFAULT NULL,
    published_at   TIMESTAMPTZ                 DEFAULT NULL,
    created_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ                 DEFAULT NULL,

    CONSTRAINT uq_pages_page_key
        UNIQUE (page_key),
    CONSTRAINT ck_pages_page_key_not_blank
        CHECK (btrim(page_key) != ''),
    CONSTRAINT ck_pages_published_metadata
        CHECK (
            status != 'PUBLISHED'
                OR (published_by IS NOT NULL AND published_at IS NOT NULL)
            ),
    CONSTRAINT fk_pages_cover_media
        FOREIGN KEY (cover_media_id)
            REFERENCES media_assets (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_pages_author
        FOREIGN KEY (author_id)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_pages_publisher
        FOREIGN KEY (published_by)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_pages_public_list
    ON pages (published_at DESC)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

CREATE TABLE page_translations
(
    page_id          BIGINT       NOT NULL,
    locale_code      VARCHAR(10)  NOT NULL,
    slug             VARCHAR(200) NOT NULL,
    title            VARCHAR(300) NOT NULL,
    content          TEXT         NOT NULL,
    meta_title       VARCHAR(200)          DEFAULT NULL,
    meta_description VARCHAR(300)          DEFAULT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_page_translations
        PRIMARY KEY (page_id, locale_code),
    CONSTRAINT uq_page_translations_locale_slug
        UNIQUE (locale_code, slug),
    CONSTRAINT ck_page_translations_slug_not_blank
        CHECK (btrim(slug) != ''),
    CONSTRAINT ck_page_translations_title_not_blank
        CHECK (btrim(title) != ''),
    CONSTRAINT ck_page_translations_content_not_blank
        CHECK (btrim(content) != ''),
    CONSTRAINT fk_page_translations_page
        FOREIGN KEY (page_id)
            REFERENCES pages (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_page_translations_locale
        FOREIGN KEY (locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT
);

CREATE TABLE banners
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    image_media_id BIGINT      NOT NULL,
    sort_order     SMALLINT    NOT NULL DEFAULT 0,
    is_active      BOOLEAN     NOT NULL DEFAULT TRUE,
    starts_at      TIMESTAMPTZ          DEFAULT NULL,
    ends_at        TIMESTAMPTZ          DEFAULT NULL,
    created_by     BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_banners_sort_order_non_negative
        CHECK (sort_order >= 0),
    CONSTRAINT ck_banners_schedule_order
        CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at),
    CONSTRAINT fk_banners_image_media
        FOREIGN KEY (image_media_id)
            REFERENCES media_assets (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_banners_created_by
        FOREIGN KEY (created_by)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_banners_active_display_order
    ON banners (sort_order, starts_at)
    WHERE is_active;

CREATE TABLE banner_translations
(
    banner_id    BIGINT      NOT NULL,
    locale_code  VARCHAR(10) NOT NULL,
    title        VARCHAR(200)         DEFAULT NULL,
    subtitle     VARCHAR(300)         DEFAULT NULL,
    button_label VARCHAR(100)         DEFAULT NULL,
    target_url   VARCHAR(500)         DEFAULT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_banner_translations
        PRIMARY KEY (banner_id, locale_code),
    CONSTRAINT fk_banner_translations_banner
        FOREIGN KEY (banner_id)
            REFERENCES banners (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_banner_translations_locale
        FOREIGN KEY (locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT
);

CREATE TABLE programs
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cover_media_id BIGINT                      DEFAULT NULL,
    status         publication_status NOT NULL DEFAULT 'DRAFT',
    sort_order     SMALLINT           NOT NULL DEFAULT 0,
    author_id      BIGINT             NOT NULL,
    published_by   BIGINT                      DEFAULT NULL,
    published_at   TIMESTAMPTZ                 DEFAULT NULL,
    created_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ                 DEFAULT NULL,

    CONSTRAINT ck_programs_sort_order_non_negative
        CHECK (sort_order >= 0),
    CONSTRAINT ck_programs_published_metadata
        CHECK (
            status != 'PUBLISHED'
                OR (published_by IS NOT NULL AND published_at IS NOT NULL)
            ),
    CONSTRAINT fk_programs_cover_media
        FOREIGN KEY (cover_media_id)
            REFERENCES media_assets (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_programs_author
        FOREIGN KEY (author_id)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_programs_publisher
        FOREIGN KEY (published_by)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_programs_public_list
    ON programs (sort_order, published_at DESC)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

CREATE TABLE program_translations
(
    program_id       BIGINT       NOT NULL,
    locale_code      VARCHAR(10)  NOT NULL,
    slug             VARCHAR(200) NOT NULL,
    title            VARCHAR(200) NOT NULL,
    summary          TEXT                  DEFAULT NULL,
    content          TEXT         NOT NULL,
    meta_title       VARCHAR(200)          DEFAULT NULL,
    meta_description VARCHAR(300)          DEFAULT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_program_translations
        PRIMARY KEY (program_id, locale_code),
    CONSTRAINT uq_program_translations_locale_slug
        UNIQUE (locale_code, slug),
    CONSTRAINT ck_program_translations_slug_not_blank
        CHECK (btrim(slug) != ''),
    CONSTRAINT ck_program_translations_title_not_blank
        CHECK (btrim(title) != ''),
    CONSTRAINT ck_program_translations_content_not_blank
        CHECK (btrim(content) != ''),
    CONSTRAINT fk_program_translations_program
        FOREIGN KEY (program_id)
            REFERENCES programs (id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_program_translations_locale
        FOREIGN KEY (locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT
);
