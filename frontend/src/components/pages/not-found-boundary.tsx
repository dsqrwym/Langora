"use client"

import {usePathname} from "next/navigation";
import {NotFoundPage} from "@/components/pages/not-found-page";
import {buildLocalizedPath, getLanguageOrDefault, stripLanguagePrefix} from "@/i18n/paths";
import {NOT_FOUND_MESSAGES} from "@/i18n/not-found-messages";

/**
 * not-found.tsx 拿不到路由参数，这里从 pathname 反推语言与归属域。
 */
export const NotFoundBoundary = () => {
    // not-found 阶段 usePathname 可能返回 null
    const currentPathname = usePathname() ?? "/";
    const language = getLanguageOrDefault(currentPathname);
    const rest = stripLanguagePrefix(currentPathname);
    const isAdmin = rest === "/admin" || rest.startsWith("/admin/");

    return <NotFoundPage
        homePath={buildLocalizedPath(language, isAdmin ? "/admin" : "/")}
        messages={NOT_FOUND_MESSAGES[language]}
    />;
};
