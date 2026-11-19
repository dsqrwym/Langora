export type ThemePreference = "light" | "dark" | "system";
export type ResolvedTheme = "light" | "dark";

export type ThemeState = {
    preference: ThemePreference;
    resolvedTheme: ResolvedTheme;
    hydrate: () => void;
    setPreference: (preference: ThemePreference) => void;
    syncSystemTheme: () => void;
};
