import {DARK_QUERY, THEME_COLOR, THEME_PREFERENCE_ATTRIBUTE, THEME_STORAGE_KEY} from "@/lib/theme/theme-constants";
import {toInlineScriptWithArgs} from "@/lib/inline-script";

/**
 * 首帧前同步执行的主题脚本，逻辑必须与 theme-store 的 readStoredTheme / resolveTheme / applyTheme 一致：
 * 存储值只认 light | dark | system，其余一律按 system 处理
 * system 时跟随 prefers-color-scheme

 export const THEME_INIT_SCRIPT = `(function(){try{
 var p=localStorage.getItem(${JSON.stringify(THEME_STORAGE_KEY)});
 if(p!=="light"&&p!=="dark")p="system";
 var t=p==="system"?(window.matchMedia(${JSON.stringify(DARK_QUERY)}).matches?"dark":"light"):p;
 var r=document.documentElement;
 r.classList.toggle("dark",t==="dark");
 r.style.colorScheme=t;
 var m=document.querySelector('meta[name="theme-color"]');
 if(m)m.content=${JSON.stringify(THEME_COLOR)}[t];
 }catch(e){}})()`;
 */

function applyInitialTheme({storageKey, darkQuery, color, preferenceAttribute}: {
    storageKey: string,
    darkQuery: string,
    color: Record<string, string>,
    preferenceAttribute: string,
}) {
    try {
        const stored = localStorage.getItem(storageKey);
        const preference = stored === "light" || stored === "dark" ? stored : "system";

        const resolved = preference === "system" ? (window.matchMedia(darkQuery).matches ? "dark" : "light") : preference;

        const root = document.documentElement;
        root.classList.toggle("dark", resolved === "dark");
        root.style.colorScheme = resolved;
        root.setAttribute(preferenceAttribute, preference);

        const meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
        if (meta) meta.content = color[resolved] ?? meta.content;
    } catch {}
}

export const THEME_INIT_SCRIPT = toInlineScriptWithArgs(applyInitialTheme, {
    storageKey: THEME_STORAGE_KEY,
    darkQuery: DARK_QUERY,
    color: THEME_COLOR,
    preferenceAttribute: THEME_PREFERENCE_ATTRIBUTE,
});