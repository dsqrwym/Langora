import {canUseWindow} from "@/lib/device";
import {LANGUAGE_COOKIE_NAME, type SupportedLanguage} from "@/i18n/routing";

const ONE_YEAR_SECONDS = 60 * 60 * 24 * 365;

/**
 * 只在用户主动切换语言时写入，浏览行为不触发。
 */
export const writePreferredLanguage = (language: SupportedLanguage): void => {
    if (!canUseWindow()) return;

    document.cookie = `${LANGUAGE_COOKIE_NAME}=${language}; path=/; max-age=${ONE_YEAR_SECONDS}; samesite=lax`;
};
