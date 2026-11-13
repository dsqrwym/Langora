import type {Dictionary} from "@/i18n/dictionaries/zh";
import type {SupportedLanguage} from "@/i18n/languages";

/**
 * 官方字典方案：每种语言一个动态 import，构建时各自成为独立 chunk，
 * 页面只下载当前语言的词典，不加载另外两种。
 */
const dictionaries: Record<SupportedLanguage, () => Promise<Dictionary>> = {
    en: () => import("@/i18n/dictionaries/en").then((module) => module.default),
    es: () => import("@/i18n/dictionaries/es").then((module) => module.default),
    zh: () => import("@/i18n/dictionaries/zh").then((module) => module.default),
};

export const getDictionary = (language: SupportedLanguage): Promise<Dictionary> =>
    dictionaries[language]();

export type {Dictionary};
