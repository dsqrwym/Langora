"use client"

import React, {type PropsWithChildren, useEffect} from "react";
import {useAuth} from "@/features/auth";
import {useSessionBootstrap} from "@/features/auth/session/auth-session";
import {FullScreenLoader} from "@/components/ui/full-screen-loader";
import {usePathname, useRouter} from "@/i18n/navigation";
import {useTranslations} from "next-intl";

export function RequireAuth({children}: PropsWithChildren) {
    const {status} = useAuth();
    const router = useRouter();
    const pathname = usePathname();
    const t = useTranslations("common");

    useSessionBootstrap();

    useEffect(() => {
        if (status !== "anonymous") return;

        const current = pathname + window.location.search;
        router.replace(`/admin/login?callbackUrl=${encodeURIComponent(current)}`);
    }, [status, router, pathname]);

    if (status !== "authenticated") return <><FullScreenLoader label={t("loading")}/> {children}</>;
    return <>{children}</>;
}