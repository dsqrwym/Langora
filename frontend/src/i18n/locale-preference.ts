import {
    LANGUAGE_COOKIE_NAME,
    isSupportedLanguage,
    type SupportedLanguage,
} from "@/i18n/languages";
import {canUseWindow} from "@/lib/device";

const ONE_YEAR_SECONDS = 60 * 60 * 24 * 365;

/**
 * 读取设备语言偏好。
 * 用 cookie 而不是 localStorage：middleware 读不到 localStorage。
 */
export const readPreferredLanguage = (): SupportedLanguage | null => {
    if (!canUseWindow()) return null;

    try {
        const match = document.cookie
            .split(";")
            .map((item) => item.trim())
            .find((item) => item.startsWith(`${LANGUAGE_COOKIE_NAME}=`));

        if (!match) return null;

        const value = decodeURIComponent(match.slice(LANGUAGE_COOKIE_NAME.length + 1));
        return isSupportedLanguage(value) ? value : null;
    } catch {
        return null;
    }
};

/**
 * 只在用户主动切换语言时写入，浏览行为不触发。
 */
export const writePreferredLanguage = (language: SupportedLanguage): void => {
    if (!canUseWindow()) return;

    document.cookie = `${LANGUAGE_COOKIE_NAME}=${language}; path=/; max-age=${ONE_YEAR_SECONDS}; samesite=lax`;
};
