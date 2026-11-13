import type {ResolvedTheme, ThemePreference} from "@/lib/theme/theme-types";
import {useThemeStore} from "@/lib/theme/theme-store";

const useThemePreference = (): ThemePreference =>
    useThemeStore((state) => state.preference);
const useResolvedTheme = (): ResolvedTheme =>
    useThemeStore((state) => state.resolvedTheme);
const setThemePreference = (preference: ThemePreference): void => {
    useThemeStore.getState().setPreference(preference);
};
export {useThemePreference, setThemePreference, useResolvedTheme };
export type {ThemePreference};
