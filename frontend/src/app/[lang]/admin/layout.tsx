import type {Metadata} from "next";
import {getDictionary, type SupportedLanguage} from "@/i18n";
import {type PropsWithChildren} from "react";

/**
 * Admin pages must stay out of search results, and the title template keeps tabs readable ("Articles · Admin").
 */
export async function generateMetadata({params}:LayoutProps<'/[lang]/admin'>): Promise<Metadata> {
    const {lang} = await params;
    const dictionary = await getDictionary(lang as SupportedLanguage);
    const sectionTitle = dictionary.admin.title;

    return {
        title: {
            default: sectionTitle,
            template: `%s · ${sectionTitle}`,
        },
        robots: {index: false, follow: false},
    };
}

export default function AdminLayout({children}: PropsWithChildren) {
    return <>{children}</>;
}