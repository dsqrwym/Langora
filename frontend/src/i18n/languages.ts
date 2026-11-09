import {canUseWindow} from "@/lib/device.ts";

export const SUPPORTED_LANGUAGES = ["en", "es", "zh"] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];


export const DEFAULT_LANGUAGE: SupportedLanguage = "en";
export const LANGUAGE_LABELS: Record<SupportedLanguage, string> = {
    en: "English",
    es: "Español",
    zh: "中文",
};

export const LANGUAGE_STORAGE_KEY = "langora.lang";

export const isSupportedLanguage = (value: unknown): value is SupportedLanguage => {
    return typeof value === "string" && (SUPPORTED_LANGUAGES as readonly string[]).includes(value);
};

export const readStoredLanguage = (): SupportedLanguage | null => {
    if (!canUseWindow()) return null;
    try {
        const raw = window.localStorage.getItem(LANGUAGE_STORAGE_KEY);
        return isSupportedLanguage(raw) ? raw : null;
    } catch {
        return null;
    }
};

export const writeStoredLanguage = (language: SupportedLanguage): void => {
    if (!canUseWindow()) return;
    try {
        window.localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    } catch {
        // do nothing
    }
};

export const detectBrowserLanguage = (): SupportedLanguage | null => {
    if (!canUseWindow()) return null;

    const candidates = navigator.languages?.length ? navigator.languages : [navigator.language];

    for (const tag of candidates) {
        const base = tag.toLowerCase().split("-")[0];
        if (isSupportedLanguage(base)) return base;
    }

    return null;
}

