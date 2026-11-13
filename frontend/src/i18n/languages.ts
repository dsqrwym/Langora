/**
 * 纯语言常量与纯函数。
 * 这里不允许出现 window / document / localStorage，middleware 与 RSC 都会直接引用本模块。
 */
export const SUPPORTED_LANGUAGES = ["en", "es", "zh"] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];

export const DEFAULT_LANGUAGE: SupportedLanguage = "en";

/** 设备语言偏好 cookie，middleware 通过它做根路径分流 */
export const LANGUAGE_COOKIE_NAME = "NEXT_LOCALE";

export const LANGUAGE_LABELS: Record<SupportedLanguage, string> = {
    en: "English",
    es: "Español",
    zh: "中文",
};

export const isSupportedLanguage = (value: unknown): value is SupportedLanguage => {
    return typeof value === "string" && (SUPPORTED_LANGUAGES as readonly string[]).includes(value);
};

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
