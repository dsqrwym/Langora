import {canUseWindow} from "@/lib/device";
import {
    DEFAULT_LANGUAGE,
    detectBrowserLanguage,
    isSupportedLanguage,
    type SupportedLanguage,
} from "@/i18n/languages";
import {readPreferredLanguage} from "@/i18n/locale-preference";

const getLanguageFromPathname = (pathname: string): SupportedLanguage | null => {
    const first = pathname.split("/")[1] ?? "";
    return isSupportedLanguage(first) ? first : null;
};

export const stripLanguagePrefix = (pathname: string): string => {
    const lang = getLanguageFromPathname(pathname);
    if (!lang) return pathname;
    const rest = pathname.slice(lang.length + 1);
    return rest === "" ? "/" : rest;
};

export const buildLocalizedPath = (lang: SupportedLanguage, path: string): string => {
    const clean = path.startsWith("/") ? path : `/${path}`;
    return clean === "/" ? `/${lang}` : `/${lang}${clean}`;
};

export const replaceLanguagePrefix = (
    pathname: string,
    next: SupportedLanguage,
): string => {
    const segments = pathname.split("/").filter(Boolean);
    if (segments.length > 0 && isSupportedLanguage(segments[0])) {
        segments[0] = next;
    } else {
        segments.unshift(next);
    }
    return `/${segments.join("/")}`;
};

/**
 * 语言优先级：URL 前缀 > 设备偏好 cookie > 浏览器检测 > 默认。
 * 不传 pathname 时只在浏览器端读取 location.pathname。
 */
export const getLanguageOrDefault = (pathname?: string | null): SupportedLanguage => {
    const currentPath = pathname ?? (canUseWindow() ? window.location.pathname : null);
    return (
        (currentPath ? getLanguageFromPathname(currentPath) : null)
        ?? readPreferredLanguage()
        ?? detectBrowserLanguage()
        ?? DEFAULT_LANGUAGE
    );
};
