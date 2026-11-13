export {
    SUPPORTED_LANGUAGES,
    DEFAULT_LANGUAGE,
    LANGUAGE_COOKIE_NAME,
    LANGUAGE_LABELS,
    isSupportedLanguage,
    detectBrowserLanguage,
    pickLanguageFromAcceptLanguage,
} from "@/i18n/languages";
export type {SupportedLanguage} from "@/i18n/languages";

export {
    buildLocalizedPath,
    replaceLanguagePrefix,
    stripLanguagePrefix,
    getLanguageOrDefault,
} from "@/i18n/paths";

export {readPreferredLanguage, writePreferredLanguage} from "@/i18n/locale-preference";

export {getDictionary} from "@/i18n/dictionary";
export type {Dictionary} from "@/i18n/dictionary";

export {formatMessage} from "@/i18n/format";
export type {MessageParams} from "@/i18n/format";
