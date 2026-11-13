import type {Metadata, Viewport} from "next";
import {notFound} from "next/navigation";
import type {ReactNode} from "react";
import {ThemeInit} from "@/components/theme/theme-init";
import {ToasterHost} from "@/components/ui/toaster-host";
import {SUPPORTED_LANGUAGES, isSupportedLanguage} from "@/i18n/languages";
import {InlineScript} from "@/components/common/inline-script";
import {THEME_INIT_SCRIPT} from "@/lib/theme/theme-script";
import "../globals.css";

export function generateStaticParams() {
    return SUPPORTED_LANGUAGES.map((language) => ({lang: language}));
}

export const metadata: Metadata = {
    title: "Langora",
    description: "Langora school platform",
};

export const viewport: Viewport = {
    // 主题脚本会在深色模式下改写成 #252525（与 theme-store 的 THEME_COLOR 对齐）
    themeColor: "#ffffff",
};

type RootLayoutProps = {
    children: ReactNode;
    params: Promise<{lang: string}>;
};

/**
 * 根布局：语言只由 URL 的 [lang] 段决定。
 * 这里不读 cookie，否则整个路由树会退化成动态渲染。
 */
export default async function RootLayout({children, params}: RootLayoutProps) {
    const {lang} = await params;

    if (!isSupportedLanguage(lang)) {
        //  抛出 nextjs 404 然后会自动使用[...notFound]下的页面
        notFound();
    }

    return (
        <html lang={lang} suppressHydrationWarning>
        <head>
            <InlineScript id="theme-script" html={THEME_INIT_SCRIPT}/>
        </head>
            <body className="min-h-svh antialiased">
                <ThemeInit/>
                {children}
                <ToasterHost/>
            </body>
        </html>
    );
}
