import React from "react";
import SiteHeader from "@/components/site/site-header";
import {getDictionary, type SupportedLanguage} from "@/i18n";

export default async function SiteLayout({children, params}: LayoutProps<'/[lang]'>) {
    const {lang} = await params as { lang: SupportedLanguage };
    const dictionary = await getDictionary(lang as SupportedLanguage);

    return <>
        <SiteHeader lang={lang} dictionary={dictionary}/>
        {children}
    </>;
}
