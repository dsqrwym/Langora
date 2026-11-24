"use client"

import React, {type PropsWithChildren, useEffect} from "react";
import {useAuth} from "@/features/auth";
import {useSessionBootstrap} from "@/features/auth/session/auth-session";
import {useRouter} from "@/i18n/navigation";
import {hasLocale} from "next-intl";
import {routing} from "@/i18n/routing";

const stripLeadingLocale = (path: string): string => {
    const match = /^\/([^/?#]+)(.*)$/.exec(path);
    if (!match) return path;

    const [, firstSegment, rest] = match;
    if (!hasLocale(routing.locales, firstSegment)) return path;

    return rest.startsWith("/") ? rest : `/${rest}`;
};

export function RedirectIfAuthed({children}: PropsWithChildren) {
    const {status} = useAuth();
    const router = useRouter();

    useSessionBootstrap();

    useEffect(() => {
        if (status !== "authenticated") return;

        const callbackUrl = new URLSearchParams(window.location.search).get("callbackUrl");
        // 只接受站内绝对路径，挡掉 //evil.com 这类开放重定向
        const target = callbackUrl && callbackUrl.startsWith("/") && !callbackUrl.startsWith("//")
            ? stripLeadingLocale(callbackUrl)
            : "/admin";

        router.replace(target);

    }, [status, router]);
    return <>{children}</>;
}
