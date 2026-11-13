"use client";

import {canUseWindow} from "@/lib/device";

/**
 * Runs during HTML parsing on initial loads; renders inertly during hydration
 * so React does not try to execute or recreate the script.
 *
 * 首次加载时，服务端输出可执行脚本；服务端不执行它，浏览器解析 HTML 时才会执行。
 * hydration 时改为 text/plain，避免 React 再处理脚本；属性差异由 suppressHydrationWarning 接受。
 *
 */
export function InlineScript({id, html}: { id?: string; html: string }) {
    return (
        <script
            id={id}
            type={canUseWindow() ? "text/plain" : "text/javascript"}
            suppressHydrationWarning
            dangerouslySetInnerHTML={{__html: html}}
        />
    );
}
