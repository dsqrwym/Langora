import React from "react";
import {RequireAuth} from "@/features/auth/components/require-auth";
import {getDictionary, SupportedLanguage} from "@/i18n";

export default async function AdminProtectedLayout({children, params}: {
    children: React.ReactNode,
    params: Promise<{ lang: string }>
}) {
    const {lang} = await params;
    const dictionary = await getDictionary(lang as SupportedLanguage);

    return <RequireAuth loadingLabel={dictionary.common.loading}>{children}</RequireAuth>
}
