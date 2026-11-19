import type {ResolvedTheme, ThemePreference} from "@/lib/theme/theme-types";
import {useThemeStore} from "@/lib/theme/theme-store";

const useThemePreference = (): ThemePreference =>
    useThemeStore((state) => state.preference);
const useResolvedTheme = (): ResolvedTheme =>
    useThemeStore((state) => state.resolvedTheme);

export {useThemePreference, useResolvedTheme};
export type {ThemePreference};
