import type {ResolvedTheme} from "@/lib/theme/theme-types";

// 无副作用的常量：服务端（内联脚本生成）与客户端（theme-store）共用，避免两边 key / 颜色漂移
export const THEME_STORAGE_KEY = "langora.theme";
export const DARK_QUERY = "(prefers-color-scheme: dark)";
// 解决 服务端渲染导致 react 读取 theme 显示图标时，更改图标会先出现服务端渲染图标，随后变更为客户端渲染图标
// 使用 HTML 属性 然后 CSS 读取应用
export const THEME_PREFERENCE_ATTRIBUTE = "data-theme-preference";
export const THEME_COLOR: Record<ResolvedTheme, string> = {
    light: "#ffffff",
    dark: "#252525", // 与 shadcn `.dark` 的 --background 对齐
};
