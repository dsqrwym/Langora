"use client"


import {usePathname} from "@/i18n/navigation";
import {NotFoundPage} from "@/components/pages/not-found-page";

/**
 * not-found.tsx 拿不到路由参数，这里从 pathname 反推语言与归属域。
 */
export const NotFoundBoundary = () => {
    // not-found 阶段 usePathname 可能返回 null
    const pathname = usePathname() ?? "/";
    const isAdmin = pathname === "/admin" || pathname.startsWith("/admin/");

    return <NotFoundPage homePath={isAdmin ? "/admin" : "/"}/>;
};
