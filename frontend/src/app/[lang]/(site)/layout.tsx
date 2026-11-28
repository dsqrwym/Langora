import React from "react";
import SiteHeader from "@/components/site/header/site-header";
import {getCachedSiteShell} from "@/features/site/api/site-server";
import {getTranslations} from "next-intl/server";
import type {SupportedLanguage} from "@/i18n/routing";
import {Link} from "@/i18n/navigation";

export default async function SiteLayout(
    {
        children,
        params,
    }: LayoutProps<"/[lang]">
) {
    const {lang} = await params as { lang: SupportedLanguage };

    const [site, tSite] = await Promise.all([
        getCachedSiteShell(lang),
        getTranslations("site"),
    ]);
    return (
        <>
            <Link
                href="#main-content"
                className="sr-only focus:not-sr-only focus:fixed focus:inset-s-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-primary focus:px-3 focus:py-2 focus:text-sm focus:font-medium focus:text-primary-foreground"
            >
                {tSite("skipToContent")}
            </Link>
            <SiteHeader
                schoolName={site.schoolName}
                showHeaderSchoolName={site.showHeaderSchoolName}
                logo={site.logo}
            />
            <main id="main-content" className="flex-1">
                {children}
            </main>
        </>
    );
}
