import type {ResolvedTheme, ThemePreference, ThemeState} from "@/lib/theme/theme-types";
import {create} from "zustand";
import {canUseDom} from "@/lib/device";
import {DARK_QUERY, THEME_COLOR, THEME_STORAGE_KEY} from "@/lib/theme/theme-constants";

export {THEME_STORAGE_KEY};

export const getSystemTheme = (): ResolvedTheme => {
    return canUseDom() && window.matchMedia(DARK_QUERY).matches ? "dark" : "light";
};

export const resolveTheme = (preference: ThemePreference): ResolvedTheme => {
    return preference === "system" ? getSystemTheme() : preference;
};

export const applyTheme = (resolved: ResolvedTheme) => {
    if (!canUseDom()) return;

    const root = document.documentElement;
    // remove or add to change shadcn-ui class
    root.classList.toggle("dark", resolved === "dark");
    // change system theme
    root.style.colorScheme = resolved;

    const meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (meta) meta.content = THEME_COLOR[resolved];
}

export const readStoredTheme = (): ThemePreference => {
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

const initialTheme = readStoredTheme();

// 文件加载即生效，index.html 内联让其更早生效
applyTheme(resolveTheme(initialTheme));

export const useThemeStore = create<ThemeState>((set, get) => ({
    preference: initialTheme,
    resolvedTheme: resolveTheme(initialTheme),
    setPreference: (preference) => {
        const resolvedTheme = resolveTheme(preference);
        writeStoredTheme(preference);
        applyTheme(resolvedTheme);
        set({preference, resolvedTheme});
    },
    syncSystemTheme: () => {
        if (get().preference !== "system") return;
        const resolvedTheme = getSystemTheme();
        applyTheme(resolvedTheme);
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
        const resolvedTheme = resolveTheme(preference);
        applyTheme(resolvedTheme);
        useThemeStore.setState({preference, resolvedTheme});
    });
}
