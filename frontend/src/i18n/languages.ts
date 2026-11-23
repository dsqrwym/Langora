/**
 * 纯语言常量与纯函数。
 * 这里不允许出现 window / document / localStorage，middleware 与 RSC 都会直接引用本模块。
 */
export type LocaleMeta = {
    label: string;
    /** 文本方向，为未来 RTL 语言预留 */
    dir: "ltr" | "rtl";
};

/** locale 新增语言只改这里，派生的列表、标签与校验会自动跟随 */
export const LOCALE_CONFIG: Record<string, LocaleMeta> = {
    en: {label: "English", dir: "ltr"},
    es: {label: "Español", dir: "ltr"},
    zh: {label: "中文", dir: "ltr"},
};

export type SupportedLanguage = keyof typeof LOCALE_CONFIG;

export const SUPPORTED_LANGUAGES = Object.keys(LOCALE_CONFIG);

export const DEFAULT_LANGUAGE: SupportedLanguage = "zh";

/** 设备语言偏好 cookie，proxy 通过它做根路径分流 */
export const LANGUAGE_COOKIE_NAME = "NEXT_LOCALE";

export const LANGUAGE_LABELS: Record<SupportedLanguage, string> = Object.fromEntries(
    SUPPORTED_LANGUAGES.map((locale) => [locale, LOCALE_CONFIG[locale].label]),
);

export const isSupportedLanguage = (value: unknown): value is SupportedLanguage =>
    typeof value === "string" && SUPPORTED_LANGUAGES.includes(value);

/**
 * 解析 Accept-Language 并按 q 值降序取第一个受支持的语言。
 */
export const pickLanguageFromAcceptLanguage = (header: string | null): SupportedLanguage | null => {
    if (!header) return null;

    const ranked = header
        .split(",")
        .map((part) => {
            const [tag, ...params] = part.trim().split(";");
            const q = params.map((param) => param.trim()).find((param) => param.startsWith("q="));
            const quality = q ? Number.parseFloat(q.slice(2)) : 1;
            return {
                tag: (tag ?? "").trim().toLowerCase(),
                quality: Number.isFinite(quality) ? quality : 1,
            };
        })
        .filter((entry) => entry.tag.length > 0)
        .sort((a, b) => b.quality - a.quality);

    for (const {tag} of ranked) {
        const base = tag.split("-")[0] ?? "";
        if (isSupportedLanguage(base)) return base;
    }

    return null;
};

/**
 * 浏览器端兜底：读取 navigator.languages。
 * 只在客户端调用，middleware 用 pickLanguageFromAcceptLanguage。
 */
export const detectBrowserLanguage = (): SupportedLanguage | null => {
    if (typeof navigator === "undefined") return null;

    const candidates = navigator.languages?.length ? navigator.languages : [navigator.language];

    for (const tag of candidates) {
        const base = tag.toLowerCase().split("-")[0];
        if (isSupportedLanguage(base)) return base;
    }

    return null;
};
