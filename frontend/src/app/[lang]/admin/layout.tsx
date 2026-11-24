import type {Metadata} from "next";
import {type PropsWithChildren} from "react";
import {getTranslations} from "next-intl/server";

/**
 * Admin pages must stay out of search results, and the title template keeps tabs readable ("Articles · Admin").
 */
export async function generateMetadata(): Promise<Metadata> {
    const t = await getTranslations("admin");
    const sectionTitle = t("title");

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