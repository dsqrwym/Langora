"use client"

import React, {useEffect} from "react";
import {useAuth} from "@/features/auth";
import {useParams, useRouter} from "next/navigation";
import {useSessionBootstrap} from "@/features/auth/session/auth-session";
import {buildLocalizedPath, getLanguageOrDefault, isSupportedLanguage} from "@/i18n";
import {FullScreenLoader} from "@/components/ui/full-screen-loader";

export function RequireAuth({children, loadingLabel}: { children: React.ReactNode, loadingLabel: string }) {
    const {status} = useAuth();
    const router = useRouter();
    const {lang} = useParams<{ lang: string }>();

    useSessionBootstrap();

    useEffect(() => {
        if (status !== "anonymous") return;

        const current = window.location.pathname + window.location.search;
        const loginUrl = buildLocalizedPath(
            isSupportedLanguage(lang) ? lang : getLanguageOrDefault(),
            "/admin/login"
        );

        router.replace(`${loginUrl}?callbackUrl=${encodeURIComponent(current)}`)
    }, [status, lang, router]);

    if (status !== "authenticated") return <><FullScreenLoader label={loadingLabel}/> {children}</>;
    return <>{children}</>;
}