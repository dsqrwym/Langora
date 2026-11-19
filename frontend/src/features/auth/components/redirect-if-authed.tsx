"use client"

import React, {type PropsWithChildren, useEffect} from "react";
import {useAuth} from "@/features/auth";
import {useParams, useRouter} from "next/navigation";
import {useSessionBootstrap} from "@/features/auth/session/auth-session";
import {buildLocalizedPath, getLanguageOrDefault, isSupportedLanguage} from "@/i18n";
import type {Route} from "next";

export function RedirectIfAuthed({children}: PropsWithChildren) {
    const {status} = useAuth();
    const router = useRouter();
    const {lang} = useParams<{ lang: string }>();

    useSessionBootstrap();

    useEffect(() => {
        if (status !== "authenticated") return;

        const callbackUrl = new URLSearchParams(window.location.search).get("callbackUrl");
        // 只接受站内绝对路径，挡掉 //evil.com 这类开放重定向
        const target = callbackUrl && callbackUrl.startsWith("/") && !callbackUrl.startsWith("//")
            ? callbackUrl
            : buildLocalizedPath(isSupportedLanguage(lang) ? lang : getLanguageOrDefault(), "/admin");

        router.replace(target as Route);

    }, [status, lang, router]);
    return <>{children}</>;
}
