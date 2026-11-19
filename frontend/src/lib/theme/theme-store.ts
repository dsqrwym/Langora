import type {ResolvedTheme, ThemePreference, ThemeState} from "@/lib/theme/theme-types";
import {create} from "zustand";
import {canUseDom} from "@/lib/device";
import {DARK_QUERY, THEME_COLOR, THEME_PREFERENCE_ATTRIBUTE, THEME_STORAGE_KEY} from "@/lib/theme/theme-constants";

export {THEME_STORAGE_KEY};

const getSystemTheme = (): ResolvedTheme => {
    return canUseDom() && window.matchMedia(DARK_QUERY).matches ? "dark" : "light";
};

const resolveTheme = (preference: ThemePreference): ResolvedTheme => {
    return preference === "system" ? getSystemTheme() : preference;
};

const applyTheme = (preference: ThemePreference) => {
    const resolved = resolveTheme(preference);
    if (!canUseDom()) return resolved;

    const root = document.documentElement;
    // remove or add to change shadcn-ui class
    root.classList.toggle("dark", resolved === "dark");
    // change system theme
    root.style.colorScheme = resolved;
    // change css variable
    root.setAttribute(THEME_PREFERENCE_ATTRIBUTE, preference);

    const meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (meta) meta.content = THEME_COLOR[resolved];

    return resolved;
}

const readStoredTheme = (): ThemePreference => {
    if (!canUseDom()) return "system";

    try {
        const raw = window.localStorage.getItem(THEME_STORAGE_KEY);
        return raw === "light" || raw === "dark" || raw === "system" ? raw : "system";
    } catch {
        return "system";
    }
};

const writeStoredTheme = (preference: ThemePreference): void => {
    if (!canUseDom()) return;
    try {
        window.localStorage.setItem(THEME_STORAGE_KEY, preference);
    } catch {
        // 隐私模式可能禁止 localStorage
    }
};

export const useThemeStore = create<ThemeState>((set, get) => ({
    // 初始值必须与 SSR 一致：服务端读不到 localStorage，只能用 system/light。
    preference: "system",
    resolvedTheme: "light",
    setPreference: (preference) => {
        const resolvedTheme = applyTheme(preference);
        writeStoredTheme(preference);
        set({preference, resolvedTheme});
    },
    // 浏览器显示由 内联脚本 + CSS 实现，真实偏好由 ThemeInit 在 hydration 后通过 hydrate() 写入，避免首帧不一致
    hydrate: () => {
        const preference = readStoredTheme();
        const resolvedTheme = applyTheme(preference);
        set({preference, resolvedTheme});
    },
    syncSystemTheme: () => {
        if (get().preference !== "system") return;
        const resolvedTheme = applyTheme("system");
        set({resolvedTheme});
    },
}));

if (canUseDom()) {
    // 监听主题变化
    window.matchMedia(DARK_QUERY).addEventListener("change", () => {
        useThemeStore.getState().syncSystemTheme();
    });
    // 跨标签同步 storage 事件只对其他标签页触发
    window.addEventListener("storage", (event) => {
        // key === null 表示其它标签页执行了 localStorage.clear()，此时偏好回落为 system
        if (event.key !== null && event.key !== THEME_STORAGE_KEY) return;
        const preference = readStoredTheme();
        const resolvedTheme = applyTheme(preference);
        useThemeStore.setState({preference, resolvedTheme});
    });
}
