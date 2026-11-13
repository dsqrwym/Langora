"use client";

import {useLayoutEffect} from "react";
import {applyTheme, readStoredTheme, resolveTheme} from "@/lib/theme/theme-store";

export const ThemeInit = () => {
    // useLayoutEffect 在浏览器绘制帧之前同步执行，避免闪烁

    useLayoutEffect(() => {
        const preference = readStoredTheme();
        const resolved = resolveTheme(preference);
        applyTheme(resolved);
    }, []);

    return null;
};
