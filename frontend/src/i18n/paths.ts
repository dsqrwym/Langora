import {
    DEFAULT_LANGUAGE,
    detectBrowserLanguage,
    isSupportedLanguage,
    readStoredLanguage,
    type SupportedLanguage
} from "@/i18n/languages.ts";
import {canUseWindow} from "@/lib/device.ts";

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

export const getLanguageOrDefault = (
    pathname: string | null = canUseWindow() ? window.location.pathname : null
): SupportedLanguage => {
    return (
        (pathname ? getLanguageFromPathname(pathname) : null)
        ?? readStoredLanguage()
        ?? detectBrowserLanguage()
        ?? DEFAULT_LANGUAGE
    );
};