"use client"

import React, {type PropsWithChildren, useEffect} from "react";
import {useAuth} from "@/features/auth";
import {useSessionBootstrap} from "@/features/auth/session/auth-session";
import {useRouter} from "@/i18n/navigation";
import {FullScreenLoader} from "@/components/ui/full-screen-loader";
import {useTranslations} from "next-intl";
import {stripLeadingLocale} from "@/i18n/routing";

export function RedirectIfAuthed({children}: PropsWithChildren) {
    const {status} = useAuth();
    const router = useRouter();
    const t = useTranslations("common");

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
    if (status !== "authenticated") return <><FullScreenLoader label={t("loading")}/> {children}</>;
    return <>{children}</>;
}
