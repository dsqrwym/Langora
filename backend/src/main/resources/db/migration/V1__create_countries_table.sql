CREATE TABLE countries
(
    code         CHAR(2) PRIMARY KEY,
    code_alpha3  CHAR(3)      NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    native_name  VARCHAR(100) NOT NULL,
    phone_prefix VARCHAR(8) DEFAULT NULL,

    CONSTRAINT uq_countries_code_alpha3
        UNIQUE (code_alpha3),
    CONSTRAINT ck_countries_code_upper
        CHECK (code = upper(code)),
    CONSTRAINT ck_countries_code_alpha3_upper
        CHECK (code_alpha3 = upper(code_alpha3)),
    CONSTRAINT ck_countries_code_not_blank
        CHECK (btrim(code) != ''),
    CONSTRAINT ck_countries_display_name_not_blank
        CHECK (btrim(display_name) != ''),
    CONSTRAINT ck_countries_native_name_not_blank
        CHECK (btrim(native_name) != '')
);

COMMENT ON TABLE countries IS
    'ISO 3166-1 country reference table. / ISO 3166-1 国家参考表。';
COMMENT ON COLUMN countries.code IS
    'ISO 3166-1 alpha-2 msg, uppercase. / ISO 3166-1 两字母代码，大写。';
COMMENT ON COLUMN countries.native_name IS
    'Country name in its own primary language (e.g., España for ES). / 国家在其主要语言中的名称（如 ES 的 España）。';