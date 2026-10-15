CREATE TABLE site_locales
(
    locale_code  VARCHAR(10) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    native_name  VARCHAR(100) NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order   SMALLINT     NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_site_locales_code_not_blank
        CHECK (btrim(locale_code) != ''),
    CONSTRAINT ck_site_locales_display_name_not_blank
        CHECK (btrim(display_name) != ''),
    CONSTRAINT ck_site_locales_native_name_not_blank
        CHECK (btrim(native_name) != ''),
    CONSTRAINT ck_site_locales_sort_order_non_negative
        CHECK (sort_order >= 0),
    CONSTRAINT ck_site_locales_default_is_active
        CHECK (NOT is_default OR is_active)
);

CREATE UNIQUE INDEX uq_site_locales_default
    ON site_locales (is_default)
    WHERE is_default;

CREATE TABLE media_assets
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    storage_key TEXT         NOT NULL,
    filename    VARCHAR(255) NOT NULL,
    mime_type   VARCHAR(128) NOT NULL,
    file_size   BIGINT       NOT NULL,
    width       INTEGER               DEFAULT NULL,
    height      INTEGER               DEFAULT NULL,
    uploaded_by BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_media_assets_storage_key
        UNIQUE (storage_key),
    CONSTRAINT ck_media_assets_storage_key_not_blank
        CHECK (btrim(storage_key) != ''),
    CONSTRAINT ck_media_assets_filename_not_blank
        CHECK (btrim(filename) != ''),
    CONSTRAINT ck_media_assets_mime_type_not_blank
        CHECK (btrim(mime_type) != ''),
    CONSTRAINT ck_media_assets_file_size_non_negative
        CHECK (file_size >= 0),
    CONSTRAINT ck_media_assets_width_positive
        CHECK (width IS NULL OR width > 0),
    CONSTRAINT ck_media_assets_height_positive
        CHECK (height IS NULL OR height > 0),
    CONSTRAINT fk_media_assets_uploaded_by
        FOREIGN KEY (uploaded_by)
            REFERENCES admin_users (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_media_assets_uploaded_by_created_at
    ON media_assets (uploaded_by, created_at DESC);

CREATE TABLE media_asset_translations
(
    media_asset_id BIGINT      NOT NULL,
    locale_code    VARCHAR(10) NOT NULL,
    alt_text       VARCHAR(300) DEFAULT NULL,
    caption        TEXT                 DEFAULT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_media_asset_translations
        PRIMARY KEY (media_asset_id, locale_code),
    CONSTRAINT fk_media_asset_translations_media_asset
        FOREIGN KEY (media_asset_id)
            REFERENCES media_assets (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_media_asset_translations_locale
        FOREIGN KEY (locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT
);

CREATE TABLE site_configuration
(
    id                  SMALLINT PRIMARY KEY  DEFAULT 1,
    default_locale_code VARCHAR(10)  NOT NULL,
    timezone            VARCHAR(32)  NOT NULL,
    public_email        VARCHAR(255) NOT NULL,
    public_phone        VARCHAR(25)  NOT NULL,
    address_line1       VARCHAR(255) NOT NULL,
    address_line2       VARCHAR(255)          DEFAULT NULL,
    city                VARCHAR(100) NOT NULL,
    state_province      VARCHAR(100)          DEFAULT NULL,
    postal_code         VARCHAR(30)           DEFAULT NULL,
    country_code        CHAR(2)      NOT NULL,
    logo                BIGINT                DEFAULT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ck_site_configuration_singleton
        CHECK (id = 1),
    CONSTRAINT ck_site_configuration_timezone_not_blank
        CHECK (btrim(timezone) != ''),
    CONSTRAINT ck_site_configuration_public_email_not_blank
        CHECK (btrim(public_email) != ''),
    CONSTRAINT ck_site_configuration_public_phone_not_blank
        CHECK (btrim(public_phone) != ''),
    CONSTRAINT fk_site_configuration_default_locale
        FOREIGN KEY (default_locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT,
    CONSTRAINT fk_site_configuration_logo
        FOREIGN KEY (logo)
            REFERENCES media_assets (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_site_configuration_country
        FOREIGN KEY (country_code)
            REFERENCES countries (code)
            ON DELETE RESTRICT
);

CREATE TABLE site_configuration_translations
(
    site_configuration_id SMALLINT     NOT NULL,
    locale_code           VARCHAR(10)  NOT NULL,
    school_name           VARCHAR(255) NOT NULL,
    tagline               TEXT                  DEFAULT NULL,
    footer_text           TEXT                  DEFAULT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version               BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_site_configuration_translations
        PRIMARY KEY (site_configuration_id, locale_code),
    CONSTRAINT ck_site_configuration_translations_school_name_not_blank
        CHECK (btrim(school_name) != ''),
    CONSTRAINT fk_site_configuration_translations_configuration
        FOREIGN KEY (site_configuration_id)
            REFERENCES site_configuration (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_site_configuration_translations_locale
        FOREIGN KEY (locale_code)
            REFERENCES site_locales (locale_code)
            ON DELETE RESTRICT
);

COMMENT ON COLUMN site_configuration.address_line1 IS 'Street name and number. / 街道名与门牌号。';
COMMENT ON COLUMN site_configuration.address_line2 IS 'Apartment, unit, floor, building. / 公寓、单元、楼层、楼栋。';
COMMENT ON COLUMN site_configuration.city IS 'City or locality. / 城市或地区。';
COMMENT ON COLUMN site_configuration.state_province IS 'State, province or region. / 州、省或地区。';
COMMENT ON COLUMN site_configuration.postal_code IS 'Postal or ZIP msg. / 邮政编码。';
COMMENT ON COLUMN site_configuration.country_code IS 'ISO 3166-1 alpha-2, references countries.msg. / ISO 3166-1 两字母代码，引用 countries.msg。';



