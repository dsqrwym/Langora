import {
    DEFAULT_LANGUAGE,
    SUPPORTED_LANGUAGES,
    type SupportedLanguage
} from "@/i18n/languages.ts";
import {canUseDom} from "@/lib/device.ts";
import {getLanguageOrDefault} from "@/i18n/paths.ts";
import i18next from "i18next";
import {initReactI18next} from "react-i18next";
import en from "@/i18n/locales/en.ts";
import zh from "@/i18n/locales/zh.ts";
import es from "@/i18n/locales/es.ts";

const initialLanguage: SupportedLanguage = getLanguageOrDefault();

void i18next
    .use(initReactI18next)
    .init({
        resources: {
            en: {translation: {en}},
            zh: {translation: {zh}},
            es: {translation: {es}}
        },
        lng: initialLanguage,
        fallbackLng: DEFAULT_LANGUAGE,
        supportedLngs: [...SUPPORTED_LANGUAGES],
        // 是否允许非显式支持的语言回退 即 zh-CN -> zh
        nonExplicitSupportedLngs: true,
        interpolation: {
            // 自动转义 React 已经自带
            escapeValue: false,
        },
        returnNull: false,
        react: {
            // 如果不关，当资源没加载完时，整个页面会触发 React Suspense 的 fallback，导致页面闪烁或空白
            useSuspense: false
        }
    });

i18next
    .on("languageChanged", (lng) => {
        if (!canUseDom()) return;
        // 只同步 DOM，不写 localStorage，浏览器导航（打开 /es/...）也会触发这里，写进去就等于"浏览 = 选择"
        document.documentElement.lang = lng.split("-")[0];
    });

if (canUseDom()) {
    document.documentElement.lang = initialLanguage;
}

export const getCurrentLanguage = (): string =>
    i18next.resolvedLanguage ?? i18next.language ?? DEFAULT_LANGUAGE;
export const changeLanguage = (lng: SupportedLanguage): Promise<unknown> =>
    i18next.changeLanguage(lng);

export default i18next;
