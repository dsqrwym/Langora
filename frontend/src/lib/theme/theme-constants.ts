import type {ResolvedTheme} from "@/lib/theme/theme-types";

// 无副作用的常量：服务端（内联脚本生成）与客户端（theme-store）共用，避免两边 key / 颜色漂移
export const THEME_STORAGE_KEY = "langora.theme";
export const DARK_QUERY = "(prefers-color-scheme: dark)";
export const THEME_COLOR: Record<ResolvedTheme, string> = {
    light: "#ffffff",
    dark: "#252525", // 与 shadcn `.dark` 的 --background 对齐
};
