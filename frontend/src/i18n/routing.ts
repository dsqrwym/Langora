import {defineRouting} from "next-intl/routing";

export type LocaleMeta = {
    label: string;
    /** Text direction */
    dir: "ltr" | "rtl";
}

/** 设备语言偏好 cookie，proxy 通过它做根路径分流 */
export const LANGUAGE_COOKIE_NAME = "NEXT_LOCALE";

export const DEFAULT_LANGUAGE = "zh";

export const routing = defineRouting({
    /** add supported locales here */
    locales: ["en", "es", "zh"],
    defaultLocale: DEFAULT_LANGUAGE,
    localePrefix: "always",
    /** to custom rule */
    localeCookie: false
});

export type SupportedLanguage = (typeof routing.locales)[number];

/** and add locale meta here */
export const LOCALE_META: Record<SupportedLanguage, LocaleMeta> = {
    en: {label: "English", dir: "ltr"},
    es: {label: "Español", dir: "ltr"},
    zh: {label: "中文", dir: "ltr"},
};

export const SUPPORTED_LANGUAGES: SupportedLanguage[] = Object.keys(LOCALE_META) as SupportedLanguage[];

export const LANGUAGE_LABELS: Record<SupportedLanguage, string> = Object.fromEntries(
    SUPPORTED_LANGUAGES.map((locale) => [locale, LOCALE_META[locale].label]),
) as Record<SupportedLanguage, string>;

